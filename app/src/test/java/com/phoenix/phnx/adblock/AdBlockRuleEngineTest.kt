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

    @Test
    fun parsesFilterListsAndHonorsExceptions() {
        val rules = AdBlockFilterParser.parse(
            """
            ! comment
            ||ads.example.com^
            @@||allowed.ads.example.com^
            0.0.0.0 tracker.example.net
            example.com##.ad
            """.trimIndent(),
        )

        assertTrue(rules.contains(AdBlockRule("ads.example.com")))
        assertTrue(rules.contains(AdBlockRule("allowed.ads.example.com", exception = true)))
        assertTrue(rules.contains(AdBlockRule("tracker.example.net")))
        assertFalse(rules.any { it.host == "example.com" })
    }

    @Test
    fun customRulesBlockMatchingHostsButNotLookalikes() {
        val settings = AdBlockSettings("profile")
        val rules = listOf(AdBlockRule("ads.example.com"))

        assertTrue(engine.evaluate("https://cdn.ads.example.com/script.js", "https://site.example", settings, rules).blocked)
        assertFalse(engine.evaluate("https://ads.example.co/script.js", "https://site.example", settings, rules).blocked)
        assertFalse(
            engine.evaluate(
                "https://cdn.ads.example.com/script.js",
                "https://site.example",
                settings,
                rules + AdBlockRule("cdn.ads.example.com", exception = true),
            ).blocked,
        )
    }
}
