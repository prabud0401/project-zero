package app.projectzero.actionresolver

import app.projectzero.domain.action.AppAffinityContext
import app.projectzero.domain.action.FallbackResolution
import app.projectzero.domain.routing.RoutingThresholds

data class HandlerCandidate(
    val packageName: String,
    val exported: Boolean,
    val enabled: Boolean,
    val userPinned: Boolean = false,
    val affinity: Double = 0.0,
)

data class RankedTarget(
    val packageName: String?,
    val fallback: FallbackResolution,
    val chooserOwnsTarget: Boolean,
)

object HandlerRanking {
    fun rank(
        context: AppAffinityContext,
        handlers: List<HandlerCandidate>,
        registryPreferred: String?,
    ): RankedTarget {
        val usable = handlers.filter { it.exported && it.enabled }
        val pinned = usable.filter { it.userPinned || context.rankedPackages.any { p -> p.packageName == it.packageName && p.userPinned } }
        if (pinned.size == 1) {
            return RankedTarget(pinned.single().packageName, FallbackResolution.NONE, false)
        }
        val registryHit = usable.filter { it.packageName == registryPreferred }
        if (registryHit.size == 1 && registryPreferred != null) {
            return RankedTarget(registryPreferred, FallbackResolution.NONE, false)
        }
        val scored = usable.map { handler ->
            val affinity = context.rankedPackages.find { it.packageName == handler.packageName }?.score ?: handler.affinity
            handler to affinity
        }.sortedByDescending { it.second }
        if (scored.size >= 2 && scored[0].second - scored[1].second <= RoutingThresholds.AFFINITY_TIE_DELTA) {
            return RankedTarget(null, FallbackResolution.ANDROID_CHOOSER, true)
        }
        if (scored.size == 1) {
            return RankedTarget(scored.single().first.packageName, FallbackResolution.NONE, false)
        }
        if (scored.isNotEmpty()) {
            return RankedTarget(scored.first().first.packageName, FallbackResolution.SYSTEM_GENERIC_INTENT, false)
        }
        return RankedTarget(null, FallbackResolution.ANDROID_CHOOSER, true)
    }
}
