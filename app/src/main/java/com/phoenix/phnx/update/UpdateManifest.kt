package com.phoenix.phnx.update

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
)
