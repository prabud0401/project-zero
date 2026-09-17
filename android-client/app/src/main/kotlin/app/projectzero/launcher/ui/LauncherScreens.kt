package app.projectzero.launcher.ui

import app.projectzero.feature.home.AppDrawerScreen
import app.projectzero.feature.intent.IntentInputScreen
import app.projectzero.feature.settings.SettingsScreen
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import app.projectzero.launcher.LockScreenRedactor
import app.projectzero.launcher.RedactedClusterView

enum class LauncherRoute {
    HOME,
    NOTIFICATION_ACCESS,
    APP_DRAWER,
    INTENT_INPUT,
    SETTINGS
}

@Composable
fun HomeScreen(
    notificationAccessGranted: Boolean,
    deviceLocked: Boolean,
    storeAvailable: Boolean,
    clusters: List<RedactedClusterView>,
    onOpenNotificationAccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .semantics { testTag = "home_root" },
        verticalArrangement = Arrangement.Top,
    ) {
        Text(
            text = "Project Zero",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { testTag = "home_title" },
        )
        Spacer(Modifier.height(12.dp))
        if (!storeAvailable) {
            Text(
                text = "Notification storage is unavailable. Home and app launch still work.",
                modifier = Modifier.semantics {
                    testTag = "store_unavailable"
                    contentDescription = "Notification storage is unavailable"
                },
            )
            Spacer(Modifier.height(12.dp))
        }
        if (!notificationAccessGranted) {
            Text(
                text = "Notification access is off. The launcher stays usable. Summaries stay local until you grant access in system settings.",
                modifier = Modifier.semantics {
                    testTag = "access_denied_banner"
                    contentDescription = "Notification access is off. Launcher remains usable."
                },
            )
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = onOpenNotificationAccess,
                modifier = Modifier.semantics { testTag = "open_notification_access" },
            ) {
                Text("Learn about notification access")
            }
        } else {
            Text(
                text = "Notification access is on. Summaries stay on this device.",
                modifier = Modifier.semantics { testTag = "access_granted_banner" },
            )
        }
        
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = { /* navigate to apps */ }, modifier = Modifier.semantics { testTag = "nav_apps" }) { Text("Apps") }
            Button(onClick = { /* navigate to intents */ }, modifier = Modifier.semantics { testTag = "nav_intents" }) { Text("Intent") }
            Button(onClick = { /* navigate to settings */ }, modifier = Modifier.semantics { testTag = "nav_settings" }) { Text("Settings") }
        }

        if (clusters.isEmpty()) {
            Text(
                text = if (deviceLocked) "Locked" else "No notification summaries",
                modifier = Modifier.semantics { testTag = "empty_clusters" },
            )
        } else {
            clusters.forEachIndexed { index, cluster ->
                ClusterCard(cluster, index, deviceLocked)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun ClusterCard(cluster: RedactedClusterView, index: Int, deviceLocked: Boolean) {
    val headline = if (deviceLocked && cluster.redacted) LockScreenRedactor.LOCKED_HEADLINE else cluster.headline
    val summary = if (deviceLocked && cluster.redacted) LockScreenRedactor.LOCKED_SUMMARY else cluster.summary
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                testTag = "cluster_$index"
                contentDescription = "$headline. $summary"
            },
    ) {
        Text(text = headline, style = MaterialTheme.typography.titleMedium)
        Text(text = summary, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun NotificationAccessEducationScreen(
    onOpenSystemSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .semantics { testTag = "notification_access_education" },
        verticalArrangement = Arrangement.Top,
    ) {
        Text(
            text = "Notification access",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Project Zero can summarize notifications on this device after you enable notification access in Android settings. Raw notification text is not uploaded. You can use the home screen without granting access.",
            modifier = Modifier.semantics { testTag = "education_body" },
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onOpenSystemSettings,
            modifier = Modifier.semantics { testTag = "open_system_listener_settings" },
        ) {
            Text("Open system settings")
        }
        TextButton(
            onClick = onBack,
            modifier = Modifier.semantics { testTag = "education_back" },
        ) {
            Text("Back to home")
        }
    }
}

@Composable
fun LauncherApp(
    route: LauncherRoute,
    notificationAccessGranted: Boolean,
    deviceLocked: Boolean,
    storeAvailable: Boolean,
    clusters: List<RedactedClusterView>,
    onOpenNotificationAccess: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onBackToHome: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        when (route) {
            LauncherRoute.HOME -> HomeScreen(
                notificationAccessGranted = notificationAccessGranted,
                deviceLocked = deviceLocked,
                storeAvailable = storeAvailable,
                clusters = clusters,
                onOpenNotificationAccess = onOpenNotificationAccess,
            )
            LauncherRoute.NOTIFICATION_ACCESS -> NotificationAccessEducationScreen(
                onOpenSystemSettings = onOpenSystemSettings,
                onBack = onBackToHome,
            )
            LauncherRoute.APP_DRAWER -> AppDrawerScreen()
            LauncherRoute.INTENT_INPUT -> IntentInputScreen()
            LauncherRoute.SETTINGS -> SettingsScreen()
        }
    }
}


