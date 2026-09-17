package app.projectzero.localai

import app.projectzero.domain.EventId
import app.projectzero.domain.notification.NotificationEvent
import app.projectzero.domain.notification.NotificationKind
import app.projectzero.domain.notification.Sensitivity

object TestEvents {
    const val UUID_1 = "018f0000-0000-7000-8000-000000000001"
    const val UUID_2 = "018f0000-0000-7000-8000-000000000002"
    const val FINGERPRINT = "abcdefghijklmnopqrstuvwxyz0123456789-_ABCDE"

    fun event(
        eventId: String = UUID_1,
        revision: Long = 1,
        sourcePackage: String = "com.example.mail",
        sourceUserSerial: Long = 0,
        kind: NotificationKind = NotificationKind.MESSAGE,
        title: String? = "Hello",
        body: String? = "World",
        groupKeyHash: String? = null,
        isOngoing: Boolean = false,
        sensitivity: Sensitivity = Sensitivity.PERSONAL,
        contentFingerprint: String = FINGERPRINT,
        postedAtEpochMs: Long = 1_700_000_000_000,
        observedAtEpochMs: Long = 1_700_000_000_100,
        expiresAtEpochMs: Long = 1_700_086_400_000,
    ) = NotificationEvent(
        schemaVersion = 1,
        eventId = EventId.parse(eventId),
        revision = revision,
        postedAtEpochMs = postedAtEpochMs,
        observedAtEpochMs = observedAtEpochMs,
        sourcePackage = sourcePackage,
        sourceUserSerial = sourceUserSerial,
        channelIdHash = FINGERPRINT,
        kind = kind,
        title = title,
        body = body,
        peopleTokens = emptySet(),
        groupKeyHash = groupKeyHash,
        isOngoing = isOngoing,
        isClearable = true,
        sensitivity = sensitivity,
        contentFingerprint = contentFingerprint,
        expiresAtEpochMs = expiresAtEpochMs,
    )
}
