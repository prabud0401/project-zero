package app.projectzero.domain

import app.projectzero.domain.action.ActionCapability
import app.projectzero.domain.action.ActionParameter
import app.projectzero.domain.action.AppAffinityContext
import app.projectzero.domain.action.Confirmation
import app.projectzero.domain.action.FallbackResolution
import app.projectzero.domain.action.IntentAction
import app.projectzero.domain.action.PackageAffinity
import app.projectzero.domain.notification.NotificationEvent
import app.projectzero.domain.notification.NotificationKind
import app.projectzero.domain.notification.Sensitivity
import app.projectzero.domain.notification.SummarizedCluster
import app.projectzero.domain.notification.SummaryOrigin

object Fixtures {
    const val UUID_1 = "018f0000-0000-7000-8000-000000000001"
    const val UUID_2 = "018f0000-0000-7000-8000-000000000002"
    const val UUID_3 = "018f0000-0000-7000-8000-000000000003"
    const val UUID_V4 = "550e8400-e29b-41d4-a716-446655440000"
    const val FINGERPRINT = "abcdefghijklmnopqrstuvwxyz0123456789-_ABCDE"
    const val PACKAGE = "com.example.mail"

    fun event(
        schemaVersion: Int = 1,
        eventId: String = UUID_1,
        revision: Long = 1,
        postedAtEpochMs: Long = 1_700_000_000_000,
        observedAtEpochMs: Long = 1_700_000_000_100,
        sourcePackage: String = PACKAGE,
        sourceUserSerial: Long = 0,
        channelIdHash: String? = FINGERPRINT,
        kind: NotificationKind = NotificationKind.MESSAGE,
        title: String? = "Hello",
        body: String? = "World",
        peopleTokens: Set<String> = setOf("tok-1"),
        groupKeyHash: String? = null,
        isOngoing: Boolean = false,
        isClearable: Boolean = true,
        sensitivity: Sensitivity = Sensitivity.PERSONAL,
        contentFingerprint: String = FINGERPRINT,
        expiresAtEpochMs: Long = 1_700_086_400_000,
    ) = NotificationEvent(
        schemaVersion = schemaVersion,
        eventId = EventId.parse(eventId),
        revision = revision,
        postedAtEpochMs = postedAtEpochMs,
        observedAtEpochMs = observedAtEpochMs,
        sourcePackage = sourcePackage,
        sourceUserSerial = sourceUserSerial,
        channelIdHash = channelIdHash,
        kind = kind,
        title = title,
        body = body,
        peopleTokens = peopleTokens,
        groupKeyHash = groupKeyHash,
        isOngoing = isOngoing,
        isClearable = isClearable,
        sensitivity = sensitivity,
        contentFingerprint = contentFingerprint,
        expiresAtEpochMs = expiresAtEpochMs,
    )

    fun cluster(
        schemaVersion: Int = 1,
        clusterId: String = UUID_2,
        baseRevision: Long = 3,
        memberEventIds: List<String> = listOf(UUID_1),
        headline: String = "Inbox",
        summary: String = "One new message",
        kind: NotificationKind = NotificationKind.MESSAGE,
        priority: Int = 50,
        unreadCount: Int = 1,
        generatedBy: SummaryOrigin = SummaryOrigin.RULE,
        confidence: Double = 0.9,
        generatedAtEpochMs: Long = 1_700_000_000_000,
        expiresAtEpochMs: Long = 1_700_086_400_000,
    ) = SummarizedCluster(
        schemaVersion = schemaVersion,
        clusterId = ClusterId.parse(clusterId),
        baseRevision = baseRevision,
        memberEventIds = memberEventIds.map(EventId::parse),
        headline = headline,
        summary = summary,
        kind = kind,
        priority = priority,
        unreadCount = unreadCount,
        generatedBy = generatedBy,
        confidence = confidence,
        generatedAtEpochMs = generatedAtEpochMs,
        expiresAtEpochMs = expiresAtEpochMs,
    )

    fun action(
        schemaVersion: Int = 1,
        actionId: String = UUID_3,
        capability: ActionCapability = ActionCapability.MESSAGE,
        androidAction: String = "android.intent.action.SENDTO",
        targetPackage: String? = PACKAGE,
        uriScheme: String? = "smsto",
        registryEntryId: String? = "reg-1:sms",
        parameters: List<ActionParameter> = listOf(
            ActionParameter.Text("body", "late by 10"),
            ActionParameter.Text("recipientToken", "maya"),
        ),
        fallbackResolution: FallbackResolution = FallbackResolution.SYSTEM_GENERIC_INTENT,
        confirmation: Confirmation = Confirmation.TARGET_APP_OWNS_COMMIT,
        rationale: String = "Send SMS",
        createdAtEpochMs: Long = 1_000,
        expiresAtEpochMs: Long = 1_000 + Validation.ACTION_TTL_MS,
    ) = IntentAction(
        schemaVersion = schemaVersion,
        actionId = ActionId.parse(actionId),
        capability = capability,
        androidAction = androidAction,
        targetPackage = targetPackage,
        uriScheme = uriScheme,
        registryEntryId = registryEntryId,
        parameters = parameters,
        fallbackResolution = fallbackResolution,
        confirmation = confirmation,
        rationale = rationale,
        createdAtEpochMs = createdAtEpochMs,
        expiresAtEpochMs = expiresAtEpochMs,
    )

    fun affinity(
        schemaVersion: Int = 1,
        capability: ActionCapability = ActionCapability.MESSAGE,
        rankedPackages: List<PackageAffinity> = listOf(
            PackageAffinity(PACKAGE, true, 4, 1_700_000_000_000, 0.8),
        ),
        updatedAtEpochMs: Long = 1_700_000_000_000,
    ) = AppAffinityContext(
        schemaVersion = schemaVersion,
        capability = capability,
        rankedPackages = rankedPackages,
        updatedAtEpochMs = updatedAtEpochMs,
    )
}
