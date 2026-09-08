package com.phoenix.phnx.identity

enum class IdentityApplyStatus {
    APPLIED,
    PARTIALLY_APPLIED,
    WEBVIEW_LIMITED,
}

enum class DevicePropertyMode {
    NATIVE_CONFIGURABLE,
    JAVASCRIPT_EMULATED,
    WEBVIEW_CONTROLLED,
    UNSUPPORTED,
}

data class DevicePropertyCapability(
    val label: String,
    val mode: DevicePropertyMode,
    val detail: String,
)

object DeviceProfileCapabilities {
    fun forProfile(profile: DeviceProfile): List<DevicePropertyCapability> = listOf(
        DevicePropertyCapability(
            label = "User-Agent",
            mode = DevicePropertyMode.NATIVE_CONFIGURABLE,
            detail = "Applied through WebSettings.userAgentString.",
        ),
        DevicePropertyCapability(
            label = "Mobile or desktop browsing behavior",
            mode = DevicePropertyMode.NATIVE_CONFIGURABLE,
            detail = "Applied through WebView viewport and overview settings; Desktop Mode is separate from the selected device profile.",
        ),
        DevicePropertyCapability(
            label = "CSS viewport",
            mode = DevicePropertyMode.NATIVE_CONFIGURABLE,
            detail = "WebView layout behavior is configured. A page's own viewport metadata can still affect the final layout.",
        ),
        DevicePropertyCapability(
            label = "Viewport dimensions",
            mode = DevicePropertyMode.JAVASCRIPT_EMULATED,
            detail = "window.innerWidth and window.innerHeight are compatibility values; Android layout bounds are unchanged.",
        ),
        DevicePropertyCapability(
            label = "Screen dimensions and color depth",
            mode = DevicePropertyMode.JAVASCRIPT_EMULATED,
            detail = "screen.* values are compatibility values only; the physical Android display is unchanged.",
        ),
        DevicePropertyCapability(
            label = "Device pixel ratio",
            mode = DevicePropertyMode.JAVASCRIPT_EMULATED,
            detail = "window.devicePixelRatio is overridden only when the page allows the safe compatibility property override.",
        ),
        DevicePropertyCapability(
            label = "Platform",
            mode = DevicePropertyMode.JAVASCRIPT_EMULATED,
            detail = "navigator.platform is a compatibility value when WebView permits a configurable property.",
        ),
        DevicePropertyCapability(
            label = "Language",
            mode = DevicePropertyMode.JAVASCRIPT_EMULATED,
            detail = "navigator.language and navigator.languages are compatibility values; WebView request headers and Android resources are unchanged.",
        ),
        DevicePropertyCapability(
            label = "Timezone",
            mode = DevicePropertyMode.JAVASCRIPT_EMULATED,
            detail = "Only resolvedOptions().timeZone is compatibility-layer output; Date, timezone offsets, and Android timezone remain unchanged.",
        ),
        DevicePropertyCapability(
            label = "Touch capability",
            mode = DevicePropertyMode.JAVASCRIPT_EMULATED,
            detail = "navigator.maxTouchPoints is a compatibility value; actual touch input, pointer media queries, and Android hardware remain native.",
        ),
        DevicePropertyCapability(
            label = "Locale and request language",
            mode = DevicePropertyMode.WEBVIEW_CONTROLLED,
            detail = "navigator language fields are compatibility values; Android resources, Intl locale defaults, and HTTP Accept-Language remain WebView-controlled.",
        ),
        DevicePropertyCapability(
            label = "Browser engine and operating system",
            mode = DevicePropertyMode.WEBVIEW_CONTROLLED,
            detail = "All profiles execute in Android System WebView. Non-Android presets can change compatibility values but cannot become native iOS, desktop, Safari, or Firefox engines.",
        ),
        DevicePropertyCapability(
            label = "Physical display metrics",
            mode = DevicePropertyMode.WEBVIEW_CONTROLLED,
            detail = "Android display size, density, and input hardware cannot be changed by an app WebView.",
        ),
        DevicePropertyCapability(
            label = "User-Agent Client Hints",
            mode = DevicePropertyMode.WEBVIEW_CONTROLLED,
            detail = "WebView/Chromium controls navigator.userAgentData and outgoing Sec-CH-UA headers; genuine iOS or desktop hints are not advertised as applied.",
        ),
        DevicePropertyCapability(
            label = "Genuine iOS or desktop Client Hints",
            mode = DevicePropertyMode.UNSUPPORTED,
            detail = "Android WebView exposes no supported API for replacing its native userAgentData or Sec-CH-UA headers.",
        ),
        DevicePropertyCapability(
            label = "WebGL and GPU identity",
            mode = DevicePropertyMode.UNSUPPORTED,
            detail = "PHNX does not change WebGL renderer, GPU vendor, extensions, or shader precision; these remain native to Android WebView.",
        ),
        DevicePropertyCapability(
            label = "Canvas and audio identity",
            mode = DevicePropertyMode.UNSUPPORTED,
            detail = "PHNX does not spoof Canvas, font, or AudioContext output. These values remain native and are not part of a selectable device profile.",
        ),
    )

    fun overallStatus(profile: DeviceProfile): IdentityApplyStatus = when {
        profile.presetId == DevicePresets.SYSTEM_DEFAULT -> IdentityApplyStatus.APPLIED
        profile.platform.equals("Android", ignoreCase = true) -> IdentityApplyStatus.PARTIALLY_APPLIED
        else -> IdentityApplyStatus.WEBVIEW_LIMITED
    }

    fun statusLabel(status: IdentityApplyStatus): String = when (status) {
        IdentityApplyStatus.APPLIED -> "APPLIED"
        IdentityApplyStatus.PARTIALLY_APPLIED -> "PARTIALLY APPLIED"
        IdentityApplyStatus.WEBVIEW_LIMITED -> "WEBVIEW-LIMITED"
    }

    fun modeLabel(mode: DevicePropertyMode): String = when (mode) {
        DevicePropertyMode.NATIVE_CONFIGURABLE -> "ACTUALLY CONFIGURABLE"
        DevicePropertyMode.JAVASCRIPT_EMULATED -> "JAVASCRIPT EMULATED"
        DevicePropertyMode.WEBVIEW_CONTROLLED -> "WEBVIEW-CONTROLLED"
        DevicePropertyMode.UNSUPPORTED -> "UNSUPPORTED"
    }
}
