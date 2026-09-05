package com.phoenix.phnx.network

data class ProxyEndpoint(
    val type: ProxyType,
    val host: String,
    val port: Int,
) {
    fun asWebViewRule(): String = when (type) {
        ProxyType.HTTP -> "http://$host:$port"
        ProxyType.HTTPS -> "https://$host:$port"
        ProxyType.SOCKS4, ProxyType.SOCKS5 -> "socks://$host:$port"
    }
}

object ProxyEndpointCodec {
    fun encode(endpoints: List<ProxyEndpoint>): String = endpoints.joinToString("\n") {
        "${it.type.name}|${it.host}|${it.port}"
    }

    fun decode(value: String): List<ProxyEndpoint> = value.lineSequence()
        .map { it.split('|') }
        .mapNotNull { parts ->
            if (parts.size != 3) return@mapNotNull null
            val type = runCatching { ProxyType.valueOf(parts[0]) }.getOrNull() ?: return@mapNotNull null
            val port = parts[2].toIntOrNull() ?: return@mapNotNull null
            if (parts[1].isBlank() || port !in 1..65535) null else ProxyEndpoint(type, parts[1], port)
        }
        .distinct()
        .toList()
}
