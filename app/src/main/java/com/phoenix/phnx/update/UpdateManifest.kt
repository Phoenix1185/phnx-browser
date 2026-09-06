package com.phoenix.phnx.update

import java.nio.charset.StandardCharsets

enum class UpdateChannel {
    STABLE,
    BETA,
    NIGHTLY,
    ;

    companion object {
        fun fromValue(value: String): UpdateChannel? = values().firstOrNull {
            it.name.equals(value.trim(), ignoreCase = true)
        }
    }
}

enum class UpdateType {
    FULL,
    DELTA,
}

data class UpdateManifest(
    val product: String,
    val platform: String,
    val architecture: String,
    val channel: UpdateChannel,
    val latestVersion: String,
    val latestVersionCode: Int,
    val minimumSupportedVersionCode: Int,
    val updateType: UpdateType,
    val mandatory: Boolean,
    val fullApkUrl: String,
    val fullApkSha256: String,
    val deltaUrl: String? = null,
    val deltaSha256: String? = null,
    val signature: String? = null,
) {
    /** Stable, signature-independent representation of the manifest fields. */
    fun canonicalPayload(): ByteArray = canonicalPayloadString().toByteArray(StandardCharsets.UTF_8)

    private fun canonicalPayloadString(): String = listOf(
        "product" to product,
        "platform" to platform,
        "architecture" to architecture,
        "channel" to channel.name.lowercase(),
        "latestVersion" to latestVersion,
        "latestVersionCode" to latestVersionCode.toString(),
        "minimumSupportedVersionCode" to minimumSupportedVersionCode.toString(),
        "updateType" to updateType.name.lowercase(),
        "mandatory" to mandatory.toString(),
        "fullApkUrl" to fullApkUrl,
        "fullApkSha256" to fullApkSha256,
        "deltaUrl" to (deltaUrl ?: ""),
        "deltaSha256" to (deltaSha256 ?: ""),
    ).joinToString("\n") { (key, value) -> "$key=${escape(value)}" }

    private fun escape(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
}

internal fun UpdateManifest.hasValidArtifactMetadata(): Boolean {
    if (!fullApkUrl.startsWith("https://") || !SHA256_PATTERN.matches(fullApkSha256)) return false
    return if (updateType == UpdateType.DELTA) {
        deltaUrl?.startsWith("https://") == true &&
            deltaSha256?.let { SHA256_PATTERN.matches(it) } == true
    } else {
        deltaUrl == null && deltaSha256 == null
    }
}

private val SHA256_PATTERN = Regex("[a-fA-F0-9]{64}")
