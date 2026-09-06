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
        if (!config.mode.usesProxy() || !config.enabled) {
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
            val primary = if (config.proxyHost.isBlank()) {
                null
            } else {
                val normalizedResult = runCatching {
                    ProxyConfigNormalizer.normalize(
                        scheme = config.proxyType,
                        host = config.proxyHost,
                        port = config.proxyPort,
                        username = config.username.takeIf { it.isNotEmpty() },
                    )
                }
                if (normalizedResult.isFailure) {
                    return@runCatching NetworkApplyResult(
                        NetworkApplyStatus.UNSUPPORTED,
                        normalizedResult.exceptionOrNull()?.message ?: "Invalid proxy configuration.",
                    )
                }
                val normalized = normalizedResult.getOrThrow()
                ProxyEndpoint(normalized.scheme, normalized.host, normalized.port)
            }
            if (config.mode == NetworkMode.FREE_PUBLIC_PROXY && primary == null) {
                return@runCatching NetworkApplyResult(NetworkApplyStatus.UNSUPPORTED, "Choose a free public proxy before applying this mode.")
            }
            val fallbacks = if (config.mode == NetworkMode.PROXY || config.mode == NetworkMode.MY_PROXY) {
                if (config.fallbackToFreeProxy) config.freeProxyFallbacks else emptyList()
            } else {
                emptyList()
            }
            if (primary == null && fallbacks.isEmpty()) {
                return@runCatching NetworkApplyResult(NetworkApplyStatus.UNSUPPORTED, "No free proxy route is available.")
            }
            val builder = ProxyConfig.Builder()
            primary?.let { builder.addProxyRule(it.asWebViewRule()) }
            fallbacks.forEach { builder.addProxyRule(it.asWebViewRule()) }
            if (config.fallbackToDirect) builder.addDirect()
            ProxyController.getInstance().setProxyOverride(builder.build(), Runnable::run) {}
            val fallbackCount = fallbacks.size + if (config.fallbackToDirect) 1 else 0
            NetworkApplyResult(
                NetworkApplyStatus.APPLIED,
                if (primary == null) {
                    "Free proxy pool enabled with ${fallbacks.size} route${if (fallbacks.size == 1) "" else "s"}."
                } else {
                    "Proxy enabled with $fallbackCount fallback route${if (fallbackCount == 1) "" else "s"}."
                },
            )
        }.getOrElse { error ->
            NetworkApplyResult(
                NetworkApplyStatus.UNSUPPORTED,
                "Could not apply WebView proxy override: ${error.message ?: error.javaClass.simpleName}.",
            )
        }
    }
}
