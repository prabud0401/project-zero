package app.projectzero.executor

import android.app.Activity
import android.app.KeyguardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import app.projectzero.actionresolver.DefaultActionPolicyGate
import app.projectzero.actionresolver.HandlerCandidate
import app.projectzero.actionresolver.PreviewDigest
import app.projectzero.domain.ActionId
import app.projectzero.domain.ReasonCode
import app.projectzero.domain.action.ActionAttempt
import app.projectzero.domain.action.ActionAttemptOutcome
import app.projectzero.domain.action.FallbackResolution
import app.projectzero.domain.action.IntentAction
import app.projectzero.domain.ports.ActionExecutor
import app.projectzero.domain.routing.ExecutionResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AndroidIntentExecutor(
    private val activity: Activity,
    private val policy: DefaultActionPolicyGate = DefaultActionPolicyGate(),
    private val registryVersion: Long = 1,
    private val policyVersion: Int = 1,
    private val elapsedMs: () -> Long = { android.os.SystemClock.elapsedRealtime() },
) : ActionExecutor {
    private val mutex = Mutex()
    private val attempts = linkedMapOf<String, ActionAttempt>()
    private var lastPreviewDigest: String? = null
    private var lastAction: IntentAction? = null

    fun preview(action: IntentAction): ActionPreview {
        val digest = PreviewDigest.of(action, registryVersion, policyVersion)
        lastPreviewDigest = digest
        lastAction = action
        return ActionPreview.from(
            action = action,
            digest = digest,
            chooserOwnsTarget = action.fallbackResolution == FallbackResolution.ANDROID_CHOOSER,
        )
    }

    override suspend fun execute(action: IntentAction, previewDigest: String): ExecutionResult {
        return mutex.withLock { executeLocked(action, previewDigest) }
    }

    private suspend fun executeLocked(action: IntentAction, previewDigest: String): ExecutionResult {
        val now = elapsedMs()
        val existing = attempts[action.actionId.value]
        if (existing != null) {
            return ExecutionResult.Rejected(ReasonCode.CONFIRMATION_CONSUMED)
        }
        val km = activity.getSystemService(KeyguardManager::class.java)
        if (km != null && (km.isKeyguardLocked || km.isDeviceLocked)) {
            return ExecutionResult.Rejected(ReasonCode.DEVICE_LOCKED)
        }
        if (action.isExpiredAt(now)) {
            return ExecutionResult.Rejected(ReasonCode.ACTION_EXPIRED)
        }
        val expected = PreviewDigest.of(action, registryVersion, policyVersion)
        if (expected != previewDigest || lastPreviewDigest != previewDigest) {
            return ExecutionResult.Rejected(ReasonCode.PREVIEW_DIGEST_MISMATCH)
        }
        val last = lastAction
        if (last != null && last.actionId == action.actionId &&
            (last.targetPackage != action.targetPackage || last.androidAction != action.androidAction)
        ) {
            return ExecutionResult.Rejected(ReasonCode.RESOLUTION_CHANGED)
        }
        val policyDecision = policy.validate(action)
        if (policyDecision is app.projectzero.domain.routing.PolicyDecision.Deny) {
            return ExecutionResult.Rejected(policyDecision.reasonCode)
        }
        val intent = SafeIntentFactory.create(action)
        if (action.fallbackResolution == FallbackResolution.ANDROID_CHOOSER || action.targetPackage == null) {
            val chooser = SafeIntentFactory.chooser(intent)
            return launch(action, chooser, now, previewDigest)
        }
        return launch(action, intent, now, previewDigest)
    }

    private fun launch(
        action: IntentAction,
        intent: Intent,
        now: Long,
        digest: String,
    ): ExecutionResult {
        attempts[action.actionId.value] = ActionAttempt(
            actionId = action.actionId,
            previewDigest = digest,
            confirmationConsumed = true,
            outcome = ActionAttemptOutcome.HANDED_OFF,
            createdAtElapsedMs = now,
            expiresAtElapsedMs = now + app.projectzero.domain.Validation.ACTION_TTL_MS,
        )
        return try {
            activity.startActivity(intent)
            ExecutionResult.HandedOff()
        } catch (_: Exception) {
            ExecutionResult.OutcomeUnknown()
        }
    }

    fun handlers(intent: Intent): List<HandlerCandidate> {
        val flags = PackageManager.MATCH_DEFAULT_ONLY
        val resolved: List<ResolveInfo> = activity.packageManager.queryIntentActivities(intent, flags)
        return resolved.map { info ->
            HandlerCandidate(
                packageName = info.activityInfo.packageName,
                exported = info.activityInfo.exported,
                enabled = info.activityInfo.enabled,
            )
        }
    }

    fun hasAttempt(actionId: ActionId): Boolean = attempts.containsKey(actionId.value)
}
