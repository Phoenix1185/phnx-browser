package com.phoenix.phnx.network

import android.util.Base64
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL
import java.nio.charset.StandardCharsets

enum class ConnectionTestState {
    SUCCESS,
    FAILURE,
    UNSUPPORTED,
}

data class ConnectionTestResult(
    val state: ConnectionTestState,
    val message: String,
    val statusCode: Int? = null,
)

class ConnectionTester(
    private val credentialProvider: (String) -> String? = { null },
    private val testUrl: URL = URL(DEFAULT_TEST_URL),
    private val timeoutMillis: Int = DEFAULT_TIMEOUT_MILLIS,
) {
    fun test(config: ProfileNetworkConfig): ConnectionTestResult {
        val errors = NetworkConfigValidator.validate(config)
        if (errors.isNotEmpty()) return failure(errors.joinToString(" "))
        if (!config.enabled) return failure("Network configuration is disabled.")
        if (config.mode == NetworkMode.PROXY && config.proxyHost.isBlank()) {
            return unsupported("Free proxy pool routes are applied by WebView; configure a primary proxy to test it directly.")
        }

        val proxy = when (config.mode) {
            NetworkMode.DIRECT -> Proxy.NO_PROXY
            NetworkMode.PROXY -> proxyFor(config)
        }
        var connection: HttpURLConnection? = null
        return try {
            connection = testUrl.openConnection(proxy) as? HttpURLConnection
                ?: return unsupported("The connection tester only supports HTTP endpoints.")
            connection!!.connectTimeout = timeoutMillis
            connection!!.readTimeout = timeoutMillis
            connection!!.instanceFollowRedirects = false
            applyProxyCredentials(connection!!, config)?.let { return failure(it) }
            val statusCode = connection!!.responseCode
            if (statusCode in 200..399) {
                ConnectionTestResult(
                    state = ConnectionTestState.SUCCESS,
                    message = "Connection succeeded.",
                    statusCode = statusCode,
                )
            } else {
                failure("Connection returned HTTP $statusCode.", statusCode)
            }
        } catch (error: Exception) {
            failure("Connection failed: ${error.message ?: error.javaClass.simpleName}.")
        } finally {
            connection?.disconnect()
        }
    }

    private fun proxyFor(config: ProfileNetworkConfig): Proxy {
        val type = when (config.proxyType) {
            ProxyType.HTTP, ProxyType.HTTPS -> Proxy.Type.HTTP
            ProxyType.SOCKS4, ProxyType.SOCKS5 -> Proxy.Type.SOCKS
            null -> return Proxy.NO_PROXY
        }
        return Proxy(type, InetSocketAddress.createUnresolved(config.proxyHost, config.proxyPort))
    }

    private fun applyProxyCredentials(connection: HttpURLConnection, config: ProfileNetworkConfig): String? {
        if (config.mode != NetworkMode.PROXY || config.username.isBlank()) return null
        val reference = config.credentialReference ?: return "Proxy credentials are incomplete."
        val secret = credentialProvider(reference) ?: return "Proxy credentials are unavailable."
        val token = "${config.username}:$secret".toByteArray(StandardCharsets.UTF_8)
        connection.setRequestProperty(
            "Proxy-Authorization",
            "Basic ${Base64.encodeToString(token, Base64.NO_WRAP)}",
        )
        return null
    }

    private fun failure(message: String, statusCode: Int? = null) = ConnectionTestResult(
        state = ConnectionTestState.FAILURE,
        message = message,
        statusCode = statusCode,
    )

    private fun unsupported(message: String) = ConnectionTestResult(
        state = ConnectionTestState.UNSUPPORTED,
        message = message,
    )

    private companion object {
        const val DEFAULT_TEST_URL = "https://example.com/"
        const val DEFAULT_TIMEOUT_MILLIS = 10_000
    }
}
