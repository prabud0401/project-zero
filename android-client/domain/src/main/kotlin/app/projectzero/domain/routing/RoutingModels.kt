package app.projectzero.domain.routing

import app.projectzero.domain.DomainBounds
import app.projectzero.domain.DomainInvariantException
import app.projectzero.domain.ReasonCode
import app.projectzero.domain.Validation
import app.projectzero.domain.action.ActionCapability
import app.projectzero.domain.action.IntentAction

object RoutingThresholds {
    const val SUPPORTED_CLARIFY_BELOW = 0.55
    const val SUPPORTED_CLOUD_MIN = 0.55
    const val SUPPORTED_LOCAL_MIN = 0.82
    const val CACHE_SIMILARITY_MIN = 0.94
    const val REDACTION_MIN = 0.98
    const val CAPABILITY_TIE_DELTA = 0.12
    const val AFFINITY_TIE_DELTA = 0.05
}

enum class RoutingGate {
    PRIVACY_DEVICE_POLICY,
    CLOUD_CONSENT,
    REDACTION_POLICY,
    LOCAL_RULE_OR_CACHE,
    CONFIDENCE_AND_COMPLEXITY,
    NETWORK_DEADLINE_BUDGET,
    ;

    val wireValue: String get() = name
}

enum class SupportedGrammarRoute {
    CLARIFY_OR_REJECT,
    CLOUD_CANDIDATE,
    LOCAL_RESOLVE,
    ;

    val wireValue: String get() = name
}

object RoutingPolicy {
    /** H05: first terminal privacy/consent/redaction gate wins; complexity never overrides below-0.55. */
    val precedence: List<RoutingGate> = RoutingGate.entries.toList()

    fun supportedGrammarRoute(confidence: Double): SupportedGrammarRoute {
        val value = Validation.requireFiniteUnitInterval(confidence, "confidence")
        return when {
            value < RoutingThresholds.SUPPORTED_CLARIFY_BELOW -> SupportedGrammarRoute.CLARIFY_OR_REJECT
            value < RoutingThresholds.SUPPORTED_LOCAL_MIN -> SupportedGrammarRoute.CLOUD_CANDIDATE
            else -> SupportedGrammarRoute.LOCAL_RESOLVE
        }
    }
}

data class TypedSlots(
    val recipientToken: String? = null,
    val message: String? = null,
    val destination: String? = null,
    val title: String? = null,
    val startEpochMs: Long? = null,
    val endEpochMs: Long? = null,
    val zoneId: String? = null,
    val query: String? = null,
) {
    init {
        recipientToken?.let {
            Validation.requireCodePointsAtMost(it, DomainBounds.MAX_SLOT_RECIPIENT_CODE_POINTS, "recipientToken")
        }
        message?.let { Validation.requireCodePointsAtMost(it, DomainBounds.MAX_BODY_CODE_POINTS, "message") }
        destination?.let {
            Validation.requireCodePointsAtMost(it, DomainBounds.MAX_SLOT_DESTINATION_CODE_POINTS, "destination")
        }
        title?.let { Validation.requireCodePointsAtMost(it, DomainBounds.MAX_TITLE_CODE_POINTS, "title") }
        startEpochMs?.let { Validation.requireEpochMs(it, "startEpochMs") }
        endEpochMs?.let { Validation.requireEpochMs(it, "endEpochMs") }
        zoneId?.let { Validation.requireCodePointsAtMost(it, DomainBounds.MAX_SLOT_ZONE_CODE_POINTS, "zoneId") }
        query?.let { Validation.requireCodePointsAtMost(it, DomainBounds.MAX_SLOT_QUERY_CODE_POINTS, "query") }
        if (startEpochMs != null && endEpochMs != null && endEpochMs < startEpochMs) {
            throw DomainInvariantException(ReasonCode.CONTRADICTORY_SLOTS, "endEpochMs must be >= startEpochMs")
        }
    }

    fun presentNames(): Set<String> = buildSet {
        if (recipientToken != null) add("recipientToken")
        if (message != null) add("message")
        if (destination != null) add("destination")
        if (title != null) add("title")
        if (startEpochMs != null) add("startEpochMs")
        if (endEpochMs != null) add("endEpochMs")
        if (zoneId != null) add("zoneId")
        if (query != null) add("query")
    }
}

sealed interface FilterDecision {
    val reasonCode: ReasonCode

    data class CloudEligible(
        override val reasonCode: ReasonCode = ReasonCode.CLOUD_ELIGIBLE,
    ) : FilterDecision

    data class LocalOnly(
        override val reasonCode: ReasonCode,
    ) : FilterDecision

    data class Drop(
        override val reasonCode: ReasonCode,
    ) : FilterDecision
}

sealed interface ParsedIntent {
    val reasonCode: ReasonCode

    data class Complete(
        val capability: ActionCapability,
        val slots: TypedSlots,
        val confidence: Double,
        val missingSlots: Set<String> = emptySet(),
        override val reasonCode: ReasonCode = ReasonCode.PARSED,
    ) : ParsedIntent {
        init {
            Validation.requireFiniteUnitInterval(confidence, "confidence")
        }
    }

    data class ClarificationRequired(
        val prompt: String,
        val requiredSlots: Set<String>,
        override val reasonCode: ReasonCode = ReasonCode.CLARIFICATION_REQUIRED,
    ) : ParsedIntent {
        init {
            Validation.requireNonBlank(prompt, "prompt")
            if (requiredSlots.isEmpty()) {
                throw DomainInvariantException(ReasonCode.MISSING_SLOT, "requiredSlots must not be empty")
            }
        }
    }

    data class Rejected(
        override val reasonCode: ReasonCode,
    ) : ParsedIntent
}

sealed interface PolicyDecision {
    val reasonCode: ReasonCode

    data class Allow(
        override val reasonCode: ReasonCode = ReasonCode.POLICY_ALLOWED,
    ) : PolicyDecision

    data class Deny(
        override val reasonCode: ReasonCode,
    ) : PolicyDecision
}

sealed interface ExecutionResult {
    val reasonCode: ReasonCode

    data class HandedOff(
        override val reasonCode: ReasonCode = ReasonCode.HANDED_OFF,
    ) : ExecutionResult

    data class Rejected(
        override val reasonCode: ReasonCode,
    ) : ExecutionResult

    data class OutcomeUnknown(
        override val reasonCode: ReasonCode = ReasonCode.OUTCOME_UNKNOWN,
    ) : ExecutionResult
}

sealed interface RouteResult {
    val reasonCode: ReasonCode

    data class Proposed(
        val action: IntentAction,
        val confidence: Double,
        override val reasonCode: ReasonCode = ReasonCode.PROPOSED,
    ) : RouteResult {
        init {
            Validation.requireFiniteUnitInterval(confidence, "confidence")
        }
    }

    data class Clarification(
        val prompt: String,
        val requiredSlots: Set<String>,
        override val reasonCode: ReasonCode = ReasonCode.CLARIFICATION_REQUIRED,
    ) : RouteResult {
        init {
            Validation.requireNonBlank(prompt, "prompt")
        }
    }

    data class Unsupported(
        override val reasonCode: ReasonCode,
    ) : RouteResult
}
