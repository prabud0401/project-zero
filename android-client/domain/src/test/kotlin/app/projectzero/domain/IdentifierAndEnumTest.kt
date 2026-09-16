package app.projectzero.domain

import app.projectzero.domain.action.ActionAttemptOutcome
import app.projectzero.domain.action.ActionCapability
import app.projectzero.domain.action.Confirmation
import app.projectzero.domain.action.FallbackResolution
import app.projectzero.domain.cache.CacheLayer
import app.projectzero.domain.notification.NotificationKind
import app.projectzero.domain.notification.NotificationWorkState
import app.projectzero.domain.notification.Sensitivity
import app.projectzero.domain.notification.SummaryOrigin
import app.projectzero.domain.operations.OperationState
import app.projectzero.domain.operations.ReservationState
import app.projectzero.domain.routing.RoutingGate
import app.projectzero.domain.routing.SupportedGrammarRoute
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class IdentifierAndEnumTest {
    @Test
    fun uuidV7IdentifiersParse() {
        assertEquals(Fixtures.UUID_1, EventId.parse(Fixtures.UUID_1).value)
        assertEquals(Fixtures.UUID_2, ClusterId.parse(Fixtures.UUID_2).value)
        assertEquals(Fixtures.UUID_3, ActionId.parse(Fixtures.UUID_3).value)
        assertEquals(Fixtures.UUID_1, RequestId.parse(Fixtures.UUID_1).value)
        assertEquals(Fixtures.UUID_2, InstallationId.parse(Fixtures.UUID_2).value)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "",
            "not-a-uuid",
            "018f0000-0000-7000-8000-00000000000",
            Fixtures.UUID_V4,
            "018f0000-0000-4000-8000-000000000001",
            "018f0000-0000-7000-0000-000000000001",
            "018F0000-0000-8000-8000-000000000001",
        ],
    )
    fun uuidV7IdentifiersRejectInvalid(value: String) {
        assertThrows(DomainInvariantException::class.java) { EventId.parse(value) }
        assertThrows(DomainInvariantException::class.java) { ClusterId.parse(value) }
        assertThrows(DomainInvariantException::class.java) { ActionId.parse(value) }
        assertThrows(DomainInvariantException::class.java) { RequestId.parse(value) }
    }

    @Test
    fun revisionsAndEpochsRejectZero() {
        assertThrows(DomainInvariantException::class.java) { EventRevision.of(0) }
        assertThrows(DomainInvariantException::class.java) { SnapshotRevision.of(0) }
        assertThrows(DomainInvariantException::class.java) { DataEpoch.of(0) }
        assertThrows(DomainInvariantException::class.java) { ConsentEpoch.of(-1) }
        assertEquals(1L, EventRevision.of(1).value)
        assertEquals(2L, SnapshotRevision.of(2).value)
        assertEquals(3L, DataEpoch.of(3).value)
    }

    @Test
    fun enumWireValuesAreStableNames() {
        assertWire(Sensitivity.entries)
        assertWire(NotificationKind.entries)
        assertWire(SummaryOrigin.entries)
        assertWire(NotificationWorkState.entries)
        assertWire(ActionCapability.entries)
        assertWire(FallbackResolution.entries)
        assertWire(Confirmation.entries)
        assertWire(ActionAttemptOutcome.entries)
        assertWire(OperationState.entries)
        assertWire(ReservationState.entries)
        assertWire(CacheLayer.entries)
        assertWire(RoutingGate.entries)
        assertWire(SupportedGrammarRoute.entries)
        assertWire(ReasonCode.entries)
        assertEquals(Sensitivity.SECRET, Sensitivity.fromWire("SECRET"))
        assertEquals(NotificationKind.MESSAGE, NotificationKind.fromWire("MESSAGE"))
        assertEquals(ActionCapability.CREATE_CALENDAR_EVENT, ActionCapability.fromWire("CREATE_CALENDAR_EVENT"))
        assertEquals(ActionAttemptOutcome.OUTCOME_UNKNOWN, ActionAttemptOutcome.fromWire("OUTCOME_UNKNOWN"))
        assertEquals(OperationState.OUTCOME_UNKNOWN, OperationState.fromWire("OUTCOME_UNKNOWN"))
        assertEquals(ReasonCode.HANDED_OFF, ReasonCode.fromWire("HANDED_OFF"))
        assertThrows(DomainInvariantException::class.java) { Sensitivity.fromWire("PRIVATE") }
        assertThrows(DomainInvariantException::class.java) { ReasonCode.fromWire("not-a-code") }
    }

    private fun assertWire(entries: List<Enum<*>>) {
        entries.forEach { entry ->
            val wire = entry.javaClass.getMethod("getWireValue").invoke(entry) as String
            assertEquals(entry.name, wire, "wire value drifted for $entry")
        }
    }
}
