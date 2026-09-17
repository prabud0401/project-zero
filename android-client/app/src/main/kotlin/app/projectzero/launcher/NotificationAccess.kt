package app.projectzero.launcher

import android.content.Context
import android.provider.Settings

object NotificationAccess {
    fun isGranted(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        ) ?: return false
        val packageName = context.packageName
        return enabled.split(':').any { component ->
            component.equals(packageName, ignoreCase = true) ||
                component.startsWith("$packageName/")
        }
    }
}
