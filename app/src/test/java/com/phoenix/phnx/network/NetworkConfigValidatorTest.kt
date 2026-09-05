package com.phoenix.phnx.network

import org.junit.Assert.assertTrue
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
}
