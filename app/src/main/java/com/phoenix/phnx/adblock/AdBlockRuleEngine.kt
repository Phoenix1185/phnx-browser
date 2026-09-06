package com.phoenix.phnx.adblock

import java.net.URI

enum class BlockCategory {
    AD,
    TRACKER,
    MALICIOUS_AD,
}

data class BlockDecision(
    val blocked: Boolean,
    val category: BlockCategory? = null,
)

class AdBlockRuleEngine {
    fun evaluate(url: String, firstPartyUrl: String, settings: AdBlockSettings): BlockDecision {
        if (!settings.enabled) return BlockDecision(blocked = false)
        val requestHost = host(url) ?: return BlockDecision(blocked = false)
        val firstPartyHost = host(firstPartyUrl)
        if (firstPartyHost != null && settings.siteExceptions.any { matchesDomain(firstPartyHost, it) }) {
            return BlockDecision(blocked = false)
        }

        val category = when {
            settings.blockMaliciousAds && matchesAny(requestHost, MALICIOUS_AD_DOMAINS) -> BlockCategory.MALICIOUS_AD
            settings.blockAds && matchesAny(requestHost, AD_DOMAINS) -> BlockCategory.AD
            settings.blockTrackers && matchesAny(requestHost, TRACKER_DOMAINS) -> BlockCategory.TRACKER
            else -> null
        }
        return BlockDecision(blocked = category != null, category = category)
    }

    fun canonicalHost(value: String): String? = host(value)

    private fun host(value: String): String? {
        val uri = runCatching { URI(value.trim()) }.getOrNull() ?: return null
        val host = uri.host?.lowercase()?.trim('.') ?: return null
        return host.takeIf { it.isNotBlank() && !it.contains(' ') }
    }

    private fun matchesAny(host: String, domains: Set<String>): Boolean = domains.any { matchesDomain(host, it) }

    private fun matchesDomain(host: String, domain: String): Boolean =
        host == domain || host.endsWith(".$domain")

    private companion object {
        val AD_DOMAINS = setOf(
            "adnxs.com",
            "adsrvr.org",
            "doubleclick.net",
            "googleadservices.com",
            "googlesyndication.com",
            "outbrain.com",
            "taboola.com",
        )
        val TRACKER_DOMAINS = setOf(
            "clarity.ms",
            "facebook.net",
            "google-analytics.com",
            "googletagmanager.com",
            "hotjar.com",
            "mixpanel.com",
            "segment.io",
        )
        val MALICIOUS_AD_DOMAINS = setOf(
            "adnxs-simple.com",
            "malvertising.example",
        )
    }
}
