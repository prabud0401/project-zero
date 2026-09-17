package app.projectzero.notificationingest

import android.service.notification.StatusBarNotification
import app.projectzero.datalocal.ClusterKeyBinding
import app.projectzero.datalocal.NotificationRepository
import app.projectzero.datalocal.PersistSignals
import app.projectzero.domain.DomainBounds
import app.projectzero.domain.EventId
import app.projectzero.domain.ReasonCode
import app.projectzero.domain.Validation
import app.projectzero.domain.identity.SnapshotApplyPolicy
import app.projectzero.domain.notification.NotificationEvent
import app.projectzero.domain.notification.NotificationWorkState
import app.projectzero.domain.notification.SummarizedCluster
import app.projectzero.domain.ports.NotificationIngestor
import app.projectzero.domain.routing.FilterDecision
import app.projectzero.localai.CaptureSignals
import app.projectzero.localai.CloudEligibleEnvelope
import app.projectzero.localai.ContentFingerprinter
import app.projectzero.localai.DeterministicSummaryEngine
import app.projectzero.localai.EpochClock
import app.projectzero.localai.LocalPrivacyFilter
import app.projectzero.localai.PrivacyClassifier
import app.projectzero.localai.UuidV7
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class IngestCoordinator(
    private val repository: NotificationRepository,
    private val normalizer: NotificationNormalizer,
    private val fingerprinter: ContentFingerprinter,
    private val filter: LocalPrivacyFilter = LocalPrivacyFilter(),
    private val summaryEngine: DeterministicSummaryEngine,
    private val clock: EpochClock = EpochClock.System,
    private val queue: IngestQueue = IngestQueue(),
) : NotificationIngestor {
    private val mutex = Mutex()
    private val eventsFlow = MutableSharedFlow<NotificationEvent>(extraBufferCapacity = 16)
    private val lastCloudEnvelope = LinkedHashMap<String, CloudEligibleEnvelope>()
    var lastWorkState: NotificationWorkState = NotificationWorkState.NO_OP
        private set
    var lastReason: ReasonCode? = null
        private set
    var lastEnvelopes: List<CloudEligibleEnvelope> = emptyList()
        private set

    override fun events(): Flow<NotificationEvent> = eventsFlow.asSharedFlow()

    fun capture(sbn: StatusBarNotification): NormalizedCapture = normalizer.normalize(sbn)

    suspend fun onPosted(sbn: StatusBarNotification) {
        enqueue(IngestWork.Posted(capture(sbn)))
    }

    suspend fun onPosted(capture: NormalizedCapture) {
        enqueue(IngestWork.Posted(capture))
    }

    suspend fun onRemoved(frameworkKey: String) {
        enqueue(IngestWork.Removed(frameworkKey))
    }

    suspend fun onDisconnected() {
        mutex.withLock {
            repository.setDisconnected(true)
            repository.markIncomplete(disconnected = true)
        }
    }

    suspend fun onConnected(active: List<NormalizedCapture>) {
        mutex.withLock {
            repository.setDisconnected(false)
            reconcileLocked(active)
        }
    }

    suspend fun onAccessRevoked() {
        mutex.withLock {
            lastCloudEnvelope.clear()
            lastEnvelopes = emptyList()
            repository.advanceEpoch()
            lastReason = ReasonCode.NOTIFICATION_ACCESS_DENIED
            lastWorkState = NotificationWorkState.CANCELLED
        }
    }

    suspend fun processPosted(capture: NormalizedCapture): NotificationWorkState =
        mutex.withLock { processPostedLocked(capture) }

    suspend fun processRemoved(frameworkKey: String): NotificationWorkState =
        mutex.withLock { processRemovedLocked(frameworkKey) }

    suspend fun publishedClusters(): List<SummarizedCluster> = repository.clustersOnce()

    fun cloudEnvelopeFor(eventId: String): CloudEligibleEnvelope? = lastCloudEnvelope[eventId]

    private suspend fun enqueue(work: IngestWork) {
        queue.offer(work)
        val drained = queue.drain()
        mutex.withLock {
            if (drained.size >= DomainBounds.MAX_INGESTION_ITEMS) {
                repository.markIncomplete()
            }
            for (item in drained) {
                when (item) {
                    is IngestWork.Removed -> processRemovedLocked(item.frameworkKey)
                    is IngestWork.Posted -> processPostedLocked(item.capture)
                }
            }
        }
    }

    private suspend fun processPostedLocked(capture: NormalizedCapture): NotificationWorkState {
        repository.enforceRetention()
        val existing = repository.liveEventByKey(capture.frameworkKey)
        val classification = PrivacyClassifier.classify(capture.title, capture.body, capture.signals())
        val fingerprint = fingerprinter.contentFingerprint(
            capture.sourcePackage,
            classification.kind.name,
            capture.title,
            capture.body,
        )
        if (existing != null && existing.contentFingerprint == fingerprint) {
            lastWorkState = NotificationWorkState.NO_OP
            lastReason = ReasonCode.DUPLICATE_EVENT
            return lastWorkState
        }
        val eventId = if (existing != null) existing.eventId else EventId.parse(UuidV7.generate(clock.nowMs()))
        val revision = if (existing != null) existing.revision + 1 else 1L
        val event = NotificationEvent(
            schemaVersion = Validation.SCHEMA_VERSION_V1,
            eventId = eventId,
            revision = revision,
            postedAtEpochMs = capture.postedAtEpochMs,
            observedAtEpochMs = capture.observedAtEpochMs,
            sourcePackage = capture.sourcePackage,
            sourceUserSerial = capture.sourceUserSerial,
            channelIdHash = fingerprinter.optionalHash(capture.channelId),
            kind = classification.kind,
            title = capture.title,
            body = capture.body,
            peopleTokens = capture.peopleTokens,
            groupKeyHash = fingerprinter.optionalHash(capture.groupKey),
            isOngoing = capture.isOngoing,
            isClearable = capture.isClearable,
            sensitivity = classification.sensitivity,
            contentFingerprint = fingerprint,
            expiresAtEpochMs = capture.observedAtEpochMs + DomainBounds.RAW_RETENTION_MS,
        )
        val decision = filter.decide(event, classification, capture.signals())
        lastReason = decision.reasonCode
        val persist = PersistSignals(
            hadRemoteViews = capture.hadRemoteViews,
            hadPendingIntent = capture.hadPendingIntent,
            hadSpans = capture.hadSpans,
            wasOversized = capture.wasOversized,
        )
        val envelope = CloudEligibleEnvelope.create(event, decision, capture.signals(), 1.0)
        if (envelope != null) {
            lastCloudEnvelope[event.eventId.value] = envelope
        } else {
            lastCloudEnvelope.remove(event.eventId.value)
        }
        lastEnvelopes = lastCloudEnvelope.values.toList()
        val cloudEligible = envelope != null && decision is FilterDecision.CloudEligible
        repository.upsertLive(
            event = event,
            frameworkKey = capture.frameworkKey,
            signals = persist,
            workState = NotificationWorkState.FILTERING,
            filterReason = decision.reasonCode.wireValue,
            cloudEligible = cloudEligible,
        )
        repository.overflowTrim()
        repository.advanceSnapshot()
        republishClustersLocked()
        eventsFlow.tryEmit(event)
        lastWorkState = NotificationWorkState.PUBLISHED
        return lastWorkState
    }

    private suspend fun processRemovedLocked(frameworkKey: String): NotificationWorkState {
        repository.enforceRetention()
        val existing = repository.liveEventByKey(frameworkKey)
        if (existing == null) {
            lastWorkState = NotificationWorkState.NO_OP
            lastReason = ReasonCode.MEMBER_NOT_LIVE
            return lastWorkState
        }
        lastCloudEnvelope.remove(existing.eventId.value)
        lastEnvelopes = lastCloudEnvelope.values.toList()
        repository.tombstone(existing.eventId, frameworkKey)
        repository.advanceSnapshot()
        republishClustersLocked()
        lastWorkState = NotificationWorkState.CANCELLED
        lastReason = ReasonCode.CANCELLED
        return lastWorkState
    }

    private suspend fun reconcileLocked(active: List<NormalizedCapture>) {
        val activeKeys = active.map { it.frameworkKey }.toSet()
        val stored = repository.liveEventsAllUsers()
        var missing = false
        for (event in stored) {
            // Reconnect cannot reconstruct missed history; drop live rows not in the OS snapshot.
            val keyKnown = active.any { capture ->
                capture.sourcePackage == event.sourcePackage &&
                    capture.sourceUserSerial == event.sourceUserSerial &&
                    capture.title == event.title &&
                    capture.body == event.body
            }
            if (!keyKnown) {
                missing = true
            }
        }
        for (capture in active) {
            processPostedLocked(capture)
        }
        val stillLive = repository.liveEventsAllUsers()
        for (event in stillLive) {
            val match = active.any { it.frameworkKeyMatches(event) || contentMatches(it, event) }
            if (!match) {
                val key = activeKeys.firstOrNull { repository.liveEventByKey(it)?.eventId == event.eventId }
                    ?: (event.sourcePackage + ":" + event.eventId.value)
                repository.tombstone(event.eventId, key)
                missing = true
            }
        }
        if (missing || active.size != stored.size) {
            repository.markIncomplete()
        }
        repository.advanceSnapshot()
        republishClustersLocked()
    }

    private suspend fun republishClustersLocked() {
        val live = repository.liveEventsAllUsers()
        val clusters = summaryEngine.summarize(live)
        val cursor = repository.cursor()
        val liveIds = live.map { it.eventId }.toSet()
        val accepted = clusters.filter { cluster ->
            cluster.memberEventIds.none { repository.isTombstoned(it) } &&
                liveIds.containsAll(cluster.memberEventIds)
        }
        val bindings = accepted.map { cluster ->
            val member = live.first { it.eventId == cluster.memberEventIds.first() }
            ClusterKeyBinding(
                sourcePackage = member.sourcePackage,
                sourceUserSerial = member.sourceUserSerial,
                groupOrKind = member.groupKeyHash ?: member.kind.name,
            )
        }
        repository.replaceClusters(accepted, bindings)
        if (liveIds.isNotEmpty()) {
            SnapshotApplyPolicy.shouldApply(
                request = app.projectzero.domain.identity.PendingCloudRequest(
                    requestId = app.projectzero.domain.RequestId.parse(UuidV7.generate(clock.nowMs())),
                    dataEpoch = cursor.dataEpoch,
                    snapshotRevision = cursor.snapshotRevision,
                    memberEventIds = liveIds.toList(),
                    pending = false,
                ),
                current = cursor,
                liveEligibleMembers = liveIds,
            )
        }
    }

    private fun contentMatches(capture: NormalizedCapture, event: NotificationEvent): Boolean =
        capture.sourcePackage == event.sourcePackage &&
            capture.sourceUserSerial == event.sourceUserSerial &&
            capture.title == event.title &&
            capture.body == event.body

    private fun NormalizedCapture.frameworkKeyMatches(event: NotificationEvent): Boolean = false
}

fun CaptureSignals.toPersist(): PersistSignals = PersistSignals(
    hadRemoteViews = hadRemoteViews,
    hadPendingIntent = hadPendingIntent,
    hadSpans = hadSpans,
    wasOversized = wasOversized,
)
