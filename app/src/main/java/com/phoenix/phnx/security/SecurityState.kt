package com.phoenix.phnx.security

enum class BrowserSecurityState {
    UNKNOWN,
    SECURE,
    NOT_SECURE,
    CERTIFICATE_ERROR,
    SAFE_BROWSING_WARNING,
}

object SecurityStateResolver {
    fun fromUrl(url: String): BrowserSecurityState = when {
        url.startsWith("https://", ignoreCase = true) -> BrowserSecurityState.SECURE
        url.startsWith("http://", ignoreCase = true) -> BrowserSecurityState.NOT_SECURE
        else -> BrowserSecurityState.UNKNOWN
    }
}
