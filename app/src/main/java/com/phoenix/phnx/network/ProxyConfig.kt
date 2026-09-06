package com.phoenix.phnx.network

import java.net.URI

/** Runtime proxy credentials stay in memory; persisted secrets use credentialReference. */
data class ProxyConfig(
    val scheme: ProxyType,
    val host: String,
    val port: Int,
    val username: String? = null,
    val password: String? = null,
) {
    fun asWebViewRule(): String = "${scheme.webViewScheme()}://${proxyAuthority(host, port)}"
}

object ProxyConfigNormalizer {
    fun normalize(
        scheme: ProxyType?,
        hostInput: String,
        portInput: String,
        usernameInput: String? = null,
        passwordInput: String? = null,
    ): ProxyConfig {
        val selectedScheme = scheme ?: throw IllegalArgumentException("Invalid proxy protocol.")
        val parts = parseHost(hostInput.trim())
        val portText = portInput.trim()
        val explicitPort = when {
            portText.isBlank() || portText == "0" -> null
            else -> portText.toIntOrNull() ?: throw invalidEndpoint()
        }
        val port = explicitPort ?: parts.port ?: throw invalidEndpoint()
        require(parts.port == null || explicitPort == null || parts.port == explicitPort) { INVALID_ENDPOINT_MESSAGE }
        require(port in 1..65535) { INVALID_ENDPOINT_MESSAGE }

        val host = parts.host.trim().trim('[', ']').trimEnd('.')
        require(isValidHost(host)) { INVALID_ENDPOINT_MESSAGE }

        val username = usernameInput?.takeIf { it.isNotEmpty() }
        val password = passwordInput?.takeIf { it.isNotEmpty() }
        require(password == null || username != null) {
            "Proxy username is required when a password is supplied."
        }
        return ProxyConfig(selectedScheme, host, port, username, password)
    }

    fun normalize(
        scheme: ProxyType?,
        host: String,
        port: Int,
        username: String? = null,
        password: String? = null,
    ): ProxyConfig = normalize(scheme, host, port.toString(), username, password)

    fun validate(config: ProfileNetworkConfig): List<String> = buildList {
        runCatching {
            normalize(
                scheme = config.proxyType,
                hostInput = config.proxyHost,
                portInput = config.proxyPort.toString(),
                usernameInput = config.username,
            )
        }.onFailure { add(it.message ?: "Invalid proxy configuration.") }
    }

    private data class HostParts(val host: String, val port: Int?)

    private fun parseHost(input: String): HostParts {
        require(input.isNotBlank()) { INVALID_ENDPOINT_MESSAGE }
        if (input.contains("://")) {
            val uri = runCatching { URI(input) }.getOrNull() ?: throw invalidEndpoint()
            require(uri.userInfo == null) { "Enter proxy credentials in the username and password fields." }
            val host = uri.host ?: uri.rawAuthority
                ?.substringAfter('[')
                ?.substringBefore(']')
                ?: throw invalidEndpoint()
            return HostParts(host, uri.port.takeIf { it > 0 })
        }
        if (input.startsWith("[")) {
            val closing = input.indexOf(']')
            require(closing > 1) { INVALID_ENDPOINT_MESSAGE }
            val host = input.substring(1, closing)
            val suffix = input.substring(closing + 1)
            val port = if (suffix.isBlank()) null else {
                require(suffix.startsWith(":")) { INVALID_ENDPOINT_MESSAGE }
                suffix.drop(1).toIntOrNull() ?: throw invalidEndpoint()
            }
            return HostParts(host, port)
        }
        val colonCount = input.count { it == ':' }
        if (colonCount == 1) {
            val separator = input.lastIndexOf(':')
            val possiblePort = input.substring(separator + 1).toIntOrNull()
            if (possiblePort != null) return HostParts(input.substring(0, separator), possiblePort)
        }
        return HostParts(input, null)
    }

    private fun isValidHost(host: String): Boolean {
        if (host.isBlank() || host.length > 253 || host.any { it.isWhitespace() }) return false
        if (host.contains(':')) return host.count { it == ':' } >= 2 && IPV6_PATTERN.matches(host)
        if (host.all { it.isDigit() || it == '.' }) {
            val parts = host.split('.')
            return parts.size == 4 && parts.all { it.toIntOrNull()?.let { value -> value in 0..255 } == true }
        }
        return HOSTNAME_PATTERN.matches(host)
    }

    private fun invalidEndpoint() = IllegalArgumentException(INVALID_ENDPOINT_MESSAGE)

    private const val INVALID_ENDPOINT_MESSAGE = "Invalid proxy host or port."

    private val HOSTNAME_PATTERN = Regex(
        "[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)*",
    )
    private val IPV6_PATTERN = Regex("[0-9A-Fa-f:.%]+")
}

internal fun ProxyType.webViewScheme(): String = when (this) {
    ProxyType.HTTP -> "http"
    ProxyType.HTTPS -> "https"
    ProxyType.SOCKS4, ProxyType.SOCKS5 -> "socks"
}

internal fun proxyAuthority(host: String, port: Int): String =
    "${if (host.contains(':') && !host.startsWith('[')) "[$host]" else host}:$port"
