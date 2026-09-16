package app.projectzero.domain.versioning

import app.projectzero.domain.Validation

/**
 * H14: version DB, wire/domain contracts, model, prompt, registry, policy, and cache independently.
 */
data class CompatibilityVersions(
    val domainContractVersion: Int,
    val wireProtocolVersion: Int,
    val databaseSchemaVersion: Int,
    val modelVersion: String,
    val promptVersion: String,
    val registryVersion: Long,
    val policyVersion: Long,
    val cacheFormatVersion: Int,
) {
    init {
        Validation.requireRevision(domainContractVersion.toLong(), "domainContractVersion")
        Validation.requireRevision(wireProtocolVersion.toLong(), "wireProtocolVersion")
        Validation.requireRevision(databaseSchemaVersion.toLong(), "databaseSchemaVersion")
        Validation.requireNonBlank(modelVersion, "modelVersion")
        Validation.requireNonBlank(promptVersion, "promptVersion")
        Validation.requireRevision(registryVersion, "registryVersion")
        Validation.requireRevision(policyVersion, "policyVersion")
        Validation.requireRevision(cacheFormatVersion.toLong(), "cacheFormatVersion")
    }
}

object ProtocolSupport {
    const val CURRENT_WIRE_GENERATION = 1
    const val PREVIOUS_WIRE_GENERATION = 1
    const val MIN_SUPPORTED_WIRE_GENERATION = 1

    fun isSupported(wireProtocolVersion: Int): Boolean =
        wireProtocolVersion in MIN_SUPPORTED_WIRE_GENERATION..CURRENT_WIRE_GENERATION
}
