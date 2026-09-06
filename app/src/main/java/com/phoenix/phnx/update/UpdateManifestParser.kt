package com.phoenix.phnx.update

import org.json.JSONObject

object UpdateManifestParser {
    fun parse(payload: String): UpdateManifest? = runCatching {
        val json = JSONObject(payload)
        val channel = UpdateChannel.fromValue(json.getString("channel")) ?: return null
        val updateType = runCatching {
            UpdateType.valueOf(json.getString("updateType").uppercase())
        }.getOrNull() ?: return null
        val manifest = UpdateManifest(
            product = json.getString("product"),
            platform = json.getString("platform"),
            architecture = json.getString("architecture"),
            channel = channel,
            latestVersion = json.getString("latestVersion"),
            latestVersionCode = json.getInt("latestVersionCode"),
            minimumSupportedVersionCode = json.getInt("minimumSupportedVersionCode"),
            updateType = updateType,
            mandatory = json.optBoolean("mandatory", false),
            fullApkUrl = json.getString("fullApkUrl"),
            fullApkSha256 = json.getString("fullApkSha256"),
            deltaUrl = json.optString("deltaUrl").takeIf { it.isNotBlank() },
            deltaSha256 = json.optString("deltaSha256").takeIf { it.isNotBlank() },
            signature = json.optString("signature").takeIf { it.isNotBlank() },
        )
        require(manifest.product == PRODUCT)
        require(manifest.platform == PLATFORM)
        require(manifest.latestVersionCode > 0)
        require(manifest.minimumSupportedVersionCode > 0)
        require(manifest.fullApkUrl.startsWith("https://"))
        require(SHA256_PATTERN.matches(manifest.fullApkSha256))
        require(manifest.deltaUrl == null || manifest.deltaUrl.startsWith("https://"))
        require(manifest.deltaSha256 == null || SHA256_PATTERN.matches(manifest.deltaSha256))
        manifest
    }.getOrNull()

    private const val PRODUCT = "phnx-browser"
    private const val PLATFORM = "android"
    private val SHA256_PATTERN = Regex("[a-fA-F0-9]{64}")
}
