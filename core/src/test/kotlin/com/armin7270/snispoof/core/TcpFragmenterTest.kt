package com.armin7270.snispoof.core

import com.armin7270.snispoof.core.desync.DesyncMethod
import com.armin7270.snispoof.core.desync.DesyncParams
import com.armin7270.snispoof.core.desync.TcpFragmenter
import com.armin7270.snispoof.core.tls.ClientHelloForge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TcpFragmenterTest {

    @Test
    fun `split_n covers payload exactly once`() {
        val params = DesyncParams(method = DesyncMethod.SPLIT_N, splitN = 5, delayMs = 0)
        val chunks = TcpFragmenter.plan(DesyncMethod.SPLIT_N, params, 100)
        assertEquals(2, chunks.size)
        assertEquals(0, chunks[0].offset)
        assertEquals(5, chunks[0].length)
        assertEquals(5, chunks[1].offset)
        assertEquals(95, chunks[1].length)
        assertEquals(100, chunks.sumOf { it.length })
    }

    @Test
    fun `multi frag covers payload and respects count`() {
        val params = DesyncParams(method = DesyncMethod.MULTI_FRAG, fragmentCount = 7)
        val chunks = TcpFragmenter.plan(DesyncMethod.MULTI_FRAG, params, 517)
        assertTrue(chunks.size in 7..8)
        assertEquals(517, chunks.sumOf { it.length })
        // contiguous
        var off = 0
        for (c in chunks) {
            assertEquals(off, c.offset)
            off += c.length
        }
    }

    @Test
    fun `split_sni cuts inside the hostname`() {
        val host = "www.speedtest.net"
        val payload = ClientHelloForge.build(host)
        val sni = com.armin7270.snispoof.core.tls.TlsParser.parseSni(payload)!!
        val params = DesyncParams(method = DesyncMethod.SPLIT_SNI, splitAt = 2)
        val chunks = TcpFragmenter.planForPayload(DesyncMethod.SPLIT_SNI, params, payload)
        assertEquals(2, chunks.size)
        val cut = chunks[0].length
        assertTrue("cut should be inside the SNI host", cut in sni.hostOffset..sni.hostEnd)
        assertEquals(payload.size, chunks.sumOf { it.length })
    }

    @Test
    fun `off and replace produce single chunk`() {
        assertEquals(1, TcpFragmenter.plan(DesyncMethod.OFF, DesyncParams(), 42).size)
        assertEquals(1, TcpFragmenter.plan(DesyncMethod.SNI_REPLACE, DesyncParams(), 42).size)
    }
}
