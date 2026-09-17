package app.projectzero.datalocal

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        EventRecord::class,
        TombstoneRecord::class,
        ClusterRecord::class,
        ClusterMemberRecord::class,
        StoreMetaRecord::class,
    ],
    version = NotificationDatabase.VERSION,
    exportSchema = true,
)
abstract class NotificationDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao

    companion object {
        const val VERSION = 2
        const val NAME = "notifications.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE store_meta ADD COLUMN listenerDisconnected INTEGER NOT NULL DEFAULT 0",
                )
            }
        }
    }
}
