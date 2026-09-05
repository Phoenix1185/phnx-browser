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
}
