package app.projectzero.launcher

import android.app.KeyguardManager
import android.content.Intent
import android.os.Bundle
import android.os.UserManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.projectzero.domain.notification.NotificationKind
import app.projectzero.domain.notification.Sensitivity
import app.projectzero.launcher.ui.LauncherApp
import app.projectzero.launcher.ui.LauncherRoute
import app.projectzero.launcher.ui.theme.ProjectZeroTheme
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val graph = (application as? ProjectZeroApp)?.graph ?: AppGraph.degraded()
        val unlocked = userUnlocked()
        setContent {
            var route by remember { mutableStateOf(LauncherRoute.HOME) }
            val accessGranted = NotificationAccess.isGranted(this)
            val locked = deviceLocked() || !unlocked
            var clusters by remember { mutableStateOf(emptyList<RedactedClusterView>()) }
            LaunchedEffect(graph.repository, unlocked, locked) {
                val repository = graph.repository
                if (repository == null || !unlocked) {
                    clusters = emptyList()
                    return@LaunchedEffect
                }
                repository.clusters().collectLatest { published ->
                    clusters = published.map { cluster ->
                        val sensitivity =
                            if (cluster.kind == NotificationKind.SYSTEM) Sensitivity.PUBLIC else Sensitivity.PERSONAL
                        LockScreenRedactor.forCluster(cluster, sensitivity, locked)
                    }
                }
            }
            ProjectZeroTheme {
                LauncherApp(
                    route = route,
                    notificationAccessGranted = accessGranted,
                    deviceLocked = locked,
                    storeAvailable = graph.notificationStoreAvailable,
                    clusters = clusters,
                    onOpenNotificationAccess = { route = LauncherRoute.NOTIFICATION_ACCESS },
                    onOpenSystemSettings = {
                        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    },
                    onBackToHome = { route = LauncherRoute.HOME },
                )
            }
        }
    }

    private fun deviceLocked(): Boolean {
        val km = getSystemService(KeyguardManager::class.java) ?: return false
        return km.isKeyguardLocked || km.isDeviceLocked
    }

    private fun userUnlocked(): Boolean {
        val um = getSystemService(UserManager::class.java) ?: return true
        return um.isUserUnlocked
    }
}
