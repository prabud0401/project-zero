package app.projectzero.registry

import app.projectzero.domain.action.ActionCapability
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

data class TrustRoot(
    val keyId: String,
    val publicKey: PublicKey,
)

sealed interface RegistryVerifyResult {
    data class Accepted(val snapshot: RegistrySnapshot) : RegistryVerifyResult
    data class Rejected(val reason: RegistryRejectReason) : RegistryVerifyResult
}

class RegistryVerifier(
    private val trustRoot: TrustRoot,
    private val lastKnownGood: RegistrySnapshot,
    private val clientVersion: Int = 1,
    private val shippedCapabilities: Set<ActionCapability> = ActionCapability.entries.toSet(),
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    fun verify(signed: SignedRegistry): RegistryVerifyResult {
        val snapshot = signed.snapshot
        if (snapshot.keyId != trustRoot.keyId) {
            return RegistryVerifyResult.Rejected(RegistryRejectReason.UNKNOWN_KEY)
        }
        if (snapshot.schema != 1) {
            return RegistryVerifyResult.Rejected(RegistryRejectReason.FUTURE_INCOMPATIBLE)
        }
        if (snapshot.minClientVersion > clientVersion) {
            return RegistryVerifyResult.Rejected(RegistryRejectReason.FUTURE_INCOMPATIBLE)
        }
        if (snapshot.registryVersion < lastKnownGood.registryVersion) {
            return RegistryVerifyResult.Rejected(RegistryRejectReason.ROLLBACK)
        }
        if (snapshot.expiresAtEpochMs < clock()) {
            return RegistryVerifyResult.Rejected(RegistryRejectReason.EXPIRED)
        }
        val escalated = snapshot.entries.any { it.capability !in shippedCapabilities }
        if (escalated) {
            return RegistryVerifyResult.Rejected(RegistryRejectReason.CAPABILITY_ESCALATION)
        }
        val ok = verifySignature(snapshot.canonicalPayload(), signed.signature, trustRoot.publicKey)
        if (!ok) {
            return RegistryVerifyResult.Rejected(RegistryRejectReason.TAMPERED)
        }
        return RegistryVerifyResult.Accepted(snapshot)
    }

    fun operational(): RegistrySnapshot = lastKnownGood

    companion object {
        fun publicKeyFromUrlBase64(value: String): PublicKey {
            val bytes = Base64.getUrlDecoder().decode(value)
            return KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(bytes))
        }

        fun verifySignature(payload: ByteArray, signature: ByteArray, publicKey: PublicKey): Boolean {
            return try {
                val verifier = Signature.getInstance("SHA256withECDSA")
                verifier.initVerify(publicKey)
                verifier.update(payload)
                verifier.verify(signature)
            } catch (_: Exception) {
                false
            }
        }
    }
}
