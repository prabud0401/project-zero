package app.projectzero.domain.identity

import app.projectzero.domain.DataEpoch
import app.projectzero.domain.DomainInvariantException
import app.projectzero.domain.EventId
import app.projectzero.domain.EventRevision
import app.projectzero.domain.ReasonCode
import app.projectzero.domain.RequestId
import app.projectzero.domain.SnapshotRevision
import app.projectzero.domain.Validation

/**
 * H02: callbacks are identified by local profile/user scope plus framework notification key.
 * Event revision is per notification lifecycle; snapshotRevision/dataEpoch are store-level.
 */
data class NotificationIdentity(
    val profileScope: String,
    val frameworkNotificationKey: String,
    val eventId: EventId,
) {
    init {
        Validation.requireNonBlank(profileScope, "profileScope")
        Validation.requireNonBlank(frameworkNotificationKey, "frameworkNotificationKey")
    }
}

data class EventLifecycle(
    val eventId: EventId,
    val eventRevision: EventRevision,
)

data class StoreCursor(
    val snapshotRevision: SnapshotRevision,
    val dataEpoch: DataEpoch,
)

data class PendingCloudRequest(
    val requestId: RequestId,
    val dataEpoch: DataEpoch,
    val snapshotRevision: SnapshotRevision,
    val memberEventIds: List<EventId>,
    val pending: Boolean,
) {
    init {
        if (memberEventIds.isEmpty()) {
            throw DomainInvariantException(ReasonCode.MEMBERSHIP_INVALID, "pending members must not be empty")
        }
        if (memberEventIds.toSet().size != memberEventIds.size) {
            throw DomainInvariantException(ReasonCode.MEMBERSHIP_INVALID, "pending members must be unique")
        }
    }
}

object SnapshotApplyPolicy {
    /**
     * Apply a cloud result only while the request remains pending, epoch and snapshot match,
     * and every member remains live and eligible.
     */
    fun shouldApply(
        request: PendingCloudRequest,
        current: StoreCursor,
        liveEligibleMembers: Set<EventId>,
    ): ApplyDecision {
        if (!request.pending) {
            return ApplyDecision.Reject(ReasonCode.REQUEST_NOT_PENDING)
        }
        if (request.dataEpoch != current.dataEpoch) {
            return ApplyDecision.Reject(ReasonCode.DATA_EPOCH_MISMATCH)
        }
        if (request.snapshotRevision != current.snapshotRevision) {
            return ApplyDecision.Reject(ReasonCode.SNAPSHOT_REVISION_MISMATCH)
        }
        if (!liveEligibleMembers.containsAll(request.memberEventIds)) {
            return ApplyDecision.Reject(ReasonCode.MEMBER_NOT_LIVE)
        }
        return ApplyDecision.Apply
    }
}

sealed interface ApplyDecision {
    data object Apply : ApplyDecision
    data class Reject(val reasonCode: ReasonCode) : ApplyDecision
}
