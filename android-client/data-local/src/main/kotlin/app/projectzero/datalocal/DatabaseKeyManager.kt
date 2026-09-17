package app.projectzero.datalocal

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class DatabaseKeyManager(
    private val context: Context,
    private val alias: String = KEY_ALIAS,
    private val wrappedKeyFile: File = File(context.noBackupFilesDir, WRAPPED_KEY_NAME),
) {
    fun getOrCreatePassphrase(): ByteArray {
        val wrappingKey = wrappingKey()
        if (!wrappedKeyFile.exists()) {
            val raw = ByteArray(32)
            SecureRandom().nextBytes(raw)
            try {
                writeWrapped(raw, wrappingKey)
            } finally {
                raw.fill(0)
            }
        }
        return unwrap(wrappingKey)
    }

    fun deleteKeyMaterial() {
        if (wrappedKeyFile.exists()) {
            wrappedKeyFile.writeBytes(ByteArray(wrappedKeyFile.length().toInt().coerceAtLeast(1)))
            wrappedKeyFile.delete()
        }
        runCatching {
            val ks = KeyStore.getInstance(ANDROID_KEYSTORE)
            ks.load(null)
            if (ks.containsAlias(alias)) {
                ks.deleteEntry(alias)
            }
        }
    }

    private fun wrappingKey(): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE)
        ks.load(null)
        val existing = ks.getKey(alias, null) as? SecretKey
        if (existing != null) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val specBuilder = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
        if (Build.VERSION.SDK_INT >= 28) {
            specBuilder.setUnlockedDeviceRequired(true)
        }
        generator.init(specBuilder.build())
        return generator.generateKey()
    }

    private fun writeWrapped(raw: ByteArray, wrappingKey: SecretKey) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, wrappingKey)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(raw)
        wrappedKeyFile.parentFile?.mkdirs()
        wrappedKeyFile.writeBytes(byteArrayOf(iv.size.toByte()) + iv + ciphertext)
    }

    private fun unwrap(wrappingKey: SecretKey): ByteArray {
        val payload = wrappedKeyFile.readBytes()
        if (payload.isEmpty()) {
            throw DatabaseKeyException("wrapped key file empty")
        }
        val ivSize = payload[0].toInt() and 0xFF
        if (ivSize !in 12..16 || payload.size <= 1 + ivSize) {
            throw DatabaseKeyException("wrapped key file corrupt")
        }
        val iv = payload.copyOfRange(1, 1 + ivSize)
        val ciphertext = payload.copyOfRange(1 + ivSize, payload.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, wrappingKey, GCMParameterSpec(128, iv))
        return try {
            cipher.doFinal(ciphertext)
        } catch (e: Exception) {
            throw DatabaseKeyException("key unwrap failed; refusing plaintext", e)
        }
    }

    companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "projectzero.notification.db"
        const val WRAPPED_KEY_NAME = "nle.key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
