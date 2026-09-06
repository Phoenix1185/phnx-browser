package com.phoenix.phnx.adblock

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdBlockRuleEngineTest {
    private val engine = AdBlockRuleEngine()

    @Test
    fun blocksBaselineAdAndTrackerDomains() {
        val settings = AdBlockSettings("profile")

        assertTrue(engine.evaluate("https://ads.example.doubleclick.net/script.js", "https://site.example", settings).blocked)
        assertTrue(engine.evaluate("https://metrics.google-analytics.com/collect", "https://site.example", settings).blocked)
    }

    @Test
    fun disabledBlockingAllowsRequests() {
        val settings = AdBlockSettings("profile", enabled = false)

        assertFalse(engine.evaluate("https://ads.example.doubleclick.net/script.js", "https://site.example", settings).blocked)
    }

    @Test
    fun firstPartyExceptionAllowsSubresources() {
        val settings = AdBlockSettings("profile", siteExceptions = setOf("site.example"))

        assertFalse(engine.evaluate("https://doubleclick.net/ad.js", "https://www.site.example/page", settings).blocked)
    }

    @Test
    fun hostMatchingDoesNotMatchLookalikeDomains() {
        val settings = AdBlockSettings("profile")

        assertFalse(engine.evaluate("https://notdoubleclick.net/ad.js", "https://site.example", settings).blocked)
    }
}
