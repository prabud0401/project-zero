package app.projectzero.executor

import app.projectzero.domain.action.ActionCapability
import app.projectzero.domain.action.Confirmation
import app.projectzero.domain.action.IntentAction

data class ActionPreview(
    val digest: String,
    val capability: ActionCapability,
    val headline: String,
    val fields: List<Pair<String, String>>,
    val confirmation: Confirmation,
    val chooserOwnsTarget: Boolean,
    val actionId: String,
) {
    init {
        require(fields.toMap().size == fields.size) { "preview fields must have unique keys" }
    }

    companion object {
        fun from(action: IntentAction, digest: String, chooserOwnsTarget: Boolean): ActionPreview {
            val fields = action.parameters.map { param ->
                param.key to param.toString().substringAfter("value=").substringBefore(")")
            }
            return ActionPreview(
                digest = digest,
                capability = action.capability,
                headline = action.rationale,
                fields = fields,
                confirmation = action.confirmation,
                chooserOwnsTarget = chooserOwnsTarget,
                actionId = action.actionId.value,
            )
        }
    }
}
