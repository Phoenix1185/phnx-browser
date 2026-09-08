package com.phoenix.phnx

import android.content.Context
import android.content.pm.PackageManager
import java.security.MessageDigest

/** Verifies the Android package identity and the signer that built the production artifact. */
object AppIntegrityVerifier {
    const val FAILURE_MESSAGE = "Modified or unofficial build detected"

    fun verify(context: Context): Boolean {
        if (BuildConfig.DEBUG) return true
        if (BuildConfig.APPLICATION_ID != OFFICIAL_PACKAGE_NAME || context.packageName != OFFICIAL_PACKAGE_NAME) {
            return false
        }
        val expectedCertificate = BuildConfig.EXPECTED_RELEASE_CERTIFICATE_SHA256.lowercase()
        if (!SHA256_PATTERN.matches(expectedCertificate)) return false

        return runCatching {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNING_CERTIFICATES,
            )
            if (packageInfo.packageName != OFFICIAL_PACKAGE_NAME ||
                packageInfo.versionName != BuildConfig.VERSION_NAME ||
                packageInfo.longVersionCode != BuildConfig.VERSION_CODE.toLong()
            ) {
                return@runCatching false
            }

            val signingInfo = packageInfo.signingInfo ?: return@runCatching false
            if (signingInfo.hasMultipleSigners()) return@runCatching false
            val signers = signingInfo.apkContentsSigners
            if (signers.size != 1) return@runCatching false

            val actualCertificate = MessageDigest.getInstance("SHA-256")
                .digest(signers.single().toByteArray())
                .toHex()
            MessageDigest.isEqual(
                expectedCertificate.toByteArray(Charsets.US_ASCII),
                actualCertificate.toByteArray(Charsets.US_ASCII),
            )
        }.getOrDefault(false)
    }

    private fun ByteArray.toHex(): String = joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }

    private const val OFFICIAL_PACKAGE_NAME = "com.phoenix.phnx"
    private val SHA256_PATTERN = Regex("[a-f0-9]{64}")
}
