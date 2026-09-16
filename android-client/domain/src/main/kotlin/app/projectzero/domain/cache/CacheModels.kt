package app.projectzero.domain.cache

import app.projectzero.domain.ConsentEpoch
import app.projectzero.domain.DataEpoch
import app.projectzero.domain.DomainInvariantException
import app.projectzero.domain.InstallationId
import app.projectzero.domain.ReasonCode
import app.projectzero.domain.Validation
import app.projectzero.domain.action.ActionCapability

enum class CacheLayer {
    DEVICE_EXACT,
    DEVICE_VECTOR,
    SERVER_EXACT,
    SERVER_SEMANTIC,
    ;

    val wireValue: String get() = name
}

data class CachePartition(
    val layer: CacheLayer,
    val profileScope: String,
    val dataEpoch: DataEpoch,
    val installationId: InstallationId?,
    val consentEpoch: ConsentEpoch?,
) {
    init {
        Validation.requireNonBlank(profileScope, "profileScope")
        if (layer == CacheLayer.SERVER_EXACT || layer == CacheLayer.SERVER_SEMANTIC) {
            if (installationId == null || consentEpoch == null) {
                throw DomainInvariantException(
                    ReasonCode.SCHEMA_INVALID,
                    "server caches must be partitioned by installation and consent epoch",
                )
            }
        }
    }
}

data class CacheVersionBinding(
    val modelVersion: String,
    val promptVersion: String,
    val redactionPolicyVersion: String,
    val registryVersion: Long,
    val schemaVersion: Int,
    val locale: String,
    val capability: ActionCapability?,
) {
    init {
        Validation.requireNonBlank(modelVersion, "modelVersion")
        Validation.requireNonBlank(promptVersion, "promptVersion")
        Validation.requireNonBlank(redactionPolicyVersion, "redactionPolicyVersion")
        Validation.requireRevision(registryVersion, "registryVersion")
        Validation.requireSchemaV1(schemaVersion)
        Validation.requireLocale(locale, "locale")
    }
}

data class CacheTemplate(
    val templateId: String,
    val positionalSourceRefs: List<Int>,
) {
    init {
        Validation.requireNonBlank(templateId, "templateId")
        positionalSourceRefs.forEach { Validation.requireNonNegative(it, "positionalSourceRef") }
    }
}
