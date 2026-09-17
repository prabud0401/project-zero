package app.projectzero.registry

import app.projectzero.domain.action.ActionCapability

object ShippedRegistry {
    const val TRUST_KEY_ID = "lkg-v1"
    const val TRUST_PUBLIC_KEY_B64 =
        "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE1FbGjcoWEz6KZ12ULsCANX19Raol_D-xxU8REVtZGP7Zm9I5afAryb2TInWt5vzSvsCl0UWqzL_OpmXtO3Unug"

    /** Test-only. Production signing is offline; this material is for unit fixtures. */
    internal const val TRUST_PRIVATE_KEY_B64 =
        "MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCB4M05Utchw8J40kqHf372IiMeIwxiASqCug-j5yP1Y8w"

    const val FAR_FUTURE_MS = 4_102_444_800_000L

    fun lastKnownGood(): RegistrySnapshot = RegistrySnapshot(
        registryVersion = 1,
        keyId = TRUST_KEY_ID,
        schema = 1,
        expiresAtEpochMs = FAR_FUTURE_MS,
        minClientVersion = 1,
        entries = listOf(
            RegistryEntry(
                entryId = "maps.geo",
                capability = ActionCapability.NAVIGATE,
                packageName = "com.google.android.apps.maps",
                allowedSchemes = setOf("geo", "https"),
                allowedHosts = setOf("www.google.com", "maps.google.com"),
                expiresAtEpochMs = FAR_FUTURE_MS,
            ),
            RegistryEntry(
                entryId = "search.https",
                capability = ActionCapability.WEB_SEARCH,
                packageName = "com.android.chrome",
                allowedSchemes = setOf("https"),
                allowedHosts = setOf("www.google.com"),
                expiresAtEpochMs = FAR_FUTURE_MS,
            ),
        ),
    )

    fun trustRoot(): TrustRoot = TrustRoot(
        keyId = TRUST_KEY_ID,
        publicKey = RegistryVerifier.publicKeyFromUrlBase64(TRUST_PUBLIC_KEY_B64),
    )
}
