package com.phoenix.phnx.identity

import android.webkit.WebView
import kotlin.math.roundToInt

enum class IdentityApplyStatus {
    SUPPORTED,
    PARTIALLY_SUPPORTED,
    NOT_SUPPORTED,
}

data class IdentityApplyResult(
    val status: IdentityApplyStatus,
    val message: String,
)

interface ChromiumIdentityAdapter {
    fun apply(webView: WebView, config: BrowserIdentityConfig): IdentityApplyResult
}

class WebViewIdentityAdapter : ChromiumIdentityAdapter {
    private val runtimeChecker = IdentityRuntimeChecker()

    override fun apply(webView: WebView, config: BrowserIdentityConfig): IdentityApplyResult {
        webView.settings.userAgentString = config.userAgent
        webView.settings.useWideViewPort = config.mobileMode
        webView.settings.loadWithOverviewMode = !config.mobileMode
        val actualScale = webView.resources.displayMetrics.density.toDouble().coerceAtLeast(0.5)
        webView.setInitialScale((config.deviceScaleFactor / actualScale * 100.0).roundToInt().coerceIn(50, 400))
        val runtimeCheck = runtimeChecker.check(webView, config)
        if (!runtimeCheck.matchesSupportedSettings) {
            return IdentityApplyResult(
                status = IdentityApplyStatus.NOT_SUPPORTED,
                message = runtimeCheck.diagnostics.joinToString(" "),
            )
        }
        return IdentityApplyResult(
            status = IdentityApplyStatus.PARTIALLY_SUPPORTED,
            message = "User-Agent and supported viewport settings applied. Screen metrics, locale, timezone, and client hints remain limited by Android WebView.",
        )
    }
}
