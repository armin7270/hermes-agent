package com.armin7270.snispoof.vpn

import android.os.ParcelFileDescriptor
import com.armin7270.snispoof.core.engine.PacketEngine
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Owns the TUN file descriptor: reads raw IP packets from the device and
 * forwards them to the [PacketEngine]; outbound engine packets are written
 * back. Bound-checked on every read (short/truncated reads are dropped).
 */
class TunPacketHandler(
    pfd: ParcelFileDescriptor,
    private val engine: PacketEngine,
) : java.io.Closeable {

    private val input = FileInputStream(pfd.fileDescriptor)
    private val output = FileOutputStream(pfd.fileDescriptor)
    private val closed = AtomicBoolean(false)

    /**
     * Blocking read loop — run on a dedicated dispatcher thread.
     * Returns when the tunnel closes or [close] is called.
     */
    fun runLoop(): Unit = try {
        val buf = ByteBuffer.allocateDirect(MAX_READ)
        val arr = ByteArray(MAX_READ)
        while (!closed.get()) {
            buf.clear()
            val len = input.channel.read(buf)
            if (len <= 0) break
            buf.flip()
            buf.get(arr, 0, len)
            engine.onPacket(arr, 0, len)
        }
    } finally {
        close()
    }

    fun write(packet: ByteArray) {
        if (closed.get()) return
        synchronized(output) {
            try {
                output.write(packet)
                output.flush()
            } catch (_: Exception) { }
        }
    }

    override fun close() {
        if (closed.getAndSet(true)) return
        runCatching { input.close() }
        runCatching { output.close() }
    }

    companion object {
        private const val MAX_READ = 65535
    }
}
