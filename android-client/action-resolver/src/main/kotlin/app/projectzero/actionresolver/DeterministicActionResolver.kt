package app.projectzero.actionresolver

import app.projectzero.domain.ActionId
import app.projectzero.domain.ReasonCode
import app.projectzero.domain.Validation
import app.projectzero.domain.action.ActionCapability
import app.projectzero.domain.action.ActionParameter
import app.projectzero.domain.action.AppAffinityContext
import app.projectzero.domain.action.Confirmation
import app.projectzero.domain.action.FallbackResolution
import app.projectzero.domain.action.IntentAction
import app.projectzero.domain.ports.DeepLinkResolver
import app.projectzero.domain.routing.ParsedIntent
import app.projectzero.domain.routing.RouteResult
import app.projectzero.localai.UuidV7
import app.projectzero.registry.RegistrySnapshot

class DeterministicActionResolver(
    private val registry: RegistrySnapshot,
    private val handlersFor: (ActionCapability, BuiltUri) -> List<HandlerCandidate> = { _, _ -> emptyList() },
    private val clockMs: () -> Long = { System.currentTimeMillis() },
    private val elapsedMs: () -> Long = { System.nanoTime() / 1_000_000L },
) : DeepLinkResolver {
    override suspend fun resolve(parsed: ParsedIntent, context: AppAffinityContext): RouteResult {
        if (parsed !is ParsedIntent.Complete) {
            return when (parsed) {
                is ParsedIntent.ClarificationRequired ->
                    RouteResult.Clarification(parsed.prompt, parsed.requiredSlots)
                is ParsedIntent.Rejected -> RouteResult.Unsupported(parsed.reasonCode)
                is ParsedIntent.Complete -> error("unreachable")
            }
        }
        if (parsed.confidence < 0.55) {
            return RouteResult.Unsupported(ReasonCode.LOW_CONFIDENCE)
        }
        if (parsed.confidence < 0.82) {
            return RouteResult.Clarification("Confirm this local action", parsed.slots.presentNames())
        }
        val built = runCatching { CapabilityUriBuilder.build(parsed.capability, parsed.slots) }
            .getOrElse { return RouteResult.Unsupported(ReasonCode.POLICY_DENIED) }
        if (built.uri != null && UriSafety.rejectReason(built.uri) != null && parsed.capability != ActionCapability.CREATE_CALENDAR_EVENT) {
            return RouteResult.Unsupported(ReasonCode.POLICY_DENIED)
        }
        val entry = registry.entries.firstOrNull { it.capability == parsed.capability && !it.disabled }
        val handlers = handlersFor(parsed.capability, built)
        val ranked = HandlerRanking.rank(context, handlers, entry?.packageName)
        val now = clockMs()
        val elapsed = elapsedMs()
        val parameters = parameters(parsed, built)
        val action = IntentAction(
            schemaVersion = Validation.SCHEMA_VERSION_V1,
            actionId = ActionId.parse(UuidV7.generate(now)),
            capability = parsed.capability,
            androidAction = built.androidAction,
            targetPackage = ranked.packageName,
            uriScheme = built.scheme,
            registryEntryId = entry?.entryId,
            parameters = parameters,
            fallbackResolution = ranked.fallback,
            confirmation = Confirmation.TARGET_APP_OWNS_COMMIT,
            rationale = rationale(parsed.capability),
            createdAtEpochMs = elapsed,
            expiresAtEpochMs = elapsed + Validation.ACTION_TTL_MS,
        )
        return RouteResult.Proposed(action, parsed.confidence)
    }

    private fun parameters(parsed: ParsedIntent.Complete, built: BuiltUri): List<ActionParameter> {
        val slots = parsed.slots
        val out = mutableListOf<ActionParameter>()
        built.uri?.let { out += ActionParameter.UriValue("data", it) }
        built.mimeType?.let { out += ActionParameter.Text("mime", it) }
        slots.recipientToken?.let { out += ActionParameter.Text("recipientToken", it) }
        slots.message?.let { out += ActionParameter.Text("message", it) }
        slots.destination?.let { out += ActionParameter.Text("destination", it) }
        slots.title?.let { out += ActionParameter.Text("title", it) }
        slots.query?.let { out += ActionParameter.Text("query", it) }
        if (slots.startEpochMs != null && slots.zoneId != null) {
            out += ActionParameter.InstantValue("start", slots.startEpochMs!!, slots.zoneId!!)
        }
        if (slots.endEpochMs != null && slots.zoneId != null) {
            out += ActionParameter.InstantValue("end", slots.endEpochMs!!, slots.zoneId!!)
        }
        return out
    }

    private fun rationale(capability: ActionCapability): String = when (capability) {
        ActionCapability.MESSAGE -> "Opens the messaging app. You send there."
        ActionCapability.NAVIGATE -> "Opens maps for the destination."
        ActionCapability.CREATE_CALENDAR_EVENT -> "Opens Calendar. You save there."
        ActionCapability.DIAL -> "Opens the dialer. You place the call."
        ActionCapability.EMAIL -> "Opens email. You send there."
        ActionCapability.WEB_SEARCH -> "Opens a search provider."
        ActionCapability.OPEN_APP -> "Opens the selected app."
    }
}
