package app.projectzero.localai

import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class ContentFingerprinter(salt: ByteArray) {
    private val salt: ByteArray = salt.copyOf()

    init {
        require(salt.size >= 16) { "install salt must be at least 16 bytes" }
    }

    fun hmac(vararg parts: String?): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(salt, "HmacSHA256"))
        parts.forEachIndexed { index, part ->
            if (index > 0) {
                mac.update(0)
            }
            if (part != null) {
                mac.update(part.toByteArray(Charsets.UTF_8))
            }
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal())
    }

    fun contentFingerprint(packageName: String, kind: String, title: String?, body: String?): String =
        hmac(packageName, kind, title, body)

    fun optionalHash(value: String?): String? = value?.takeIf { it.isNotEmpty() }?.let { hmac(it) }
}
