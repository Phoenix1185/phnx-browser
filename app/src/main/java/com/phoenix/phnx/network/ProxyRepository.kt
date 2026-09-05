package com.phoenix.phnx.network

class ProxyRepository(
    private val fetcher: PublicProxyFetcher = PublicProxyFetcher(),
    private val cache: ProxyCache = ProxyCache(),
) {
    fun discover(limit: Int = PublicProxyFetcher.DEFAULT_LIMIT, forceRefresh: Boolean = false): List<ProxyEndpoint> {
        if (!forceRefresh) cache.getFresh()?.let { return it.take(limit) }
        return fetcher.fetch(limit).also(cache::put)
    }

    fun clearCache() = cache.clear()
}
