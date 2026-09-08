package com.phoenix.phnx.identity

import java.time.ZoneId
import java.util.Locale

object DeviceProfileValidator {
    fun validate(config: BrowserIdentityConfig): List<String> = buildList {
        if (config.profileId.isBlank()) add("A profile is required.")
        if (config.presetId.isBlank()) add("A device preset is required.")
        if (config.name.isBlank()) add("A device profile name is required.")
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
            if (!isLanguageTag(config.locale)) add("Locale must be a valid language tag.")
            if (!isLanguageTag(config.language)) add("Primary language must be a valid language tag.")
            if (config.languages.any { !isLanguageTag(it) }) add("Language preferences must be valid language tags.")
            val localeLanguage = config.locale.substringBefore('-').lowercase()
            val declaredLanguage = config.language.substringBefore('-').lowercase()
            if (localeLanguage != declaredLanguage) add("Locale and primary language do not match.")
            if (config.languages.first().substringBefore('-').lowercase() != declaredLanguage) {
                add("The first language preference must match the primary language.")
            }
        }
        if (config.timezone.isBlank()) {
            add("A timezone is required.")
        } else if (runCatching { ZoneId.of(config.timezone) }.isFailure) {
            add("Timezone must be a valid IANA timezone.")
        }

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
        if (platform == "ios" && !operatingSystem.contains("ios") && !operatingSystem.contains("ipados")) {
            add("iOS platform and operating system do not match.")
        }
        if (platform == "macos" && !userAgent.contains("macintosh")) {
            add("macOS configuration requires a Macintosh User-Agent.")
        }
        if (platform == "linux" && !userAgent.contains("linux")) {
            add("Linux configuration requires a Linux User-Agent.")
        }
        if (platform == "chromeos" && !userAgent.contains("cros")) {
            add("ChromeOS configuration requires a CrOS User-Agent.")
        }
        if (config.mobileMode && !config.touchSupport) add("Mobile mode requires touch support.")
        if (config.mobileMode && config.viewportWidth > 1600) add("Mobile viewport is too wide.")
        if (!config.mobileMode && config.viewportWidth < 640) add("Desktop viewport is too narrow.")
        if (config.mobileMode) {
            val widthScale = config.screenWidth.toDouble() / config.viewportWidth
            val heightScale = config.screenHeight.toDouble() / config.viewportHeight
            if (kotlin.math.abs(widthScale - heightScale) > 0.15 ||
                kotlin.math.abs(widthScale - config.deviceScaleFactor) > 0.15
            ) {
                add("Mobile screen dimensions and device scale factor must describe the same emulation model.")
            }
        }
        if (config.clientHints.platform.isBlank()) add("Client-hints platform is required.")
        if (config.clientHints.mobile != config.mobileMode) add("Client-hints mobile mode does not match the profile.")
        if (config.clientHints.platform.lowercase() != platform) add("Client-hints platform does not match the profile platform.")
        if (config.clientHints.brands.isEmpty()) add("At least one client-hints brand is required.")
        if (platform == "ios" && config.clientHints.brands.any { it.contains("android", ignoreCase = true) }) {
            add("iOS profiles cannot declare Android client-hint brands.")
        }
        if (platform == "android" && config.clientHints.brands.any { it.contains("safari", ignoreCase = true) }) {
            add("Android profiles cannot declare Safari client-hint brands.")
        }
        val browserBrands = config.clientHints.brands.joinToString(" ").lowercase()
        when {
            userAgent.contains("firefox") && !browserBrands.contains("firefox") ->
                add("Firefox User-Agents must declare a Firefox client-hint brand.")
            userAgent.contains("edg/") && !browserBrands.contains("edge") ->
                add("Edge User-Agents must declare an Edge client-hint brand.")
            userAgent.contains("safari") && !userAgent.contains("chrome") && !browserBrands.contains("safari") ->
                add("Safari User-Agents must declare a Safari client-hint brand.")
            (userAgent.contains("chrome") || userAgent.contains("chromium")) &&
                !browserBrands.contains("chrome") && !browserBrands.contains("chromium") && !browserBrands.contains("edge") ->
                add("Chromium User-Agents must declare a Chromium, Chrome, or Edge client-hint brand.")
        }
    }

    private fun isLanguageTag(value: String): Boolean {
        val normalized = value.replace('_', '-')
        val parsed = Locale.forLanguageTag(normalized)
        return parsed.language.isNotBlank() && parsed.toLanguageTag().equals(normalized, ignoreCase = true)
    }
}
