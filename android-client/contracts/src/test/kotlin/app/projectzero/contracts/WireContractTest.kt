package app.projectzero.contracts

import app.projectzero.contracts.proto.GetRegistryRequest
import app.projectzero.contracts.proto.GetRegistryResponse
import app.projectzero.contracts.proto.ReasoningProto
import app.projectzero.domain.DomainInvariantException
import app.projectzero.domain.ReasonCode
import app.projectzero.domain.action.ActionCapability
import app.projectzero.domain.notification.NotificationKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class WireContractTest {
    @Test
    fun goldenSummaryRequestRoundTripPreservesNullVersusEmpty() {
        val json = resource("golden/v1/summary-batch-request.json")
        val (proto, original) = PresenceMapper.summaryRequestFromJson(json)
        assertEquals(1, proto.schemaVersion)
        assertEquals(12L, proto.baseRevision)
        val event = proto.getEvents(0)
        assertFalse(event.hasRedactedTitle())
        assertTrue(event.hasRedactedBody())
        assertEquals("", event.redactedBody)
        val back = PresenceMapper.summaryRequestToJson(proto)
        assertTrue(back.path("events").get(0).path("redactedTitle").isNull)
        assertEquals("", back.path("events").get(0).path("redactedBody").asText())
        assertEquals(StrictJson.canonicalize(original), StrictJson.canonicalize(back))
    }

    @Test
    fun goldenSummaryResponseRoundTrip() {
        val json = resource("golden/v1/summary-batch-response.json")
        val (proto, original) = PresenceMapper.summaryResponseFromJson(json)
        assertEquals(1, proto.clustersCount)
        assertEquals(NotificationKind.MESSAGE.wireValue, proto.getClusters(0).kind)
        assertEquals(12L, proto.baseRevision)
        assertFalse(proto.usage.cacheHit)
        JsonSchemaCatalog.validate(JsonSchemaCatalog.SUMMARY_BATCH_RESPONSE, original)
    }

    @Test
    fun goldenIntentRouteRoundTripAndSlotPresence() {
        val request = PresenceMapper.intentRequestFromJson(resource("golden/v1/intent-route-request.json"))
        assertEquals(listOf("MESSAGE", "EMAIL"), request.capabilityHintsList)
        val (response, slots) = PresenceMapper.intentResponseFromJson(resource("golden/v1/intent-route-response.json"))
        assertEquals(ActionCapability.MESSAGE.wireValue, response.capability)
        assertTrue(response.slots.hasRecipientToken())
        assertTrue(response.slots.hasMessage())
        assertEquals("", response.slots.message)
        assertTrue(response.slots.hasStartEpochMs())
        assertEquals(0L, response.slots.startEpochMs)
        assertFalse(response.slots.hasQuery())
        assertEquals("", slots.message)
        assertEquals(0L, slots.startEpochMs)
        assertEquals(setOf("recipientToken", "message", "startEpochMs"), slots.presentNames())
        val slotJson = PresenceMapper.slotsToJson(response.slots)
        assertFalse(slotJson.has("query"))
        assertEquals("", slotJson.path("message").asText())
        assertEquals(0L, slotJson.path("startEpochMs").asLong())
    }

    @Test
    fun missingAndUnknownFieldsAreRejected() {
        val missing = """{"schemaVersion":1,"requestId":"018f0000-0000-7000-8000-000000000001","baseRevision":1,"locale":"en"}"""
        val missingEx = assertThrows(DomainInvariantException::class.java) {
            PresenceMapper.summaryRequestFromJson(missing)
        }
        assertEquals(ReasonCode.SCHEMA_INVALID, missingEx.reasonCode)

        val extra = resource("golden/v1/summary-batch-request.json").replace(
            "\"locale\": \"en-US\"",
            "\"locale\": \"en-US\", \"executableUri\": \"intent://evil\"",
        )
        val extraEx = assertThrows(DomainInvariantException::class.java) {
            PresenceMapper.summaryRequestFromJson(extra)
        }
        assertTrue(
            extraEx.reasonCode == ReasonCode.UNKNOWN_FIELD || extraEx.reasonCode == ReasonCode.SCHEMA_INVALID,
            extraEx.message,
        )
    }

    @Test
    fun duplicateKeysAreRejected() {
        val json = """{"schemaVersion":1,"schemaVersion":2,"requestId":"018f0000-0000-7000-8000-000000000001","baseRevision":1,"locale":"en","events":[{"eventId":"018f0000-0000-7000-8000-000000000002","kind":"MESSAGE","redactedTitle":null,"redactedBody":null,"contentFingerprint":"abcdefghijklmnopqrstuvwxyz0123456789-_ABCDE"}]}"""
        val ex = assertThrows(DomainInvariantException::class.java) { PresenceMapper.summaryRequestFromJson(json) }
        assertEquals(ReasonCode.DUPLICATE_KEY, ex.reasonCode)
    }

    @ParameterizedTest
    @ValueSource(strings = ["MESSAGE", "SOCIAL", "CALENDAR", "DELIVERY", "MEDIA", "SYSTEM", "OTHER"])
    fun notificationKindWireValuesAreAccepted(kind: String) {
        val json = resource("golden/v1/summary-batch-request.json").replace("\"MESSAGE\"", "\"$kind\"")
        val (proto, _) = PresenceMapper.summaryRequestFromJson(json)
        assertEquals(kind, proto.getEvents(0).kind)
        assertEquals(kind, NotificationKind.fromWire(kind).wireValue)
    }

    @ParameterizedTest
    @ValueSource(strings = ["MESSAGE", "NAVIGATE", "CREATE_CALENDAR_EVENT", "DIAL", "EMAIL", "WEB_SEARCH", "OPEN_APP"])
    fun capabilityWireValuesAreAccepted(capability: String) {
        val json = resource("golden/v1/intent-route-response.json").replace("\"MESSAGE\"", "\"$capability\"")
        val (proto, _) = PresenceMapper.intentResponseFromJson(json)
        assertEquals(capability, proto.capability)
    }

    @Test
    fun invalidUtf8IsRejected() {
        val bytes = byteArrayOf('{'.code.toByte(), 0xFF.toByte(), '}'.code.toByte())
        val ex = assertThrows(DomainInvariantException::class.java) { StrictJson.parseUtf8(bytes) }
        assertEquals(ReasonCode.INVALID_UTF8, ex.reasonCode)
    }

    @Test
    fun canonicalizationIsDeterministic() {
        val node = StrictJson.parse("""{"b":2,"a":1}""")
        assertEquals("""{"a":1,"b":2}""", StrictJson.canonicalize(node))
        assertEquals(StrictJson.canonicalize(node), StrictJson.canonicalize(StrictJson.parse(StrictJson.canonicalize(node))))
    }

    @Test
    fun uuidV4IsRejectedByDomainIdentityRules() {
        val json = resource("golden/v1/summary-batch-request.json")
            .replace("018f0000-0000-7000-8000-000000000001", "550e8400-e29b-41d4-a716-446655440000")
        assertThrows(DomainInvariantException::class.java) { PresenceMapper.summaryRequestFromJson(json) }
    }

    @Test
    fun registryAndReasoningServicesAreDefined() {
        val file = ReasoningProto.getDescriptor()
        assertEquals("GetRegistry", file.findServiceByName("RegistryService").findMethodByName("GetRegistry").name)
        assertEquals("Summarize", file.findServiceByName("ReasoningService").findMethodByName("Summarize").name)
        assertEquals("RouteIntent", file.findServiceByName("ReasoningService").findMethodByName("RouteIntent").name)
        val request = GetRegistryRequest.newBuilder().setSchemaVersion(1).setVersion(7).build()
        assertTrue(request.hasVersion())
        assertEquals(7L, request.version)
        val absent = GetRegistryRequest.newBuilder().setSchemaVersion(1).build()
        assertFalse(absent.hasVersion())
        val response = GetRegistryResponse.newBuilder().setSchemaVersion(1).build()
        assertEquals(1, response.schemaVersion)
        assertEquals(1, app.projectzero.contracts.proto.SummaryBatchRequest.getDescriptor().findFieldByName("schema_version").number)
        assertEquals(3, app.projectzero.contracts.proto.RedactedNotification.getDescriptor().findFieldByName("redacted_title").number)
        assertTrue(
            app.projectzero.contracts.proto.RedactedNotification.getDescriptor()
                .findFieldByName("redacted_title").hasPresence(),
        )
        assertTrue(
            app.projectzero.contracts.proto.IntentSlots.getDescriptor()
                .findFieldByName("start_epoch_ms").hasPresence(),
        )
    }

    @Test
    fun int64PresenceIsJsonNumberNotString() {
        val json = resource("golden/v1/summary-batch-request.json")
        val node = StrictJson.parse(json)
        assertTrue(node.path("baseRevision").isIntegralNumber)
        assertFalse(node.path("baseRevision").isTextual)
        val (proto, _) = PresenceMapper.summaryRequestFromJson(json)
        assertEquals(node.path("baseRevision").asLong(), proto.baseRevision)
    }

    private fun resource(path: String): String =
        checkNotNull(javaClass.classLoader.getResourceAsStream(path)).bufferedReader().use { it.readText() }
}
