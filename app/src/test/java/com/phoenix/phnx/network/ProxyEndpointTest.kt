package com.phoenix.phnx.network

import org.junit.Assert.assertEquals
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
}
