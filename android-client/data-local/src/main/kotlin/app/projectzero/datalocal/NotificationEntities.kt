package app.projectzero.datalocal

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "events",
    indices = [
        Index(value = ["profileScope", "frameworkKey"]),
        Index(value = ["eventId"], unique = true),
        Index(value = ["expiresAtEpochMs"]),
        Index(value = ["sourceUserSerial"]),
    ],
)
data class EventRecord(
    @PrimaryKey val eventId: String,
    val revision: Long,
    val profileScope: String,
    val frameworkKey: String,
    val sourcePackage: String,
    val sourceUserSerial: Long,
    val channelIdHash: String?,
    val kind: String,
    val title: String?,
    val body: String?,
    val peopleTokensCsv: String,
    val groupKeyHash: String?,
    val isOngoing: Boolean,
    val isClearable: Boolean,
    val sensitivity: String,
    val contentFingerprint: String,
    val postedAtEpochMs: Long,
    val observedAtEpochMs: Long,
    val expiresAtEpochMs: Long,
    val workState: String,
    val filterReason: String?,
    val cloudEligible: Boolean,
    val hadRemoteViews: Boolean,
    val hadSpans: Boolean,
    val wasOversized: Boolean,
    val hadPendingIntent: Boolean,
    val tombstoned: Boolean,
    val tombstonedAtEpochMs: Long?,
)

@Entity(tableName = "tombstones", primaryKeys = ["profileScope", "eventId"])
data class TombstoneRecord(
    val profileScope: String,
    val eventId: String,
    val frameworkKey: String,
    val tombstonedAtEpochMs: Long,
)

@Entity(tableName = "clusters")
data class ClusterRecord(
    @PrimaryKey val clusterId: String,
    val profileScope: String,
    val sourcePackage: String,
    val sourceUserSerial: Long,
    val groupOrKind: String,
    val baseRevision: Long,
    val headline: String,
    val summary: String,
    val kind: String,
    val priority: Int,
    val unreadCount: Int,
    val generatedBy: String,
    val confidence: Double,
    val generatedAtEpochMs: Long,
    val expiresAtEpochMs: Long,
)

@Entity(tableName = "cluster_members", primaryKeys = ["clusterId", "eventId"])
data class ClusterMemberRecord(
    val clusterId: String,
    val eventId: String,
)

@Entity(tableName = "store_meta")
data class StoreMetaRecord(
    @PrimaryKey val profileScope: String,
    val snapshotRevision: Long,
    val dataEpoch: Long,
    val historyIncomplete: Boolean,
    val listenerDisconnected: Boolean,
)
