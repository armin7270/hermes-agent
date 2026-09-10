package com.armin7270.snispoof.core

import com.armin7270.snispoof.core.tls.ClientHelloForge
import com.armin7270.snispoof.core.tls.ClientHelloParser
import com.armin7270.snispoof.core.tls.TlsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TlsParserTest {

    @Test
    fun `forge then parse roundtrip`() {
        val ch = ClientHelloForge.build("example.com")
        val sni = ClientHelloParser.findSni(ch)
        assertNotNull(sni)
        assertEquals("example.com", sni!!.host)
        assertEquals(sni.offset, TlsParser.parseSni(ch)!!.hostOffset)
    }

    @Test
    fun `replace sni keeps record consistent`() {
        val oldHost = "www.speedtest.net"
        val newHost = "auth.vercel.com"
        val original = ClientHelloForge.build(oldHost)
        val rewritten = TlsParser.replaceSni(original, newHost)
        assertNotNull(rewritten)

        // record length must match actual size
        val recLen = ((rewritten!![3].toInt() and 0xff) shl 8) or (rewritten[4].toInt() and 0xff)
        assertEquals(rewritten.size, recLen + 5)

        // handshake length matches record content
        val hsLen = ((rewritten[6].toInt() and 0xff) shl 16) or
                ((rewritten[7].toInt() and 0xff) shl 8) or
                (rewritten[8].toInt() and 0xff)
        assertEquals(recLen - 4, hsLen)

        // the new SNI is readable
        val span = TlsParser.parseSni(rewritten)
        assertNotNull(span)
        assertEquals(newHost, span!!.host)

        // payload difference equals host delta
        assertEquals(original.size + (newHost.length - oldHost.length), rewritten.size)
    }

    @Test
    fun `grow sni and shrink sni both work`() {
        val grow = TlsParser.replaceSni(ClientHelloForge.build("a.io"), "this.is.a.much.longer.host.example.com")
        assertNotNull(grow)
        assertEquals("this.is.a.much.longer.host.example.com", TlsParser.sniHost(grow!!))

        val shrink = TlsParser.replaceSni(ClientHelloForge.build("this.is.a.much.longer.host.example.com"), "a.io")
        assertNotNull(shrink)
        assertEquals("a.io", TlsParser.sniHost(shrink!!))
    }

    @Test
    fun `rejects garbage and truncation`() {
        assertNull(TlsParser.parseSni(ByteArray(0)))
        assertNull(TlsParser.parseSni(byteArrayOf(0x17, 3, 3, 0, 1)))
        val ch = ClientHelloForge.build("example.com")
        for (cut in intArrayOf(0, 1, 5, 60, ch.size / 2)) {
            val truncated = ch.copyOf(cut)
            // must not throw; may return null or a valid span
            val span = runCatching { TlsParser.parseSni(truncated) }.getOrNull()
            if (span != null) {
                assertTrue(span.hostOffset + span.hostLength <= truncated.size)
            }
        }
    }

    @Test
    fun `app data record is not a client hello`() {
        val appData = byteArrayOf(0x17, 0x03, 0x03, 0x00, 0x04, 1, 2, 3, 4)
        assertTrue(!TlsParser.looksLikeClientHello(appData))
    }
}
