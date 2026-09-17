package app.projectzero.notificationingest

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ZeroNotificationListenerService : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1))

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val coordinator = IngestRuntime.coordinator() ?: return
        if (sbn == null) return
        scope.launch {
            runCatching { coordinator.onPosted(sbn) }
                .onFailure { error -> SafeLog.e(null, null, "post failed", error) }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        val coordinator = IngestRuntime.coordinator() ?: return
        if (sbn == null) return
        val key = sbn.key
        scope.launch {
            runCatching { coordinator.onRemoved(key) }
                .onFailure { error -> SafeLog.e(null, null, "remove failed", error) }
        }
    }

    override fun onListenerConnected() {
        val coordinator = IngestRuntime.coordinator() ?: return
        val active = runCatching { activeNotifications?.toList().orEmpty() }.getOrDefault(emptyList())
        scope.launch {
            val captures = active.map { sbn -> coordinator.capture(sbn) }
            runCatching { coordinator.onConnected(captures) }
                .onFailure { error -> SafeLog.e(null, null, "connect failed", error) }
        }
    }

    override fun onListenerDisconnected() {
        val coordinator = IngestRuntime.coordinator() ?: return
        scope.launch {
            runCatching { coordinator.onDisconnected() }
                .onFailure { error -> SafeLog.e(null, null, "disconnect failed", error) }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
