package com.phoenix.phnx.identity

data class ClientHintsConfig(
    val platform: String,
    val mobile: Boolean,
    val brands: List<String>,
)

/** The single runtime and persisted model for a selected device profile. */
data class DeviceProfile(
    val profileId: String,
    val presetId: String,
    val name: String,
    val userAgent: String,
    val platform: String,
    val operatingSystem: String,
    val viewportWidth: Int,
    val viewportHeight: Int,
    val screenWidth: Int,
    val screenHeight: Int,
    val colorDepth: Int,
    val deviceScaleFactor: Double,
    val locale: String,
    val language: String,
    val languages: List<String>,
    val timezone: String,
    val touchSupport: Boolean,
    val mobileMode: Boolean,
    val clientHints: ClientHintsConfig,
)

/** Source compatibility for the existing profile-scoped identity APIs. */
typealias BrowserIdentityConfig = DeviceProfile
