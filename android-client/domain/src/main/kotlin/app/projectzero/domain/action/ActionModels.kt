package app.projectzero.domain.action

import app.projectzero.domain.ActionId
import app.projectzero.domain.DomainBounds
import app.projectzero.domain.DomainInvariantException
import app.projectzero.domain.ReasonCode
import app.projectzero.domain.Validation

enum class ActionCapability {
    MESSAGE,
    NAVIGATE,
    CREATE_CALENDAR_EVENT,
    DIAL,
    EMAIL,
    WEB_SEARCH,
    OPEN_APP,
    ;

    val wireValue: String get() = name

    companion object {
        fun fromWire(value: String): ActionCapability =
            entries.find { it.name == value }
                ?: throw DomainInvariantException(ReasonCode.INVALID_ENUM, "Unknown capability: $value")
    }
}

enum class FallbackResolution {
    NONE,
    VERIFIED_WEB_LINK,
    SYSTEM_GENERIC_INTENT,
    ANDROID_CHOOSER,
    ;

    val wireValue: String get() = name

    companion object {
        fun fromWire(value: String): FallbackResolution =
            entries.find { it.name == value }
                ?: throw DomainInvariantException(ReasonCode.INVALID_ENUM, "Unknown fallback: $value")
    }
}

enum class Confirmation {
    PREVIEW_REQUIRED,
    TARGET_APP_OWNS_COMMIT,
    ;

    val wireValue: String get() = name

    companion object {
        fun fromWire(value: String): Confirmation =
            entries.find { it.name == value }
                ?: throw DomainInvariantException(ReasonCode.INVALID_ENUM, "Unknown confirmation: $value")
    }
}

sealed interface ActionParameter {
    val key: String

    data class Text(override val key: String, val value: String) : ActionParameter {
        init {
            validateKey(key)
            Validation.requireCodePointsAtMost(value, DomainBounds.MAX_BODY_CODE_POINTS, "parameter.value")
        }
    }

    data class UriValue(override val key: String, val value: String) : ActionParameter {
        init {
            validateKey(key)
            Validation.requireCodePointsAtMost(value, DomainBounds.MAX_URI_CODE_POINTS, "parameter.uri")
        }
    }

    data class InstantValue(
        override val key: String,
        val epochMs: Long,
        val zoneId: String,
    ) : ActionParameter {
        init {
            validateKey(key)
            Validation.requireEpochMs(epochMs, "parameter.epochMs")
            Validation.requireNonBlank(zoneId, "parameter.zoneId")
            Validation.requireCodePointsAtMost(zoneId, DomainBounds.MAX_SLOT_ZONE_CODE_POINTS, "parameter.zoneId")
        }
    }

    data class StringList(override val key: String, val values: List<String>) : ActionParameter {
        init {
            validateKey(key)
            values.forEach { Validation.requireCodePointsAtMost(it, DomainBounds.MAX_BODY_CODE_POINTS, "parameter.values") }
        }
    }

    companion object {
        fun validateKey(key: String) {
            Validation.requireNonBlank(key, "parameter.key")
        }

        fun requireUniqueKeys(parameters: List<ActionParameter>) {
            val keys = parameters.map { it.key }
            if (keys.toSet().size != keys.size) {
                throw DomainInvariantException(ReasonCode.DUPLICATE_PARAMETER, "duplicate ActionParameter keys")
            }
        }
    }
}

object AllowlistedAndroidActions {
    val VALUES: Set<String> = setOf(
        "android.intent.action.SENDTO",
        "android.intent.action.SEND",
        "android.intent.action.VIEW",
        "android.intent.action.INSERT",
        "android.intent.action.DIAL",
        "android.intent.action.MAIN",
    )
}

object AllowlistedUriSchemes {
    val VALUES: Set<String> = setOf("smsto", "sms", "mailto", "tel", "geo", "https")
    val FORBIDDEN: Set<String> = setOf("intent", "file", "javascript")
}

data class IntentAction(
    val schemaVersion: Int,
    val actionId: ActionId,
    val capability: ActionCapability,
    val androidAction: String,
    val targetPackage: String?,
    val uriScheme: String?,
    val registryEntryId: String?,
    val parameters: List<ActionParameter>,
    val fallbackResolution: FallbackResolution,
    val confirmation: Confirmation,
    val rationale: String,
    val createdAtEpochMs: Long,
    val expiresAtEpochMs: Long,
) {
    init {
        Validation.requireSchemaV1(schemaVersion)
        if (androidAction !in AllowlistedAndroidActions.VALUES) {
            throw DomainInvariantException(ReasonCode.POLICY_DENIED, "androidAction is not allowlisted")
        }
        targetPackage?.let { Validation.requirePackageName(it, "targetPackage") }
        if (uriScheme != null) {
            val normalized = uriScheme.lowercase()
            if (normalized in AllowlistedUriSchemes.FORBIDDEN || normalized !in AllowlistedUriSchemes.VALUES) {
                throw DomainInvariantException(ReasonCode.POLICY_DENIED, "uriScheme is not allowlisted: $uriScheme")
            }
        }
        registryEntryId?.let { Validation.requireNonBlank(it, "registryEntryId") }
        ActionParameter.requireUniqueKeys(parameters)
        Validation.requireCodePointsAtMost(rationale, DomainBounds.MAX_RATIONALE_CODE_POINTS, "rationale")
        Validation.requireEpochMs(createdAtEpochMs, "createdAtEpochMs")
        Validation.requireEpochMs(expiresAtEpochMs, "expiresAtEpochMs")
        if (expiresAtEpochMs < createdAtEpochMs) {
            throw DomainInvariantException(ReasonCode.EXPIRY_INVALID, "expiresAtEpochMs must be >= createdAtEpochMs")
        }
        if (expiresAtEpochMs - createdAtEpochMs > Validation.ACTION_TTL_MS) {
            throw DomainInvariantException(
                ReasonCode.EXPIRY_INVALID,
                "expiresAtEpochMs must be <= createdAtEpochMs + 5 minutes",
            )
        }
    }

    fun isExpiredAt(nowElapsedOrWallMs: Long): Boolean = nowElapsedOrWallMs > expiresAtEpochMs
}

data class PackageAffinity(
    val packageName: String,
    val userPinned: Boolean,
    val successfulLaunches30d: Int,
    val lastUsedAtEpochMs: Long?,
    val score: Double,
) {
    init {
        Validation.requirePackageName(packageName, "packageName")
        Validation.requireNonNegative(successfulLaunches30d, "successfulLaunches30d")
        lastUsedAtEpochMs?.let { Validation.requireEpochMs(it, "lastUsedAtEpochMs") }
        Validation.requireFiniteUnitInterval(score, "score")
    }
}

data class AppAffinityContext(
    val schemaVersion: Int,
    val capability: ActionCapability,
    val rankedPackages: List<PackageAffinity>,
    val updatedAtEpochMs: Long,
) {
    init {
        Validation.requireSchemaV1(schemaVersion)
        Validation.requireSizeAtMost(rankedPackages.size, DomainBounds.MAX_RANKED_PACKAGES, "rankedPackages")
        val names = rankedPackages.map { it.packageName }
        if (names.toSet().size != names.size) {
            throw DomainInvariantException(ReasonCode.DUPLICATE_KEY, "rankedPackages package names must be unique")
        }
        Validation.requireEpochMs(updatedAtEpochMs, "updatedAtEpochMs")
    }
}
