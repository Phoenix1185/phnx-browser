package com.phoenix.phnx.identity

import android.webkit.WebView
import kotlin.math.roundToInt

data class IdentityApplyResult(
    val status: IdentityApplyStatus,
    val message: String,
    val capabilities: List<DevicePropertyCapability> = emptyList(),
)

interface ChromiumIdentityAdapter {
    fun apply(webView: WebView, config: BrowserIdentityConfig): IdentityApplyResult
}

class WebViewIdentityAdapter : ChromiumIdentityAdapter {
    private val runtimeChecker = IdentityRuntimeChecker()

    override fun apply(webView: WebView, config: BrowserIdentityConfig): IdentityApplyResult {
        return apply(webView, config, pageZoomPercent = 100)
    }

    fun apply(
        webView: WebView,
        config: DeviceProfile,
        pageZoomPercent: Int,
    ): IdentityApplyResult {
        webView.settings.userAgentString = config.userAgent
        webView.settings.useWideViewPort = config.mobileMode
        webView.settings.loadWithOverviewMode = !config.mobileMode
        val actualScale = webView.resources.displayMetrics.density.toDouble().coerceAtLeast(0.5)
        val initialScale = if (config.mobileMode) {
            config.deviceScaleFactor / actualScale * pageZoomPercent
        } else {
            pageZoomPercent.toDouble()
        }
        webView.setInitialScale(initialScale.roundToInt().coerceIn(50, 400))
        WebViewIdentityCompatibility.install(webView, config)
        val runtimeCheck = runtimeChecker.check(webView, config)
        if (!runtimeCheck.matchesSupportedSettings) {
            return IdentityApplyResult(
                status = IdentityApplyStatus.WEBVIEW_LIMITED,
                message = runtimeCheck.diagnostics.joinToString(" "),
                capabilities = DeviceProfileCapabilities.forProfile(config),
            )
        }
        val status = DeviceProfileCapabilities.overallStatus(config)
        return IdentityApplyResult(
            status = status,
            message = when (status) {
                IdentityApplyStatus.APPLIED -> "The system profile is applied using native WebView behavior."
                IdentityApplyStatus.PARTIALLY_APPLIED -> "Native WebView settings and safe JavaScript compatibility values are applied; physical display metrics and client hints remain WebView-controlled."
                IdentityApplyStatus.WEBVIEW_LIMITED -> "Native WebView settings and safe JavaScript compatibility values are applied, but this profile cannot become a genuine iOS or desktop browser inside Android WebView."
            },
            capabilities = DeviceProfileCapabilities.forProfile(config),
        )
    }
}
