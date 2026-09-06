package com.phoenix.phnx.adblock

import android.content.Context
import com.phoenix.phnx.update.ChecksumVerifier
import java.io.ByteArrayInputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class AdBlockManager(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val ruleEngine = AdBlockRuleEngine()
    private val counters = ConcurrentHashMap<String, Counters>()
    private val rulesCache = ConcurrentHashMap<String, List<AdBlockRule>>()

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
        val profileCounters = counters.getOrPut(profileId) { Counters(readStats(profileId)) }
        profileCounters.evaluatedRequests.incrementAndGet()
        val decision = ruleEngine.evaluate(url, firstPartyUrl, getSettings(profileId), getRules(profileId))
        if (decision.blocked) {
            profileCounters.blockedRequests.incrementAndGet()
            when (decision.category) {
                BlockCategory.AD -> profileCounters.blockedAds.incrementAndGet()
                BlockCategory.TRACKER -> profileCounters.blockedTrackers.incrementAndGet()
                BlockCategory.MALICIOUS_AD -> profileCounters.blockedMaliciousAds.incrementAndGet()
                null -> Unit
            }
        }
        if (profileCounters.evaluatedRequests.get() % PERSIST_STATS_EVERY == 0L) persistStats(profileId, profileCounters.snapshot())
        return decision
    }

    fun installRuleset(profileId: String, payload: String, expectedSha256: String): Boolean {
        if (!ChecksumVerifier.verify(ByteArrayInputStream(payload.toByteArray()), expectedSha256)) return false
        val rules = AdBlockFilterParser.parse(payload)
        if (rules.isEmpty()) return false
        preferences.edit().putString(key(profileId, RULESET), payload).apply()
        rulesCache[profileId] = rules
        return true
    }

    fun rulesetRuleCount(profileId: String): Int = getRules(profileId).size

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

    fun stats(profileId: String): BlockedRequestStats {
        val stats = counters[profileId]?.snapshot() ?: readStats(profileId)
        persistStats(profileId, stats)
        return stats
    }

    fun clearProfile(profileId: String) {
        val editor = preferences.edit()
        KEYS.forEach { editor.remove(key(profileId, it)) }
        editor.apply()
        counters.remove(profileId)
        rulesCache.remove(profileId)
    }

    private fun key(profileId: String, field: String): String = "$field:$profileId"

    private fun getRules(profileId: String): List<AdBlockRule> = rulesCache.getOrPut(profileId) {
        AdBlockFilterParser.parse(preferences.getString(key(profileId, RULESET), "").orEmpty())
    }

    private fun readStats(profileId: String): BlockedRequestStats = BlockedRequestStats(
        evaluatedRequests = preferences.getLong(key(profileId, EVALUATED), 0),
        blockedRequests = preferences.getLong(key(profileId, BLOCKED), 0),
        blockedAds = preferences.getLong(key(profileId, ADS), 0),
        blockedTrackers = preferences.getLong(key(profileId, TRACKERS), 0),
        blockedMaliciousAds = preferences.getLong(key(profileId, MALICIOUS_ADS), 0),
    )

    private fun persistStats(profileId: String, stats: BlockedRequestStats) {
        preferences.edit()
            .putLong(key(profileId, EVALUATED), stats.evaluatedRequests)
            .putLong(key(profileId, BLOCKED), stats.blockedRequests)
            .putLong(key(profileId, ADS), stats.blockedAds)
            .putLong(key(profileId, TRACKERS), stats.blockedTrackers)
            .putLong(key(profileId, MALICIOUS_ADS), stats.blockedMaliciousAds)
            .apply()
    }

    private class Counters(initial: BlockedRequestStats) {
        val evaluatedRequests = AtomicLong(initial.evaluatedRequests)
        val blockedRequests = AtomicLong(initial.blockedRequests)
        val blockedAds = AtomicLong(initial.blockedAds)
        val blockedTrackers = AtomicLong(initial.blockedTrackers)
        val blockedMaliciousAds = AtomicLong(initial.blockedMaliciousAds)

        fun snapshot() = BlockedRequestStats(
            evaluatedRequests = evaluatedRequests.get(),
            blockedRequests = blockedRequests.get(),
            blockedAds = blockedAds.get(),
            blockedTrackers = blockedTrackers.get(),
            blockedMaliciousAds = blockedMaliciousAds.get(),
        )
    }

    private companion object {
        const val PREFERENCES = "ad_block_settings"
        const val ENABLED = "enabled"
        const val BLOCK_ADS = "block_ads"
        const val BLOCK_TRACKERS = "block_trackers"
        const val BLOCK_MALICIOUS_ADS = "block_malicious_ads"
        const val SITE_EXCEPTIONS = "site_exceptions"
        const val RULESET = "ruleset"
        const val EVALUATED = "evaluated"
        const val BLOCKED = "blocked"
        const val ADS = "ads"
        const val TRACKERS = "trackers"
        const val MALICIOUS_ADS = "malicious_ads"
        const val PERSIST_STATS_EVERY = 25L
        val KEYS = setOf(
            ENABLED,
            BLOCK_ADS,
            BLOCK_TRACKERS,
            BLOCK_MALICIOUS_ADS,
            SITE_EXCEPTIONS,
            RULESET,
            EVALUATED,
            BLOCKED,
            ADS,
            TRACKERS,
            MALICIOUS_ADS,
        )
    }
}
