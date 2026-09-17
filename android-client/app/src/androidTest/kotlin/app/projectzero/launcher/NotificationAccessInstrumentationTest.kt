package app.projectzero.launcher

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import app.projectzero.launcher.ui.LauncherApp
import app.projectzero.launcher.ui.LauncherRoute
import org.junit.Rule
import org.junit.Test

/**
 * Instrumentation counterpart of [LauncherUiTest]. Requires a connected device/emulator.
 * Host `adb devices` was empty at Task 2 implementation time.
 */
class NotificationAccessInstrumentationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun deniedAccessKeepsHomeUsable() {
        composeRule.setContent {
            LauncherApp(
                route = LauncherRoute.HOME,
                notificationAccessGranted = false,
                deviceLocked = false,
                storeAvailable = true,
                clusters = emptyList(),
                onOpenNotificationAccess = {},
                onOpenSystemSettings = {},
                onBackToHome = {},
            )
        }
        composeRule.onNodeWithTag("home_root").assertIsDisplayed()
        composeRule.onNodeWithTag("access_denied_banner").assertIsDisplayed()
    }
}
