package app.projectzero.localai

import app.projectzero.domain.DomainBounds
import app.projectzero.domain.ReasonCode
import app.projectzero.domain.notification.NotificationEvent
import app.projectzero.domain.notification.Sensitivity
import app.projectzero.domain.ports.NotificationFilter
import app.projectzero.domain.routing.FilterDecision

class LocalPrivacyFilter(
    private val personalUserSerial: Long = DomainBounds.PERSONAL_USER_SERIAL,
) : NotificationFilter {
    override suspend fun evaluate(event: NotificationEvent): FilterDecision {
        val classification = PrivacyClassifier.classify(
            title = event.title,
            body = event.body,
            signals = CaptureSignals(
                sourceUserSerial = event.sourceUserSerial,
                packageName = event.sourcePackage,
            ),
        )
        return decide(event, classification, CaptureSignals(sourceUserSerial = event.sourceUserSerial))
    }

    fun decide(
        event: NotificationEvent,
        classification: ClassificationResult,
        signals: CaptureSignals,
    ): FilterDecision {
        if (event.sourceUserSerial != personalUserSerial || ReasonCode.MANAGED_PROFILE in classification.reasons) {
            return FilterDecision.LocalOnly(ReasonCode.MANAGED_PROFILE)
        }
        if (event.sensitivity == Sensitivity.SECRET || ReasonCode.SECRET in classification.reasons) {
            val reason = classification.primaryReason.takeIf { it != ReasonCode.CLOUD_ELIGIBLE } ?: ReasonCode.SECRET
            return FilterDecision.LocalOnly(reason)
        }
        if (classification.cloudProhibited ||
            signals.hadRemoteViews ||
            signals.hadPendingIntent ||
            signals.hadSpans ||
            signals.wasOversized
        ) {
            val reason = when {
                signals.wasOversized || ReasonCode.OVERSIZED in classification.reasons -> ReasonCode.OVERSIZED
                classification.reasons.isNotEmpty() -> classification.primaryReason
                else -> ReasonCode.LOCAL_ONLY
            }
            return FilterDecision.LocalOnly(reason)
        }
        return FilterDecision.CloudEligible()
    }
}
