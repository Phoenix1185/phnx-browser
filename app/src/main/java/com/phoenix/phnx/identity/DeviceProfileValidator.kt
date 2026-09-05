package com.phoenix.phnx.identity

object DeviceProfileValidator {
    fun validate(config: BrowserIdentityConfig): List<String> = buildList {
        if (config.profileId.isBlank()) add("A profile is required.")
        if (config.presetId.isBlank()) add("A device preset is required.")
        if (config.userAgent.isBlank()) add("A User-Agent is required.")
        if (config.platform.isBlank()) add("A platform is required.")
        if (config.operatingSystem.isBlank()) add("An operating system is required.")
        if (config.viewportWidth <= 0 || config.viewportHeight <= 0) add("Viewport dimensions must be positive.")
        if (config.screenWidth <= 0 || config.screenHeight <= 0) add("Screen dimensions must be positive.")
        if (config.screenWidth < config.viewportWidth || config.screenHeight < config.viewportHeight) {
            add("Screen dimensions must contain the viewport.")
        }
        if (config.colorDepth !in setOf(16, 24, 30, 32)) add("Color depth is invalid.")
        if (config.deviceScaleFactor !in 0.5..4.0) add("Device scale factor must be between 0.5 and 4.0.")
        if (config.locale.isBlank() || config.language.isBlank() || config.languages.isEmpty()) {
            add("Locale and language preferences are required.")
        } else {
            val localeLanguage = config.locale.substringBefore('-').lowercase()
            val declaredLanguage = config.language.substringBefore('-').lowercase()
            if (localeLanguage != declaredLanguage) add("Locale and primary language do not match.")
            if (config.languages.first().substringBefore('-').lowercase() != declaredLanguage) {
                add("The first language preference must match the primary language.")
            }
        }
        if (config.timezone.isBlank()) add("A timezone is required.")

        val userAgent = config.userAgent.lowercase()
        val operatingSystem = config.operatingSystem.lowercase()
        val platform = config.platform.lowercase()
        if (operatingSystem.contains("android") && !userAgent.contains("android")) {
            add("Android configuration requires an Android User-Agent.")
        }
        if (operatingSystem.contains("windows") && !userAgent.contains("windows nt")) {
            add("Windows configuration requires a Windows User-Agent.")
        }
        if (platform.contains("android") && !operatingSystem.contains("android")) {
            add("Android platform and operating system do not match.")
        }
        if (platform.contains("windows") && !operatingSystem.contains("windows")) {
            add("Windows platform and operating system do not match.")
        }
        if (platform == "ios" && !userAgent.contains("iphone") && !userAgent.contains("ipad")) {
            add("iOS configuration requires an iPhone or iPad User-Agent.")
        }
        if (platform == "macos" && !userAgent.contains("macintosh")) {
            add("macOS configuration requires a Macintosh User-Agent.")
        }
        if (platform == "linux" && !userAgent.contains("linux")) {
            add("Linux configuration requires a Linux User-Agent.")
        }
        if (config.mobileMode && !config.touchSupport) add("Mobile mode requires touch support.")
        if (config.mobileMode && config.viewportWidth > 1600) add("Mobile viewport is too wide.")
        if (!config.mobileMode && config.viewportWidth < 640) add("Desktop viewport is too narrow.")
        if (config.clientHints.platform.isBlank()) add("Client-hints platform is required.")
        if (config.clientHints.mobile != config.mobileMode) add("Client-hints mobile mode does not match the profile.")
        if (config.clientHints.platform.lowercase() != platform) add("Client-hints platform does not match the profile platform.")
        if (config.clientHints.brands.isEmpty()) add("At least one client-hints brand is required.")
    }
}
