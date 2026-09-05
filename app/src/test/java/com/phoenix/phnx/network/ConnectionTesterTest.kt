package com.phoenix.phnx.network

import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectionTesterTest {
    @Test
    fun disabledConfigurationDoesNotAttemptConnection() {
        val config = ProfileNetworkConfig(
            id = "network_a",
            profileId = "profile_a",
            enabled = false,
        )

        val result = ConnectionTester().test(config)

        assertEquals(ConnectionTestState.FAILURE, result.state)
        assertEquals("Network configuration is disabled.", result.message)
    }
}
