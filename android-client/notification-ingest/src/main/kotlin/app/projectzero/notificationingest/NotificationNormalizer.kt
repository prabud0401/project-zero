package app.projectzero.notificationingest

import android.app.Notification
import android.os.UserManager
import android.service.notification.StatusBarNotification
import android.text.Spanned
import app.projectzero.domain.DomainBounds
import app.projectzero.localai.ContentFingerprinter
import app.projectzero.localai.EpochClock
import app.projectzero.localai.TextNormalizer

class NotificationNormalizer(
    private val userManager: UserManager?,
    private val fingerprinter: ContentFingerprinter,
    private val clock: EpochClock = EpochClock.System,
    private val personalSerial: Long = DomainBounds.PERSONAL_USER_SERIAL,
) {
    fun normalize(sbn: StatusBarNotification): NormalizedCapture {
        val notification = sbn.notification
        val extras = notification.extras
        val titleRaw = extras.getCharSequence(Notification.EXTRA_TITLE)
            ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)
            ?: extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)
        val bodyRaw = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_INFO_TEXT)
            ?: joinLines(extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES))
        val titleNorm = TextNormalizer.normalizeTitle(titleRaw, titleRaw is Spanned)
        val bodyNorm = TextNormalizer.normalizeBody(bodyRaw, bodyRaw is Spanned)
        val people = peopleTokens(extras)
        val userSerial = resolveUserSerial(sbn)
        val hadRemoteViews = notification.contentView != null ||
            notification.bigContentView != null ||
            notification.headsUpContentView != null
        val hadPendingIntent = notification.contentIntent != null ||
            notification.deleteIntent != null ||
            notification.fullScreenIntent != null
        return NormalizedCapture(
            frameworkKey = sbn.key,
            sourcePackage = sbn.packageName,
            sourceUserSerial = userSerial,
            channelId = notification.channelId,
            groupKey = notification.group,
            title = titleNorm.value,
            body = bodyNorm.value,
            peopleTokens = people,
            isOngoing = notification.flags and Notification.FLAG_ONGOING_EVENT != 0,
            isClearable = notification.flags and Notification.FLAG_NO_CLEAR == 0,
            postedAtEpochMs = sbn.postTime.coerceAtLeast(0L),
            observedAtEpochMs = clock.nowMs(),
            category = notification.category,
            hadRemoteViews = hadRemoteViews,
            hadPendingIntent = hadPendingIntent,
            hadSpans = titleNorm.hadSpans || bodyNorm.hadSpans,
            wasOversized = titleNorm.wasOversized || bodyNorm.wasOversized,
        )
    }

    fun normalizeIdempotent(capture: NormalizedCapture): NormalizedCapture {
        val title = TextNormalizer.normalizeTitle(capture.title)
        val body = TextNormalizer.normalizeBody(capture.body)
        return capture.copy(
            title = title.value,
            body = body.value,
            wasOversized = capture.wasOversized || title.wasOversized || body.wasOversized,
            hadSpans = capture.hadSpans || title.hadSpans || body.hadSpans,
        )
    }

    private fun resolveUserSerial(sbn: StatusBarNotification): Long {
        val handle = sbn.user
        val serial = try {
            userManager?.getSerialNumberForUser(handle)
        } catch (_: Exception) {
            null
        }
        if (serial != null && serial >= 0L) return serial
        return if (handle == android.os.Process.myUserHandle()) personalSerial else personalSerial + 10L
    }

    private fun peopleTokens(extras: android.os.Bundle): Set<String> {
        val names = mutableListOf<String>()
        extras.getStringArray(Notification.EXTRA_PEOPLE)?.forEach { names += it }
        runCatching {
            extras.getStringArrayList(Notification.EXTRA_PEOPLE_LIST)?.forEach { names += it }
        }
        return names.asSequence()
            .map { TextNormalizer.stripControls(it) }
            .filter { it.isNotBlank() }
            .map { fingerprinter.hmac("person", it) }
            .take(DomainBounds.MAX_PEOPLE_TOKENS)
            .toSet()
    }

    private fun joinLines(lines: Array<CharSequence>?): CharSequence? {
        if (lines.isNullOrEmpty()) return null
        return lines.joinToString("\n") { it.toString() }
    }
}
