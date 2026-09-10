package com.armin7270.snispoof.core.util

import java.io.Closeable
import java.io.IOException

/** Simple in-memory ring buffer of log lines, thread-safe. */
class LogRing(private val capacity: Int = 2000) {
    private val lock = Any()
    private val lines = ArrayDeque<String>(capacity)

    fun log(line: String) {
        val stamped = "%1\$TH:%1\$TM:%1\$TS.%1\$TL ${line}"
        synchronized(lock) {
            if (lines.size >= capacity) lines.removeFirst()
            lines.addLast(stamped)
        }
    }

    fun snapshot(): List<String> = synchronized(lock) { lines.toList() }

    fun clear() = synchronized(lock) { lines.clear() }
}

object Io {
    fun closeQuietly(c: Closeable?) {
        try { c?.close() } catch (_: IOException) { } catch (_: Throwable) { }
    }
}

object Hex {
    fun encode(bytes: ByteArray, limit: Int = bytes.size): String {
        val n = minOf(limit, bytes.size)
        val sb = StringBuilder(n * 2)
        for (i in 0 until n) {
            val v = bytes[i].toInt() and 0xff
            sb.append("0123456789abcdef"[v ushr 4]).append("0123456789abcdef"[v and 0xf])
        }
        return sb.toString()
    }

    fun decode(s: String): ByteArray {
        val clean = s.replace(" ", "").replace("\n", "").replace(":", "")
        require(clean.length % 2 == 0) { "bad hex length" }
        val out = ByteArray(clean.length / 2)
        for (i in out.indices) {
            out[i] = ((Character.digit(clean[i * 2], 16) shl 4)
                    or Character.digit(clean[i * 2 + 1], 16)).toByte()
        }
        return out
    }
}
