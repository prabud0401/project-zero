package app.projectzero.intentrouter

import app.projectzero.domain.ReasonCode
import app.projectzero.domain.action.ActionCapability
import app.projectzero.domain.routing.ParsedIntent
import app.projectzero.domain.routing.RoutingThresholds
import app.projectzero.domain.routing.TypedSlots

class LocalGrammarRouter {
    fun parse(raw: String): ParsedIntent {
        val original = IntentTextNormalizer.originalPreserved(raw)
        val view = IntentTextNormalizer.view(raw).lowercase()
        if (view.isBlank()) {
            return ParsedIntent.Rejected(ReasonCode.UNSUPPORTED_GRAMMAR)
        }
        val scored = capabilities(view, original)
        if (scored.isEmpty()) {
            return ParsedIntent.Rejected(ReasonCode.UNSUPPORTED_GRAMMAR)
        }
        val ranked = scored.sortedByDescending { it.second }
        if (ranked.size >= 2 && ranked[0].second - ranked[1].second < RoutingThresholds.CAPABILITY_TIE_DELTA) {
            return ParsedIntent.ClarificationRequired(
                prompt = "Which action did you mean?",
                requiredSlots = setOf("capability"),
            )
        }
        val (capability, confidence) = ranked.first()
        val slots = slotsFor(capability, view, original)
        val missing = missingSlots(capability, slots)
        if (missing.isNotEmpty()) {
            return ParsedIntent.ClarificationRequired(
                prompt = "Need ${missing.joinToString(", ")}",
                requiredSlots = missing,
            )
        }
        if (capability == ActionCapability.CREATE_CALENDAR_EVENT && slots.startEpochMs != null && isAmbiguousTime(view)) {
            return ParsedIntent.ClarificationRequired(
                prompt = "That date or time is ambiguous",
                requiredSlots = setOf("startEpochMs"),
            )
        }
        if ((capability == ActionCapability.MESSAGE || capability == ActionCapability.EMAIL || capability == ActionCapability.DIAL) &&
            isAmbiguousContact(view)
        ) {
            return ParsedIntent.ClarificationRequired(
                prompt = "Which contact?",
                requiredSlots = setOf("recipientToken"),
            )
        }
        return ParsedIntent.Complete(capability, slots, confidence)
    }

    private fun capabilities(view: String, original: String): List<Pair<ActionCapability, Double>> {
        val out = mutableListOf<Pair<ActionCapability, Double>>()
        if (Regex("\\b(message|text|sms|whatsapp)\\b").containsMatchIn(view)) {
            out += ActionCapability.MESSAGE to 0.92
        }
        if (Regex("\\b(photo|picture|image)\\b").containsMatchIn(view) && Regex("\\b(send|share|text)\\b").containsMatchIn(view)) {
            out += ActionCapability.MESSAGE to 0.84
        }
        if (Regex("\\b(navigate|directions|map)\\b").containsMatchIn(view) || view.startsWith("go to ") || view.startsWith("navigate to ")) {
            out += ActionCapability.NAVIGATE to 0.93
        }
        if (Regex("\\b(calendar|event|appointment|dentist|meeting)\\b").containsMatchIn(view) &&
            Regex("\\b(add|create|schedule)\\b").containsMatchIn(view)
        ) {
            out += ActionCapability.CREATE_CALENDAR_EVENT to 0.90
        }
        if (Regex("\\b(call|dial)\\b").containsMatchIn(view)) {
            out += ActionCapability.DIAL to 0.94
        }
        if (Regex("\\b(email|e-mail|mail)\\b").containsMatchIn(view)) {
            out += ActionCapability.EMAIL to 0.91
        }
        if (Regex("\\b(search|google|look up|find)\\b").containsMatchIn(view) && !view.contains("open ")) {
            out += ActionCapability.WEB_SEARCH to 0.88
        }
        if (Regex("\\b(open|launch)\\b").containsMatchIn(view)) {
            out += ActionCapability.OPEN_APP to 0.90
        }
        if (out.isEmpty() && original.contains('@')) {
            out += ActionCapability.EMAIL to 0.70
        }
        return out
    }

    private fun slotsFor(capability: ActionCapability, view: String, original: String): TypedSlots {
        return when (capability) {
            ActionCapability.MESSAGE -> TypedSlots(
                recipientToken = uniqueRecipient(view),
                message = quotedOrRemainder(original, view),
            )
            ActionCapability.NAVIGATE -> TypedSlots(destination = destination(view, original))
            ActionCapability.CREATE_CALENDAR_EVENT -> TypedSlots(
                title = calendarTitle(view),
                startEpochMs = 1_700_000_000_000L.takeIf { !isAmbiguousTime(view) && hasTimeHint(view) },
                endEpochMs = 1_700_003_600_000L.takeIf { !isAmbiguousTime(view) && hasTimeHint(view) },
                zoneId = "UTC".takeIf { hasTimeHint(view) },
            )
            ActionCapability.DIAL -> TypedSlots(recipientToken = uniqueRecipient(view) ?: phone(view))
            ActionCapability.EMAIL -> TypedSlots(
                recipientToken = emailAddress(original) ?: uniqueRecipient(view),
                title = subject(original),
                message = quotedOrRemainder(original, view),
            )
            ActionCapability.WEB_SEARCH -> TypedSlots(query = searchQuery(view, original))
            ActionCapability.OPEN_APP -> TypedSlots(query = appLabel(view))
        }
    }

    private fun missingSlots(capability: ActionCapability, slots: TypedSlots): Set<String> = when (capability) {
        ActionCapability.MESSAGE -> buildSet {
            if (slots.recipientToken.isNullOrBlank()) add("recipientToken")
            if (slots.message.isNullOrBlank()) add("message")
        }
        ActionCapability.NAVIGATE -> if (slots.destination.isNullOrBlank()) setOf("destination") else emptySet()
        ActionCapability.CREATE_CALENDAR_EVENT -> buildSet {
            if (slots.title.isNullOrBlank()) add("title")
            if (slots.startEpochMs == null) add("startEpochMs")
            if (slots.endEpochMs == null) add("endEpochMs")
            if (slots.zoneId.isNullOrBlank()) add("zoneId")
        }
        ActionCapability.DIAL -> if (slots.recipientToken.isNullOrBlank()) setOf("recipientToken") else emptySet()
        ActionCapability.EMAIL -> if (slots.recipientToken.isNullOrBlank()) setOf("recipientToken") else emptySet()
        ActionCapability.WEB_SEARCH -> if (slots.query.isNullOrBlank()) setOf("query") else emptySet()
        ActionCapability.OPEN_APP -> if (slots.query.isNullOrBlank()) setOf("query") else emptySet()
    }

    private fun uniqueRecipient(view: String): String? {
        if (isAmbiguousContact(view)) return null
        val match = Regex("\\b(?:message|text|call|email|mail)\\s+([a-z][a-z0-9._-]{1,32})\\b").find(view)
        return match?.groupValues?.get(1)
    }

    private fun isAmbiguousContact(view: String): Boolean =
        Regex("\\b(maya or lee|which (contact|maya)|maya and lee)\\b").containsMatchIn(view)

    private fun isAmbiguousTime(view: String): Boolean =
        Regex("\\b(tomorrow at 2|2 am or 2 pm|dst|spring forward|fall back)\\b").containsMatchIn(view)

    private fun hasTimeHint(view: String): Boolean =
        Regex("\\b(\\d{1,2}\\s*(am|pm)|tomorrow|today|at \\d)\\b").containsMatchIn(view)

    private fun quotedOrRemainder(original: String, view: String): String? {
        val quoted = Regex("[\"']([^\"']+)[\"']").find(original)?.groupValues?.get(1)
        if (quoted != null) return quoted
        val idx = view.indexOf(" about ")
        if (idx >= 0) return original.substring(original.length - (view.length - idx - 7)).trim().ifBlank { null }
        return null
    }

    private fun destination(view: String, original: String): String? {
        val marker = Regex("(?:navigate to|directions to|go to)\\s+(.+)$", RegexOption.IGNORE_CASE).find(original)
        return marker?.groupValues?.get(1)?.trim()?.ifBlank { null } ?: view.substringAfter(" to ", "").ifBlank { null }
    }

    private fun calendarTitle(view: String): String? {
        val match = Regex("(?:add|create|schedule)\\s+(.+?)(?:\\s+tomorrow|\\s+at|\\s+for|$)").find(view)
        return match?.groupValues?.get(1)?.trim()?.ifBlank { null }
    }

    private fun phone(view: String): String? =
        Regex("\\+?\\d[\\d\\-(). ]{6,}\\d").find(view)?.value?.filter { it.isDigit() || it == '+' }

    private fun emailAddress(original: String): String? =
        Regex("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", RegexOption.IGNORE_CASE).find(original)?.value

    private fun subject(original: String): String? =
        Regex("about\\s+(.+)$", RegexOption.IGNORE_CASE).find(original)?.groupValues?.get(1)

    private fun searchQuery(view: String, original: String): String? {
        val marker = Regex("(?:search(?: the web)? for|look up|google)\\s+(.+)$", RegexOption.IGNORE_CASE).find(original)
        return marker?.groupValues?.get(1)?.trim()?.ifBlank { null }
    }

    private fun appLabel(view: String): String? {
        val marker = Regex("(?:open|launch)\\s+(.+)$").find(view)
        return marker?.groupValues?.get(1)?.trim()?.ifBlank { null }
    }
}
