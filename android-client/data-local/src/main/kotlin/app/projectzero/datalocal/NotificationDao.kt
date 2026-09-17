package app.projectzero.datalocal

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEvent(record: EventRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTombstone(record: TombstoneRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCluster(record: ClusterRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMembers(members: List<ClusterMemberRecord>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMeta(record: StoreMetaRecord)

    @Query("SELECT * FROM events WHERE eventId = :eventId LIMIT 1")
    suspend fun eventById(eventId: String): EventRecord?

    @Query(
        "SELECT * FROM events WHERE profileScope = :scope AND frameworkKey = :key AND tombstoned = 0 LIMIT 1",
    )
    suspend fun liveEventByKey(scope: String, key: String): EventRecord?

    @Query("SELECT * FROM tombstones WHERE profileScope = :scope AND eventId = :eventId LIMIT 1")
    suspend fun tombstone(scope: String, eventId: String): TombstoneRecord?

    @Query("SELECT * FROM tombstones WHERE profileScope = :scope AND frameworkKey = :key LIMIT 1")
    suspend fun tombstoneByKey(scope: String, key: String): TombstoneRecord?

    @Query(
        """
        SELECT * FROM events
        WHERE profileScope = :scope
          AND tombstoned = 0
          AND expiresAtEpochMs > :now
          AND sourceUserSerial = :userSerial
        """,
    )
    suspend fun liveEvents(scope: String, userSerial: Long, now: Long): List<EventRecord>

    @Query(
        """
        SELECT * FROM events
        WHERE profileScope = :scope
          AND tombstoned = 0
          AND expiresAtEpochMs > :now
        """,
    )
    suspend fun liveEventsAllUsers(scope: String, now: Long): List<EventRecord>

    @Query("SELECT COUNT(*) FROM events WHERE profileScope = :scope AND tombstoned = 0 AND expiresAtEpochMs > :now")
    suspend fun liveCount(scope: String, now: Long): Int

    @Query("SELECT * FROM store_meta WHERE profileScope = :scope LIMIT 1")
    suspend fun meta(scope: String): StoreMetaRecord?

    @Query("SELECT * FROM clusters WHERE profileScope = :scope AND expiresAtEpochMs > :now")
    fun clusters(scope: String, now: Long): Flow<List<ClusterRecord>>

    @Query("SELECT * FROM clusters WHERE profileScope = :scope AND expiresAtEpochMs > :now")
    suspend fun clustersOnce(scope: String, now: Long): List<ClusterRecord>

    @Query("SELECT * FROM cluster_members WHERE clusterId = :clusterId")
    suspend fun members(clusterId: String): List<ClusterMemberRecord>

    @Query("DELETE FROM cluster_members WHERE clusterId = :clusterId")
    suspend fun deleteMembers(clusterId: String)

    @Query("DELETE FROM clusters WHERE clusterId = :clusterId")
    suspend fun deleteCluster(clusterId: String)

    @Query("DELETE FROM clusters WHERE profileScope = :scope")
    suspend fun deleteClusters(scope: String)

    @Query("DELETE FROM cluster_members WHERE eventId = :eventId")
    suspend fun deleteMembership(eventId: String)

    @Query(
        """
        UPDATE events SET title = NULL, body = NULL, peopleTokensCsv = '',
        tombstoned = 1, tombstonedAtEpochMs = :now, workState = :state
        WHERE eventId = :eventId
        """,
    )
    suspend fun tombstoneEvent(eventId: String, now: Long, state: String)

    @Query(
        """
        UPDATE events SET title = NULL, body = NULL, peopleTokensCsv = ''
        WHERE expiresAtEpochMs <= :now AND tombstoned = 0
        """,
    )
    suspend fun redactExpiredRaw(now: Long)

    @Query("DELETE FROM events WHERE expiresAtEpochMs <= :now AND tombstoned = 1")
    suspend fun deleteExpiredTombstoned(now: Long)

    @Query("DELETE FROM clusters WHERE expiresAtEpochMs <= :now")
    suspend fun deleteExpiredClusters(now: Long)

    @Query("DELETE FROM events WHERE profileScope = :scope")
    suspend fun deleteEvents(scope: String)

    @Query("DELETE FROM tombstones WHERE profileScope = :scope")
    suspend fun deleteTombstones(scope: String)

    @Query(
        """
        SELECT * FROM events
        WHERE profileScope = :scope AND tombstoned = 0 AND isOngoing = 0
        ORDER BY observedAtEpochMs ASC
        LIMIT :limit
        """,
    )
    suspend fun oldestLive(scope: String, limit: Int): List<EventRecord>

    @Transaction
    suspend fun replaceCluster(cluster: ClusterRecord, members: List<ClusterMemberRecord>) {
        deleteMembers(cluster.clusterId)
        upsertCluster(cluster)
        if (members.isNotEmpty()) {
            upsertMembers(members)
        }
    }
}
