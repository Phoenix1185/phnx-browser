package com.phoenix.phnx.network

data class ProfileNetworkConfig(
    val id: String,
    val profileId: String,
    val mode: NetworkMode = NetworkMode.DIRECT,
    val proxyType: ProxyType? = null,
    val proxyHost: String = "",
    val proxyPort: Int = 0,
    val username: String = "",
    val credentialReference: String? = null,
    val enabled: Boolean = true,
    val fallbackToFreeProxy: Boolean = false,
    val fallbackToDirect: Boolean = false,
    val freeProxyFallbacks: List<ProxyEndpoint> = emptyList(),
)

object NetworkConfigValidator {
    fun validate(config: ProfileNetworkConfig): List<String> = buildList {
        if (config.profileId.isBlank()) add("A profile is required.")
        if (config.id.isBlank()) add("A configuration ID is required.")
        if (config.mode == NetworkMode.PROXY && config.enabled) {
            if (config.proxyType == null) add("A proxy type is required.")
            if (config.proxyHost.isBlank()) add("A proxy host is required.")
            if (config.proxyPort !in 1..65535) add("Proxy port must be between 1 and 65535.")
            if (config.username.isNotBlank() && config.credentialReference.isNullOrBlank()) {
                add("Proxy credentials are incomplete.")
            }
        }
        if (config.fallbackToFreeProxy && config.mode != NetworkMode.PROXY) {
            add("Free proxy fallback requires proxy mode.")
        }
        if (config.fallbackToDirect && config.mode != NetworkMode.PROXY) {
            add("Direct fallback requires proxy mode.")
        }
    }
}
