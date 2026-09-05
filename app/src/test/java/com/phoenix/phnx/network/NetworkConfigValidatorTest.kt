package com.phoenix.phnx.network

import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class NetworkConfigValidatorTest {
    @Test
    fun directConfigurationIsValid() {
        val config = ProfileNetworkConfig(id = "network_a", profileId = "profile_a")

        assertTrue(NetworkConfigValidator.validate(config).isEmpty())
    }

    @Test
    fun proxyRequiresEndpointAndType() {
        val config = ProfileNetworkConfig(
            id = "network_a",
            profileId = "profile_a",
            mode = NetworkMode.PROXY,
        )

        assertTrue(NetworkConfigValidator.validate(config).size >= 3)
    }

    @Test
    fun proxyUsernameRequiresSecureCredentialReference() {
        val config = ProfileNetworkConfig(
            id = "network_a",
            profileId = "profile_a",
            mode = NetworkMode.PROXY,
            proxyType = ProxyType.HTTP,
            proxyHost = "proxy.example",
            proxyPort = 8080,
            username = "user",
        )

        assertTrue(NetworkConfigValidator.validate(config).any { it.contains("credentials") })
    }

    @Test
    fun disabledProxyCanBeSavedBeforeItsEndpointIsConfigured() {
        val config = ProfileNetworkConfig(
            id = "network_a",
            profileId = "profile_a",
            mode = NetworkMode.PROXY,
            enabled = false,
        )

        assertFalse(NetworkConfigValidator.validate(config).isNotEmpty())
    }

    @Test
    fun freeProxyOnlyConfigurationDoesNotRequirePrimaryEndpoint() {
        val config = ProfileNetworkConfig(
            id = "network_a",
            profileId = "profile_a",
            mode = NetworkMode.PROXY,
            fallbackToFreeProxy = true,
        )

        assertTrue(NetworkConfigValidator.validate(config).isEmpty())
    }

    @Test
    fun explicitFreePublicModeCanBeSavedBeforeSelection() {
        val config = ProfileNetworkConfig(
            id = "network_a",
            profileId = "profile_a",
            mode = NetworkMode.FREE_PUBLIC_PROXY,
        )

        assertTrue(NetworkConfigValidator.validate(config).isEmpty())
    }
}
