package app.projectzero.notificationingest

import app.projectzero.domain.DomainBounds
import app.projectzero.localai.CaptureSignals

data class NormalizedCapture(
    val frameworkKey: String,
    val sourcePackage: String,
    val sourceUserSerial: Long,
    val channelId: String?,
    val groupKey: String?,
    val title: String?,
    val body: String?,
    val peopleTokens: Set<String>,
    val isOngoing: Boolean,
    val isClearable: Boolean,
    val postedAtEpochMs: Long,
    val observedAtEpochMs: Long,
    val category: String?,
    val hadRemoteViews: Boolean,
    val hadPendingIntent: Boolean,
    val hadSpans: Boolean,
    val wasOversized: Boolean,
) {
    fun signals(): CaptureSignals = CaptureSignals(
        hadRemoteViews = hadRemoteViews,
        hadPendingIntent = hadPendingIntent,
        hadSpans = hadSpans,
        wasOversized = wasOversized,
        sourceUserSerial = sourceUserSerial,
        category = category,
        packageName = sourcePackage,
    )

    fun isPersonalProfile(): Boolean = sourceUserSerial == DomainBounds.PERSONAL_USER_SERIAL
}
