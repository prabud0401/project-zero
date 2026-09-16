package app.projectzero.contracts

import app.projectzero.contracts.proto.Cluster
import app.projectzero.contracts.proto.IntentRouteRequest
import app.projectzero.contracts.proto.IntentRouteResponse
import app.projectzero.contracts.proto.IntentSlots
import app.projectzero.contracts.proto.RedactedNotification
import app.projectzero.contracts.proto.SummaryBatchRequest
import app.projectzero.contracts.proto.SummaryBatchResponse
import app.projectzero.contracts.proto.Usage
import app.projectzero.domain.ClusterId
import app.projectzero.domain.DomainInvariantException
import app.projectzero.domain.EventId
import app.projectzero.domain.ReasonCode
import app.projectzero.domain.RequestId
import app.projectzero.domain.Validation
import app.projectzero.domain.action.ActionCapability
import app.projectzero.domain.notification.NotificationKind
import app.projectzero.domain.routing.TypedSlots
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode

/**
 * H04 explicit REST/protobuf mapping: JSON null for required redacted fields maps to absent
 * optional protobuf strings; empty string remains distinct. int64 is a JSON number, not a string.
 */
object PresenceMapper {
    fun summaryRequestFromJson(text: String): Pair<SummaryBatchRequest, JsonNode> {
        val node = StrictJson.parse(text)
        JsonSchemaCatalog.validate(JsonSchemaCatalog.SUMMARY_BATCH_REQUEST, node)
        rejectNonFinite(node)
        val builder = SummaryBatchRequest.newBuilder()
            .setSchemaVersion(node.path("schemaVersion").asInt())
            .setRequestId(RequestId.parse(node.path("requestId").asText()).value)
            .setBaseRevision(node.path("baseRevision").asLong())
            .setLocale(Validation.requireLocale(node.path("locale").asText(), "locale"))
        node.path("events").forEach { event ->
            builder.addEvents(redactedFromJson(event))
        }
        return builder.build() to node
    }

    fun summaryRequestToJson(message: SummaryBatchRequest): ObjectNode {
        val root = JsonNodeFactory.instance.objectNode()
        root.put("schemaVersion", message.schemaVersion)
        root.put("requestId", message.requestId)
        root.put("baseRevision", message.baseRevision)
        root.put("locale", message.locale)
        val events = root.putArray("events")
        message.eventsList.forEach { event ->
            val item = events.addObject()
            item.put("eventId", event.eventId)
            item.put("kind", event.kind)
            if (event.hasRedactedTitle()) {
                item.put("redactedTitle", event.redactedTitle)
            } else {
                item.putNull("redactedTitle")
            }
            if (event.hasRedactedBody()) {
                item.put("redactedBody", event.redactedBody)
            } else {
                item.putNull("redactedBody")
            }
            item.put("contentFingerprint", event.contentFingerprint)
        }
        return root
    }

    fun summaryResponseFromJson(text: String): Pair<SummaryBatchResponse, JsonNode> {
        val node = StrictJson.parse(text)
        JsonSchemaCatalog.validate(JsonSchemaCatalog.SUMMARY_BATCH_RESPONSE, node)
        rejectNonFinite(node)
        val builder = SummaryBatchResponse.newBuilder()
            .setSchemaVersion(node.path("schemaVersion").asInt())
            .setRequestId(RequestId.parse(node.path("requestId").asText()).value)
            .setBaseRevision(node.path("baseRevision").asLong())
        node.path("clusters").forEach { cluster ->
            val members = cluster.path("memberEventIds").map { EventId.parse(it.asText()).value }
            builder.addClusters(
                Cluster.newBuilder()
                    .setClusterId(ClusterId.parse(cluster.path("clusterId").asText()).value)
                    .addAllMemberEventIds(members)
                    .setHeadline(cluster.path("headline").asText())
                    .setSummary(cluster.path("summary").asText())
                    .setKind(NotificationKind.fromWire(cluster.path("kind").asText()).wireValue)
                    .setPriority(cluster.path("priority").asInt())
                    .setConfidence(cluster.path("confidence").asDouble())
                    .setExpiresAtEpochMs(cluster.path("expiresAtEpochMs").asLong())
                    .build(),
            )
        }
        val usage = node.path("usage")
        builder.usage = Usage.newBuilder()
            .setCacheHit(usage.path("cacheHit").asBoolean())
            .setInputTokens(usage.path("inputTokens").asInt())
            .setOutputTokens(usage.path("outputTokens").asInt())
            .build()
        return builder.build() to node
    }

    fun intentRequestFromJson(text: String): IntentRouteRequest {
        val node = StrictJson.parse(text)
        JsonSchemaCatalog.validate(JsonSchemaCatalog.INTENT_ROUTE, node)
        val builder = IntentRouteRequest.newBuilder()
            .setSchemaVersion(node.path("schemaVersion").asInt())
            .setRequestId(RequestId.parse(node.path("requestId").asText()).value)
            .setLocale(node.path("locale").asText())
            .setRedactedText(node.path("redactedText").asText())
        node.path("capabilityHints").forEach { hint ->
            builder.addCapabilityHints(ActionCapability.fromWire(hint.asText()).wireValue)
        }
        return builder.build()
    }

    fun intentResponseFromJson(text: String): Pair<IntentRouteResponse, TypedSlots> {
        val node = StrictJson.parse(text)
        JsonSchemaCatalog.validate(JsonSchemaCatalog.INTENT_ROUTE, node)
        rejectNonFinite(node)
        val slotsNode = node.path("slots")
        val slotsBuilder = IntentSlots.newBuilder()
        copyOptionalString(slotsNode, "recipientToken", slotsBuilder::setRecipientToken)
        copyOptionalString(slotsNode, "message", slotsBuilder::setMessage)
        copyOptionalString(slotsNode, "destination", slotsBuilder::setDestination)
        copyOptionalString(slotsNode, "title", slotsBuilder::setTitle)
        copyOptionalInt64(slotsNode, "startEpochMs", slotsBuilder::setStartEpochMs)
        copyOptionalInt64(slotsNode, "endEpochMs", slotsBuilder::setEndEpochMs)
        copyOptionalString(slotsNode, "zoneId", slotsBuilder::setZoneId)
        copyOptionalString(slotsNode, "query", slotsBuilder::setQuery)
        val proto = IntentRouteResponse.newBuilder()
            .setSchemaVersion(node.path("schemaVersion").asInt())
            .setRequestId(RequestId.parse(node.path("requestId").asText()).value)
            .setCapability(ActionCapability.fromWire(node.path("capability").asText()).wireValue)
            .setSlots(slotsBuilder)
            .addAllMissingSlots(node.path("missingSlots").map { it.asText() })
            .setConfidence(node.path("confidence").asDouble())
            .build()
        return proto to slotsToDomain(proto.slots)
    }

    fun slotsToDomain(slots: IntentSlots): TypedSlots = TypedSlots(
        recipientToken = slots.recipientTokenOrNull(),
        message = slots.messageOrNull(),
        destination = slots.destinationOrNull(),
        title = slots.titleOrNull(),
        startEpochMs = if (slots.hasStartEpochMs()) slots.startEpochMs else null,
        endEpochMs = if (slots.hasEndEpochMs()) slots.endEpochMs else null,
        zoneId = slots.zoneIdOrNull(),
        query = slots.queryOrNull(),
    )

    fun slotsToJson(slots: IntentSlots): ObjectNode {
        val node = JsonNodeFactory.instance.objectNode()
        if (slots.hasRecipientToken()) node.put("recipientToken", slots.recipientToken)
        if (slots.hasMessage()) node.put("message", slots.message)
        if (slots.hasDestination()) node.put("destination", slots.destination)
        if (slots.hasTitle()) node.put("title", slots.title)
        if (slots.hasStartEpochMs()) node.put("startEpochMs", slots.startEpochMs)
        if (slots.hasEndEpochMs()) node.put("endEpochMs", slots.endEpochMs)
        if (slots.hasZoneId()) node.put("zoneId", slots.zoneId)
        if (slots.hasQuery()) node.put("query", slots.query)
        return node
    }

    private fun redactedFromJson(event: JsonNode): RedactedNotification {
        val builder = RedactedNotification.newBuilder()
            .setEventId(EventId.parse(event.path("eventId").asText()).value)
            .setKind(NotificationKind.fromWire(event.path("kind").asText()).wireValue)
            .setContentFingerprint(Validation.requireFingerprint(event.path("contentFingerprint").asText(), "contentFingerprint"))
        setOptionalNullableString(event, "redactedTitle", builder::setRedactedTitle)
        setOptionalNullableString(event, "redactedBody", builder::setRedactedBody)
        return builder.build()
    }

    private fun setOptionalNullableString(
        node: JsonNode,
        field: String,
        setter: (String) -> Any,
    ) {
        if (!node.has(field) || node.get(field).isNull) {
            return
        }
        if (!node.get(field).isTextual) {
            throw DomainInvariantException(ReasonCode.SCHEMA_INVALID, "$field must be string or null")
        }
        setter(node.get(field).asText())
    }

    private fun copyOptionalString(node: JsonNode, field: String, setter: (String) -> Any) {
        if (!node.has(field) || node.get(field).isNull) return
        setter(node.get(field).asText())
    }

    private fun copyOptionalInt64(node: JsonNode, field: String, setter: (Long) -> Any) {
        if (!node.has(field) || node.get(field).isNull) return
        val value = node.get(field)
        if (!value.isIntegralNumber) {
            throw DomainInvariantException(ReasonCode.SCHEMA_INVALID, "$field must be an integer JSON number")
        }
        setter(value.asLong())
    }

    private fun IntentSlots.recipientTokenOrNull(): String? = if (hasRecipientToken()) recipientToken else null
    private fun IntentSlots.messageOrNull(): String? = if (hasMessage()) message else null
    private fun IntentSlots.destinationOrNull(): String? = if (hasDestination()) destination else null
    private fun IntentSlots.titleOrNull(): String? = if (hasTitle()) title else null
    private fun IntentSlots.zoneIdOrNull(): String? = if (hasZoneId()) zoneId else null
    private fun IntentSlots.queryOrNull(): String? = if (hasQuery()) query else null

    private fun rejectNonFinite(node: JsonNode) {
        val stack = ArrayDeque<JsonNode>()
        stack.add(node)
        while (stack.isNotEmpty()) {
            val current = stack.removeFirst()
            when {
                current.isNumber && !current.doubleValue().isFinite() -> {
                    throw DomainInvariantException(ReasonCode.NON_FINITE_NUMBER, "non-finite number")
                }
                current.isArray || current.isObject -> current.forEach { stack.add(it) }
            }
        }
    }
}
