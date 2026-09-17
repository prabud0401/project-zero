package app.projectzero.localai

import app.projectzero.domain.ReasonCode
import app.projectzero.domain.notification.Sensitivity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PrivacyClassifierTest {
    @Test
    fun otpIsSecretAndCloudProhibited() {
        val result = PrivacyClassifier.classify(
            "Login",
            "Your verification code is 123456",
            CaptureSignals(),
        )
        assertEquals(Sensitivity.SECRET, result.sensitivity)
        assertTrue(result.cloudProhibited)
        assertTrue(ReasonCode.OTP in result.reasons)
    }

    @Test
    fun healthIsSecret() {
        val result = PrivacyClassifier.classify("Clinic", "Lab results: A1C 7.2", CaptureSignals())
        assertTrue(ReasonCode.HEALTH in result.reasons)
        assertEquals(Sensitivity.SECRET, result.sensitivity)
    }

    @Test
    fun financeIsSecret() {
        val result = PrivacyClassifier.classify("Bank", "Account ending 1234 available balance $12", CaptureSignals())
        assertTrue(ReasonCode.FINANCIAL in result.reasons)
    }

    @Test
    fun managedProfileIsCloudProhibited() {
        val result = PrivacyClassifier.classify(
            "Hi",
            "Hello",
            CaptureSignals(sourceUserSerial = 10),
        )
        assertTrue(ReasonCode.MANAGED_PROFILE in result.reasons)
        assertTrue(result.cloudProhibited)
    }

    @Test
    fun remoteViewsPendingIntentSpansOversizedNeverCloudEligible() {
        val filter = LocalPrivacyFilter()
        val event = TestEvents.event(sensitivity = Sensitivity.PUBLIC, title = "Ping", body = "Server up")
        val decision = filter.decide(
            event,
            PrivacyClassifier.classify(event.title, event.body, CaptureSignals()),
            CaptureSignals(hadRemoteViews = true),
        )
        assertTrue(decision is app.projectzero.domain.routing.FilterDecision.LocalOnly)
    }
}

class CloudEligibleEnvelopeTest {
    @Test
    fun hostileSignalsNeverCreateEnvelope() {
        val event = TestEvents.event(title = "Ping", body = "Server up")
        val decision = app.projectzero.domain.routing.FilterDecision.CloudEligible()
        val cases = listOf(
            CaptureSignals(hadRemoteViews = true),
            CaptureSignals(hadPendingIntent = true),
            CaptureSignals(hadSpans = true),
            CaptureSignals(wasOversized = true),
            CaptureSignals(sourceUserSerial = 10),
        )
        cases.forEach { signals ->
            assertNull(CloudEligibleEnvelope.create(event, decision, signals, 1.0), signals.toString())
        }
    }

    @Test
    fun otpEventNeverCreatesEnvelopeEvenIfMislabelledEligible() {
        val event = TestEvents.event(title = "Code", body = "Your verification code is 999111")
        val classified = PrivacyClassifier.classify(event.title, event.body, CaptureSignals())
        val decision = LocalPrivacyFilter().decide(event, classified, CaptureSignals())
        assertNull(CloudEligibleEnvelope.create(event, decision, CaptureSignals(), 1.0))
    }

    @Test
    fun envelopeContainsOnlyRedactedFields() {
        val event = TestEvents.event(title = "Ping", body = "Server up")
        val envelope = CloudEligibleEnvelope.create(
            event,
            app.projectzero.domain.routing.FilterDecision.CloudEligible(),
            CaptureSignals(),
            1.0,
        )
        assertEquals(event.eventId, envelope!!.eventId)
        assertEquals(event.kind, envelope.kind)
        assertEquals("Ping", envelope.redactedTitle)
        assertEquals("Server up", envelope.redactedBody)
        assertEquals(event.contentFingerprint, envelope.contentFingerprint)
    }

    @Test
    fun lowRedactionConfidenceDenied() {
        val event = TestEvents.event(title = "Ping", body = "Server up")
        assertNull(
            CloudEligibleEnvelope.create(
                event,
                app.projectzero.domain.routing.FilterDecision.CloudEligible(),
                CaptureSignals(),
                0.5,
            ),
        )
    }
}
