package app.projectzero.domain

import app.projectzero.domain.action.ActionAttempt
import app.projectzero.domain.action.ActionAttemptOutcome
import app.projectzero.domain.action.ActionCapability
import app.projectzero.domain.cache.CacheLayer
import app.projectzero.domain.cache.CachePartition
import app.projectzero.domain.cache.CacheVersionBinding
import app.projectzero.domain.identity.ApplyDecision
import app.projectzero.domain.identity.NotificationIdentity
import app.projectzero.domain.identity.PendingCloudRequest
import app.projectzero.domain.identity.SnapshotApplyPolicy
import app.projectzero.domain.identity.StoreCursor
import app.projectzero.domain.operations.BudgetReservation
import app.projectzero.domain.operations.OperationIdempotency
import app.projectzero.domain.operations.OperationJoin
import app.projectzero.domain.operations.OperationKey
import app.projectzero.domain.operations.OperationRecord
import app.projectzero.domain.operations.OperationState
import app.projectzero.domain.operations.ReservationState
import app.projectzero.domain.routing.ExecutionResult
import app.projectzero.domain.routing.FilterDecision
import app.projectzero.domain.routing.ParsedIntent
import app.projectzero.domain.routing.PolicyDecision
import app.projectzero.domain.routing.RouteResult
import app.projectzero.domain.routing.RoutingGate
import app.projectzero.domain.routing.RoutingPolicy
import app.projectzero.domain.routing.RoutingThresholds
import app.projectzero.domain.routing.SupportedGrammarRoute
import app.projectzero.domain.routing.TypedSlots
import app.projectzero.domain.transport.WireError
import app.projectzero.domain.transport.WireErrorCatalog
import app.projectzero.domain.versioning.CompatibilityVersions
import app.projectzero.domain.versioning.ProtocolSupport
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ResultAndControlTest {
    @Test
    fun sealedResultsAlwaysCarryReasonCodes() {
        val drop = FilterDecision.Drop(ReasonCode.SECRET)
        val local = FilterDecision.LocalOnly(ReasonCode.OTP)
        val cloud = FilterDecision.CloudEligible()
        assertEquals(ReasonCode.SECRET, drop.reasonCode)
        assertEquals(ReasonCode.OTP, local.reasonCode)
        assertEquals(ReasonCode.CLOUD_ELIGIBLE, cloud.reasonCode)

        val parsed = ParsedIntent.Complete(ActionCapability.MESSAGE, TypedSlots(message = "hi"), 0.9)
        val clarify = ParsedIntent.ClarificationRequired("who?", setOf("recipientToken"))
        val rejected = ParsedIntent.Rejected(ReasonCode.UNSUPPORTED_GRAMMAR)
        assertEquals(ReasonCode.PARSED, parsed.reasonCode)
        assertEquals(ReasonCode.CLARIFICATION_REQUIRED, clarify.reasonCode)
        assertEquals(ReasonCode.UNSUPPORTED_GRAMMAR, rejected.reasonCode)

        assertEquals(ReasonCode.POLICY_ALLOWED, PolicyDecision.Allow().reasonCode)
        assertEquals(ReasonCode.POLICY_DENIED, PolicyDecision.Deny(ReasonCode.POLICY_DENIED).reasonCode)

        assertEquals(ReasonCode.HANDED_OFF, ExecutionResult.HandedOff().reasonCode)
        assertEquals(ReasonCode.ACTION_EXPIRED, ExecutionResult.Rejected(ReasonCode.ACTION_EXPIRED).reasonCode)
        assertEquals(ReasonCode.OUTCOME_UNKNOWN, ExecutionResult.OutcomeUnknown().reasonCode)

        val proposed = RouteResult.Proposed(Fixtures.action(), 0.95)
        assertEquals(ReasonCode.PROPOSED, proposed.reasonCode)
        assertEquals(ReasonCode.CLARIFICATION_REQUIRED, RouteResult.Clarification("more", setOf("query")).reasonCode)
        assertEquals(ReasonCode.UNSUPPORTED_GRAMMAR, RouteResult.Unsupported(ReasonCode.UNSUPPORTED_GRAMMAR).reasonCode)
    }

    @Test
    fun routingThresholdsMatchH05() {
        assertEquals(SupportedGrammarRoute.CLARIFY_OR_REJECT, RoutingPolicy.supportedGrammarRoute(0.549))
        assertEquals(SupportedGrammarRoute.CLOUD_CANDIDATE, RoutingPolicy.supportedGrammarRoute(0.55))
        assertEquals(SupportedGrammarRoute.CLOUD_CANDIDATE, RoutingPolicy.supportedGrammarRoute(0.819))
        assertEquals(SupportedGrammarRoute.LOCAL_RESOLVE, RoutingPolicy.supportedGrammarRoute(0.82))
        assertEquals(SupportedGrammarRoute.LOCAL_RESOLVE, RoutingPolicy.supportedGrammarRoute(1.0))
        assertEquals(
            listOf(
                RoutingGate.PRIVACY_DEVICE_POLICY,
                RoutingGate.CLOUD_CONSENT,
                RoutingGate.REDACTION_POLICY,
                RoutingGate.LOCAL_RULE_OR_CACHE,
                RoutingGate.CONFIDENCE_AND_COMPLEXITY,
                RoutingGate.NETWORK_DEADLINE_BUDGET,
            ),
            RoutingPolicy.precedence,
        )
        assertEquals(0.94, RoutingThresholds.CACHE_SIMILARITY_MIN)
        assertEquals(0.98, RoutingThresholds.REDACTION_MIN)
    }

    @Test
    fun h02ApplyRequiresPendingEpochSnapshotAndLiveMembers() {
        val request = PendingCloudRequest(
            requestId = RequestId.parse(Fixtures.UUID_1),
            dataEpoch = DataEpoch.of(4),
            snapshotRevision = SnapshotRevision.of(10),
            memberEventIds = listOf(EventId.parse(Fixtures.UUID_2)),
            pending = true,
        )
        val cursor = StoreCursor(SnapshotRevision.of(10), DataEpoch.of(4))
        val live = setOf(EventId.parse(Fixtures.UUID_2))
        assertEquals(ApplyDecision.Apply, SnapshotApplyPolicy.shouldApply(request, cursor, live))
        assertEquals(
            ReasonCode.REQUEST_NOT_PENDING,
            (SnapshotApplyPolicy.shouldApply(request.copy(pending = false), cursor, live) as ApplyDecision.Reject).reasonCode,
        )
        assertEquals(
            ReasonCode.DATA_EPOCH_MISMATCH,
            (SnapshotApplyPolicy.shouldApply(request, cursor.copy(dataEpoch = DataEpoch.of(5)), live) as ApplyDecision.Reject).reasonCode,
        )
        assertEquals(
            ReasonCode.SNAPSHOT_REVISION_MISMATCH,
            (SnapshotApplyPolicy.shouldApply(request, cursor.copy(snapshotRevision = SnapshotRevision.of(11)), live) as ApplyDecision.Reject).reasonCode,
        )
        assertEquals(
            ReasonCode.MEMBER_NOT_LIVE,
            (SnapshotApplyPolicy.shouldApply(request, cursor, emptySet()) as ApplyDecision.Reject).reasonCode,
        )
        NotificationIdentity("personal", "pkg|1|tag", EventId.parse(Fixtures.UUID_1))
        assertThrows(DomainInvariantException::class.java) {
            NotificationIdentity(" ", "key", EventId.parse(Fixtures.UUID_1))
        }
    }

    @Test
    fun h03AtMostOnceDispatchAndUnknownOutcome() {
        val attempt = ActionAttempt(
            actionId = ActionId.parse(Fixtures.UUID_3),
            previewDigest = Fixtures.FINGERPRINT,
            confirmationConsumed = true,
            outcome = null,
            createdAtElapsedMs = 0,
            expiresAtElapsedMs = Validation.ACTION_TTL_MS,
        )
        assertNull(attempt.canDispatch(1_000))
        assertEquals(ReasonCode.ACTION_EXPIRED, attempt.canDispatch(Validation.ACTION_TTL_MS + 1))
        val consumed = attempt.copy(outcome = ActionAttemptOutcome.OUTCOME_UNKNOWN)
        assertEquals(ReasonCode.CONFIRMATION_CONSUMED, consumed.canDispatch(1))
        assertThrows(DomainInvariantException::class.java) {
            ActionAttempt(
                actionId = ActionId.parse(Fixtures.UUID_3),
                previewDigest = Fixtures.FINGERPRINT,
                confirmationConsumed = false,
                outcome = ActionAttemptOutcome.HANDED_OFF,
                createdAtElapsedMs = 0,
                expiresAtElapsedMs = 1,
            )
        }
        assertEquals("OUTCOME_UNKNOWN", ActionAttemptOutcome.OUTCOME_UNKNOWN.wireValue)
        assertEquals("HANDED_OFF", ActionAttemptOutcome.HANDED_OFF.wireValue)
    }

    @Test
    fun h10JoinSamePayloadAndConflictOnDigestMismatch() {
        val key = OperationKey(
            InstallationId.parse(Fixtures.UUID_1),
            "POST /v1/summary:batch",
            RequestId.parse(Fixtures.UUID_2),
        )
        val record = OperationRecord(
            key = key,
            payloadDigest = Fixtures.FINGERPRINT,
            state = OperationState.RESERVED,
            reservation = BudgetReservation(1600, null, ReservationState.HELD),
            createdAtEpochMs = 0,
            retainUntilEpochMs = DomainBounds.OPERATION_RETENTION_MS,
        )
        assertTrue(OperationIdempotency.joinOrConflict(record, key, Fixtures.FINGERPRINT) is OperationJoin.Same)
        val conflict = OperationIdempotency.joinOrConflict(record, key, "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_abcde")
        assertTrue(conflict is OperationJoin.Conflict)
        assertEquals(ReasonCode.CONFLICT, (conflict as OperationJoin.Conflict).reasonCode)
        assertThrows(DomainInvariantException::class.java) {
            OperationRecord(
                key, Fixtures.FINGERPRINT, OperationState.RECEIVED,
                BudgetReservation(1, null, ReservationState.HELD),
                0, DomainBounds.OPERATION_RETENTION_MS - 1,
            )
        }
        assertThrows(DomainInvariantException::class.java) {
            BudgetReservation(10, 11, ReservationState.SETTLED)
        }
        OperationState.entries.forEach { assertEquals(it.name, it.wireValue) }
    }

    @Test
    fun h06CachePartitionAndVersionBinding() {
        val device = CachePartition(
            CacheLayer.DEVICE_EXACT, "personal", DataEpoch.of(1), null, null,
        )
        assertEquals(CacheLayer.DEVICE_EXACT, device.layer)
        assertThrows(DomainInvariantException::class.java) {
            CachePartition(CacheLayer.SERVER_EXACT, "personal", DataEpoch.of(1), null, null)
        }
        CachePartition(
            CacheLayer.SERVER_EXACT,
            "personal",
            DataEpoch.of(1),
            InstallationId.parse(Fixtures.UUID_1),
            ConsentEpoch.of(2),
        )
        CacheVersionBinding("m1", "p1", "r1", 4, 1, "en-US", ActionCapability.MESSAGE)
        assertThrows(DomainInvariantException::class.java) {
            CacheVersionBinding("m1", "p1", "r1", 4, 1, "en_US", null)
        }
    }

    @Test
    fun h12AndH14Contracts() {
        assertEquals(256, DomainBounds.MAX_INGESTION_ITEMS)
        assertEquals(1_000, DomainBounds.MAX_ACTIVE_EVENTS_PER_PROFILE)
        assertEquals(50, DomainBounds.MAX_EVENTS_PER_SUMMARY_BATCH)
        assertEquals(1, DomainBounds.MAX_IN_FLIGHT_SUMMARY_PER_INSTALLATION)
        val versions = CompatibilityVersions(1, 1, 1, "gemini-local", "prompt-1", 3, 2, 1)
        assertTrue(ProtocolSupport.isSupported(versions.wireProtocolVersion))
        assertThrows(DomainInvariantException::class.java) {
            CompatibilityVersions(0, 1, 1, "m", "p", 1, 1, 1)
        }
        val error = WireError(1, RequestId.parse(Fixtures.UUID_1), ReasonCode.RATE_LIMITED, true, 30_000, "Slow down")
        assertEquals(ReasonCode.RATE_LIMITED, error.code)
        assertTrue(WireErrorCatalog.mappings.any { it.reasonCode == ReasonCode.BUDGET_EXHAUSTED })
        assertTrue(WireErrorCatalog.mappings.any { it.reasonCode == ReasonCode.OUTCOME_UNKNOWN }.not())
        assertTrue(ReasonCode.entries.contains(ReasonCode.CLOCK_SKEW))
        assertTrue(ReasonCode.entries.contains(ReasonCode.OUTCOME_UNKNOWN))
    }

    @Test
    fun typedSlotsPreserveEmptyStringVersusAbsent() {
        val empty = TypedSlots(message = "")
        val absent = TypedSlots()
        assertEquals(setOf("message"), empty.presentNames())
        assertTrue(absent.presentNames().isEmpty())
        assertThrows(DomainInvariantException::class.java) {
            TypedSlots(startEpochMs = 10, endEpochMs = 9)
        }
    }
}
