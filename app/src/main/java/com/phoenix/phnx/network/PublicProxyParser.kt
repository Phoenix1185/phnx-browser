package com.phoenix.phnx.network

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
        return OBJECT_PATTERN.findAll(json)
            .mapNotNull { match -> hProxyEndpoint(match.groupValues[1]) }
            .distinctBy { "${it.type}:${it.host}:${it.port}" }
            .take(limit.coerceIn(1, 30))
            .toList()
    }

    private fun hProxyEndpoint(row: String): ProxyEndpoint? {
        val host = stringField(row, "ip")?.trim().orEmpty()
        val port = field(row, "port")?.toIntOrNull() ?: 0
        if (host.isBlank() || port !in 1..65535) return null
        val supported = arrayField(row, "protocols").map(String::lowercase)
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
            countryCode = stringField(row, "country_code")?.trim()?.uppercase().orEmpty(),
            countryName = stringField(row, "country")?.trim().orEmpty(),
            httpsSupported = "https" in supported,
            anonymity = stringField(row, "anonymity")?.trim().orEmpty(),
            reportedLatencyMs = field(row, "latency_ms")?.toIntOrNull()?.takeIf { it > 0 },
        )
    }

    private fun field(row: String, key: String): String? = Regex(
        "\"${Regex.escape(key)}\"\\s*:\\s*(\"(?:\\\\.|[^\"\\\\])*\"|\\[[^]]*\\]|-?\\d+)",
    ).find(row)?.groupValues?.getOrNull(1)

    private fun stringField(row: String, key: String): String? = field(row, key)
        ?.takeIf { it.length >= 2 && it.first() == '"' && it.last() == '"' }
        ?.let { it.substring(1, it.length - 1) }
        ?.replace("\\\"", "\"")
        ?.replace("\\\\", "\\")

    private fun arrayField(row: String, key: String): List<String> = field(row, key)
        ?.let { value -> STRING_PATTERN.findAll(value).map { it.groupValues[1] }.toList() }
        .orEmpty()

    private val OBJECT_PATTERN = Regex("\\{([^{}]*)}")
    private val STRING_PATTERN = Regex("\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"")
}
