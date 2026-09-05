package com.phoenix.phnx.network

class ProxyManager(
    private val repository: ProxyRepository = ProxyRepository(),
    private val healthChecker: ProxyHealthChecker = ProxyHealthChecker(),
) {
    fun fetchAndCheck(limit: Int = PublicProxyFetcher.DEFAULT_LIMIT, forceRefresh: Boolean = false): List<ProxyEndpoint> =
        healthChecker.checkAll(repository.discover(limit, forceRefresh))

    fun check(endpoint: ProxyEndpoint): ProxyEndpoint = healthChecker.check(endpoint).updatedEndpoint()

    fun bestHealthy(proxies: List<ProxyEndpoint>): ProxyEndpoint? = ProxySelector.bestHealthy(proxies)

    fun clearCache() = repository.clearCache()
}
