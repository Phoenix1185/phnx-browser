package com.phoenix.phnx.update

import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

object SignatureVerifier {
    private const val ALGORITHM = "SHA256withECDSA"
    private val embeddedPublicKey: PublicKey? by lazy {
        parseEcPublicKey(UpdateSigningKey.PUBLIC_KEY_BASE64_DER)
    }

    fun verify(manifest: UpdateManifest): Boolean = embeddedPublicKey?.let {
        verify(manifest, it)
    } == true

    fun verify(manifest: UpdateManifest, publicKey: PublicKey): Boolean {
        val encodedSignature = manifest.signature ?: return false
        val signatureBytes = runCatching { Base64.getDecoder().decode(encodedSignature) }.getOrNull()
            ?: return false

        return runCatching {
            Signature.getInstance(ALGORITHM).run {
                initVerify(publicKey)
                update(manifest.canonicalPayload())
                verify(signatureBytes)
            }
        }.getOrDefault(false)
    }

    fun parseEcPublicKey(base64Der: String): PublicKey? = runCatching {
        val der = Base64.getDecoder().decode(base64Der)
        KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(der))
    }.getOrNull()
}
