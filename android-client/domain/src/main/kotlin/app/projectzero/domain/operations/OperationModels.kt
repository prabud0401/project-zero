package app.projectzero.domain.operations

import app.projectzero.domain.DomainBounds
import app.projectzero.domain.DomainInvariantException
import app.projectzero.domain.InstallationId
import app.projectzero.domain.ReasonCode
import app.projectzero.domain.RequestId
import app.projectzero.domain.Validation

enum class OperationState {
    RECEIVED,
    RESERVED,
    DISPATCHED,
    SUCCEEDED,
    FAILED_SAFE,
    OUTCOME_UNKNOWN,
    ;

    val wireValue: String get() = name

    companion object {
        fun fromWire(value: String): OperationState =
            entries.find { it.name == value }
                ?: throw DomainInvariantException(ReasonCode.INVALID_ENUM, "Unknown operation state: $value")
    }
}

enum class ReservationState {
    HELD,
    SETTLED,
    UNKNOWN_USAGE_ACCOUNTED,
    ;

    val wireValue: String get() = name
}

data class OperationKey(
    val installationId: InstallationId,
    val endpoint: String,
    val requestId: RequestId,
) {
    init {
        Validation.requireNonBlank(endpoint, "endpoint")
    }
}

data class BudgetReservation(
    val reservedCostMicros: Long,
    val settledCostMicros: Long?,
    val state: ReservationState,
) {
    init {
        Validation.requireNonNegative(reservedCostMicros, "reservedCostMicros")
        settledCostMicros?.let { Validation.requireNonNegative(it, "settledCostMicros") }
        if (state == ReservationState.SETTLED && settledCostMicros == null) {
            throw DomainInvariantException(ReasonCode.SCHEMA_INVALID, "SETTLED reservation requires settledCostMicros")
        }
        if (settledCostMicros != null && settledCostMicros > reservedCostMicros) {
            throw DomainInvariantException(ReasonCode.BOUNDS_EXCEEDED, "settled cost cannot exceed reserved maximum")
        }
    }
}

data class OperationRecord(
    val key: OperationKey,
    val payloadDigest: String,
    val state: OperationState,
    val reservation: BudgetReservation,
    val createdAtEpochMs: Long,
    val retainUntilEpochMs: Long,
) {
    init {
        Validation.requireFingerprint(payloadDigest, "payloadDigest")
        Validation.requireEpochMs(createdAtEpochMs, "createdAtEpochMs")
        Validation.requireEpochMs(retainUntilEpochMs, "retainUntilEpochMs")
        if (retainUntilEpochMs - createdAtEpochMs < DomainBounds.OPERATION_RETENTION_MS) {
            throw DomainInvariantException(
                ReasonCode.TIMESTAMP_INVALID,
                "operation records must be retained at least 10 minutes",
            )
        }
    }
}

sealed interface OperationJoin {
    data class Same(val record: OperationRecord) : OperationJoin
    data class Conflict(val reasonCode: ReasonCode = ReasonCode.CONFLICT) : OperationJoin
}

object OperationIdempotency {
    fun joinOrConflict(existing: OperationRecord, incomingKey: OperationKey, incomingDigest: String): OperationJoin {
        if (existing.key != incomingKey) {
            throw DomainInvariantException(ReasonCode.SCHEMA_INVALID, "operation keys do not match")
        }
        return if (existing.payloadDigest == incomingDigest) {
            OperationJoin.Same(existing)
        } else {
            OperationJoin.Conflict()
        }
    }
}
