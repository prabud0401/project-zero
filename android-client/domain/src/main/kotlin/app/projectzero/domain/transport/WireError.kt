package app.projectzero.domain.transport

import app.projectzero.domain.DomainInvariantException
import app.projectzero.domain.ReasonCode
import app.projectzero.domain.RequestId
import app.projectzero.domain.Validation

data class WireErrorMapping(
    val httpStatus: Int,
    val grpcStatus: String,
    val reasonCode: ReasonCode,
) {
    init {
        if (httpStatus < 400 || httpStatus > 599) {
            throw DomainInvariantException(ReasonCode.SCHEMA_INVALID, "httpStatus must be 4xx/5xx")
        }
        Validation.requireNonBlank(grpcStatus, "grpcStatus")
    }
}

data class WireError(
    val schemaVersion: Int,
    val requestId: RequestId,
    val code: ReasonCode,
    val retryable: Boolean,
    val retryAfterMs: Long?,
    val message: String,
) {
    init {
        Validation.requireSchemaV1(schemaVersion)
        retryAfterMs?.let { Validation.requireNonNegative(it, "retryAfterMs") }
        Validation.requireNonBlank(message, "message")
        Validation.requireCodePointsAtMost(message, 160, "message")
    }
}

object WireErrorCatalog {
    val mappings: List<WireErrorMapping> = listOf(
        WireErrorMapping(400, "INVALID_ARGUMENT", ReasonCode.SCHEMA_INVALID),
        WireErrorMapping(401, "UNAUTHENTICATED", ReasonCode.AUTH_FAILED),
        WireErrorMapping(403, "PERMISSION_DENIED", ReasonCode.AUTH_FAILED),
        WireErrorMapping(409, "ABORTED", ReasonCode.STALE_REVISION),
        WireErrorMapping(409, "ALREADY_EXISTS", ReasonCode.CONFLICT),
        WireErrorMapping(429, "RESOURCE_EXHAUSTED", ReasonCode.RATE_LIMITED),
        WireErrorMapping(429, "RESOURCE_EXHAUSTED", ReasonCode.BUDGET_EXHAUSTED),
        WireErrorMapping(503, "UNAVAILABLE", ReasonCode.UPSTREAM_UNAVAILABLE),
    )
}
