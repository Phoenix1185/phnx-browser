package com.phoenix.phnx.network

import org.json.JSONArray

object PublicProxyParser {
    fun parse(source: ProxySource, json: String, limit: Int = 8): List<ProxyEndpoint> {
        if (source.id == "hproxy") return parseHProxy(json, limit)
        if (source.id == "proxyscrape") return parsePlainHttp(json, limit)
        return emptyList()
    }

    private fun parsePlainHttp(value: String, limit: Int): List<ProxyEndpoint> = value
        .lineSequence()
        .map { it.trim() }
        .mapNotNull { row ->
            val parts = row.split(':')
            val port = parts.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
            if (parts[0].isBlank() || port !in 1..65535) return@mapNotNull null
            ProxyEndpoint(ProxyType.HTTP, parts[0], port)
        }
        .distinctBy { "${it.type}:${it.host}:${it.port}" }
        .take(limit.coerceIn(1, 30))
        .toList()

    private fun parseHProxy(json: String, limit: Int): List<ProxyEndpoint> {
        val rows = runCatching { JSONArray(json) }.getOrNull() ?: return emptyList()
        return (0 until rows.length())
            .asSequence()
            .mapNotNull { index -> rows.optJSONObject(index)?.let(::hProxyEndpoint) }
            .distinctBy { "${it.type}:${it.host}:${it.port}" }
            .take(limit.coerceIn(1, 30))
            .toList()
    }

    private fun hProxyEndpoint(row: org.json.JSONObject): ProxyEndpoint? {
        val host = row.optString("ip").trim()
        val port = row.optInt("port", 0)
        if (host.isBlank() || port !in 1..65535) return null
        val protocols = row.optJSONArray("protocols") ?: return null
        val supported = (0 until protocols.length()).map { protocols.optString(it).lowercase() }
        val type = when {
            "https" in supported -> ProxyType.HTTPS
            "http" in supported -> ProxyType.HTTP
            "socks5" in supported -> ProxyType.SOCKS5
            "socks4" in supported -> ProxyType.SOCKS4
            else -> return null
        }
        return ProxyEndpoint(
            type = type,
            host = host,
            port = port,
            countryCode = row.optString("country_code").trim().uppercase(),
            countryName = row.optString("country").trim(),
            httpsSupported = "https" in supported,
            anonymity = row.optString("anonymity").trim(),
            reportedLatencyMs = row.optInt("latency_ms", 0).takeIf { it > 0 },
        )
    }
}
