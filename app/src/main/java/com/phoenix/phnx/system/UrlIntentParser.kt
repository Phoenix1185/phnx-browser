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
        if (value.isEmpty() || value.any { it.isISOControl() }) return null
        val uri = runCatching { URI(value) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase() ?: return null
        val host = uri.host?.lowercase()?.trim('.') ?: return null
        if (scheme !in SUPPORTED_SCHEMES || host.isBlank() || uri.userInfo != null) {
            return null
        }
        return value
    }

    private val SUPPORTED_SCHEMES = setOf("http", "https")
}
