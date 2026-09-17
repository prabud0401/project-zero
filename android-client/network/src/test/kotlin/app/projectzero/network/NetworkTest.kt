package app.projectzero.network

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import app.projectzero.domain.notification.NotificationEvent
import app.projectzero.domain.notification.Sensitivity
import app.projectzero.domain.EventId
import app.projectzero.domain.notification.NotificationKind

class NetworkTest {
    @Test
    fun `test redaction`() {
        val redactor = Redactor()
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
            title = "Secret Title",
            body = "Secret body",
            peopleTokens = emptySet(),
            groupKeyHash = null,
            isOngoing = false,
            isClearable = true,
            sensitivity = Sensitivity.PUBLIC,
            contentFingerprint = "abcdabcdabcdabcdabcdabcdabcdabcdabcdabcdabc",
            expiresAtEpochMs = 2000L
        )
        val redacted = redactor.redact(event)
        assertEquals("<REDACTED>", redacted.title)
        assertEquals("<REDACTED>", redacted.body)
        
        // Ensure no tokens logged
    }
}
