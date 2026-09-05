package com.phoenix.phnx.browser

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object NavigationController {
    private const val DEFAULT_SEARCH_ENGINE = "https://www.google.com/search?q="

    fun resolveInput(input: String, searchEngine: String = DEFAULT_SEARCH_ENGINE): String {
        val value = input.trim()
        if (value.isEmpty()) return "about:blank"

        val scheme = value.substringBefore(':', missingDelimiterValue = "").lowercase().takeIf { it.isNotEmpty() }
        if (scheme == "http" || scheme == "https") return value

        val looksLikeHost = !value.any(Char::isWhitespace) &&
            (value == "localhost" || value.contains('.') || value.startsWith("["))
        if (looksLikeHost) return "https://$value"

        return searchEngine + URLEncoder.encode(value, StandardCharsets.UTF_8.toString()).replace("+", "%20")
    }
}
