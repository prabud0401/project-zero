package app.projectzero.registry

import app.projectzero.domain.action.ActionCapability
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64

class RegistryVerifierTest {
    private val privateKey = KeyFactory.getInstance("EC").generatePrivate(
        PKCS8EncodedKeySpec(Base64.getUrlDecoder().decode(ShippedRegistry.TRUST_PRIVATE_KEY_B64)),
    )
    private val last = ShippedRegistry.lastKnownGood()
    private val verifier = RegistryVerifier(ShippedRegistry.trustRoot(), last, clock = { 1_700_000_000_000L })

    @Test
    fun acceptsSignedLastKnownGood() {
        val signed = RegistrySigner.sign(last, privateKey)
        val result = verifier.verify(signed)
        assertTrue(result is RegistryVerifyResult.Accepted)
    }

    @Test
    fun rejectsTamperedPayload() {
        val signed = RegistrySigner.sign(last, privateKey)
        val tampered = last.copy(registryVersion = 2)
        val result = verifier.verify(SignedRegistry(tampered, signed.signature))
        assertTrue((result as RegistryVerifyResult.Rejected).reason == RegistryRejectReason.TAMPERED)
    }

    @Test
    fun rejectsExpired() {
        val expired = last.copy(expiresAtEpochMs = 1L)
        val signed = RegistrySigner.sign(expired, privateKey)
        val result = verifier.verify(signed)
        assertTrue((result as RegistryVerifyResult.Rejected).reason == RegistryRejectReason.EXPIRED)
    }

    @Test
    fun rejectsFutureIncompatibleSchema() {
        val future = last.copy(minClientVersion = 99)
        val signed = RegistrySigner.sign(future, privateKey)
        val result = verifier.verify(signed)
        assertTrue((result as RegistryVerifyResult.Rejected).reason == RegistryRejectReason.FUTURE_INCOMPATIBLE)
    }

    @Test
    fun rejectsRollback() {
        val older = last.copy(registryVersion = 1)
        val newerLast = last.copy(registryVersion = 2)
        val signed = RegistrySigner.sign(older, privateKey)
        val rolling = RegistryVerifier(ShippedRegistry.trustRoot(), newerLast, clock = { 1_700_000_000_000L })
        val result = rolling.verify(signed)
        assertTrue((result as RegistryVerifyResult.Rejected).reason == RegistryRejectReason.ROLLBACK)
    }

    @Test
    fun rejectsUnknownKey() {
        val other = last.copy(keyId = "other")
        val signed = RegistrySigner.sign(other, privateKey)
        val result = verifier.verify(signed)
        assertTrue((result as RegistryVerifyResult.Rejected).reason == RegistryRejectReason.UNKNOWN_KEY)
    }

    @Test
    fun rejectsCapabilityEscalation() {
        val escalated = last.copy(
            entries = last.entries + last.entries.first().copy(
                entryId = "evil",
                capability = ActionCapability.DIAL,
                packageName = "com.evil.dial",
                allowedSchemes = setOf("tel"),
            ),
        )
        val shipped = setOf(ActionCapability.NAVIGATE, ActionCapability.WEB_SEARCH)
        val strict = RegistryVerifier(
            ShippedRegistry.trustRoot(),
            last,
            shippedCapabilities = shipped,
            clock = { 1_700_000_000_000L },
        )
        val signed = RegistrySigner.sign(escalated, privateKey)
        val result = strict.verify(signed)
        assertTrue((result as RegistryVerifyResult.Rejected).reason == RegistryRejectReason.CAPABILITY_ESCALATION)
    }

    @Test
    fun lastKnownGoodRemainsOperationalAfterReject() {
        val signed = RegistrySigner.sign(last.copy(keyId = "nope"), privateKey)
        verifier.verify(signed)
        assertTrue(verifier.operational().registryVersion == 1L)
    }
}
