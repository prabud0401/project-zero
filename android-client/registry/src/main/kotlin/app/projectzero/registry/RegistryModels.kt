package app.projectzero.registry

import app.projectzero.domain.DomainBounds
import app.projectzero.domain.Validation
import app.projectzero.domain.action.ActionCapability
import app.projectzero.domain.action.AllowlistedUriSchemes

data class RegistryEntry(
    val entryId: String,
    val capability: ActionCapability,
    val packageName: String,
    val certificateSha256Pins: List<String> = emptyList(),
    val minVersionCode: Long = 1,
    val allowedSchemes: Set<String>,
    val allowedHosts: Set<String> = emptySet(),
    val allowedPathRegexes: List<Regex> = emptyList(),
    val parameterMappings: Map<String, String> = emptyMap(),
    val fallbackId: String? = null,
    val expiresAtEpochMs: Long,
    val disabled: Boolean = false,
) {
    init {
        Validation.requireNonBlank(entryId, "entryId")
        Validation.requirePackageName(packageName, "packageName")
        allowedSchemes.forEach { scheme ->
            val normalized = scheme.lowercase()
            require(normalized in AllowlistedUriSchemes.VALUES) { "scheme not allowlisted: $scheme" }
            require(normalized !in AllowlistedUriSchemes.FORBIDDEN) { "forbidden scheme: $scheme" }
        }
        certificateSha256Pins.forEach { Validation.requireFingerprint(it, "certificatePin") }
        Validation.requireEpochMs(expiresAtEpochMs, "expiresAtEpochMs")
        Validation.requireNonNegative(minVersionCode, "minVersionCode")
    }
}

data class RegistrySnapshot(
    val registryVersion: Long,
    val keyId: String,
    val schema: Int,
    val expiresAtEpochMs: Long,
    val minClientVersion: Int,
    val entries: List<RegistryEntry>,
) {
    init {
        Validation.requireRevision(registryVersion, "registryVersion")
        Validation.requireNonBlank(keyId, "keyId")
        Validation.requireSchemaV1(schema)
        Validation.requireEpochMs(expiresAtEpochMs, "expiresAtEpochMs")
        require(minClientVersion >= 1) { "minClientVersion must be >= 1" }
        val ids = entries.map { it.entryId }
        require(ids.size == ids.toSet().size) { "duplicate registry entry ids" }
        require(entries.size <= 64) { "too many registry entries" }
        Validation.requireSizeAtMost(entries.size, DomainBounds.MAX_RANKED_PACKAGES * 8, "entries")
    }

    fun canonicalPayload(): ByteArray = CanonicalJson.encode(this)
}

data class SignedRegistry(
    val snapshot: RegistrySnapshot,
    val signature: ByteArray,
)

enum class RegistryRejectReason {
    TAMPERED,
    EXPIRED,
    FUTURE_INCOMPATIBLE,
    ROLLBACK,
    UNKNOWN_KEY,
    CAPABILITY_ESCALATION,
    MALFORMED,
}
