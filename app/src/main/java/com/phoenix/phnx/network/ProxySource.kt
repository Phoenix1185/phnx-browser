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
            endpoint = "https://api.proxyscrape.com/v2/?request=displayproxies&protocol=http&timeout=10000&country=all&ssl=all&anonymity=all",
        ),
        ProxySource(
            id = "geonode",
            name = "Geonode public feed",
            endpoint = "https://proxylist.geonode.com/api/proxy-list?limit=50&page=1&sort_by=lastChecked&sort_type=desc",
        ),
        ProxySource(
            id = "proxifly",
            name = "Proxifly public feed",
            endpoint = "https://raw.githubusercontent.com/proxifly/free-proxy-list/main/proxies/all/data.json",
        ),
        ProxySource(
            id = "iplocate_http",
            name = "IPLocate HTTP feed",
            endpoint = "https://raw.githubusercontent.com/iplocate/free-proxy-list/main/protocols/http.txt",
        ),
        ProxySource(
            id = "iplocate_https",
            name = "IPLocate HTTPS feed",
            endpoint = "https://raw.githubusercontent.com/iplocate/free-proxy-list/main/protocols/https.txt",
        ),
        ProxySource(
            id = "iplocate_socks5",
            name = "IPLocate SOCKS5 feed",
            endpoint = "https://raw.githubusercontent.com/iplocate/free-proxy-list/main/protocols/socks5.txt",
        ),
        ProxySource(
            id = "thespeedx_http",
            name = "TheSpeedX HTTP feed",
            endpoint = "https://raw.githubusercontent.com/TheSpeedX/PROXY-List/master/http.txt",
        ),
        ProxySource(
            id = "thespeedx_socks4",
            name = "TheSpeedX SOCKS4 feed",
            endpoint = "https://raw.githubusercontent.com/TheSpeedX/PROXY-List/master/socks4.txt",
        ),
        ProxySource(
            id = "thespeedx_socks5",
            name = "TheSpeedX SOCKS5 feed",
            endpoint = "https://raw.githubusercontent.com/TheSpeedX/PROXY-List/master/socks5.txt",
        ),
        ProxySource(
            id = "monosans_http",
            name = "Monosans HTTP feed",
            endpoint = "https://raw.githubusercontent.com/monosans/proxy-list/main/proxies/http.txt",
        ),
        ProxySource(
            id = "monosans_socks4",
            name = "Monosans SOCKS4 feed",
            endpoint = "https://raw.githubusercontent.com/monosans/proxy-list/main/proxies/socks4.txt",
        ),
        ProxySource(
            id = "monosans_socks5",
            name = "Monosans SOCKS5 feed",
            endpoint = "https://raw.githubusercontent.com/monosans/proxy-list/main/proxies/socks5.txt",
        ),
        ProxySource(
            id = "openproxylist_https",
            name = "OpenProxyList HTTPS feed",
            endpoint = "https://raw.githubusercontent.com/roosterkid/openproxylist/main/HTTPS_RAW.txt",
        ),
    )
}
