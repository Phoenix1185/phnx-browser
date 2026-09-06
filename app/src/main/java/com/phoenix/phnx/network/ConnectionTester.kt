package com.phoenix.phnx.network

import android.util.Base64
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.NoRouteToHostException
import java.net.Proxy
import java.net.SocketTimeoutException
import java.net.URL
import java.nio.charset.StandardCharsets
import javax.net.ssl.SSLException
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

enum class ConnectionTestState {
    SUCCESS,
    FAILURE,
    UNSUPPORTED,
}

data class ConnectionTestResult(
    val state: ConnectionTestState,
    val message: String,
    val statusCode: Int? = null,
    val latencyMs: Int? = null,
)

class ConnectionTester(
    private val credentialProvider: (String) -> String? = { null },
    private val testUrl: URL = URL(DEFAULT_TEST_URL),
    private val timeoutMillis: Int = DEFAULT_TIMEOUT_MILLIS,
) {
    fun test(config: ProfileNetworkConfig): ConnectionTestResult = testConfig(config)

    fun testProxy(config: ProfileNetworkConfig, endpoint: ProxyEndpoint): ConnectionTestResult =
        testConfig(
            config.copy(
                proxyType = endpoint.type,
                proxyHost = endpoint.host,
                proxyPort = endpoint.port,
                username = "",
                credentialReference = null,
                fallbackToFreeProxy = false,
                fallbackToDirect = false,
            ),
        )

    private fun testConfig(config: ProfileNetworkConfig): ConnectionTestResult {
        val errors = NetworkConfigValidator.validate(config)
        if (errors.isNotEmpty()) return failure(errors.joinToString(" "))
        if (!config.enabled) return failure("Network configuration is disabled.")
        if (config.mode.usesProxy() && config.proxyHost.isBlank()) {
            return unsupported("Select a proxy route before testing it.")
        }

        val proxyConfig = if (config.mode.usesProxy() && config.proxyHost.isNotBlank()) {
            runCatching {
                ProxyConfigNormalizer.normalize(
                    scheme = config.proxyType,
                    host = config.proxyHost,
                    port = config.proxyPort,
                    username = config.username.takeIf { it.isNotEmpty() },
                )
            }.getOrElse { return failure(it.message ?: "Invalid proxy configuration.") }
        } else {
            null
        }
        if (proxyConfig?.scheme in setOf(ProxyType.SOCKS4, ProxyType.SOCKS5) && proxyConfig.username != null) {
            return unsupported("SOCKS proxy authentication is not supported by this Android HTTP test stack.")
        }
        if (proxyConfig?.scheme == ProxyType.HTTPS) {
            return testSecureProxy(config, proxyConfig)
        }

        val proxy = when (config.mode) {
            NetworkMode.DIRECT -> Proxy.NO_PROXY
            else -> proxyFor(proxyConfig ?: return unsupported("Select a proxy route before testing it."))
        }
        var connection: HttpURLConnection? = null
        return try {
            val startedAt = System.nanoTime()
            connection = testUrl.openConnection(proxy) as? HttpURLConnection
                ?: return unsupported("The connection tester only supports HTTP endpoints.")
            connection!!.connectTimeout = timeoutMillis
            connection!!.readTimeout = timeoutMillis
            connection!!.instanceFollowRedirects = false
            applyProxyCredentials(connection!!, proxyConfig, config)?.let { return failure(it) }
            val statusCode = connection!!.responseCode
            if (statusCode in 200..399) {
                ConnectionTestResult(
                    state = ConnectionTestState.SUCCESS,
                    message = "Connection succeeded.",
                    statusCode = statusCode,
                    latencyMs = ((System.nanoTime() - startedAt) / 1_000_000L).toInt(),
                )
            } else if (statusCode == 407) {
                failure("Proxy authentication failed. Check your proxy credentials.", statusCode)
            } else {
                failure("Connection returned HTTP $statusCode.", statusCode)
            }
        } catch (error: Exception) {
            failure(protocolError(error))
        } finally {
            connection?.disconnect()
        }
    }

    private fun proxyFor(config: ProxyConfig): Proxy {
        val type = when (config.scheme) {
            ProxyType.HTTP, ProxyType.HTTPS -> Proxy.Type.HTTP
            ProxyType.SOCKS4, ProxyType.SOCKS5 -> Proxy.Type.SOCKS
        }
        return Proxy(type, InetSocketAddress.createUnresolved(config.host, config.port))
    }

    private fun applyProxyCredentials(
        connection: HttpURLConnection,
        proxyConfig: ProxyConfig?,
        config: ProfileNetworkConfig,
    ): String? {
        if (proxyConfig?.username == null) return null
        val reference = config.credentialReference ?: return "Proxy credentials are incomplete."
        val secret = credentialProvider(reference) ?: return "Proxy credentials are unavailable."
        val token = "${proxyConfig.username}:$secret".toByteArray(StandardCharsets.UTF_8)
        connection.setRequestProperty(
            "Proxy-Authorization",
            "Basic ${Base64.encodeToString(token, Base64.NO_WRAP)}",
        )
        return null
    }

    private fun testSecureProxy(config: ProfileNetworkConfig, proxy: ProxyConfig): ConnectionTestResult {
        val startedAt = System.nanoTime()
        val socket = runCatching {
            (SSLSocketFactory.getDefault() as SSLSocketFactory).createSocket().apply {
                connect(InetSocketAddress.createUnresolved(proxy.host, proxy.port), timeoutMillis)
                soTimeout = timeoutMillis
                startHandshake()
            }
        }.getOrElse { return failure(protocolError(it)) }

        return (socket as SSLSocket).use { secureSocket ->
            runCatching {
                val targetPort = testUrl.port.takeIf { it > 0 } ?: if (testUrl.protocol == "https") 443 else 80
                val target = proxyAuthority(testUrl.host, targetPort)
                val writer = secureSocket.outputStream.bufferedWriter(StandardCharsets.US_ASCII)
                writer.append("CONNECT ").append(target).append(" HTTP/1.1\r\n")
                writer.append("Host: ").append(target).append("\r\n")
                writer.append("Proxy-Connection: Keep-Alive\r\n")
                if (proxy.username != null) {
                    val reference = config.credentialReference ?: return@runCatching failure("Proxy credentials are incomplete.")
                    val secret = credentialProvider(reference) ?: return@runCatching failure("Proxy credentials are unavailable.")
                    val token = "${proxy.username}:$secret".toByteArray(StandardCharsets.UTF_8)
                    writer.append("Proxy-Authorization: Basic ")
                        .append(Base64.encodeToString(token, Base64.NO_WRAP))
                        .append("\r\n")
                }
                writer.append("\r\n").flush()
                val reader = BufferedReader(InputStreamReader(secureSocket.inputStream, StandardCharsets.US_ASCII))
                val statusLine = reader.readLine().orEmpty()
                val statusCode = Regex("HTTP/\\d(?:\\.\\d)? (\\d{3})").find(statusLine)
                    ?.groupValues?.get(1)?.toIntOrNull()
                    ?: return@runCatching failure("Proxy returned an invalid HTTP response.")
                when {
                    statusCode == 200 -> ConnectionTestResult(
                        state = ConnectionTestState.SUCCESS,
                        message = "Connection succeeded.",
                        statusCode = statusCode,
                        latencyMs = ((System.nanoTime() - startedAt) / 1_000_000L).toInt(),
                    )
                    statusCode == 407 -> failure("Proxy authentication failed. Check your proxy credentials.", statusCode)
                    else -> failure("Proxy connection returned HTTP $statusCode.", statusCode)
                }
            }.getOrElse { failure(protocolError(it)) }
        }
    }

    private fun protocolError(error: Throwable): String = when {
        error is SocketTimeoutException -> "Proxy connection timed out."
        error is SSLException -> "TLS connection to proxy failed."
        error is ConnectException || error is NoRouteToHostException ||
            error.message.orEmpty().contains("connection refused", ignoreCase = true) ->
            "Could not connect to proxy. Connection refused."
        else -> "Proxy connection failed: ${error.message ?: error.javaClass.simpleName}."
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
