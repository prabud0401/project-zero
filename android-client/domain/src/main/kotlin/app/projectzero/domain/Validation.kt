package app.projectzero.domain

object Validation {
    const val SCHEMA_VERSION_V1 = 1
    const val ACTION_TTL_MS = 5L * 60L * 1000L
    const val UUID_V7_PATTERN =
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-7[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$"
    private val uuidV7 = Regex(UUID_V7_PATTERN)
    private val base64UrlSha256 = Regex("^[A-Za-z0-9_-]{43}$")
    private val androidPackage = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)*$")
    val localePattern = Regex("^[A-Za-z]{2,3}(-[A-Za-z0-9]{2,8})*$")

    fun codePointCount(value: String): Int = value.codePointCount(0, value.length)

    fun requireSchemaV1(schemaVersion: Int) {
        if (schemaVersion != SCHEMA_VERSION_V1) {
            throw DomainInvariantException(
                ReasonCode.UNSUPPORTED_SCHEMA,
                "schemaVersion must be $SCHEMA_VERSION_V1, got $schemaVersion",
            )
        }
    }

    fun requireUuidV7(value: String, field: String): String {
        if (!uuidV7.matches(value)) {
            throw DomainInvariantException(
                ReasonCode.INVALID_UUID,
                "$field must be a UUIDv7, got '$value'",
            )
        }
        return value
    }

    fun requireCodePointsAtMost(value: String, max: Int, field: String) {
        val count = codePointCount(value)
        if (count > max) {
            throw DomainInvariantException(
                ReasonCode.UNICODE_LIMIT,
                "$field exceeds $max Unicode code points ($count)",
            )
        }
    }

    fun requireCodePointsIn(value: String, min: Int, max: Int, field: String) {
        val count = codePointCount(value)
        if (count < min || count > max) {
            throw DomainInvariantException(
                ReasonCode.UNICODE_LIMIT,
                "$field must be $min..$max Unicode code points ($count)",
            )
        }
    }

    fun requireNonBlank(value: String, field: String): String {
        if (value.isBlank()) {
            throw DomainInvariantException(ReasonCode.SCHEMA_INVALID, "$field must be non-blank")
        }
        return value
    }

    fun requireEpochMs(value: Long, field: String): Long {
        if (value < 0L) {
            throw DomainInvariantException(ReasonCode.TIMESTAMP_INVALID, "$field must be >= 0")
        }
        return value
    }

    fun requireRevision(value: Long, field: String): Long {
        if (value < 1L) {
            throw DomainInvariantException(ReasonCode.STALE_REVISION, "$field must be >= 1")
        }
        return value
    }

    fun requireFiniteUnitInterval(value: Double, field: String): Double {
        if (!value.isFinite() || value < 0.0 || value > 1.0) {
            throw DomainInvariantException(
                ReasonCode.CONFIDENCE_INVALID,
                "$field must be a finite value in [0.0, 1.0], got $value",
            )
        }
        return value
    }

    fun requireIntIn(value: Int, min: Int, max: Int, field: String): Int {
        if (value < min || value > max) {
            throw DomainInvariantException(
                ReasonCode.BOUNDS_EXCEEDED,
                "$field must be $min..$max, got $value",
            )
        }
        return value
    }

    fun requireNonNegative(value: Int, field: String): Int {
        if (value < 0) {
            throw DomainInvariantException(ReasonCode.BOUNDS_EXCEEDED, "$field must be >= 0")
        }
        return value
    }

    fun requireNonNegative(value: Long, field: String): Long {
        if (value < 0L) {
            throw DomainInvariantException(ReasonCode.BOUNDS_EXCEEDED, "$field must be >= 0")
        }
        return value
    }

    fun requireFingerprint(value: String, field: String): String {
        if (!base64UrlSha256.matches(value)) {
            throw DomainInvariantException(
                ReasonCode.SCHEMA_INVALID,
                "$field must be 43-character unpadded base64url SHA-256",
            )
        }
        return value
    }

    fun requireOptionalFingerprint(value: String?, field: String): String? =
        value?.let { requireFingerprint(it, field) }

    fun requirePackageName(value: String, field: String): String {
        if (!androidPackage.matches(value) || value.length > DomainBounds.MAX_PACKAGE_NAME_LENGTH) {
            throw DomainInvariantException(ReasonCode.SCHEMA_INVALID, "$field is not a valid package name")
        }
        return value
    }

    fun requireLocale(value: String, field: String): String {
        if (!localePattern.matches(value)) {
            throw DomainInvariantException(ReasonCode.UNSUPPORTED_LOCALE, "$field is not a valid locale tag")
        }
        return value
    }

    fun requireSizeAtMost(size: Int, max: Int, field: String) {
        if (size > max) {
            throw DomainInvariantException(
                ReasonCode.BOUNDS_EXCEEDED,
                "$field size $size exceeds $max",
            )
        }
    }
}
