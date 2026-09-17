package app.projectzero.datalocal

import android.app.Application
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class MigrationAndEncryptionTest {
    @Test
    fun migration1To2AddsListenerDisconnectedWithoutDroppingRows() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null)
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            CREATE TABLE store_meta (
                              profileScope TEXT NOT NULL PRIMARY KEY,
                              snapshotRevision INTEGER NOT NULL,
                              dataEpoch INTEGER NOT NULL,
                              historyIncomplete INTEGER NOT NULL
                            )
                            """.trimIndent(),
                        )
                        db.execSQL(
                            "INSERT INTO store_meta VALUES ('personal', 4, 2, 0)",
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build(),
        )
        val db = helper.writableDatabase
        NotificationDatabase.MIGRATION_1_2.migrate(db)
        val info = db.query("PRAGMA table_info(store_meta)")
        val columns = mutableListOf<String>()
        while (info.moveToNext()) {
            columns += info.getString(1)
        }
        info.close()
        assertTrue(columns.contains("listenerDisconnected"))
        val rows = db.query("SELECT snapshotRevision, dataEpoch FROM store_meta WHERE profileScope = 'personal'")
        assertTrue(rows.moveToFirst())
        assertEquals(4L, rows.getLong(0))
        assertEquals(2L, rows.getLong(1))
        rows.close()
        db.close()
    }

    @Test
    fun encryptedOpenRefusesPlaintextWhenSqlcipherCannotLoad() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        var refused = false
        try {
            NotificationDatabaseFactory.encrypted(context)
        } catch (e: DatabaseKeyException) {
            refused = true
            assertTrue(e.message!!.contains("refusing plaintext"))
        } catch (e: Exception) {
            refused = true
            assertFalse(e.message?.contains("opened unencrypted") == true)
        }
        assertTrue(refused)
    }
}
