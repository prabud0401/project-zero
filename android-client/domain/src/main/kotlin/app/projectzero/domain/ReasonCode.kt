package app.projectzero.domain

/**
 * Closed, machine-readable reason taxonomy for Task 1 contracts.
 * Wire values are the enum names and MUST remain stable.
 */
enum class ReasonCode {
    // Construction / schema
    SCHEMA_INVALID,
    UNSUPPORTED_SCHEMA,
    UNKNOWN_FIELD,
    DUPLICATE_KEY,
    DUPLICATE_PARAMETER,
    NON_FINITE_NUMBER,
    OVERSIZED,
    INVALID_UTF8,
    INVALID_ENUM,
    INVALID_UUID,
    UNICODE_LIMIT,
    TIMESTAMP_INVALID,
    EXPIRY_INVALID,
    CONFIDENCE_INVALID,
    MEMBERSHIP_INVALID,
    BOUNDS_EXCEEDED,

    // Filter / privacy (H05)
    CLOUD_ELIGIBLE,
    LOCAL_ONLY,
    DROP,
    SECRET,
    OTP,
    FINANCIAL,
    HEALTH,
    PRECISE_LOCATION,
    PASSWORD,
    MINORS_POLICY,
    MANAGED_PROFILE,
    WORK_POLICY,
    CLOUD_DISABLED,
    CONSENT_MISSING,
    REDACTION_FAILED,
    REDACTION_LOW_CONFIDENCE,
    NOTIFICATION_ACCESS_DENIED,

    // Routing / parse
    PARSED,
    PROPOSED,
    CLARIFICATION_REQUIRED,
    UNSUPPORTED_GRAMMAR,
    LOW_CONFIDENCE,
    AMBIGUOUS,
    MISSING_SLOT,
    CONTRADICTORY_SLOTS,
    CAPABILITY_TIE,
    LOCAL_CACHE_HIT,
    COMPLEXITY_TRIGGER,

    // Policy / execution (H03)
    POLICY_ALLOWED,
    POLICY_DENIED,
    HANDED_OFF,
    REJECTED,
    ACTION_EXPIRED,
    PREVIEW_DIGEST_MISMATCH,
    DEVICE_LOCKED,
    RESOLUTION_CHANGED,
    CONFIRMATION_MISSING,
    CONFIRMATION_CONSUMED,
    CHOOSER_OWNS_TARGET,
    OUTCOME_UNKNOWN,

    // Identity / snapshot (H02)
    DUPLICATE_EVENT,
    STALE_REVISION,
    DATA_EPOCH_MISMATCH,
    SNAPSHOT_REVISION_MISMATCH,
    MEMBER_NOT_LIVE,
    REQUEST_NOT_PENDING,
    INCOMPLETE_HISTORY,

    // Operations / budget (H10)
    CONFLICT,
    RATE_LIMITED,
    BUDGET_EXHAUSTED,
    RESERVATION_HELD,
    LEDGER_UNAVAILABLE,

    // Transport
    AUTH_FAILED,
    CLOCK_SKEW,
    CANCELLED,
    UPSTREAM_UNAVAILABLE,
    UNSUPPORTED_LOCALE,
    PRIVACY_DENIED,
    VALIDATION_DENIED,
    CIRCUIT_OPEN,
    NETWORK_UNAVAILABLE,
    ;

    val wireValue: String get() = name

    companion object {
        fun fromWire(value: String): ReasonCode =
            entries.find { it.name == value }
                ?: throw DomainInvariantException(INVALID_ENUM, "Unknown reason code: $value")
    }
}
