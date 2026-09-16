package app.projectzero.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SummarizedClusterTest {
    @Test
    fun validClusterIsAccepted() {
        assertEquals(1, Fixtures.cluster().memberEventIds.size)
    }

    @Test
    fun memberEventIdsMustBeOneToFiftyUnique() {
        assertThrows(DomainInvariantException::class.java) { Fixtures.cluster(memberEventIds = emptyList()) }
        val fifty = (1..50).map { index ->
            "018f0000-0000-7000-8000-${index.toString().padStart(12, '0')}"
        }
        Fixtures.cluster(memberEventIds = fifty)
        val fiftyOne = fifty + "018f0000-0000-7000-8000-000000000051"
        assertThrows(DomainInvariantException::class.java) { Fixtures.cluster(memberEventIds = fiftyOne) }
        val duplicate = listOf(Fixtures.UUID_1, Fixtures.UUID_1)
        val ex = assertThrows(DomainInvariantException::class.java) { Fixtures.cluster(memberEventIds = duplicate) }
        assertEquals(ReasonCode.MEMBERSHIP_INVALID, ex.reasonCode)
    }

    @Test
    fun headlineAndSummaryHonorCodePoints() {
        Fixtures.cluster(headline = "a".repeat(80), summary = "b".repeat(280))
        assertThrows(DomainInvariantException::class.java) { Fixtures.cluster(headline = "") }
        assertThrows(DomainInvariantException::class.java) { Fixtures.cluster(headline = "a".repeat(81)) }
        assertThrows(DomainInvariantException::class.java) { Fixtures.cluster(summary = "") }
        assertThrows(DomainInvariantException::class.java) { Fixtures.cluster(summary = "b".repeat(281)) }
        Fixtures.cluster(headline = "😀".repeat(80), summary = "🙂".repeat(280))
        assertThrows(DomainInvariantException::class.java) { Fixtures.cluster(headline = "😀".repeat(81)) }
    }

    @Test
    fun priorityUnreadAndConfidenceBounds() {
        Fixtures.cluster(priority = 0, unreadCount = 0, confidence = 0.0)
        Fixtures.cluster(priority = 100, unreadCount = 9, confidence = 1.0)
        assertThrows(DomainInvariantException::class.java) { Fixtures.cluster(priority = -1) }
        assertThrows(DomainInvariantException::class.java) { Fixtures.cluster(priority = 101) }
        assertThrows(DomainInvariantException::class.java) { Fixtures.cluster(unreadCount = -1) }
        assertThrows(DomainInvariantException::class.java) { Fixtures.cluster(confidence = -0.01) }
        assertThrows(DomainInvariantException::class.java) { Fixtures.cluster(confidence = 1.01) }
        assertThrows(DomainInvariantException::class.java) { Fixtures.cluster(confidence = Double.NaN) }
        assertThrows(DomainInvariantException::class.java) { Fixtures.cluster(confidence = Double.POSITIVE_INFINITY) }
    }

    @Test
    fun snapshotBaseRevisionIsDistinctFromEventRevision() {
        val cluster = Fixtures.cluster(baseRevision = 9)
        val event = Fixtures.event(revision = 2)
        assertEquals(9, cluster.baseRevision)
        assertEquals(2, event.revision)
        assertThrows(DomainInvariantException::class.java) { Fixtures.cluster(baseRevision = 0) }
    }
}
