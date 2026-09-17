package app.projectzero.datalocal

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteOpenHelper
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File

object NotificationDatabaseFactory {
    fun encrypted(
        context: Context,
        keyManager: DatabaseKeyManager = DatabaseKeyManager(context),
        file: File = File(context.noBackupFilesDir, NotificationDatabase.NAME),
    ): NotificationDatabase {
        val passphrase = try {
            keyManager.getOrCreatePassphrase()
        } catch (e: Exception) {
            throw DatabaseKeyException("key failure; refusing plaintext", e)
        }
        try {
            System.loadLibrary("sqlcipher")
        } catch (e: UnsatisfiedLinkError) {
            passphrase.fill(0)
            throw DatabaseKeyException("sqlcipher unavailable; refusing plaintext", e)
        }
        val factory: SupportSQLiteOpenHelper.Factory = SupportOpenHelperFactory(passphrase)
        return open(context, file.absolutePath, factory)
    }

    fun inMemory(context: Context): NotificationDatabase =
        Room.inMemoryDatabaseBuilder(context, NotificationDatabase::class.java)
            .allowMainThreadQueries()
            .addMigrations(NotificationDatabase.MIGRATION_1_2)
            .build()

    fun fileUnencryptedForMigrationTests(context: Context, file: File): NotificationDatabase =
        Room.databaseBuilder(context, NotificationDatabase::class.java, file.absolutePath)
            .allowMainThreadQueries()
            .addMigrations(NotificationDatabase.MIGRATION_1_2)
            .build()

    private fun open(
        context: Context,
        name: String,
        factory: SupportSQLiteOpenHelper.Factory,
    ): NotificationDatabase =
        Room.databaseBuilder(context, NotificationDatabase::class.java, name)
            .openHelperFactory(factory)
            .addMigrations(NotificationDatabase.MIGRATION_1_2)
            .build()
}
