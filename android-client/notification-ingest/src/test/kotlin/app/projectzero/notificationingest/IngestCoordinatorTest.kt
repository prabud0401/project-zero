package app.projectzero.notificationingest

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import app.projectzero.datalocal.NotificationDatabaseFactory
import app.projectzero.datalocal.NotificationRepository
import app.projectzero.domain.DomainBounds
import app.projectzero.domain.ReasonCode
import app.projectzero.domain.notification.NotificationWorkState
import app.projectzero.localai.ContentFingerprinter
import app.projectzero.localai.DeterministicSummaryEngine
import app.projectzero.localai.EpochClock
import app.projectzero.localai.LocalPrivacyFilter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class IngestCoordinatorTest {
    private lateinit var coordinator: IngestCoordinator
    private lateinit var repository: NotificationRepository
    private var now = 1_700_000_000_000L
    private val salt = ByteArray(32) { 3 }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val db = NotificationDatabaseFactory.inMemory(context)
        repository = NotificationRepository(db, clock = { now })
        coordinator = coordinatorOf(repository)
    }

    @Test
    fun postUpdateRemove() = runBlocking {
        val posted = coordinator.processPosted(capture("key-a", title = "Hi", body = "One"))
        assertEquals(NotificationWorkState.PUBLISHED, posted)
        val firstId = repository.liveEventByKey("key-a")!!.eventId
        coordinator.processPosted(capture("key-a", title = "Hi", body = "Two"))
        val updated = repository.liveEventByKey("key-a")!!
        assertEquals(firstId, updated.eventId)
        assertEquals(2L, updated.revision)
        assertEquals("Two", updated.body)
        coordinator.processRemoved("key-a")
        assertNull(repository.liveEventByKey("key-a"))
        assertTrue(repository.isTombstoned(firstId))
    }

    @Test
    fun sameKeyRepostAfterRemovalGetsNewEventId() = runBlocking {
        coordinator.processPosted(capture("key-a", body = "One"))
        val first = repository.liveEventByKey("key-a")!!.eventId
        coordinator.processRemoved("key-a")
        coordinator.processPosted(capture("key-a", body = "Two"))
        val second = repository.liveEventByKey("key-a")!!.eventId
        assertNotEquals(first, second)
        assertTrue(repository.isTombstoned(first))
    }

    @Test
    fun duplicateUnchangedPostIsNoOp() = runBlocking {
        coordinator.processPosted(capture("key-a"))
        val revision = repository.liveEventByKey("key-a")!!.revision
        val state = coordinator.processPosted(capture("key-a"))
        assertEquals(NotificationWorkState.NO_OP, state)
        assertEquals(revision, repository.liveEventByKey("key-a")!!.revision)
    }

    @Test
    fun processDeathSimulationRestoresPublishedEvents() = runBlocking {
        coordinator.processPosted(capture("key-a", body = "persist-me"))
        val restored = coordinatorOf(repository)
        val event = restored.let { repository.liveEventByKey("key-a") }
        assertEquals("persist-me", event!!.body)
        assertEquals(1, restored.publishedClusters().size)
    }

    @Test
    fun listenerDisconnectMarksIncompleteHistory() = runBlocking {
        coordinator.processPosted(capture("key-a"))
        coordinator.onDisconnected()
        assertTrue(repository.isHistoryIncomplete())
    }

    @Test
    fun reconnectDoesNotInventMissedHistory() = runBlocking {
        coordinator.processPosted(capture("key-a", body = "old"))
        coordinator.onConnected(listOf(capture("key-b", body = "current")))
        assertNull(repository.liveEventByKey("key-a"))
        assertEquals("current", repository.liveEventByKey("key-b")!!.body)
        assertTrue(repository.isHistoryIncomplete())
    }

    @Test
    fun multiUserSerialsStaySeparated() = runBlocking {
        coordinator.processPosted(capture("0|com.example.mail|1", userSerial = 0, title = "personal"))
        coordinator.processPosted(capture("10|com.example.mail|1", userSerial = 10, title = "work"))
        val clusters = coordinator.publishedClusters()
        assertEquals(2, clusters.size)
        val personal = repository.liveEvents(0)
        val work = repository.liveEvents(10)
        assertEquals(1, personal.size)
        assertEquals(1, work.size)
        assertEquals(0L, personal.single().sourceUserSerial)
        assertEquals(10L, work.single().sourceUserSerial)
    }

    @Test
    fun ongoingNotificationStaysUntilRemoved() = runBlocking {
        coordinator.processPosted(capture("key-on", ongoing = true, body = "playing"))
        assertTrue(repository.liveEventByKey("key-on")!!.isOngoing)
        coordinator.processRemoved("key-on")
        assertNull(repository.liveEventByKey("key-on"))
    }

    @Test
    fun outOfOrderRemoveThenPost() = runBlocking {
        val removed = coordinator.processRemoved("missing")
        assertEquals(NotificationWorkState.NO_OP, removed)
        coordinator.processPosted(capture("missing", body = "later"))
        assertEquals("later", repository.liveEventByKey("missing")!!.body)
    }

    @Test
    fun outOfOrderStaleDuplicateAfterUpdate() = runBlocking {
        coordinator.processPosted(capture("key-a", body = "v1"))
        coordinator.processPosted(capture("key-a", body = "v2"))
        val state = coordinator.processPosted(capture("key-a", body = "v2"))
        assertEquals(NotificationWorkState.NO_OP, state)
        assertEquals(2L, repository.liveEventByKey("key-a")!!.revision)
    }

    @Test
    fun tombstoneBlocksStaleClusterRevival() = runBlocking {
        coordinator.processPosted(capture("key-a", body = "gone"))
        val id = repository.liveEventByKey("key-a")!!.eventId
        coordinator.processRemoved("key-a")
        val stale = coordinator.publishedClusters()
        assertTrue(stale.none { id in it.memberEventIds })
        assertTrue(repository.isTombstoned(id))
    }

    @Test
    fun rawRetentionBoundIs24Hours() = runBlocking {
        coordinator.processPosted(capture("key-a"))
        now += DomainBounds.RAW_RETENTION_MS + 5
        val later = coordinatorOf(repository)
        later.processPosted(capture("key-b", body = "fresh"))
        assertNull(repository.liveEventByKey("key-a"))
    }

    @Test
    fun accessRevokeAdvancesEpoch() = runBlocking {
        coordinator.processPosted(capture("key-a"))
        val before = repository.cursor()
        coordinator.onAccessRevoked()
        val after = repository.cursor()
        assertTrue(after.dataEpoch.value > before.dataEpoch.value)
        assertNull(repository.liveEventByKey("key-a"))
        assertEquals(ReasonCode.NOTIFICATION_ACCESS_DENIED, coordinator.lastReason)
    }

    private fun coordinatorOf(repo: NotificationRepository): IngestCoordinator {
        val fingerprinter = ContentFingerprinter(salt)
        return IngestCoordinator(
            repository = repo,
            normalizer = NotificationNormalizer(null, fingerprinter, EpochClock { now }),
            fingerprinter = fingerprinter,
            filter = LocalPrivacyFilter(),
            summaryEngine = DeterministicSummaryEngine(clock = EpochClock { now }, fingerprinter = fingerprinter),
            clock = EpochClock { now },
        )
    }

    private fun capture(
        key: String,
        title: String? = "Hello",
        body: String? = "World",
        userSerial: Long = 0,
        ongoing: Boolean = false,
        packageName: String = "com.example.mail",
        hadRemoteViews: Boolean = false,
        hadPendingIntent: Boolean = false,
        hadSpans: Boolean = false,
        wasOversized: Boolean = false,
        category: String? = "msg",
    ) = NormalizedCapture(
        frameworkKey = key,
        sourcePackage = packageName,
        sourceUserSerial = userSerial,
        channelId = "inbox",
        groupKey = null,
        title = title,
        body = body,
        peopleTokens = emptySet(),
        isOngoing = ongoing,
        isClearable = !ongoing,
        postedAtEpochMs = now,
        observedAtEpochMs = now,
        category = category,
        hadRemoteViews = hadRemoteViews,
        hadPendingIntent = hadPendingIntent,
        hadSpans = hadSpans,
        wasOversized = wasOversized,
    )
}
