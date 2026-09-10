package com.armin7270.snispoof.core

import com.armin7270.snispoof.core.packet.Checksum
import com.armin7270.snispoof.core.packet.Ip4
import com.armin7270.snispoof.core.packet.IpPacket
import com.armin7270.snispoof.core.packet.PacketBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PacketTest {

    @Test
    fun `tcp packet survives a build-parse roundtrip`() {
        val payload = "hello tunnel".encodeToByteArray()
        val pkt = PacketBuilder.tcp(
            srcIp = Ip4.parse("198.18.0.1"), dstIp = Ip4.parse("1.2.3.4"),
            srcPort = 443, dstPort = 50000,
            seq = 1000, ack = 2000, flags = 0x10, window = 65535,
            payload = payload, psh = true, mssOpt = 0, ipId = 7,
        )
        assertTrue(IpPacket.valid(pkt, 0, pkt.size))
        val p = IpPacket(pkt, 0, pkt.size)
        assertTrue(p.isTcp)
        assertEquals(443, p.srcPort)
        assertEquals(50000, p.dstPort)
        assertEquals(1000L, p.tcpSeq)
        assertEquals(2000L, p.tcpAck)
        assertEquals(payload.size, p.tcpPayloadLength)
    }

    @Test
    fun `ip4 helpers`() {
        assertEquals("10.0.0.255", Ip4.toString(Ip4.parse("10.0.0.255")))
        assertTrue(Ip4.inCidr(Ip4.parse("104.16.1.1"), Ip4.parse("104.16.0.0"), 13))
        assertFalse(Ip4.inCidr(Ip4.parse("8.8.8.8"), Ip4.parse("104.16.0.0"), 13))
    }

    @Test
    fun `valid rejects truncated buffers`() {
        val pkt = PacketBuilder.tcp(0, 0, 1, 2, 0, 0, 0x10, 65535, null, psh = false)
        assertFalse(IpPacket.valid(pkt, 0, 10))
        assertFalse(IpPacket.valid(pkt, 0, 0))
        assertTrue(IpPacket.valid(pkt, 0, pkt.size))
    }

    @Test
    fun `udp packet roundtrip`() {
        val payload = ByteArray(40) { it.toByte() }
        val pkt = PacketBuilder.udp(
            srcIp = Ip4.parse("198.18.0.2"), dstIp = Ip4.parse("8.8.8.8"),
            srcPort = 12345, dstPort = 53, payload = payload,
        )
        val p = IpPacket(pkt, 0, pkt.size)
        assertTrue(p.isUdp)
        assertEquals(53, p.dstPort)
        assertEquals(40, p.udpPayloadLength)
    }
}
