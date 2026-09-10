package com.armin7270.snispoof.core.scanner

import com.armin7270.snispoof.core.desync.DesyncMethod
import com.armin7270.snispoof.core.desync.DesyncParams
import com.armin7270.snispoof.core.engine.SocketProtector
import com.armin7270.snispoof.core.packet.Ip4
import com.armin7270.snispoof.core.proxy.Tls13Client
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

/**
 * The UAC-style scanners: clean Cloudflare edge discovery and fake-SNI probing.
 * Both measure against the real network using our own TLS engine so results
 * match what the tunnel will actually experience.
 */
object Scanner {

    data class IpResult(val ip: String, val latencyMs: Long, val tlsOk: Boolean)
    data class SniResult(val sni: String, val ok: Boolean, val latencyMs: Long)

    /** Well-known fake SNIs probed by the scanner. */
    val FAKE_SNIS = listOf(
        "auth.vercel.com",
        "www.speedtest.net",
        "mci.ir",
        "www.google.com",
        "www.cloudflare.com",
        "cdn.jsdelivr.net",
        "www.wikipedia.org",
        "yahoo.com",
        "www.bing.com",
        "api.telegram.org",
    )

    /** Cloudflare IPv4 edges to probe (published ranges + known anycast IPs). */
    val CLOUDFLARE_EDGES = listOf(
        "188.114.96.0", "188.114.96.1", "188.114.97.0", "188.114.97.1",
        "188.114.98.0", "188.114.98.1", "188.114.99.0", "188.114.99.1",
        "104.16.0.0", "104.16.0.1", "104.16.132.229", "104.17.0.0",
        "104.18.0.0", "104.18.32.7", "104.19.0.0", "104.20.0.0",
        "104.21.0.0", "104.22.0.0", "172.64.0.0", "172.64.149.0",
        "172.65.0.0", "172.66.0.0", "172.67.0.0", "162.158.0.0",
        "162.159.0.0", "108.162.192.0", "141.101.64.0", "190.93.240.0",
        "198.41.128.0", "131.0.72.0", "103.21.244.0", "103.22.200.0",
    )

    /**
     * Probes [ips]: TCP connect to :443 and (optionally) a TLS 1.3 handshake
     * with the given SNI. Returns results sorted by latency, clean IPs first.
     */
    suspend fun scanIps(
        ips: List<String>,
        protector: SocketProtector?,
        sni: String = "www.speedtest.net",
        timeoutMs: Int = 4000,
        tlsCheck: Boolean = true,
        concurrency: Int = 8,
    ): List<IpResult> = withContext(Dispatchers.IO) {
        coroutineScope {
            ips.chunked(concurrency).flatMap { batch ->
                batch.map { ip ->
                    async {
                        probeIp(ip, protector, sni, timeoutMs, tlsCheck)
                    }
                }.awaitAll()
            }
        }.filter { it.latencyMs >= 0 }.sortedBy { it.latencyMs }
    }

    private fun probeIp(
        ip: String,
        protector: SocketProtector?,
        sni: String,
        timeoutMs: Int,
        tlsCheck: Boolean,
    ): IpResult {
        val start = System.currentTimeMillis()
        val socket = Socket()
        protector?.protectSocket(socket)
        socket.tcpNoDelay = true
        try {
            val addr = InetAddress.getByAddress(Ip4.bytes(Ip4.parse(ip)))
            socket.connect(InetSocketAddress(addr, 443), timeoutMs)
            val connectMs = System.currentTimeMillis() - start
            if (!tlsCheck) {
                return IpResult(ip, connectMs, false)
            }
            socket.soTimeout = timeoutMs
            Tls13Client.handshake(socket.getInputStream(), socket.getOutputStream(), sni)
            return IpResult(ip, System.currentTimeMillis() - start, true)
        } catch (e: Exception) {
            return IpResult(ip, -1, false)
        } finally {
            runCatching { socket.close() }
        }
    }

    /**
     * Probes candidate fake SNIs against [ip] with a full TLS 1.3 handshake:
     * a working fake SNI completes the handshake on the chosen edge.
     */
    suspend fun scanSnis(
        snis: List<String>,
        ip: String,
        protector: SocketProtector?,
        timeoutMs: Int = 5000,
        concurrency: Int = 5,
    ): List<SniResult> = withContext(Dispatchers.IO) {
        coroutineScope {
            snis.chunked(concurrency).flatMap { batch ->
                batch.map { sni ->
                    async {
                        val start = System.currentTimeMillis()
                        val socket = Socket()
                        protector?.protectSocket(socket)
                        socket.tcpNoDelay = true
                        try {
                            val addr = InetAddress.getByAddress(Ip4.bytes(Ip4.parse(ip)))
                            socket.connect(InetSocketAddress(addr, 443), timeoutMs)
                            socket.soTimeout = timeoutMs
                            Tls13Client.handshake(socket.getInputStream(), socket.getOutputStream(), sni)
                            SniResult(sni, true, System.currentTimeMillis() - start)
                        } catch (e: Exception) {
                            SniResult(sni, false, -1)
                        } finally {
                            runCatching { socket.close() }
                        }
                    }
                }.awaitAll()
            }
        }.sortedWith(compareBy({ !it.ok }, { if (it.latencyMs < 0) Long.MAX_VALUE else it.latencyMs }))
    }

    /** Default desync params used while scanning (split inside the SNI). */
    val scanDesync = DesyncParams(method = DesyncMethod.SPLIT_SNI, splitAt = 2, delayMs = 0)
}
