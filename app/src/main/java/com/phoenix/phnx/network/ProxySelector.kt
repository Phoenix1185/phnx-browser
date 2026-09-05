package com.phoenix.phnx.network

object ProxySelector {
    fun bestHealthy(proxies: List<ProxyEndpoint>): ProxyEndpoint? = proxies
        .filter { it.health == ProxyHealthStatus.HEALTHY || it.health == ProxyHealthStatus.SLOW }
        .minWithOrNull(compareBy<ProxyEndpoint> { it.health != ProxyHealthStatus.HEALTHY }.thenBy { it.latencyMs ?: Int.MAX_VALUE })
}
