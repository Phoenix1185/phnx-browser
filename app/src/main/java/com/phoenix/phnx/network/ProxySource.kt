package com.phoenix.phnx.network

data class ProxySource(
    val id: String,
    val name: String,
    val endpoint: String,
)

object ProxySources {
    val default: List<ProxySource> = listOf(
        ProxySource(
            id = "hproxy",
            name = "HProxy public feed",
            endpoint = "https://hproxy.com/api/proxy-list?format=json&protocol=http,https,socks5&recent=true&limit=30",
        ),
        ProxySource(
            id = "proxyscrape",
            name = "ProxyScrape HTTP feed",
            endpoint = "https://api.proxyscrape.com/v2/?request=getproxies&protocol=http&timeout=10000&country=all&ssl=all&anonymity=all",
        ),
    )
}
