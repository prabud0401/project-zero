package app.projectzero.datalocal

import androidx.test.core.app.ApplicationProvider
import android.app.Application
import app.projectzero.domain.DomainBounds
import app.projectzero.domain.EventId
import app.projectzero.domain.notification.NotificationEvent
import app.projectzero.domain.notification.NotificationKind
import app.projectzero.domain.notification.NotificationWorkState
import app.projectzero.domain.notification.Sensitivity
import app.projectzero.domain.notification.SummarizedCluster
import app.projectzero.domain.notification.SummaryOrigin
import app.projectzero.domain.ClusterId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class NotificationRepositoryTest {
    private lateinit var db: NotificationDatabase
    private lateinit var repo: NotificationRepository
    private var now = 1_700_000_000_000L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        db = NotificationDatabaseFactory.inMemory(context)
        repo = NotificationRepository(db, clock = RepositoryClock { now })
    }

    @Test
    fun tombstonedIdsCannotReappearFromStaleCluster() = runBlocking {
        val event = sampleEvent()
        repo.upsertLive(event, "key-1", PersistSignals(), NotificationWorkState.PUBLISHED, null, false)
        repo.replaceClusters(listOf(sampleCluster(listOf(event.eventId))), listOf(ClusterKeyBinding(event.sourcePackage, 0, "MESSAGE")))
        repo.tombstone(event.eventId, "key-1")
        repo.replaceClusters(listOf(sampleCluster(listOf(event.eventId))), listOf(ClusterKeyBinding(event.sourcePackage, 0, "MESSAGE")))
        assertTrue(repo.isTombstoned(event.eventId))
        assertTrue(repo.clustersOnce().isEmpty())
        assertNull(repo.eventById(event.eventId))
    }

    @Test
    fun rawRetentionMakesEventInaccessibleAt24h() = runBlocking {
        val event = sampleEvent(expiresAt = now + DomainBounds.RAW_RETENTION_MS)
        repo.upsertLive(event, "key-1", PersistSignals(), NotificationWorkState.PUBLISHED, null, false)
        assertNotNull(repo.eventById(event.eventId))
        now += DomainBounds.RAW_RETENTION_MS + 1
        assertNull(repo.eventById(event.eventId))
    }

    @Test
    fun processDeathRestoresPersistedLiveEvent() = runBlocking {
        val event = sampleEvent()
        repo.upsertLive(event, "key-1", PersistSignals(), NotificationWorkState.PUBLISHED, null, false)
        val restored = NotificationRepository(db, clock = RepositoryClock { now })
        val loaded = restored.liveEventByKey("key-1")
        assertEquals(event.eventId, loaded!!.eventId)
        assertEquals(event.body, loaded.body)
    }

    @Test
    fun epochAdvanceErasesAndBlocksRevival() = runBlocking {
        val event = sampleEvent()
        repo.upsertLive(event, "key-1", PersistSignals(), NotificationWorkState.PUBLISHED, null, false)
        repo.tombstone(event.eventId, "key-1")
        val before = repo.cursor()
        repo.advanceEpoch()
        val after = repo.cursor()
        assertTrue(after.dataEpoch.value > before.dataEpoch.value)
        assertFalse(repo.isTombstoned(event.eventId))
        assertNull(repo.eventById(event.eventId))
    }

    private fun sampleEvent(
        id: String = "018f0000-0000-7000-8000-000000000001",
        expiresAt: Long = now + DomainBounds.RAW_RETENTION_MS,
        body: String? = "World",
    ) = NotificationEvent(
        schemaVersion = 1,
        eventId = EventId.parse(id),
        revision = 1,
        postedAtEpochMs = now,
        observedAtEpochMs = now,
        sourcePackage = "com.example.mail",
        sourceUserSerial = 0,
        channelIdHash = "abcdefghijklmnopqrstuvwxyz0123456789-_ABCDE",
        kind = NotificationKind.MESSAGE,
        title = "Hello",
        body = body,
        peopleTokens = emptySet(),
        groupKeyHash = null,
        isOngoing = false,
        isClearable = true,
        sensitivity = Sensitivity.PERSONAL,
        contentFingerprint = "abcdefghijklmnopqrstuvwxyz0123456789-_ABCDE",
        expiresAtEpochMs = expiresAt,
    )

    private fun sampleCluster(members: List<EventId>) = SummarizedCluster(
        schemaVersion = 1,
        clusterId = ClusterId.parse("018f0000-0000-7000-8000-000000000002"),
        baseRevision = 1,
        memberEventIds = members,
        headline = "Hello",
        summary = "World",
        kind = NotificationKind.MESSAGE,
        priority = 50,
        unreadCount = members.size,
        generatedBy = SummaryOrigin.RULE,
        confidence = 1.0,
        generatedAtEpochMs = now,
        expiresAtEpochMs = now + DomainBounds.SUMMARY_RETENTION_MS,
    )
}
