package app.projectzero.actionresolver

import app.projectzero.domain.ReasonCode
import app.projectzero.domain.Validation
import app.projectzero.domain.action.ActionCapability
import app.projectzero.domain.action.AllowlistedAndroidActions
import app.projectzero.domain.action.AllowlistedUriSchemes
import app.projectzero.domain.action.IntentAction
import app.projectzero.domain.ports.ActionPolicyGate
import app.projectzero.domain.routing.PolicyDecision

class DefaultActionPolicyGate(
    private val nowMs: () -> Long = { System.currentTimeMillis() },
) : ActionPolicyGate {
    override suspend fun validate(action: IntentAction): PolicyDecision {
        if (action.androidAction !in AllowlistedAndroidActions.VALUES) {
            return PolicyDecision.Deny(ReasonCode.POLICY_DENIED)
        }
        if (action.androidAction == "android.intent.action.CALL") {
            return PolicyDecision.Deny(ReasonCode.POLICY_DENIED)
        }
        val scheme = action.uriScheme?.lowercase()
        if (scheme != null && (scheme in AllowlistedUriSchemes.FORBIDDEN || scheme !in AllowlistedUriSchemes.VALUES)) {
            return PolicyDecision.Deny(ReasonCode.POLICY_DENIED)
        }
        action.parameters.filterIsInstance<app.projectzero.domain.action.ActionParameter.UriValue>().forEach { uri ->
            if (uri.value.startsWith("content://")) return@forEach
            if (UriSafety.rejectReason(uri.value) != null) {
                return PolicyDecision.Deny(ReasonCode.POLICY_DENIED)
            }
        }
        if (action.isExpiredAt(nowMs())) {
            return PolicyDecision.Deny(ReasonCode.ACTION_EXPIRED)
        }
        if (action.capability == ActionCapability.DIAL && action.androidAction != "android.intent.action.DIAL") {
            return PolicyDecision.Deny(ReasonCode.POLICY_DENIED)
        }
        Validation.requireSchemaV1(action.schemaVersion)
        return PolicyDecision.Allow()
    }
}
