package app.projectzero.registry

import java.security.PrivateKey
import java.security.Signature

object RegistrySigner {
    fun sign(snapshot: RegistrySnapshot, privateKey: PrivateKey): SignedRegistry {
        val signer = Signature.getInstance("SHA256withECDSA")
        signer.initSign(privateKey)
        signer.update(snapshot.canonicalPayload())
        return SignedRegistry(snapshot, signer.sign())
    }
}
