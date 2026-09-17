package app.projectzero.datalocal

import android.content.Context
import java.io.File
import java.security.SecureRandom

class InstallSaltStore(
    context: Context,
    private val file: File = File(context.noBackupFilesDir, "install.salt"),
) {
    fun getOrCreate(): ByteArray {
        if (file.exists() && file.length() >= 32L) {
            return file.readBytes().copyOf(32)
        }
        val salt = ByteArray(32)
        SecureRandom().nextBytes(salt)
        file.parentFile?.mkdirs()
        file.writeBytes(salt)
        return salt.copyOf()
    }

    fun delete() {
        if (file.exists()) {
            file.writeBytes(ByteArray(file.length().toInt().coerceAtLeast(1)))
            file.delete()
        }
    }
}
