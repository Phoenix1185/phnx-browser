package com.phoenix.phnx.network

import java.net.HttpURLConnection
import java.net.URL
import java.io.Reader

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
            val payload = connection.inputStream.bufferedReader().use(::readLimited) ?: return emptyList()
            PublicProxyParser.parse(source, payload, limit)
        } catch (_: Exception) {
            emptyList()
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val DEFAULT_LIMIT = 8
        const val DEFAULT_TIMEOUT_MILLIS = 10_000
        const val MAX_RESPONSE_CHARS = 512 * 1024
    }

    private fun readLimited(reader: Reader): String? {
        val output = StringBuilder()
        val buffer = CharArray(8 * 1024)
        while (true) {
            val read = reader.read(buffer)
            if (read < 0) return output.toString()
            output.append(buffer, 0, read)
            if (output.length > MAX_RESPONSE_CHARS) return null
        }
    }
}
