package com.phoenix.phnx.adblock

import android.content.Context
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class AdBlockManager(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val ruleEngine = AdBlockRuleEngine()
    private val counters = ConcurrentHashMap<String, Counters>()

    fun getSettings(profileId: String): AdBlockSettings = AdBlockSettings(
        profileId = profileId,
        enabled = preferences.getBoolean(key(profileId, ENABLED), true),
        blockAds = preferences.getBoolean(key(profileId, BLOCK_ADS), true),
        blockTrackers = preferences.getBoolean(key(profileId, BLOCK_TRACKERS), true),
        blockMaliciousAds = preferences.getBoolean(key(profileId, BLOCK_MALICIOUS_ADS), true),
        siteExceptions = preferences.getStringSet(key(profileId, SITE_EXCEPTIONS), emptySet()).orEmpty().toSet(),
    )

    fun saveSettings(settings: AdBlockSettings) {
        preferences.edit()
            .putBoolean(key(settings.profileId, ENABLED), settings.enabled)
            .putBoolean(key(settings.profileId, BLOCK_ADS), settings.blockAds)
            .putBoolean(key(settings.profileId, BLOCK_TRACKERS), settings.blockTrackers)
            .putBoolean(key(settings.profileId, BLOCK_MALICIOUS_ADS), settings.blockMaliciousAds)
            .putStringSet(key(settings.profileId, SITE_EXCEPTIONS), settings.siteExceptions)
            .apply()
    }

    fun evaluate(profileId: String, url: String, firstPartyUrl: String): BlockDecision {
        val profileCounters = counters.getOrPut(profileId) { Counters() }
        profileCounters.evaluatedRequests.incrementAndGet()
        val decision = ruleEngine.evaluate(url, firstPartyUrl, getSettings(profileId))
        if (decision.blocked) {
            profileCounters.blockedRequests.incrementAndGet()
            when (decision.category) {
                BlockCategory.AD -> profileCounters.blockedAds.incrementAndGet()
                BlockCategory.TRACKER -> profileCounters.blockedTrackers.incrementAndGet()
                BlockCategory.MALICIOUS_AD -> profileCounters.blockedMaliciousAds.incrementAndGet()
                null -> Unit
            }
        }
        return decision
    }

    fun addSiteException(profileId: String, host: String): Boolean {
        val canonicalHost = ruleEngine.canonicalHost(host) ?: return false
        val settings = getSettings(profileId)
        saveSettings(settings.copy(siteExceptions = settings.siteExceptions + canonicalHost))
        return true
    }

    fun removeSiteException(profileId: String, host: String) {
        val settings = getSettings(profileId)
        saveSettings(settings.copy(siteExceptions = settings.siteExceptions - host.lowercase().trim('.')))
    }

    fun stats(profileId: String): BlockedRequestStats = BlockedRequestStats(
        evaluatedRequests = counters[profileId]?.evaluatedRequests?.get() ?: 0,
        blockedRequests = counters[profileId]?.blockedRequests?.get() ?: 0,
        blockedAds = counters[profileId]?.blockedAds?.get() ?: 0,
        blockedTrackers = counters[profileId]?.blockedTrackers?.get() ?: 0,
        blockedMaliciousAds = counters[profileId]?.blockedMaliciousAds?.get() ?: 0,
    )

    fun clearProfile(profileId: String) {
        val editor = preferences.edit()
        KEYS.forEach { editor.remove(key(profileId, it)) }
        editor.apply()
        counters.remove(profileId)
    }

    private fun key(profileId: String, field: String): String = "$field:$profileId"

    private class Counters {
        val evaluatedRequests = AtomicLong()
        val blockedRequests = AtomicLong()
        val blockedAds = AtomicLong()
        val blockedTrackers = AtomicLong()
        val blockedMaliciousAds = AtomicLong()
    }

    private companion object {
        const val PREFERENCES = "ad_block_settings"
        const val ENABLED = "enabled"
        const val BLOCK_ADS = "block_ads"
        const val BLOCK_TRACKERS = "block_trackers"
        const val BLOCK_MALICIOUS_ADS = "block_malicious_ads"
        const val SITE_EXCEPTIONS = "site_exceptions"
        val KEYS = setOf(
            ENABLED,
            BLOCK_ADS,
            BLOCK_TRACKERS,
            BLOCK_MALICIOUS_ADS,
            SITE_EXCEPTIONS,
        )
    }
}
