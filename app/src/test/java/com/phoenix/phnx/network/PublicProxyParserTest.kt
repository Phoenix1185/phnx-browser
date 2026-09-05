package com.phoenix.phnx.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PublicProxyParserTest {
    @Test
    fun parsesHProxyMetadata() {
        val source = ProxySource("hproxy", "HProxy", "https://example.com/feed")
        val proxies = PublicProxyParser.parse(
            source,
            """[{"ip":"203.0.113.10","port":1080,"protocols":["socks5","https"],"country_code":"US","anonymity":"elite","latency_ms":142}]""",
        )

        assertEquals(1, proxies.size)
        assertEquals(ProxyType.HTTPS, proxies.single().type)
        assertEquals("US", proxies.single().countryCode)
        assertTrue(proxies.single().httpsSupported)
        assertEquals(142, proxies.single().reportedLatencyMs)
    }

    @Test
    fun parsesPlainProxyFeedAndSkipsInvalidRows() {
        val source = ProxySource("proxyscrape", "ProxyScrape", "https://example.com/feed")
        val proxies = PublicProxyParser.parse(source, "203.0.113.10:8080\ninvalid\n198.51.100.7:65536\n198.51.100.7:1080", limit = 5)

        assertEquals(2, proxies.size)
        assertTrue(proxies.all { it.type == ProxyType.HTTP })
    }

    @Test
    fun parsesGeonodeMetadata() {
        val source = ProxySource("geonode", "Geonode", "https://example.com/feed")
        val proxies = PublicProxyParser.parse(
            source,
            """{"data":[{"ip":"203.0.113.10","port":"8443","country":"US","protocols":["https"],"anonymityLevel":"elite","latency":142.5}]}""",
        )

        assertEquals(1, proxies.size)
        assertEquals(ProxyType.HTTPS, proxies.single().type)
        assertEquals("US", proxies.single().countryCode)
        assertEquals("Geonode", proxies.single().source)
        assertEquals(142, proxies.single().reportedLatencyMs)
    }

    @Test
    fun parsesProxiflyMetadata() {
        val source = ProxySource("proxifly", "Proxifly", "https://example.com/feed")
        val proxies = PublicProxyParser.parse(
            source,
            """[{"proxy":"socks5://203.0.113.10:1080","protocol":"socks5","ip":"203.0.113.10","port":1080,"https":false,"anonymity":"transparent","geolocation":{"country":"US","city":"Test"}}]""",
        )

        assertEquals(1, proxies.size)
        assertEquals(ProxyType.SOCKS5, proxies.single().type)
        assertEquals("US", proxies.single().countryCode)
        assertEquals("Proxifly", proxies.single().source)
    }

    @Test
    fun bundledSourcesAreHttpsAndCoverTheConfiguredPublicFeeds() {
        val ids = ProxySources.default.map(ProxySource::id)

        assertTrue(ids.containsAll(listOf("hproxy", "proxyscrape", "geonode", "proxifly", "iplocate_http", "iplocate_https", "iplocate_socks5")))
        assertTrue(ProxySources.default.all { it.endpoint.startsWith("https://") })
    }
}
