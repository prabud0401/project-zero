package app.projectzero.contracts

import app.projectzero.domain.DomainBounds
import app.projectzero.domain.DomainInvariantException
import app.projectzero.domain.ReasonCode
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.StreamReadConstraints
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.json.JsonMapper
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

object StrictJson {
    private val mapper: JsonMapper = JsonMapper.builder()
        .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
        .build()
        .apply {
            factory.setStreamReadConstraints(
                StreamReadConstraints.builder()
                    .maxNestingDepth(32)
                    .maxStringLength(DomainBounds.MAX_EXPANDED_BODY_BYTES)
                    .maxDocumentLength(DomainBounds.MAX_EXPANDED_BODY_BYTES.toLong())
                    .build(),
            )
        }

    fun parseUtf8(bytes: ByteArray): JsonNode {
        if (bytes.size > DomainBounds.MAX_EXPANDED_BODY_BYTES) {
            throw DomainInvariantException(ReasonCode.OVERSIZED, "expanded JSON exceeds 256 KiB")
        }
        val decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        val text = try {
            decoder.decode(java.nio.ByteBuffer.wrap(bytes)).toString()
        } catch (_: CharacterCodingException) {
            throw DomainInvariantException(ReasonCode.INVALID_UTF8, "payload is not valid UTF-8")
        }
        return parse(text)
    }

    fun parse(text: String): JsonNode {
        if (text.length > DomainBounds.MAX_EXPANDED_BODY_BYTES) {
            throw DomainInvariantException(ReasonCode.OVERSIZED, "expanded JSON exceeds 256 KiB")
        }
        return try {
            mapper.readTree(text)
        } catch (duplicate: com.fasterxml.jackson.core.JsonParseException) {
            val message = duplicate.message.orEmpty()
            if (message.contains("Duplicate field", ignoreCase = true)) {
                throw DomainInvariantException(ReasonCode.DUPLICATE_KEY, message)
            }
            throw DomainInvariantException(ReasonCode.SCHEMA_INVALID, message)
        }
    }

    fun canonicalize(node: JsonNode): String {
        val out = StringBuilder()
        writeCanonical(node, out)
        return out.toString()
    }

    private fun writeCanonical(node: JsonNode, out: StringBuilder) {
        when {
            node.isNull -> out.append("null")
            node.isBoolean -> out.append(if (node.booleanValue()) "true" else "false")
            node.isNumber -> {
                val n = node.decimalValue()
                out.append(n.stripTrailingZeros().toPlainString())
            }
            node.isTextual -> out.append(mapper.writeValueAsString(node.textValue()))
            node.isArray -> {
                out.append('[')
                node.forEachIndexed { index, child ->
                    if (index > 0) out.append(',')
                    writeCanonical(child, out)
                }
                out.append(']')
            }
            node.isObject -> {
                out.append('{')
                val fields = node.properties().sortedBy { it.key }
                fields.forEachIndexed { index, field ->
                    if (index > 0) out.append(',')
                    out.append(mapper.writeValueAsString(field.key))
                    out.append(':')
                    writeCanonical(field.value, out)
                }
                out.append('}')
            }
            else -> throw DomainInvariantException(ReasonCode.SCHEMA_INVALID, "unsupported JSON token")
        }
    }
}
