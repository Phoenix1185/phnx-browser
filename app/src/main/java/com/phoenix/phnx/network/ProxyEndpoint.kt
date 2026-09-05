package com.phoenix.phnx.network

data class ProxyEndpoint(
    val type: ProxyType,
    val host: String,
    val port: Int,
    val countryCode: String = "",
    val countryName: String = "",
    val httpsSupported: Boolean = false,
    val anonymity: String = "",
    val reportedLatencyMs: Int? = null,
    val latencyMs: Int? = null,
    val lastCheckedAt: Long? = null,
    val health: ProxyHealthStatus = ProxyHealthStatus.UNKNOWN,
    val failureCount: Int = 0,
    val lastError: String = "",
) {
    fun asWebViewRule(): String = when (type) {
        ProxyType.HTTP -> "http://$host:$port"
        ProxyType.HTTPS -> "https://$host:$port"
        ProxyType.SOCKS4, ProxyType.SOCKS5 -> "socks://$host:$port"
    }
}

object ProxyEndpointCodec {
    fun encode(endpoints: List<ProxyEndpoint>): String = endpoints.joinToString("\n") {
        listOf(
            it.type.name,
            it.host,
            it.port.toString(),
            it.countryCode,
            it.countryName,
            it.httpsSupported.toString(),
            it.anonymity,
            it.reportedLatencyMs?.toString().orEmpty(),
            it.latencyMs?.toString().orEmpty(),
            it.lastCheckedAt?.toString().orEmpty(),
            it.health.name,
            it.failureCount.toString(),
            it.lastError,
        ).joinToString("|")
    }

    fun decode(value: String): List<ProxyEndpoint> = value.lineSequence()
        .map { it.split('|') }
        .mapNotNull { parts ->
            if (parts.size != 3) return@mapNotNull null
            val type = runCatching { ProxyType.valueOf(parts[0]) }.getOrNull() ?: return@mapNotNull null
            val port = parts[2].toIntOrNull() ?: return@mapNotNull null
            if (parts[1].isBlank() || port !in 1..65535) {
                null
            } else {
                ProxyEndpoint(
                    type = type,
                    host = parts[1],
                    port = port,
                    countryCode = parts.getOrNull(3).orEmpty(),
                    countryName = parts.getOrNull(4).orEmpty(),
                    httpsSupported = parts.getOrNull(5)?.toBooleanStrictOrNull() ?: false,
                    anonymity = parts.getOrNull(6).orEmpty(),
                    reportedLatencyMs = parts.getOrNull(7)?.toIntOrNull(),
                    latencyMs = parts.getOrNull(8)?.toIntOrNull(),
                    lastCheckedAt = parts.getOrNull(9)?.toLongOrNull(),
                    health = parts.getOrNull(10)?.let { runCatching { ProxyHealthStatus.valueOf(it) }.getOrNull() }
                        ?: ProxyHealthStatus.UNKNOWN,
                    failureCount = parts.getOrNull(11)?.toIntOrNull() ?: 0,
                    lastError = parts.getOrNull(12).orEmpty(),
                )
            }
        }
        .distinctBy { "${it.type}:${it.host}:${it.port}" }
        .toList()
}
