package com.phoenix.phnx.network

import java.net.HttpURLConnection
import java.net.URL

class PublicProxyFetcher(
    private val sources: List<ProxySource> = ProxySources.default,
    private val timeoutMillis: Int = DEFAULT_TIMEOUT_MILLIS,
) {
    fun fetch(limit: Int = DEFAULT_LIMIT): List<ProxyEndpoint> = sources
        .asSequence()
        .flatMap { source -> fetchSource(source, limit).asSequence() }
        .distinctBy { "${it.type}:${it.host}:${it.port}" }
        .take(limit.coerceIn(1, 30))
        .toList()

    private fun fetchSource(source: ProxySource, limit: Int): List<ProxyEndpoint> {
        val connection = runCatching { URL(source.endpoint).openConnection() as HttpURLConnection }.getOrNull()
            ?: return emptyList()
        return try {
            connection.connectTimeout = timeoutMillis
            connection.readTimeout = timeoutMillis
            connection.setRequestProperty("Accept", "application/json")
            if (connection.responseCode !in 200..299) return emptyList()
            PublicProxyParser.parse(source, connection.inputStream.bufferedReader().use { it.readText() }, limit)
        } catch (_: Exception) {
            emptyList()
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val DEFAULT_LIMIT = 8
        const val DEFAULT_TIMEOUT_MILLIS = 10_000
    }
}
