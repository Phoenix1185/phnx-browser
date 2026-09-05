package com.phoenix.phnx.network

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
    override fun apply(config: ProfileNetworkConfig): NetworkApplyResult =
        if (config.mode == NetworkMode.DIRECT) {
            NetworkApplyResult(NetworkApplyStatus.APPLIED, "Direct connection selected.")
        } else {
            NetworkApplyResult(
                NetworkApplyStatus.UNSUPPORTED,
                "Per-profile proxy routing is not exposed by the Android WebView adapter.",
            )
        }
}
