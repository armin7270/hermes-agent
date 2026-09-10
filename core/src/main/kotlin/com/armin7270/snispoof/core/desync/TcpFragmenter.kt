package com.armin7270.snispoof.core.desync

import com.armin7270.snispoof.core.tls.TlsParser

/**
 * Pure segmentation planner: turns one payload into a list of wire chunks
 * (offset, length, delay-after) that [NoRootDesync] then writes to the socket
 * with TCP_NODELAY so each chunk leaves the device as its own TCP segment.
 */
object TcpFragmenter {

    class Chunk(val offset: Int, val length: Int, val delayAfterMs: Int)

    /**
     * Splits [payloadLen] bytes according to [method]/[params].
     * Always returns at least one chunk that covers the whole payload.
     */
    fun plan(
        method: DesyncMethod,
        params: DesyncParams,
        payloadLen: Int,
    ): List<Chunk> {
        require(payloadLen > 0) { "empty payload" }
        return when (method) {
            DesyncMethod.OFF, DesyncMethod.FAKE_RST_REBIND, DesyncMethod.WRONG_SEQ, DesyncMethod.SNI_REPLACE ->
                listOf(Chunk(0, payloadLen, 0))

            DesyncMethod.SPLIT_N -> {
                val n = params.splitN.coerceIn(1, maxOf(1, payloadLen - 1))
                twoChunks(0, n, payloadLen, params.delayMs)
            }

            DesyncMethod.SPLIT_SNI, DesyncMethod.COMBINED -> {
                // split point arrives from the caller via splitAt; plan a 2-way
                // split at the provided byte index when available
                val at = params.splitAt.coerceIn(1, maxOf(1, payloadLen - 1))
                twoChunks(0, at, payloadLen, params.delayMs)
            }

            DesyncMethod.MULTI_FRAG -> {
                val parts = params.fragmentCount.coerceIn(2, 64)
                val chunk = (payloadLen + parts - 1) / parts
                buildList {
                    var off = 0
                    while (off < payloadLen) {
                        val n = minOf(chunk, payloadLen - off)
                        add(Chunk(off, n, if (off + n < payloadLen) 1 else 0))
                        off += n
                    }
                }
            }

            DesyncMethod.DELAYED -> {
                val n = params.splitN.coerceIn(1, maxOf(1, payloadLen - 1))
                twoChunks(0, n, payloadLen, params.delayMs)
            }
        }
    }

    /**
     * Plan for the actual payload: uses the real SNI position (split a few
     * bytes into the hostname, the classic "wrong_seq family" cut point) when
     * the payload is a ClientHello with SNI, otherwise falls back to [method]'s
     * generic plan.
     */
    fun planForPayload(method: DesyncMethod, params: DesyncParams, payload: ByteArray): List<Chunk> {
        if (method == DesyncMethod.SPLIT_SNI || method == DesyncMethod.COMBINED) {
            val span = TlsParser.parseSni(payload)
            if (span != null) {
                val into = params.splitAt.coerceIn(0, span.hostLength - 1)
                val cut = (span.hostOffset + into).coerceIn(1, payload.size - 1)
                return twoChunks(0, cut, payload.size, params.delayMs)
            }
        }
        return plan(method, params, payload.size)
    }

    private fun twoChunks(from: Int, cut: Int, total: Int, delayMs: Int): List<Chunk> =
        listOf(
            Chunk(from, cut - from, delayMs.coerceIn(0, 500)),
            Chunk(cut, total - cut, 0),
        )
}
