package app.projectzero.localai

import app.projectzero.domain.ClusterId
import app.projectzero.domain.DomainBounds
import app.projectzero.domain.EventId
import app.projectzero.domain.Validation
import app.projectzero.domain.notification.NotificationEvent
import app.projectzero.domain.notification.NotificationKind
import app.projectzero.domain.notification.SummarizedCluster
import app.projectzero.domain.notification.SummaryOrigin
import app.projectzero.domain.ports.SummaryEngine

data class ClusterKey(
    val sourcePackage: String,
    val sourceUserSerial: Long,
    val groupOrKind: String,
)

class DeterministicSummaryEngine(
    private val clock: EpochClock = EpochClock.System,
    private val fingerprinter: ContentFingerprinter? = null,
    private val clusterIdFactory: (ClusterKey) -> ClusterId = { ClusterId.parse(UuidV7.generate()) },
) : SummaryEngine {

    override suspend fun summarize(events: List<NotificationEvent>): List<SummarizedCluster> {
        val now = clock.nowMs()
        val live = events.filter { it.expiresAtEpochMs > now }
        val grouped = linkedMapOf<ClusterKey, MutableList<NotificationEvent>>()
        for (event in live) {
            val key = ClusterKey(
                sourcePackage = event.sourcePackage,
                sourceUserSerial = event.sourceUserSerial,
                groupOrKind = event.groupKeyHash ?: event.kind.name,
            )
            grouped.getOrPut(key) { mutableListOf() }.add(event)
        }
        return grouped.entries.mapNotNull { (key, members) ->
            val unique = uniqueMembers(members)
            if (unique.isEmpty()) null else toCluster(key, unique, now)
        }
    }

    fun uniqueMembers(members: List<NotificationEvent>): List<NotificationEvent> {
        val seen = LinkedHashSet<EventId>()
        val out = ArrayList<NotificationEvent>(members.size.coerceAtMost(DomainBounds.MAX_EVENTS_PER_SUMMARY_BATCH))
        for (member in members) {
            if (seen.add(member.eventId)) {
                out.add(member)
            }
            if (out.size == DomainBounds.MAX_EVENTS_PER_SUMMARY_BATCH) break
        }
        return out
    }

    private fun toCluster(key: ClusterKey, members: List<NotificationEvent>, now: Long): SummarizedCluster {
        val first = members.first()
        val memberExpiry = members.minOf { it.expiresAtEpochMs }
        val expires = minOf(now + DomainBounds.SUMMARY_RETENTION_MS, memberExpiry + DomainBounds.SUMMARY_RETENTION_MS)
        return SummarizedCluster(
            schemaVersion = Validation.SCHEMA_VERSION_V1,
            clusterId = clusterIdFor(key),
            baseRevision = 1L,
            memberEventIds = members.map { it.eventId },
            headline = headline(first, members.size),
            summary = summaryText(members),
            kind = majorityKind(members),
            priority = priority(first),
            unreadCount = members.size,
            generatedBy = SummaryOrigin.RULE,
            confidence = 1.0,
            generatedAtEpochMs = now,
            expiresAtEpochMs = expires.coerceAtLeast(now),
        )
    }

    private fun clusterIdFor(key: ClusterKey): ClusterId {
        val fp = fingerprinter
        if (fp != null) {
            val material = fp.hmac(key.sourcePackage, key.sourceUserSerial.toString(), key.groupOrKind)
            var acc = 0L
            for (ch in material) {
                acc = acc * 31 + ch.code
            }
            return ClusterId.parse(UuidV7.generate(kotlin.math.abs(acc % 1_000_000_000_000L), java.util.Random(acc)))
        }
        return clusterIdFactory(key)
    }

    private fun headline(first: NotificationEvent, count: Int): String {
        val raw = first.title?.let { TextNormalizer.stripControls(it) }.orEmpty()
        val candidate = if (count <= 1 && raw.isNotEmpty()) {
            raw
        } else {
            val label = first.sourcePackage.substringAfterLast('.')
            "$count $label notifications"
        }
        return fit(candidate, DomainBounds.MIN_HEADLINE_CODE_POINTS, DomainBounds.MAX_HEADLINE_CODE_POINTS, "Notice")
    }

    private fun summaryText(members: List<NotificationEvent>): String {
        val parts = members.mapNotNull { event ->
            listOfNotNull(event.title, event.body).joinToString(": ").takeIf { it.isNotBlank() }
        }
        val joined = parts.joinToString(" · ").ifBlank { "${members.size} notifications" }
        return fit(joined, DomainBounds.MIN_SUMMARY_CODE_POINTS, DomainBounds.MAX_SUMMARY_CODE_POINTS, "Update")
    }

    private fun majorityKind(members: List<NotificationEvent>): NotificationKind =
        members.groupingBy { it.kind }.eachCount().maxBy { it.value }.key

    private fun priority(first: NotificationEvent): Int = when (first.kind) {
        NotificationKind.MESSAGE -> 80
        NotificationKind.CALENDAR -> 70
        NotificationKind.DELIVERY -> 60
        NotificationKind.SOCIAL -> 50
        NotificationKind.MEDIA -> 30
        NotificationKind.SYSTEM -> 20
        NotificationKind.OTHER -> 40
    }

    private fun fit(value: String, min: Int, max: Int, fallback: String): String {
        val stripped = TextNormalizer.stripControls(value).ifBlank { fallback }
        val truncated = TextNormalizer.normalize(stripped, max).value ?: fallback
        return if (Validation.codePointCount(truncated) < min) fallback else truncated
    }
}
