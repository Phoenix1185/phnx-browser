package com.phoenix.phnx.system

import android.content.Intent
import java.net.URI

object UrlIntentParser {
    enum class Classification {
        NORMAL_WEB,
        ANDROID_APP_LINK,
        INVALID,
    }

    data class Inspection(
        val normalizedUrl: String?,
        val scheme: String?,
        val host: String?,
        val classification: Classification,
    )

    fun parse(intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_VIEW) return null
        return parseUrl(intent.dataString)
    }

    fun parseUrl(rawUrl: String?): String? = inspect(rawUrl).normalizedUrl

    fun inspect(rawUrl: String?): Inspection {
        val value = rawUrl?.trim().orEmpty()
        if (value.isEmpty() || value.any { it.isISOControl() }) return invalid()

        val hasScheme = SCHEME_PATTERN.containsMatchIn(value)
        val normalized = when {
            value.startsWith("//") -> "https:$value"
            hasScheme -> value
            looksLikeBareHost(value) -> "https://$value"
            else -> null
        } ?: return invalid()

        val uri = runCatching { URI(normalized) }.getOrNull() ?: return invalid()
        val scheme = uri.scheme?.lowercase()
        val host = uri.host?.lowercase()?.trim('.')
        if (scheme !in SUPPORTED_SCHEMES) {
            val classification = if (scheme in ANDROID_SCHEMES) {
                Classification.ANDROID_APP_LINK
            } else {
                Classification.INVALID
            }
            return Inspection(null, scheme, host, classification)
        }
        if (host.isNullOrBlank() || uri.userInfo != null) {
            return invalid(scheme, host)
        }
        return Inspection(normalized, scheme, host, Classification.NORMAL_WEB)
    }

    fun summary(rawUrl: String?): String {
        val inspection = inspect(rawUrl)
        return "scheme=${inspection.scheme ?: "none"} " +
            "host=${inspection.host ?: "none"} " +
            "classification=${inspection.classification}"
    }

    private fun looksLikeBareHost(value: String): Boolean {
        if (value.any { it.isWhitespace() }) return false
        val authority = value.substringBefore('/').substringBefore('?').substringBefore('#')
        return authority == "localhost" || authority.startsWith("www.") ||
            authority.contains('.') || authority.startsWith("[")
    }

    private fun invalid(scheme: String? = null, host: String? = null): Inspection =
        Inspection(null, scheme, host, Classification.INVALID)

    private val SCHEME_PATTERN = Regex("^[A-Za-z][A-Za-z0-9+.-]*:")
    private val ANDROID_SCHEMES = setOf("market", "tel", "mailto", "geo", "intent")
    private val SUPPORTED_SCHEMES = setOf("http", "https")
}
