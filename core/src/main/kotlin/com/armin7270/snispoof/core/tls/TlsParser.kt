package com.armin7270.snispoof.core.tls

/**
 * Deep TLS record / ClientHello parser used by the DPI-evasion engine.
 *
 * Beyond locating the SNI host bytes (like [ClientHelloParser]) it also
 * reports the absolute offsets of every enclosing length field, so the engine
 * can rewrite the SNI in place (fake-SNI substitution) while keeping the
 * record, handshake, extension and list lengths consistent on the wire.
 */
object TlsParser {

    /** Byte positions of the length fields that must change when SNI is rewritten. */
    class SniSpan(
        /** absolute offset of the host bytes */
        val hostOffset: Int,
        /** length of the host bytes */
        val hostLength: Int,
        val host: String,
        /** absolute offset of the 2-byte server_name entry length */
        val entryLenOffset: Int,
        /** absolute offset of the 2-byte server_name_list length */
        val listLenOffset: Int,
        /** absolute offset of the 2-byte SNI extension length */
        val extLenOffset: Int,
        /** absolute offset of the 2-byte extensions total length */
        val extTotalLenOffset: Int,
        /** absolute offset of the 3-byte handshake body length */
        val handshakeLenOffset: Int,
        /** absolute offset of the 2-byte TLS record length */
        val recordLenOffset: Int,
        /** absolute end of the record */
        val recordEnd: Int,
    ) {
        val hostEnd: Int get() = hostOffset + hostLength
    }

    /** Quick check: does the buffer start with a TLS handshake record? */
    fun looksLikeTls(data: ByteArray, off: Int = 0, len: Int = data.size): Boolean {
        if (len < 6 || off < 0 || off + 6 > data.size) return false
        if (data[off] != 0x16.toByte()) return false
        val major = data[off + 1].toInt() and 0xff
        val minor = data[off + 2].toInt() and 0xff
        return major == 0x03 && minor in 0x00..0x04
    }

    /** Quick check for a ClientHello handshake inside a TLS record. */
    fun looksLikeClientHello(data: ByteArray, off: Int = 0, len: Int = data.size): Boolean =
        looksLikeTls(data, off, len) && data[off + 5] == 0x01.toByte()

    /**
     * Full ClientHello SNI scan with bounds checking everywhere.
     * Returns null for anything that is not a well-formed ClientHello with SNI.
     */
    fun parseSni(data: ByteArray, off: Int = 0, len: Int = data.size): SniSpan? {
        if (!looksLikeClientHello(data, off, len)) return null
        val end = off + len
        val recordLen = u16(data, off + 3)
        val recordEnd = minOf(end, off + 5 + recordLen)
        var p = off + 5
        val hsBodyLen = u24(data, p + 1)
        val hsEnd = minOf(recordEnd, p + 4 + hsBodyLen)
        p += 4 // handshake header
        if (p + 2 > hsEnd) return null
        p += 2 // client version
        if (p + 32 > hsEnd) return null
        p += 32 // random
        if (p >= hsEnd) return null
        val sidLen = data[p].toInt() and 0xff
        p += 1 + sidLen
        if (p + 2 > hsEnd) return null
        val cipherLen = u16(data, p)
        p += 2 + cipherLen
        if (p >= hsEnd) return null
        val compLen = data[p].toInt() and 0xff
        p += 1 + compLen
        if (p + 2 > hsEnd) return null
        val extTotalOff = p
        val extTotal = u16(data, p)
        p += 2
        val extEnd = minOf(hsEnd, p + extTotal)
        while (p + 4 <= extEnd) {
            val type = u16(data, p)
            val extLen = u16(data, p + 2)
            val extLenOff = p + 2
            val body = p + 4
            if (body + extLen > extEnd) return null
            if (type == 0x0000 && extLen >= 5) {
                val listLenOff = body
                val listLen = u16(data, body)
                var q = body + 2
                val listEnd = minOf(body + extLen, body + 2 + listLen)
                while (q + 3 <= listEnd) {
                    val nameType = data[q].toInt() and 0xff
                    val nameLen = u16(data, q + 1)
                    val nameOff = q + 3
                    if (nameType == 0 && nameLen in 1..(listEnd - nameOff)) {
                        return SniSpan(
                            hostOffset = nameOff,
                            hostLength = nameLen,
                            host = String(data, nameOff, nameLen, Charsets.US_ASCII),
                            entryLenOffset = q + 1,
                            listLenOffset = listLenOff,
                            extLenOffset = extLenOff,
                            extTotalLenOffset = extTotalOff,
                            handshakeLenOffset = off + 6,
                            recordLenOffset = off + 3,
                            recordEnd = recordEnd,
                        )
                    }
                    q += 3 + nameLen
                }
                return null
            }
            p = body + extLen
        }
        return null
    }

    /**
     * Fake-SNI substitution: rewrite the SNI host bytes of a ClientHello with
     * [newHost] and fix every enclosing length field. Returns the new payload
     * or null when the input is not a rewritable ClientHello (bad format or
     * the new host does not fit 255 bytes).
     */
    fun replaceSni(payload: ByteArray, newHost: String): ByteArray? {
        val span = parseSni(payload) ?: return null
        val hostBytes = newHost.encodeToByteArray()
        if (hostBytes.isEmpty() || hostBytes.size > 255) return null
        val delta = hostBytes.size - span.hostLength
        val out = ByteArray(payload.size + delta)
        System.arraycopy(payload, 0, out, 0, span.hostOffset)
        System.arraycopy(hostBytes, 0, out, span.hostOffset, hostBytes.size)
        System.arraycopy(payload, span.hostEnd, out, span.hostEnd + delta, payload.size - span.hostEnd)
        add16(out, span.entryLenOffset, delta)
        add16(out, span.listLenOffset, delta)
        add16(out, span.extLenOffset, delta)
        add16(out, span.extTotalLenOffset, delta)
        add24(out, span.handshakeLenOffset, delta)
        add16(out, span.recordLenOffset, delta)
        return out
    }

    /** Extracts the SNI hostname of a ClientHello payload, or null. */
    fun sniHost(payload: ByteArray): String? = parseSni(payload)?.host

    private fun add16(b: ByteArray, off: Int, delta: Int) {
        val v = (u16(b, off) + delta) and 0xffff
        b[off] = (v ushr 8).toByte()
        b[off + 1] = v.toByte()
    }

    private fun add24(b: ByteArray, off: Int, delta: Int) {
        val v = (u24(b, off) + delta).coerceIn(0, 0xffffff)
        b[off] = ((v ushr 16) and 0xff).toByte()
        b[off + 1] = ((v ushr 8) and 0xff).toByte()
        b[off + 2] = v.toByte()
    }

    private fun u16(b: ByteArray, i: Int): Int =
        ((b[i].toInt() and 0xff) shl 8) or (b[i + 1].toInt() and 0xff)

    private fun u24(b: ByteArray, i: Int): Int =
        ((b[i].toInt() and 0xff) shl 16) or ((b[i + 1].toInt() and 0xff) shl 8) or
                (b[i + 2].toInt() and 0xff)
}
