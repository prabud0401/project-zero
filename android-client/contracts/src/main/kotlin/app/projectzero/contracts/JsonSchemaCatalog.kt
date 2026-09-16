package app.projectzero.contracts

import app.projectzero.domain.DomainInvariantException
import app.projectzero.domain.ReasonCode
import com.fasterxml.jackson.databind.JsonNode
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SchemaValidatorsConfig
import com.networknt.schema.SpecVersion
import com.networknt.schema.ValidationMessage

object JsonSchemaCatalog {
    const val SUMMARY_BATCH_REQUEST = "schemas/v1/summary-batch-request.json"
    const val SUMMARY_BATCH_RESPONSE = "schemas/v1/summary-batch-response.json"
    const val INTENT_ROUTE = "schemas/v1/intent-route.json"

    private val factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
    private val config = SchemaValidatorsConfig.builder()
        .formatAssertionsEnabled(true)
        .build()

    fun schema(resourcePath: String): JsonSchema {
        val stream = checkNotNull(javaClass.classLoader.getResourceAsStream(resourcePath)) {
            "Missing schema resource $resourcePath"
        }
        return stream.use { factory.getSchema(it, config) }
    }

    fun validate(resourcePath: String, node: JsonNode) {
        val errors: Set<ValidationMessage> = schema(resourcePath).validate(node)
        if (errors.isNotEmpty()) {
            val extraField = errors.any { it.message.contains("additional properties", ignoreCase = true) }
            val reason = if (extraField) ReasonCode.UNKNOWN_FIELD else ReasonCode.SCHEMA_INVALID
            throw DomainInvariantException(reason, errors.joinToString("; ") { it.message })
        }
    }
}
