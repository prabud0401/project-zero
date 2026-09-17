package app.projectzero.executor

import android.app.Activity
import android.content.Intent
import app.projectzero.domain.ActionId
import app.projectzero.domain.ReasonCode
import app.projectzero.domain.Validation
import app.projectzero.domain.action.ActionCapability
import app.projectzero.domain.action.ActionParameter
import app.projectzero.domain.action.Confirmation
import app.projectzero.domain.action.FallbackResolution
import app.projectzero.domain.action.IntentAction
import app.projectzero.domain.routing.ExecutionResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowActivity

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class AndroidIntentExecutorTest {
    private val activity = Robolectric.buildActivity(Activity::class.java).setup().get()

    @Test
    fun emittedIntentMatchesAction() {
        val action = sample(ActionCapability.DIAL, "android.intent.action.DIAL", "tel", "tel:5551234")
        val intent = SafeIntentFactory.create(action)
        assertEquals(Intent.ACTION_DIAL, intent.action)
        assertEquals("tel:5551234", intent.data.toString())
        assertEquals("com.example.dialer", intent.`package`)
    }

    @Test
    fun chooserWhenFallbackChooser() {
        val action = sample(
            ActionCapability.WEB_SEARCH,
            "android.intent.action.VIEW",
            "https",
            "https://www.google.com/search?q=a",
            fallback = FallbackResolution.ANDROID_CHOOSER,
            pkg = null,
        )
        val executor = AndroidIntentExecutor(activity, policy = app.projectzero.actionresolver.DefaultActionPolicyGate(nowMs = { 1_000L }), elapsedMs = { 1_000L })
        val preview = executor.preview(action)
        val result = runBlocking { executor.execute(action, preview.digest) }
        assertTrue(result is ExecutionResult.HandedOff)
        val shadow: ShadowActivity = Shadows.shadowOf(activity)
        val started = shadow.nextStartedActivity
        assertEquals(Intent.ACTION_CHOOSER, started.action)
    }

    @Test
    fun secondExecuteRejected() {
        val action = sample(ActionCapability.DIAL, "android.intent.action.DIAL", "tel", "tel:555")
        val executor = AndroidIntentExecutor(activity, policy = app.projectzero.actionresolver.DefaultActionPolicyGate(nowMs = { 1_000L }), elapsedMs = { 1_000L })
        val digest = executor.preview(action).digest
        runBlocking {
            assertTrue(executor.execute(action, digest) is ExecutionResult.HandedOff)
            val second = executor.execute(action, digest)
            assertTrue(second is ExecutionResult.Rejected)
            assertEquals(ReasonCode.CONFIRMATION_CONSUMED, (second as ExecutionResult.Rejected).reasonCode)
        }
    }

    @Test
    fun digestMismatchRejected() {
        val action = sample(ActionCapability.DIAL, "android.intent.action.DIAL", "tel", "tel:555")
        val executor = AndroidIntentExecutor(activity, policy = app.projectzero.actionresolver.DefaultActionPolicyGate(nowMs = { 1_000L }), elapsedMs = { 1_000L })
        executor.preview(action)
        val result = runBlocking { executor.execute(action, "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA") }
        assertTrue(result is ExecutionResult.Rejected)
    }

    @Test
    fun packageRemovalBetweenPreviewAndLaunch() {
        val action = sample(ActionCapability.OPEN_APP, "android.intent.action.MAIN", null, null, pkg = "com.missing.app")
        val changed = sample(ActionCapability.OPEN_APP, "android.intent.action.MAIN", null, null, pkg = "com.other.app")
        val executor = AndroidIntentExecutor(activity, policy = app.projectzero.actionresolver.DefaultActionPolicyGate(nowMs = { 1_000L }), elapsedMs = { 1_000L })
        val digest = executor.preview(action).digest
        val result = runBlocking { executor.execute(changed.copy(actionId = action.actionId), digest) }
        assertTrue(result is ExecutionResult.Rejected)
    }

    @Test
    fun grantFlagOnContentShare() {
        val action = sample(
            ActionCapability.MESSAGE,
            "android.intent.action.SEND",
            null,
            "content://app.projectzero.launcher.export/export/a.jpg",
            pkg = null,
        )
        val intent = SafeIntentFactory.create(action)
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }

    private fun sample(
        capability: ActionCapability,
        androidAction: String,
        scheme: String?,
        uri: String?,
        fallback: FallbackResolution = FallbackResolution.NONE,
        pkg: String? = "com.example.dialer",
    ) = IntentAction(
        schemaVersion = 1,
        actionId = ActionId.parse("018f0000-0000-7000-8000-0000000000aa"),
        capability = capability,
        androidAction = androidAction,
        targetPackage = pkg,
        uriScheme = scheme,
        registryEntryId = null,
        parameters = listOfNotNull(uri?.let { ActionParameter.UriValue("data", it) }),
        fallbackResolution = fallback,
        confirmation = Confirmation.TARGET_APP_OWNS_COMMIT,
        rationale = "Preview",
        createdAtEpochMs = 1_000L,
        expiresAtEpochMs = 1_000L + Validation.ACTION_TTL_MS,
    )
}
