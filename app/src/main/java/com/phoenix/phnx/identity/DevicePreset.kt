package com.phoenix.phnx.identity

data class DevicePreset(
    val id: String,
    val name: String,
    val operatingSystem: String,
    val platform: String,
    val userAgent: String,
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
) {
    fun forProfile(profileId: String) = BrowserIdentityConfig(
        profileId = profileId,
        presetId = id,
        userAgent = userAgent,
        platform = platform,
        operatingSystem = operatingSystem,
        viewportWidth = viewportWidth,
        viewportHeight = viewportHeight,
        screenWidth = screenWidth,
        screenHeight = screenHeight,
        colorDepth = colorDepth,
        deviceScaleFactor = deviceScaleFactor,
        locale = locale,
        language = language,
        languages = languages,
        timezone = timezone,
        touchSupport = touchSupport,
        mobileMode = mobileMode,
        clientHints = clientHints,
    )
}

object DevicePresets {
    const val ANDROID_PHONE = "android_phone"
    const val ANDROID_TABLET = "android_tablet"
    const val DESKTOP = "desktop"

    private val presets = listOf(
        DevicePreset(
            id = ANDROID_PHONE,
            name = "Android Phone",
            operatingSystem = "Android 14",
            platform = "Android",
            userAgent = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
            viewportWidth = 412,
            viewportHeight = 915,
            screenWidth = 1080,
            screenHeight = 2400,
            colorDepth = 24,
            deviceScaleFactor = 2.625,
            locale = "en-US",
            language = "en-US",
            languages = listOf("en-US", "en"),
            timezone = "America/New_York",
            touchSupport = true,
            mobileMode = true,
            clientHints = ClientHintsConfig("Android", true, listOf("Chromium", "Google Chrome")),
        ),
        DevicePreset(
            id = ANDROID_TABLET,
            name = "Android Tablet",
            operatingSystem = "Android 14",
            platform = "Android",
            userAgent = "Mozilla/5.0 (Linux; Android 14; Pixel Tablet) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
            viewportWidth = 1280,
            viewportHeight = 800,
            screenWidth = 2560,
            screenHeight = 1600,
            colorDepth = 24,
            deviceScaleFactor = 2.0,
            locale = "en-US",
            language = "en-US",
            languages = listOf("en-US", "en"),
            timezone = "America/New_York",
            touchSupport = true,
            mobileMode = true,
            clientHints = ClientHintsConfig("Android", true, listOf("Chromium", "Google Chrome")),
        ),
        DevicePreset(
            id = DESKTOP,
            name = "Desktop",
            operatingSystem = "Windows 11",
            platform = "Windows",
            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
            viewportWidth = 1440,
            viewportHeight = 900,
            screenWidth = 1920,
            screenHeight = 1080,
            colorDepth = 24,
            deviceScaleFactor = 1.0,
            locale = "en-US",
            language = "en-US",
            languages = listOf("en-US", "en"),
            timezone = "America/New_York",
            touchSupport = false,
            mobileMode = false,
            clientHints = ClientHintsConfig("Windows", false, listOf("Chromium", "Google Chrome")),
        ),
    )

    fun all(): List<DevicePreset> = presets

    fun get(id: String): DevicePreset? = presets.firstOrNull { it.id == id }

    fun default(): DevicePreset = presets.first()
}
