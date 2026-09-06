package com.phoenix.phnx.update

import java.io.InputStream
import java.security.MessageDigest
import java.security.PublicKey
import java.security.Signature
import java.util.Base64

object ArtifactVerifier {
    fun verify(
        input: InputStream,
        expectedSha256: String,
        encodedSignature: String,
        publicKey: PublicKey,
    ): Boolean {
        if (!SHA256_PATTERN.matches(expectedSha256)) return false
        val signatureBytes = runCatching { Base64.getDecoder().decode(encodedSignature) }.getOrNull()
            ?: return false
        return runCatching {
            val digest = MessageDigest.getInstance("SHA-256")
            val verifier = Signature.getInstance("SHA256withECDSA").apply { initVerify(publicKey) }
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
                verifier.update(buffer, 0, read)
            }
            digest.digest().toHex() == expectedSha256.lowercase() && verifier.verify(signatureBytes)
        }.getOrDefault(false)
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private const val DEFAULT_BUFFER_SIZE = 16 * 1024
    private val SHA256_PATTERN = Regex("[a-fA-F0-9]{64}")
}
