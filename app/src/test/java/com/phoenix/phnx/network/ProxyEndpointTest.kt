package com.phoenix.phnx.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProxyEndpointTest {
    @Test
    fun codecRoundTripsFallbackEndpoints() {
        val endpoints = listOf(
            ProxyEndpoint(ProxyType.HTTPS, "203.0.113.10", 8443),
            ProxyEndpoint(ProxyType.SOCKS5, "198.51.100.7", 1080),
        )

        assertEquals(endpoints, ProxyEndpointCodec.decode(ProxyEndpointCodec.encode(endpoints)))
    }

    @Test
    fun webViewRulesUseSupportedProxySchemes() {
        assertEquals("http://proxy.example:8080", ProxyEndpoint(ProxyType.HTTP, "proxy.example", 8080).asWebViewRule())
        assertEquals("socks://proxy.example:1080", ProxyEndpoint(ProxyType.SOCKS5, "proxy.example", 1080).asWebViewRule())
    }

    @Test
    fun codecRoundTripsHealthMetadata() {
        val endpoint = ProxyEndpoint(
            type = ProxyType.SOCKS5,
            host = "198.51.100.7",
            port = 1080,
            countryCode = "US",
            countryName = "United States",
            httpsSupported = true,
            anonymity = "elite",
            reportedLatencyMs = 100,
            latencyMs = 142,
            lastCheckedAt = 1234L,
            health = ProxyHealthStatus.HEALTHY,
        )

        assertEquals(endpoint, ProxyEndpointCodec.decode(ProxyEndpointCodec.encode(listOf(endpoint))).single())
    }

    @Test
    fun selectorPrefersHealthyConnectivityOverSlowOrFailedRoutes() {
        val selected = ProxySelector.bestHealthy(
            listOf(
                ProxyEndpoint(ProxyType.HTTP, "slow", 80, latencyMs = 20, health = ProxyHealthStatus.SLOW),
                ProxyEndpoint(ProxyType.HTTP, "healthy", 80, latencyMs = 142, health = ProxyHealthStatus.HEALTHY),
                ProxyEndpoint(ProxyType.HTTP, "failed", 80, latencyMs = 1, health = ProxyHealthStatus.FAILED),
            ),
        )

        assertEquals("healthy", selected?.host)
        assertTrue(selected?.health == ProxyHealthStatus.HEALTHY)
    }
}
