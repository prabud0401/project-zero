package app.projectzero.feature.home

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertIsDisplayed
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class AppDrawerScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun screenRenders() {
        composeRule.setContent { AppDrawerScreen() }
        composeRule.onNodeWithTag("app_drawer").assertIsDisplayed()
        composeRule.onNodeWithTag("app_search_input").assertIsDisplayed()
    }
}
