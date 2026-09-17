package app.projectzero.launcher

import app.projectzero.domain.notification.Sensitivity
import app.projectzero.domain.notification.SummarizedCluster

data class RedactedClusterView(
    val headline: String,
    val summary: String,
    val redacted: Boolean,
)

object LockScreenRedactor {
    const val LOCKED_HEADLINE = "Notification hidden"
    const val LOCKED_SUMMARY = "Unlock to view"

    fun forDisplay(
        headline: String,
        summary: String,
        sensitivity: Sensitivity,
        deviceLocked: Boolean,
    ): RedactedClusterView {
        val hide = deviceLocked && sensitivity != Sensitivity.PUBLIC
        return if (hide) {
            RedactedClusterView(LOCKED_HEADLINE, LOCKED_SUMMARY, redacted = true)
        } else {
            RedactedClusterView(headline, summary, redacted = false)
        }
    }

    fun forCluster(cluster: SummarizedCluster, sensitivity: Sensitivity, deviceLocked: Boolean): RedactedClusterView =
        forDisplay(cluster.headline, cluster.summary, sensitivity, deviceLocked)
}
