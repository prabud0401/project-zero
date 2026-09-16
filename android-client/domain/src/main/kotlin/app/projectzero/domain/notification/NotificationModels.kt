package app.projectzero.domain.notification

import app.projectzero.domain.ClusterId
import app.projectzero.domain.DomainBounds
import app.projectzero.domain.DomainInvariantException
import app.projectzero.domain.EventId
import app.projectzero.domain.ReasonCode
import app.projectzero.domain.Validation

enum class Sensitivity {
    PUBLIC,
    PERSONAL,
    SECRET,
    ;

    val wireValue: String get() = name

    companion object {
        fun fromWire(value: String): Sensitivity =
            entries.find { it.name == value }
                ?: throw DomainInvariantException(ReasonCode.INVALID_ENUM, "Unknown sensitivity: $value")
    }
}

enum class NotificationKind {
    MESSAGE,
    SOCIAL,
    CALENDAR,
    DELIVERY,
    MEDIA,
    SYSTEM,
    OTHER,
    ;

    val wireValue: String get() = name

    companion object {
        fun fromWire(value: String): NotificationKind =
            entries.find { it.name == value }
                ?: throw DomainInvariantException(ReasonCode.INVALID_ENUM, "Unknown notification kind: $value")
    }
}

enum class SummaryOrigin {
    RULE,
    LOCAL_MODEL,
    CLOUD_MODEL,
    CACHE,
    ;

    val wireValue: String get() = name

    companion object {
        fun fromWire(value: String): SummaryOrigin =
            entries.find { it.name == value }
                ?: throw DomainInvariantException(ReasonCode.INVALID_ENUM, "Unknown summary origin: $value")
    }
}

enum class NotificationWorkState {
    OBSERVED,
    FILTERING,
    LOCAL_REDUCING,
    CLOUD_PENDING,
    RECONCILING,
    NO_OP,
    DROPPED,
    CANCELLED,
    DISCARDED,
    PUBLISHED,
    ;

    val wireValue: String get() = name
}

data class NotificationEvent(
    val schemaVersion: Int,
    val eventId: EventId,
    val revision: Long,
    val postedAtEpochMs: Long,
    val observedAtEpochMs: Long,
    val sourcePackage: String,
    val sourceUserSerial: Long,
    val channelIdHash: String?,
    val kind: NotificationKind,
    val title: String?,
    val body: String?,
    val peopleTokens: Set<String>,
    val groupKeyHash: String?,
    val isOngoing: Boolean,
    val isClearable: Boolean,
    val sensitivity: Sensitivity,
    val contentFingerprint: String,
    val expiresAtEpochMs: Long,
) {
    init {
        Validation.requireSchemaV1(schemaVersion)
        Validation.requireRevision(revision, "revision")
        Validation.requireEpochMs(postedAtEpochMs, "postedAtEpochMs")
        Validation.requireEpochMs(observedAtEpochMs, "observedAtEpochMs")
        Validation.requirePackageName(sourcePackage, "sourcePackage")
        Validation.requireOptionalFingerprint(channelIdHash, "channelIdHash")
        title?.let { Validation.requireCodePointsAtMost(it, DomainBounds.MAX_TITLE_CODE_POINTS, "title") }
        body?.let { Validation.requireCodePointsAtMost(it, DomainBounds.MAX_BODY_CODE_POINTS, "body") }
        Validation.requireSizeAtMost(peopleTokens.size, DomainBounds.MAX_PEOPLE_TOKENS, "peopleTokens")
        peopleTokens.forEach { Validation.requireNonBlank(it, "peopleToken") }
        Validation.requireOptionalFingerprint(groupKeyHash, "groupKeyHash")
        Validation.requireFingerprint(contentFingerprint, "contentFingerprint")
        Validation.requireEpochMs(expiresAtEpochMs, "expiresAtEpochMs")
    }
}

data class SummarizedCluster(
    val schemaVersion: Int,
    val clusterId: ClusterId,
    val baseRevision: Long,
    val memberEventIds: List<EventId>,
    val headline: String,
    val summary: String,
    val kind: NotificationKind,
    val priority: Int,
    val unreadCount: Int,
    val generatedBy: SummaryOrigin,
    val confidence: Double,
    val generatedAtEpochMs: Long,
    val expiresAtEpochMs: Long,
) {
    init {
        Validation.requireSchemaV1(schemaVersion)
        Validation.requireRevision(baseRevision, "baseRevision")
        if (memberEventIds.isEmpty() || memberEventIds.size > DomainBounds.MAX_EVENTS_PER_SUMMARY_BATCH) {
            throw DomainInvariantException(
                ReasonCode.MEMBERSHIP_INVALID,
                "memberEventIds must contain 1..${DomainBounds.MAX_EVENTS_PER_SUMMARY_BATCH} unique ids",
            )
        }
        if (memberEventIds.toSet().size != memberEventIds.size) {
            throw DomainInvariantException(ReasonCode.MEMBERSHIP_INVALID, "memberEventIds must be unique")
        }
        Validation.requireCodePointsIn(
            headline,
            DomainBounds.MIN_HEADLINE_CODE_POINTS,
            DomainBounds.MAX_HEADLINE_CODE_POINTS,
            "headline",
        )
        Validation.requireCodePointsIn(
            summary,
            DomainBounds.MIN_SUMMARY_CODE_POINTS,
            DomainBounds.MAX_SUMMARY_CODE_POINTS,
            "summary",
        )
        Validation.requireIntIn(priority, DomainBounds.MIN_PRIORITY, DomainBounds.MAX_PRIORITY, "priority")
        Validation.requireNonNegative(unreadCount, "unreadCount")
        Validation.requireFiniteUnitInterval(confidence, "confidence")
        Validation.requireEpochMs(generatedAtEpochMs, "generatedAtEpochMs")
        Validation.requireEpochMs(expiresAtEpochMs, "expiresAtEpochMs")
    }
}
