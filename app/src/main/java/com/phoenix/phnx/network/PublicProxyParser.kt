package com.phoenix.phnx.network

object PublicProxyParser {
    fun parse(source: ProxySource, json: String, limit: Int = 8): List<ProxyEndpoint> {
        return when (source.id) {
            "hproxy" -> parseHProxy(source, json, limit)
            "proxyscrape" -> parsePlainHttp(source, json, limit)
            "geonode" -> parseGeonode(source, json, limit)
            "proxifly" -> parseProxifly(source, json, limit)
            "iplocate_http" -> parsePlainHttp(source, json, limit, ProxyType.HTTP)
            "iplocate_https" -> parsePlainHttp(source, json, limit, ProxyType.HTTPS)
            "iplocate_socks5" -> parsePlainHttp(source, json, limit, ProxyType.SOCKS5)
            else -> emptyList()
        }
    }

    private fun parsePlainHttp(
        source: ProxySource,
        value: String,
        limit: Int,
        type: ProxyType = ProxyType.HTTP,
    ): List<ProxyEndpoint> = value
        .lineSequence()
        .map { it.trim() }
        .mapNotNull { row ->
            val separator = row.lastIndexOf(':')
            if (separator <= 0) return@mapNotNull null
            val host = row.substring(0, separator).trim()
            val port = row.substring(separator + 1).trim().toIntOrNull() ?: return@mapNotNull null
            if (host.isBlank() || port !in 1..65535) return@mapNotNull null
            ProxyEndpoint(type, host, port, source = source.name)
        }
        .distinctBy { "${it.type}:${it.host}:${it.port}" }
        .take(limit.coerceIn(1, 30))
        .toList()

    private fun parseHProxy(source: ProxySource, json: String, limit: Int): List<ProxyEndpoint> {
        return OBJECT_PATTERN.findAll(json)
            .mapNotNull { match -> hProxyEndpoint(source, match.groupValues[1]) }
            .distinctBy { "${it.type}:${it.host}:${it.port}" }
            .take(limit.coerceIn(1, 30))
            .toList()
    }

    private fun hProxyEndpoint(source: ProxySource, row: String): ProxyEndpoint? {
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
            source = source.name,
        )
    }

    private fun parseGeonode(source: ProxySource, json: String, limit: Int): List<ProxyEndpoint> =
        OBJECT_PATTERN.findAll(json)
            .mapNotNull { match -> geonodeEndpoint(source, match.groupValues[1]) }
            .distinctBy { "${it.type}:${it.host}:${it.port}" }
            .take(limit.coerceIn(1, 30))
            .toList()

    private fun geonodeEndpoint(source: ProxySource, row: String): ProxyEndpoint? {
        val host = stringField(row, "ip")?.trim().orEmpty()
        val port = field(row, "port")?.unquoted()?.toIntOrNull() ?: 0
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
            countryCode = stringField(row, "country")?.trim()?.uppercase().orEmpty(),
            httpsSupported = "https" in supported,
            anonymity = stringField(row, "anonymityLevel")?.trim().orEmpty(),
            reportedLatencyMs = field(row, "latency")?.toDoubleOrNull()?.toInt()?.takeIf { it > 0 },
            source = source.name,
        )
    }

    private fun parseProxifly(source: ProxySource, json: String, limit: Int): List<ProxyEndpoint> =
        json.split(PROXYFLY_ENTRY_START)
            .drop(1)
            .mapNotNull { row -> proxiflyEndpoint(source, row) }
            .distinctBy { "${it.type}:${it.host}:${it.port}" }
            .take(limit.coerceIn(1, 30))

    private fun proxiflyEndpoint(source: ProxySource, row: String): ProxyEndpoint? {
        val protocol = stringField(row, "protocol")?.trim()?.lowercase() ?: return null
        val type = runCatching { ProxyType.valueOf(protocol.uppercase()) }.getOrNull() ?: return null
        val host = stringField(row, "ip")?.trim().orEmpty()
        val port = field(row, "port")?.unquoted()?.toIntOrNull() ?: 0
        if (host.isBlank() || port !in 1..65535) return null
        val country = Regex("\\\"geolocation\\\"\\s*:\\s*\\{[^}]*\\\"country\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"")
            .find(row)?.groupValues?.getOrNull(1).orEmpty()
        return ProxyEndpoint(
            type = type,
            host = host,
            port = port,
            countryCode = country.uppercase(),
            httpsSupported = field(row, "https")?.toBooleanStrictOrNull() ?: false,
            anonymity = stringField(row, "anonymity")?.trim().orEmpty(),
            source = source.name,
        )
    }

    private fun field(row: String, key: String): String? = Regex(
        "\"${Regex.escape(key)}\"\\s*:\\s*(\"(?:\\\\.|[^\"\\\\])*\"|\\[[^]]*\\]|-?\\d+(?:\\.\\d+)?|true|false)",
    ).find(row)?.groupValues?.getOrNull(1)

    private fun String.unquoted(): String =
        if (length >= 2 && first() == '\"' && last() == '\"') substring(1, length - 1) else this

    private fun stringField(row: String, key: String): String? = field(row, key)
        ?.takeIf { it.length >= 2 && it.first() == '"' && it.last() == '"' }
        ?.let { it.substring(1, it.length - 1) }
        ?.replace("\\\"", "\"")
        ?.replace("\\\\", "\\")

    private fun arrayField(row: String, key: String): List<String> = field(row, key)
        ?.let { value -> STRING_PATTERN.findAll(value).map { it.groupValues[1] }.toList() }
        .orEmpty()

    private val OBJECT_PATTERN = Regex("\\{([^{}]*)}")
    private val PROXYFLY_ENTRY_START = Regex("(?=\\{\\s*\"proxy\"\\s*:)" )
    private val STRING_PATTERN = Regex("\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"")
}
