package com.phoenix.phnx.update

import java.io.InputStream
import java.security.MessageDigest

object ChecksumVerifier {
    fun verify(input: InputStream, expectedSha256: String): Boolean {
        if (!SHA256_PATTERN.matches(expectedSha256)) return false
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
        return digest.digest().toHex() == expectedSha256.lowercase()
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private const val DEFAULT_BUFFER_SIZE = 16 * 1024
    private val SHA256_PATTERN = Regex("[a-fA-F0-9]{64}")
}
