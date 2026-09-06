package com.phoenix.phnx.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProxyConfigTest {
    @Test
    fun normalizesGenericAuthenticatedEndpointWithoutChangingCredentials() {
        val config = ProxyConfigNormalizer.normalize(
            scheme = ProxyType.HTTP,
            hostInput = "proxy.example.net:4711",
            portInput = "",
            usernameInput = "user@region",
            passwordInput = "p@ss:word/with?symbols",
        )

        assertEquals(ProxyType.HTTP, config.scheme)
        assertEquals("proxy.example.net", config.host)
        assertEquals(4711, config.port)
        assertEquals("user@region", config.username)
        assertEquals("p@ss:word/with?symbols", config.password)
        assertEquals("http://proxy.example.net:4711", config.asWebViewRule())
    }

    @Test
    fun supportsBracketedAndUnbracketedIpv6() {
        val bracketed = ProxyConfigNormalizer.normalize(
            scheme = ProxyType.HTTPS,
            hostInput = "[2001:db8::10]:9443",
            portInput = "",
        )
        val unbracketed = ProxyConfigNormalizer.normalize(
            scheme = ProxyType.SOCKS5,
            hostInput = "2001:db8::11",
            portInput = "1087",
        )

        assertEquals("https://[2001:db8::10]:9443", bracketed.asWebViewRule())
        assertEquals("socks://[2001:db8::11]:1087", unbracketed.asWebViewRule())
    }

    @Test
    fun rejectsMalformedHostAndPortWithUserFacingReason() {
        val errors = NetworkConfigValidator.validate(
            ProfileNetworkConfig(
                id = "network_a",
                profileId = "profile_a",
                mode = NetworkMode.MY_PROXY,
                proxyType = ProxyType.HTTP,
                proxyHost = "bad host",
                proxyPort = 70000,
            ),
        )

        assertTrue(errors.contains("Proxy port must be between 1 and 65535."))
        assertTrue(ProxyConfigNormalizer.validate(
            ProfileNetworkConfig(
                id = "network_a",
                profileId = "profile_a",
                mode = NetworkMode.MY_PROXY,
                proxyType = ProxyType.HTTP,
                proxyHost = "bad host",
                proxyPort = 31234,
            ),
        ).contains("Invalid proxy host or port."))
    }
}
