package com.phoenix.phnx.identity

import android.webkit.WebView

data class IdentityRuntimeCheck(
    val matchesSupportedSettings: Boolean,
    val diagnostics: List<String>,
)

class IdentityRuntimeChecker {
    fun check(webView: WebView, config: BrowserIdentityConfig): IdentityRuntimeCheck {
        val diagnostics = buildList {
            if (webView.settings.userAgentString != config.userAgent) {
                add("The applied User-Agent does not match the selected profile.")
            }
            if (webView.settings.useWideViewPort != config.mobileMode) {
                add("The applied viewport mode does not match the selected profile.")
            }
            if (webView.settings.loadWithOverviewMode == config.mobileMode) {
                add("The applied overview mode does not match the selected profile.")
            }
        }
        return IdentityRuntimeCheck(
            matchesSupportedSettings = diagnostics.isEmpty(),
            diagnostics = diagnostics,
        )
    }
}
