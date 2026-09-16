package app.projectzero.domain.action

import app.projectzero.domain.ActionId
import app.projectzero.domain.DomainInvariantException
import app.projectzero.domain.ReasonCode
import app.projectzero.domain.Validation

/**
 * H03: ordinary intents cannot guarantee exactly-once completion in another app.
 * Locally allow at most one launch attempt per actionId. HANDED_OFF means Android
 * accepted a launch, not that a message was sent. Ambiguous dispatch is OUTCOME_UNKNOWN.
 */
enum class ActionAttemptOutcome {
    HANDED_OFF,
    REJECTED,
    EXPIRED,
    LOCKED,
    RESOLUTION_CHANGED,
    CONFIRMATION_MISSING,
    CONFIRMATION_CONSUMED,
    OUTCOME_UNKNOWN,
    ;

    val wireValue: String get() = name

    companion object {
        fun fromWire(value: String): ActionAttemptOutcome =
            entries.find { it.name == value }
                ?: throw DomainInvariantException(ReasonCode.INVALID_ENUM, "Unknown action outcome: $value")
    }
}

enum class ActionProposalState {
    CAPTURED,
    PARSED,
    RESOLVED,
    PREVIEWED,
    CONFIRMATION_CONSUMED,
    DISPATCHED,
    CLARIFY_OR_REJECT,
    UNAVAILABLE,
    REJECTED,
    ;

    val wireValue: String get() = name
}

data class ActionAttempt(
    val actionId: ActionId,
    val previewDigest: String,
    val confirmationConsumed: Boolean,
    val outcome: ActionAttemptOutcome?,
    val createdAtElapsedMs: Long,
    val expiresAtElapsedMs: Long,
) {
    init {
        Validation.requireFingerprint(previewDigest, "previewDigest")
        Validation.requireNonNegative(createdAtElapsedMs, "createdAtElapsedMs")
        Validation.requireNonNegative(expiresAtElapsedMs, "expiresAtElapsedMs")
        if (expiresAtElapsedMs < createdAtElapsedMs) {
            throw DomainInvariantException(ReasonCode.EXPIRY_INVALID, "elapsed expiry must be >= created")
        }
        if (expiresAtElapsedMs - createdAtElapsedMs > Validation.ACTION_TTL_MS) {
            throw DomainInvariantException(ReasonCode.EXPIRY_INVALID, "elapsed expiry exceeds 5 minutes")
        }
        if (outcome != null && !confirmationConsumed && outcome == ActionAttemptOutcome.HANDED_OFF) {
            throw DomainInvariantException(
                ReasonCode.CONFIRMATION_MISSING,
                "HANDED_OFF requires confirmation to have been consumed",
            )
        }
    }

    fun canDispatch(nowElapsedMs: Long): ReasonCode? {
        if (outcome != null) return ReasonCode.CONFIRMATION_CONSUMED
        if (!confirmationConsumed) return ReasonCode.CONFIRMATION_MISSING
        if (nowElapsedMs > expiresAtElapsedMs) return ReasonCode.ACTION_EXPIRED
        return null
    }
}
