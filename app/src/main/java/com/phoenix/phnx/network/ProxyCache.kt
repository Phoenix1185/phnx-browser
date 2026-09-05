package com.phoenix.phnx.network

class ProxyCache(
    private val ttlMillis: Long = DEFAULT_TTL_MILLIS,
) {
    private var cachedAt: Long = 0L
    private var cached: List<ProxyEndpoint> = emptyList()

    @Synchronized
    fun getFresh(now: Long = System.currentTimeMillis()): List<ProxyEndpoint>? =
        cached.takeIf { it.isNotEmpty() && now - cachedAt < ttlMillis }

    @Synchronized
    fun put(value: List<ProxyEndpoint>, now: Long = System.currentTimeMillis()) {
        cached = value
        cachedAt = now
    }

    @Synchronized
    fun clear() {
        cached = emptyList()
        cachedAt = 0L
    }

    companion object {
        const val DEFAULT_TTL_MILLIS = 5 * 60 * 1000L
    }
}
