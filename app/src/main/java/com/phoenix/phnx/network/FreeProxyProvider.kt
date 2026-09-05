package com.phoenix.phnx.network

import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

class FreeProxyProvider(
    private val endpoint: URL = URL(DEFAULT_ENDPOINT),
    private val timeoutMillis: Int = DEFAULT_TIMEOUT_MILLIS,
) {
    fun fetch(limit: Int = DEFAULT_LIMIT): List<ProxyEndpoint> {
        val connection = endpoint.openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = timeoutMillis
            connection.readTimeout = timeoutMillis
            connection.setRequestProperty("Accept", "application/json")
            if (connection.responseCode !in 200..299) return emptyList()
            parse(connection.inputStream.bufferedReader().use { it.readText() }, limit)
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val DEFAULT_ENDPOINT = "https://hproxy.com/api/proxy-list?format=json&protocol=http,https,socks5&recent=true&limit=30"
        const val DEFAULT_LIMIT = 8

        fun parse(json: String, limit: Int = DEFAULT_LIMIT): List<ProxyEndpoint> {
            val rows = runCatching { JSONArray(json) }.getOrNull() ?: return emptyList()
            return (0 until rows.length())
                .asSequence()
                .mapNotNull { index -> rows.optJSONObject(index)?.toEndpoint() }
                .distinct()
                .take(limit.coerceIn(1, 30))
                .toList()
        }

        private fun org.json.JSONObject.toEndpoint(): ProxyEndpoint? {
            val host = optString("ip").trim()
            val port = optInt("port", 0)
            if (host.isBlank() || port !in 1..65535) return null
            val protocols = optJSONArray("protocols") ?: return null
            val supported = (0 until protocols.length()).map { protocols.optString(it).lowercase() }
            val type = when {
                "https" in supported -> ProxyType.HTTPS
                "http" in supported -> ProxyType.HTTP
                "socks5" in supported -> ProxyType.SOCKS5
                "socks4" in supported -> ProxyType.SOCKS4
                else -> return null
            }
            return ProxyEndpoint(type, host, port)
        }
    }
}
