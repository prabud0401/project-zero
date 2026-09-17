package app.projectzero.executor

import android.content.Intent
import android.net.Uri
import app.projectzero.domain.action.ActionCapability
import app.projectzero.domain.action.ActionParameter
import app.projectzero.domain.action.FallbackResolution
import app.projectzero.domain.action.IntentAction

object SafeIntentFactory {
    fun create(action: IntentAction): Intent {
        require(action.androidAction != Intent.ACTION_CALL) { "ACTION_CALL is forbidden" }
        val intent = Intent(action.androidAction)
        action.parameters.filterIsInstance<ActionParameter.UriValue>()
            .firstOrNull { it.key == "data" }
            ?.let { intent.data = Uri.parse(it.value) }
        action.parameters.filterIsInstance<ActionParameter.Text>()
            .firstOrNull { it.key == "mime" }
            ?.let { intent.type = it.value }
        if (action.capability == ActionCapability.OPEN_APP) {
            intent.addCategory(Intent.CATEGORY_LAUNCHER)
        }
        if (action.capability == ActionCapability.CREATE_CALENDAR_EVENT) {
            extrasForCalendar(action, intent)
        }
        if (action.targetPackage != null && action.fallbackResolution != FallbackResolution.ANDROID_CHOOSER) {
            intent.setPackage(action.targetPackage)
        }
        if (intent.data != null && intent.data.toString().startsWith("content://") &&
            action.capability == ActionCapability.MESSAGE
        ) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return intent
    }

    fun chooser(intent: Intent, title: CharSequence = "Complete action"): Intent =
        Intent.createChooser(intent, title)

    private fun extrasForCalendar(action: IntentAction, intent: Intent) {
        action.parameters.filterIsInstance<ActionParameter.Text>()
            .firstOrNull { it.key == "title" }
            ?.let { intent.putExtra("title", it.value) }
        action.parameters.filterIsInstance<ActionParameter.InstantValue>().forEach { instant ->
            if (instant.key == "start") intent.putExtra("beginTime", instant.epochMs)
            if (instant.key == "end") intent.putExtra("endTime", instant.epochMs)
        }
    }
}
