package app.projectzero.cloudpolicy

import app.projectzero.domain.notification.NotificationEvent
import app.projectzero.domain.notification.Sensitivity

class CloudPolicyGate {
    fun isEligibleForCloud(event: NotificationEvent, isOptedIn: Boolean, budgetAvailable: Boolean): Boolean {
        if (!isOptedIn || !budgetAvailable) return false
        if (event.sensitivity == Sensitivity.SECRET) return false
        // other rules
        return true
    }
}
