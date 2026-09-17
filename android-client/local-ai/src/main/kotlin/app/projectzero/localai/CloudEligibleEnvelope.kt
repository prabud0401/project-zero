package app.projectzero.localai

import app.projectzero.domain.EventId
import app.projectzero.domain.ReasonCode
import app.projectzero.domain.notification.NotificationEvent
import app.projectzero.domain.notification.NotificationKind
import app.projectzero.domain.routing.FilterDecision
import app.projectzero.domain.routing.RoutingThresholds

/**
 * Task 4 wire-shaped local object. Task 2 never transmits it.
 * Hostile or insufficiently redacted content cannot construct an instance.
 */
@ConsistentCopyVisibility
data class CloudEligibleEnvelope internal constructor(
    val eventId: EventId,
    val kind: NotificationKind,
    val redactedTitle: String?,
    val redactedBody: String?,
    val contentFingerprint: String,
) {
    companion object {
        fun create(
            event: NotificationEvent,
            decision: FilterDecision,
            signals: CaptureSignals,
            redactionConfidence: Double,
        ): CloudEligibleEnvelope? {
            if (decision !is FilterDecision.CloudEligible) return null
            if (signals.hadRemoteViews) return null
            if (signals.hadPendingIntent) return null
            if (signals.hadSpans) return null
            if (signals.wasOversized) return null
            if (signals.sourceUserSerial != app.projectzero.domain.DomainBounds.PERSONAL_USER_SERIAL) return null
            if (redactionConfidence < RoutingThresholds.REDACTION_MIN) return null
            val redactedTitle = Redactor.redact(event.title)
            val redactedBody = Redactor.redact(event.body)
            if (redactedTitle.confidence < RoutingThresholds.REDACTION_MIN ||
                redactedBody.confidence < RoutingThresholds.REDACTION_MIN
            ) {
                return null
            }
            return CloudEligibleEnvelope(
                eventId = event.eventId,
                kind = event.kind,
                redactedTitle = redactedTitle.text,
                redactedBody = redactedBody.text,
                contentFingerprint = event.contentFingerprint,
            )
        }
    }
}

data class RedactionResult(val text: String?, val confidence: Double)

object Redactor {
    private val email = Regex("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", RegexOption.IGNORE_CASE)
    private val phone = Regex("\\+?\\d[\\d\\s().-]{8,}\\d")
    private val url = Regex("(?i)https?://\\S+")
    private val longNumber = Regex("\\b\\d{7,}\\b")

    fun redact(input: String?): RedactionResult {
        if (input == null) return RedactionResult(null, 1.0)
        var person = 0
        var text = input
        var replacements = 0
        text = email.replace(text) {
            replacements++
            "<PERSON_${++person}>"
        }
        text = url.replace(text) {
            replacements++
            "<URL>"
        }
        text = phone.replace(text) {
            replacements++
            "<PHONE>"
        }
        text = longNumber.replace(text) {
            replacements++
            "<ID>"
        }
        val residualPii = email.containsMatchIn(text) || phone.containsMatchIn(text) || url.containsMatchIn(text)
        val confidence = if (residualPii) 0.0 else 1.0
        return RedactionResult(text, confidence)
    }

    fun reasonIfDenied(input: String?): ReasonCode? {
        val result = redact(input)
        return if (result.confidence < RoutingThresholds.REDACTION_MIN) ReasonCode.REDACTION_LOW_CONFIDENCE else null
    }
}
