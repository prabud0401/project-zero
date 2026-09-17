package app.projectzero.localai

import java.security.SecureRandom
import java.util.Random as JavaRandom
import kotlin.random.Random

object UuidV7 {
    private val secureRandom = SecureRandom()

    fun generate(timestampMs: Long = System.currentTimeMillis(), random: JavaRandom = secureRandom): String {
        require(timestampMs >= 0L) { "timestampMs must be >= 0" }
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        bytes[0] = (timestampMs ushr 40).toByte()
        bytes[1] = (timestampMs ushr 32).toByte()
        bytes[2] = (timestampMs ushr 24).toByte()
        bytes[3] = (timestampMs ushr 16).toByte()
        bytes[4] = (timestampMs ushr 8).toByte()
        bytes[5] = timestampMs.toByte()
        bytes[6] = ((bytes[6].toInt() and 0x0F) or 0x70).toByte()
        bytes[8] = ((bytes[8].toInt() and 0x3F) or 0x80).toByte()
        return format(bytes)
    }

    fun generate(timestampMs: Long, random: Random): String =
        generate(timestampMs, JavaRandom(random.nextLong()))

    private fun format(bytes: ByteArray): String {
        val hex = CharArray(32)
        val alphabet = "0123456789abcdef"
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            hex[i * 2] = alphabet[v ushr 4]
            hex[i * 2 + 1] = alphabet[v and 0x0F]
        }
        return buildString(36) {
            append(hex, 0, 8)
            append('-')
            append(hex, 8, 4)
            append('-')
            append(hex, 12, 4)
            append('-')
            append(hex, 16, 4)
            append('-')
            append(hex, 20, 12)
        }
    }
}
