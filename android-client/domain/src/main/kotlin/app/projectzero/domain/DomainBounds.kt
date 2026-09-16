package app.projectzero.domain

/** Initial H12 policy limits. These are contracts, not measured capacity. */
object DomainBounds {
    const val MAX_INGESTION_ITEMS = 256
    const val MAX_ACTIVE_EVENTS_PER_PROFILE = 1_000
    const val MAX_EVENTS_PER_SUMMARY_BATCH = 50
    const val MAX_CLUSTERS_PER_RESPONSE = 50
    const val MAX_IN_FLIGHT_SUMMARY_PER_INSTALLATION = 1
    const val MAX_IN_FLIGHT_INTENT_PER_INSTALLATION = 1
    const val MAX_PEOPLE_TOKENS = 16
    const val MAX_RANKED_PACKAGES = 8
    const val MAX_TITLE_CODE_POINTS = 160
    const val MAX_BODY_CODE_POINTS = 2_000
    const val MIN_HEADLINE_CODE_POINTS = 1
    const val MAX_HEADLINE_CODE_POINTS = 80
    const val MIN_SUMMARY_CODE_POINTS = 1
    const val MAX_SUMMARY_CODE_POINTS = 280
    const val MAX_RATIONALE_CODE_POINTS = 160
    const val MAX_CAPABILITY_HINTS = 7
    const val MIN_PRIORITY = 0
    const val MAX_PRIORITY = 100
    const val MAX_URI_CODE_POINTS = 4_096
    const val MAX_REDACTED_TEXT_CODE_POINTS = 2_000
    const val MAX_SLOT_RECIPIENT_CODE_POINTS = 128
    const val MAX_SLOT_DESTINATION_CODE_POINTS = 300
    const val MAX_SLOT_ZONE_CODE_POINTS = 64
    const val MAX_SLOT_QUERY_CODE_POINTS = 500
    const val MAX_PACKAGE_NAME_LENGTH = 256
    const val MAX_EXPANDED_BODY_BYTES = 256 * 1024
    const val MAX_COMPRESSED_BODY_BYTES = 64 * 1024
    const val OPERATION_RETENTION_MS = 10L * 60L * 1000L
    const val TRANSPORT_CLOCK_SKEW_MS = 120_000L
    const val NONCE_RETENTION_MS = 10L * 60L * 1000L
    const val INSTALLATION_TOKEN_MAX_LIFETIME_MS = 15L * 60L * 1000L
    const val MIN_RELATED_EVENTS_FOR_CLOUD_SUMMARY = 4
}
