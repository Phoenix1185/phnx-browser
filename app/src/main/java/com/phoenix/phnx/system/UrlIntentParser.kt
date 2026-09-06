package com.phoenix.phnx.system

import android.content.Intent
import java.net.URI

object UrlIntentParser {
    fun parse(intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_VIEW) return null
        return parseUrl(intent.dataString)
    }

    fun parseUrl(rawUrl: String?): String? {
        val value = rawUrl?.trim().orEmpty()
        if (value.any(Char::isISOControl)) return null
        val uri = runCatching { URI(value) }.getOrNull() ?: return null
        if (uri.scheme?.lowercase() !in SUPPORTED_SCHEMES || uri.host.isNullOrBlank() || uri.userInfo != null) {
            return null
        }
        return uri.toASCIIString()
    }

    private val SUPPORTED_SCHEMES = setOf("http", "https")
}
