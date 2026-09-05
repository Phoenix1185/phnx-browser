package com.phoenix.phnx.identity

import android.webkit.WebView
import org.json.JSONObject
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
            message = "User-Agent and viewport preferences applied. Page-visible screen metrics are applied after navigation; locale, timezone, and client hints remain limited by Android WebView.",
        )
    }

    fun applyPageIdentity(webView: WebView, config: BrowserIdentityConfig) {
        val languages = config.languages.joinToString(",") { JSONObject.quote(it) }
        val script = """
            (function() {
              var values = {
                viewportWidth: ${config.viewportWidth},
                viewportHeight: ${config.viewportHeight},
                screenWidth: ${config.screenWidth},
                screenHeight: ${config.screenHeight},
                colorDepth: ${config.colorDepth},
                deviceScaleFactor: ${config.deviceScaleFactor},
                language: ${JSONObject.quote(config.language)},
                languages: [$languages],
                platform: ${JSONObject.quote(config.platform)},
                touchPoints: ${if (config.touchSupport) 5 else 0}
              };
              function define(target, key, value) {
                try { Object.defineProperty(target, key, { configurable: true, get: function() { return value; } }); } catch (_) {}
              }
              define(window, 'innerWidth', values.viewportWidth);
              define(window, 'innerHeight', values.viewportHeight);
              define(window, 'outerWidth', values.viewportWidth);
              define(window, 'outerHeight', values.viewportHeight);
              define(window, 'devicePixelRatio', values.deviceScaleFactor);
              define(navigator, 'platform', values.platform);
              define(navigator, 'language', values.language);
              define(navigator, 'languages', values.languages);
              define(navigator, 'maxTouchPoints', values.touchPoints);
              define(screen, 'width', values.screenWidth);
              define(screen, 'height', values.screenHeight);
              define(screen, 'availWidth', values.screenWidth);
              define(screen, 'availHeight', values.screenHeight);
              define(screen, 'colorDepth', values.colorDepth);
              define(screen, 'pixelDepth', values.colorDepth);
            })();
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    }
}
