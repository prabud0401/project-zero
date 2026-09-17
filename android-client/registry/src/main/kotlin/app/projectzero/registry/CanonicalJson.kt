package app.projectzero.registry

import java.nio.charset.StandardCharsets

object CanonicalJson {
    fun encode(snapshot: RegistrySnapshot): ByteArray {
        val entries = snapshot.entries.sortedBy { it.entryId }.joinToString(",") { entry ->
            val pins = entry.certificateSha256Pins.sorted().joinToString(",") { jsonString(it) }
            val schemes = entry.allowedSchemes.map { it.lowercase() }.sorted().joinToString(",") { jsonString(it) }
            val hosts = entry.allowedHosts.map { it.lowercase() }.sorted().joinToString(",") { jsonString(it) }
            val paths = entry.allowedPathRegexes.map { it.pattern }.sorted().joinToString(",") { jsonString(it) }
            val mappings = entry.parameterMappings.toSortedMap().entries.joinToString(",") { (k, v) ->
                "${jsonString(k)}:${jsonString(v)}"
            }
            buildString {
                append("{")
                append("\"allowedHosts\":[$hosts],")
                append("\"allowedPathRegexes\":[$paths],")
                append("\"allowedSchemes\":[$schemes],")
                append("\"capability\":${jsonString(entry.capability.name)},")
                append("\"certificateSha256Pins\":[$pins],")
                append("\"disabled\":${entry.disabled},")
                append("\"entryId\":${jsonString(entry.entryId)},")
                append("\"expiresAtEpochMs\":${entry.expiresAtEpochMs},")
                append("\"fallbackId\":${entry.fallbackId?.let { jsonString(it) } ?: "null"},")
                append("\"minVersionCode\":${entry.minVersionCode},")
                append("\"packageName\":${jsonString(entry.packageName)},")
                append("\"parameterMappings\":{$mappings}")
                append("}")
            }
        }
        val json = buildString {
            append("{")
            append("\"entries\":[$entries],")
            append("\"expiresAtEpochMs\":${snapshot.expiresAtEpochMs},")
            append("\"keyId\":${jsonString(snapshot.keyId)},")
            append("\"minClientVersion\":${snapshot.minClientVersion},")
            append("\"registryVersion\":${snapshot.registryVersion},")
            append("\"schema\":${snapshot.schema}")
            append("}")
        }
        return json.toByteArray(StandardCharsets.UTF_8)
    }

    private fun jsonString(value: String): String {
        val escaped = value.replace("\\", "\\\\").replace("\"", "\\\"")
        return "\"$escaped\""
    }
}
