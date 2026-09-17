package app.projectzero.localai

import app.projectzero.domain.DomainBounds
import app.projectzero.domain.EventId

import kotlinx.coroutines.test.runTest
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.constraints.IntRange
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DeterministicSummaryEngineTest {
    private val clock = EpochClock { 1_700_000_000_000 }
    private val engine = DeterministicSummaryEngine(
        clock = clock,
        fingerprinter = ContentFingerprinter(ByteArray(32) { 7 }),
    )

    @Test
    fun clustersHaveUniqueMembers() = runTest {
        val a = TestEvents.event(eventId = TestEvents.UUID_1, title = "A")
        val dup = TestEvents.event(eventId = TestEvents.UUID_1, title = "A")
        val b = TestEvents.event(eventId = TestEvents.UUID_2, title = "B")
        val clusters = engine.summarize(listOf(a, dup, b))
        assertEquals(1, clusters.size)
        assertEquals(2, clusters.single().memberEventIds.size)
        assertEquals(clusters.single().memberEventIds.toSet().size, clusters.single().memberEventIds.size)
    }

    @Property(tries = 80)
    fun propertyNoDuplicateClusterMembers(@ForAll @IntRange(min = 1, max = 20) count: Int) {
        val members = List(count) { index ->
            val id = "018f0000-0000-7000-8000-${(index % 7 + 1).toString().padStart(12, '0')}"
            TestEvents.event(eventId = id, title = "m$index")
        }
        val unique = engine.uniqueMembers(members + members)
        assertEquals(unique.map { it.eventId }.toSet().size, unique.size)
        assertTrue(unique.size <= DomainBounds.MAX_EVENTS_PER_SUMMARY_BATCH)
    }

    @Test
    fun expiredEventsAreOmitted() = runTest {
        val live = TestEvents.event(expiresAtEpochMs = 1_800_000_000_000)
        val dead = TestEvents.event(
            eventId = TestEvents.UUID_2,
            expiresAtEpochMs = 1_000L,
        )
        val clusters = engine.summarize(listOf(live, dead))
        assertEquals(listOf(EventId.parse(TestEvents.UUID_1)), clusters.single().memberEventIds)
    }

    @Test
    fun differentUserSerialsDoNotShareACluster() = runTest {
        val personal = TestEvents.event(sourceUserSerial = 0, title = "p")
        val work = TestEvents.event(eventId = TestEvents.UUID_2, sourceUserSerial = 10, title = "w")
        val clusters = engine.summarize(listOf(personal, work))
        assertEquals(2, clusters.size)
    }
}
