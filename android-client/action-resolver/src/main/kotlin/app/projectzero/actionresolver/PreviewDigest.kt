package app.projectzero.actionresolver

import app.projectzero.domain.action.IntentAction
import java.security.MessageDigest
import java.util.Base64

object PreviewDigest {
    fun of(action: IntentAction, registryVersion: Long, policyVersion: Int): String {
        val canonical = buildString {
            append(action.capability.name)
            append('\u0000')
            append(action.androidAction)
            append('\u0000')
            append(action.targetPackage.orEmpty())
            append('\u0000')
            append(action.uriScheme.orEmpty())
            append('\u0000')
            action.parameters.forEach { param ->
                append(param.key)
                append('=')
                append(param.toString())
                append('\u0000')
            }
            append(action.fallbackResolution.name)
            append('\u0000')
            append(action.confirmation.name)
            append('\u0000')
            append(registryVersion)
            append('\u0000')
            append(policyVersion)
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray())
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }
}
