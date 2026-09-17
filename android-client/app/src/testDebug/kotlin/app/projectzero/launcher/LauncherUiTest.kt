package app.projectzero.launcher

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.projectzero.domain.notification.Sensitivity
import app.projectzero.launcher.ui.LauncherApp
import app.projectzero.launcher.ui.LauncherRoute
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class LauncherUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun deniedListenerShowsNonBlockingExplanationAndHomeRemainsUsable() {
        var route by mutableStateOf(LauncherRoute.HOME)
        composeRule.setContent {
            LauncherApp(
                route = route,
                notificationAccessGranted = false,
                deviceLocked = false,
                storeAvailable = true,
                clusters = emptyList(),
                onOpenNotificationAccess = { route = LauncherRoute.NOTIFICATION_ACCESS },
                onOpenSystemSettings = {},
                onBackToHome = { route = LauncherRoute.HOME },
            )
        }
        composeRule.onNodeWithTag("home_root").assertIsDisplayed()
        composeRule.onNodeWithTag("home_title").assertIsDisplayed()
        composeRule.onNodeWithTag("access_denied_banner").assertIsDisplayed()
        composeRule.onNodeWithTag("open_notification_access").performClick()
        composeRule.onNodeWithTag("notification_access_education").assertIsDisplayed()
        composeRule.onNodeWithTag("education_back").performClick()
        composeRule.onNodeWithTag("home_root").assertIsDisplayed()
    }

    @Test
    fun lockScreenRedactsPersonalAndSecretTextFromSemantics() {
        val secret = LockScreenRedactor.forDisplay(
            headline = "OTP 123456",
            summary = "Bank account 9999",
            sensitivity = Sensitivity.SECRET,
            deviceLocked = true,
        )
        composeRule.setContent {
            LauncherApp(
                route = LauncherRoute.HOME,
                notificationAccessGranted = true,
                deviceLocked = true,
                storeAvailable = true,
                clusters = listOf(secret),
                onOpenNotificationAccess = {},
                onOpenSystemSettings = {},
                onBackToHome = {},
            )
        }
        composeRule.onNodeWithText("OTP 123456").assertDoesNotExist()
        composeRule.onNodeWithText("Bank account 9999").assertDoesNotExist()
        composeRule.onNodeWithText(LockScreenRedactor.LOCKED_HEADLINE).assertIsDisplayed()
        composeRule.onNodeWithText(LockScreenRedactor.LOCKED_SUMMARY).assertIsDisplayed()
    }

    @Test
    fun unlockedShowsClusterText() {
        val view = LockScreenRedactor.forDisplay("Hello", "World", Sensitivity.PERSONAL, deviceLocked = false)
        composeRule.setContent {
            LauncherApp(
                route = LauncherRoute.HOME,
                notificationAccessGranted = true,
                deviceLocked = false,
                storeAvailable = true,
                clusters = listOf(view),
                onOpenNotificationAccess = {},
                onOpenSystemSettings = {},
                onBackToHome = {},
            )
        }
        composeRule.onNodeWithText("Hello").assertIsDisplayed()
        composeRule.onNodeWithText("World").assertIsDisplayed()
    }
}
