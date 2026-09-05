package com.phoenix.phnx.network

import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature

enum class NetworkApplyStatus {
    APPLIED,
    UNSUPPORTED,
}

data class NetworkApplyResult(
    val status: NetworkApplyStatus,
    val message: String,
)

interface ChromiumNetworkAdapter {
    fun apply(config: ProfileNetworkConfig): NetworkApplyResult
}

class WebViewNetworkAdapter : ChromiumNetworkAdapter {
    override fun apply(config: ProfileNetworkConfig): NetworkApplyResult {
        if (config.mode != NetworkMode.PROXY || !config.enabled) {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
                ProxyController.getInstance().clearProxyOverride(Runnable::run) {}
            }
            return NetworkApplyResult(NetworkApplyStatus.APPLIED, "Proxy disabled; direct connection selected.")
        }

        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            return NetworkApplyResult(
                NetworkApplyStatus.UNSUPPORTED,
                "This Android WebView does not support proxy override.",
            )
        }

        if (config.username.isNotBlank()) {
            return NetworkApplyResult(
                NetworkApplyStatus.UNSUPPORTED,
                "WebView proxy override does not support proxy authentication.",
            )
        }

        return runCatching {
            val primary = ProxyEndpoint(
                type = config.proxyType ?: return@runCatching NetworkApplyResult(NetworkApplyStatus.UNSUPPORTED, "Proxy type is missing."),
                host = config.proxyHost,
                port = config.proxyPort,
            )
            val fallbacks = if (config.fallbackToFreeProxy) config.freeProxyFallbacks else emptyList()
            val builder = ProxyConfig.Builder().addProxyRule(primary.asWebViewRule())
            fallbacks.forEach { builder.addProxyRule(it.asWebViewRule()) }
            if (config.fallbackToDirect) builder.addDirect()
            ProxyController.getInstance().setProxyOverride(builder.build(), Runnable::run) {}
            val fallbackCount = fallbacks.size + if (config.fallbackToDirect) 1 else 0
            NetworkApplyResult(
                NetworkApplyStatus.APPLIED,
                "Proxy enabled with $fallbackCount fallback route${if (fallbackCount == 1) "" else "s"}.",
            )
        }.getOrElse { error ->
            NetworkApplyResult(
                NetworkApplyStatus.UNSUPPORTED,
                "Could not apply WebView proxy override: ${error.message ?: error.javaClass.simpleName}.",
            )
        }
    }
}
