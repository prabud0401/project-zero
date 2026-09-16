package app.projectzero.domain

@JvmInline
value class EventId private constructor(val value: String) {
    companion object {
        fun parse(value: String): EventId = EventId(Validation.requireUuidV7(value, "eventId"))
    }
}

@JvmInline
value class ClusterId private constructor(val value: String) {
    companion object {
        fun parse(value: String): ClusterId = ClusterId(Validation.requireUuidV7(value, "clusterId"))
    }
}

@JvmInline
value class ActionId private constructor(val value: String) {
    companion object {
        fun parse(value: String): ActionId = ActionId(Validation.requireUuidV7(value, "actionId"))
    }
}

@JvmInline
value class RequestId private constructor(val value: String) {
    companion object {
        fun parse(value: String): RequestId = RequestId(Validation.requireUuidV7(value, "requestId"))
    }
}

@JvmInline
value class InstallationId private constructor(val value: String) {
    companion object {
        fun parse(value: String): InstallationId =
            InstallationId(Validation.requireUuidV7(value, "installationId"))
    }
}

/** Per-notification lifecycle counter. Distinct from [SnapshotRevision]. */
@JvmInline
value class EventRevision private constructor(val value: Long) {
    companion object {
        fun of(value: Long): EventRevision = EventRevision(Validation.requireRevision(value, "eventRevision"))
    }
}

/** Personal-profile notification store revision. SummaryBatchRequest.baseRevision refers to this. */
@JvmInline
value class SnapshotRevision private constructor(val value: Long) {
    companion object {
        fun of(value: Long): SnapshotRevision =
            SnapshotRevision(Validation.requireRevision(value, "snapshotRevision"))
    }
}

/** Advances on erase, listener revocation, and store reset. Invalidates outstanding requests. */
@JvmInline
value class DataEpoch private constructor(val value: Long) {
    companion object {
        fun of(value: Long): DataEpoch = DataEpoch(Validation.requireRevision(value, "dataEpoch"))
    }
}

@JvmInline
value class ConsentEpoch private constructor(val value: Long) {
    companion object {
        fun of(value: Long): ConsentEpoch = ConsentEpoch(Validation.requireRevision(value, "consentEpoch"))
    }
}
