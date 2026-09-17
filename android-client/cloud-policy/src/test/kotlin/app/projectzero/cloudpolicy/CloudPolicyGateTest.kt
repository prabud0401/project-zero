package app.projectzero.cloudpolicy

import app.projectzero.domain.notification.NotificationEvent
import app.projectzero.domain.notification.Sensitivity
import app.projectzero.domain.EventId
import app.projectzero.domain.notification.NotificationKind
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class CloudPolicyGateTest {
    @Test
    fun `test cloud policy eligibility`() {
        val gate = CloudPolicyGate()
        val event = NotificationEvent(
            schemaVersion = 1,
            eventId = EventId.parse("00000000-0000-7000-8000-000000000000"),
            revision = 1L,
            postedAtEpochMs = 1000L,
            observedAtEpochMs = 1000L,
            sourcePackage = "com.example.app",
            sourceUserSerial = 1L,
            channelIdHash = null,
            kind = NotificationKind.MESSAGE,
            title = "Test",
            body = "Test body",
            peopleTokens = emptySet(),
            groupKeyHash = null,
            isOngoing = false,
            isClearable = true,
            sensitivity = Sensitivity.PUBLIC,
            contentFingerprint = "abcdabcdabcdabcdabcdabcdabcdabcdabcdabcdabc",
            expiresAtEpochMs = 2000L
        )
        assertTrue(gate.isEligibleForCloud(event, true, true))
        assertFalse(gate.isEligibleForCloud(event.copy(sensitivity = Sensitivity.SECRET), true, true))
        assertFalse(gate.isEligibleForCloud(event, false, true))
        assertFalse(gate.isEligibleForCloud(event, true, false))
    }
}
