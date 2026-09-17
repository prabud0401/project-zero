package app.projectzero.datalocal

import app.projectzero.domain.ClusterId
import app.projectzero.domain.DataEpoch
import app.projectzero.domain.DomainBounds
import app.projectzero.domain.EventId
import app.projectzero.domain.SnapshotRevision
import app.projectzero.domain.identity.StoreCursor
import app.projectzero.domain.notification.NotificationEvent
import app.projectzero.domain.notification.NotificationKind
import app.projectzero.domain.notification.NotificationWorkState
import app.projectzero.domain.notification.Sensitivity
import app.projectzero.domain.notification.SummarizedCluster
import app.projectzero.domain.notification.SummaryOrigin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class NotificationRepository(
    private val db: NotificationDatabase,
    private val clock: RepositoryClock = RepositoryClock.Wall,
    private val profileScope: String = DomainBounds.PERSONAL_PROFILE_SCOPE,
) {
    private val dao = db.notificationDao()

    suspend fun cursor(): StoreCursor {
        val meta = ensureMeta()
        return StoreCursor(SnapshotRevision.of(meta.snapshotRevision), DataEpoch.of(meta.dataEpoch))
    }

    suspend fun isHistoryIncomplete(): Boolean = ensureMeta().historyIncomplete

    suspend fun liveEventByKey(frameworkKey: String): NotificationEvent? {
        enforceRetention()
        val record = dao.liveEventByKey(profileScope, frameworkKey) ?: return null
        return record.toDomainOrNull(clock.nowMs())
    }

    suspend fun eventById(eventId: EventId): NotificationEvent? {
        enforceRetention()
        return dao.eventById(eventId.value)?.toDomainOrNull(clock.nowMs())
    }

    suspend fun isTombstoned(eventId: EventId): Boolean =
        dao.tombstone(profileScope, eventId.value) != null

    suspend fun isTombstonedKey(frameworkKey: String): Boolean =
        dao.tombstoneByKey(profileScope, frameworkKey) != null

    suspend fun liveEvents(userSerial: Long = DomainBounds.PERSONAL_USER_SERIAL): List<NotificationEvent> {
        enforceRetention()
        return dao.liveEvents(profileScope, userSerial, clock.nowMs()).mapNotNull { it.toDomainOrNull(clock.nowMs()) }
    }

    suspend fun liveEventsAllUsers(): List<NotificationEvent> {
        enforceRetention()
        return dao.liveEventsAllUsers(profileScope, clock.nowMs()).mapNotNull { it.toDomainOrNull(clock.nowMs()) }
    }

    suspend fun liveCount(): Int {
        enforceRetention()
        return dao.liveCount(profileScope, clock.nowMs())
    }

    suspend fun upsertLive(event: NotificationEvent, signals: PersistSignals, workState: NotificationWorkState, filterReason: String?, cloudEligible: Boolean) {
        dao.upsertEvent(
            event.toRecord(
                profileScope = profileScope,
                frameworkKey = signalsToKeyFallback(event, signals),
                workState = workState.name,
                filterReason = filterReason,
                cloudEligible = cloudEligible,
                signals = signals,
            ),
        )
    }

    suspend fun upsertLive(
        event: NotificationEvent,
        frameworkKey: String,
        signals: PersistSignals,
        workState: NotificationWorkState,
        filterReason: String?,
        cloudEligible: Boolean,
    ) {
        dao.upsertEvent(
            event.toRecord(
                profileScope = profileScope,
                frameworkKey = frameworkKey,
                workState = workState.name,
                filterReason = filterReason,
                cloudEligible = cloudEligible,
                signals = signals,
            ),
        )
    }

    suspend fun tombstone(eventId: EventId, frameworkKey: String) {
        val now = clock.nowMs()
        dao.tombstoneEvent(eventId.value, now, NotificationWorkState.CANCELLED.name)
        dao.upsertTombstone(TombstoneRecord(profileScope, eventId.value, frameworkKey, now))
        dao.deleteMembership(eventId.value)
        stripEmptyClusters()
    }

    suspend fun replaceClusters(clusters: List<SummarizedCluster>, keys: List<ClusterKeyBinding>) {
        val now = clock.nowMs()
        val tombstoned = clusters.flatMap { it.memberEventIds }.filter { isTombstoned(it) }.toSet()
        dao.deleteClusters(profileScope)
        clusters.forEachIndexed { index, cluster ->
            val liveMembers = cluster.memberEventIds.filterNot { it in tombstoned }
            if (liveMembers.isEmpty()) return@forEachIndexed
            val binding = keys.getOrNull(index)
            val record = ClusterRecord(
                clusterId = cluster.clusterId.value,
                profileScope = profileScope,
                sourcePackage = binding?.sourcePackage ?: "unknown.pkg",
                sourceUserSerial = binding?.sourceUserSerial ?: DomainBounds.PERSONAL_USER_SERIAL,
                groupOrKind = binding?.groupOrKind ?: cluster.kind.name,
                baseRevision = cluster.baseRevision,
                headline = cluster.headline,
                summary = cluster.summary,
                kind = cluster.kind.name,
                priority = cluster.priority,
                unreadCount = cluster.unreadCount,
                generatedBy = cluster.generatedBy.name,
                confidence = cluster.confidence,
                generatedAtEpochMs = cluster.generatedAtEpochMs,
                expiresAtEpochMs = cluster.expiresAtEpochMs.coerceAtLeast(now),
            )
            dao.replaceCluster(
                record,
                liveMembers.distinct().map { ClusterMemberRecord(cluster.clusterId.value, it.value) },
            )
        }
    }

    suspend fun advanceSnapshot(): SnapshotRevision {
        val meta = ensureMeta()
        val next = meta.copy(snapshotRevision = meta.snapshotRevision + 1)
        dao.upsertMeta(next)
        return SnapshotRevision.of(next.snapshotRevision)
    }

    suspend fun advanceEpoch() {
        val meta = ensureMeta()
        dao.deleteEvents(profileScope)
        dao.deleteTombstones(profileScope)
        dao.deleteClusters(profileScope)
        dao.upsertMeta(
            meta.copy(
                dataEpoch = meta.dataEpoch + 1,
                snapshotRevision = meta.snapshotRevision + 1,
                historyIncomplete = false,
                listenerDisconnected = false,
            ),
        )
    }

    suspend fun markIncomplete(disconnected: Boolean? = null) {
        val meta = ensureMeta()
        dao.upsertMeta(
            meta.copy(
                historyIncomplete = true,
                listenerDisconnected = disconnected ?: meta.listenerDisconnected,
            ),
        )
    }

    suspend fun setDisconnected(disconnected: Boolean) {
        val meta = ensureMeta()
        dao.upsertMeta(meta.copy(listenerDisconnected = disconnected))
    }

    suspend fun overflowTrim(): Int {
        val now = clock.nowMs()
        var removed = 0
        while (dao.liveCount(profileScope, now) > DomainBounds.MAX_ACTIVE_EVENTS_PER_PROFILE) {
            val oldest = dao.oldestLive(profileScope, 1).firstOrNull() ?: break
            tombstone(EventId.parse(oldest.eventId), oldest.frameworkKey)
            removed++
        }
        if (removed > 0) {
            markIncomplete()
        }
        return removed
    }

    suspend fun enforceRetention() {
        val now = clock.nowMs()
        dao.redactExpiredRaw(now)
        dao.deleteExpiredTombstoned(now)
        dao.deleteExpiredClusters(now)
    }

    fun clusters(): Flow<List<SummarizedCluster>> = flow {
        dao.clusters(profileScope, 0L).collect { records ->
            val now = clock.nowMs()
            emit(
                records.mapNotNull { record ->
                    if (record.expiresAtEpochMs <= now) return@mapNotNull null
                    val members = dao.members(record.clusterId)
                        .map { EventId.parse(it.eventId) }
                        .filterNot { isTombstoned(it) }
                    if (members.isEmpty()) return@mapNotNull null
                    SummarizedCluster(
                        schemaVersion = 1,
                        clusterId = ClusterId.parse(record.clusterId),
                        baseRevision = record.baseRevision,
                        memberEventIds = members,
                        headline = record.headline,
                        summary = record.summary,
                        kind = NotificationKind.fromWire(record.kind),
                        priority = record.priority,
                        unreadCount = record.unreadCount,
                        generatedBy = SummaryOrigin.fromWire(record.generatedBy),
                        confidence = record.confidence,
                        generatedAtEpochMs = record.generatedAtEpochMs,
                        expiresAtEpochMs = record.expiresAtEpochMs,
                    )
                },
            )
        }
    }

    suspend fun clustersOnce(): List<SummarizedCluster> {
        enforceRetention()
        return dao.clustersOnce(profileScope, clock.nowMs()).mapNotNull { record ->
            val members = dao.members(record.clusterId)
                .map { EventId.parse(it.eventId) }
                .filterNot { isTombstoned(it) }
            if (members.isEmpty()) return@mapNotNull null
            SummarizedCluster(
                schemaVersion = 1,
                clusterId = ClusterId.parse(record.clusterId),
                baseRevision = record.baseRevision,
                memberEventIds = members,
                headline = record.headline,
                summary = record.summary,
                kind = NotificationKind.fromWire(record.kind),
                priority = record.priority,
                unreadCount = record.unreadCount,
                generatedBy = SummaryOrigin.fromWire(record.generatedBy),
                confidence = record.confidence,
                generatedAtEpochMs = record.generatedAtEpochMs,
                expiresAtEpochMs = record.expiresAtEpochMs,
            )
        }
    }

    private suspend fun stripEmptyClusters() {
        val now = clock.nowMs()
        dao.clustersOnce(profileScope, now).forEach { cluster ->
            val members = dao.members(cluster.clusterId)
            if (members.isEmpty()) {
                dao.deleteCluster(cluster.clusterId)
            }
        }
    }

    private suspend fun ensureMeta(): StoreMetaRecord {
        val existing = dao.meta(profileScope)
        if (existing != null) return existing
        val created = StoreMetaRecord(
            profileScope = profileScope,
            snapshotRevision = 1L,
            dataEpoch = 1L,
            historyIncomplete = false,
            listenerDisconnected = false,
        )
        dao.upsertMeta(created)
        return created
    }

    private fun signalsToKeyFallback(event: NotificationEvent, signals: PersistSignals): String =
        event.sourcePackage + ":" + event.sourceUserSerial
}

data class ClusterKeyBinding(
    val sourcePackage: String,
    val sourceUserSerial: Long,
    val groupOrKind: String,
)

data class PersistSignals(
    val hadRemoteViews: Boolean = false,
    val hadPendingIntent: Boolean = false,
    val hadSpans: Boolean = false,
    val wasOversized: Boolean = false,
)

fun interface RepositoryClock {
    fun nowMs(): Long

    companion object {
        val Wall: RepositoryClock = RepositoryClock { System.currentTimeMillis() }
    }
}

internal fun EventRecord.toDomainOrNull(now: Long): NotificationEvent? {
    if (tombstoned) return null
    if (expiresAtEpochMs <= now) return null
    return NotificationEvent(
        schemaVersion = 1,
        eventId = EventId.parse(eventId),
        revision = revision,
        postedAtEpochMs = postedAtEpochMs,
        observedAtEpochMs = observedAtEpochMs,
        sourcePackage = sourcePackage,
        sourceUserSerial = sourceUserSerial,
        channelIdHash = channelIdHash,
        kind = NotificationKind.fromWire(kind),
        title = title,
        body = body,
        peopleTokens = if (peopleTokensCsv.isBlank()) emptySet() else peopleTokensCsv.split(',').toSet(),
        groupKeyHash = groupKeyHash,
        isOngoing = isOngoing,
        isClearable = isClearable,
        sensitivity = Sensitivity.fromWire(sensitivity),
        contentFingerprint = contentFingerprint,
        expiresAtEpochMs = expiresAtEpochMs,
    )
}

internal fun NotificationEvent.toRecord(
    profileScope: String,
    frameworkKey: String,
    workState: String,
    filterReason: String?,
    cloudEligible: Boolean,
    signals: PersistSignals,
): EventRecord = EventRecord(
    eventId = eventId.value,
    revision = revision,
    profileScope = profileScope,
    frameworkKey = frameworkKey,
    sourcePackage = sourcePackage,
    sourceUserSerial = sourceUserSerial,
    channelIdHash = channelIdHash,
    kind = kind.name,
    title = title,
    body = body,
    peopleTokensCsv = peopleTokens.sorted().joinToString(","),
    groupKeyHash = groupKeyHash,
    isOngoing = isOngoing,
    isClearable = isClearable,
    sensitivity = sensitivity.name,
    contentFingerprint = contentFingerprint,
    postedAtEpochMs = postedAtEpochMs,
    observedAtEpochMs = observedAtEpochMs,
    expiresAtEpochMs = expiresAtEpochMs,
    workState = workState,
    filterReason = filterReason,
    cloudEligible = cloudEligible,
    hadRemoteViews = signals.hadRemoteViews,
    hadSpans = signals.hadSpans,
    wasOversized = signals.wasOversized,
    hadPendingIntent = signals.hadPendingIntent,
    tombstoned = false,
    tombstonedAtEpochMs = null,
)
