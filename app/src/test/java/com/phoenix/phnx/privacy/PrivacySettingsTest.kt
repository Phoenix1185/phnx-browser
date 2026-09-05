package com.phoenix.phnx.privacy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacySettingsTest {
    @Test
    fun defaultsAllowJavascriptAndSafeBrowsingButBlockThirdPartyCookies() {
        val settings = PrivacySettings("profile_a")

        assertTrue(settings.javascriptEnabled)
        assertTrue(settings.safeBrowsingEnabled)
        assertEquals(false, settings.thirdPartyCookiesAllowed)
        assertEquals(TrackingProtectionLevel.STANDARD, settings.trackingProtection)
    }

    @Test
    fun invalidPersistedTrackingLevelFallsBackToStandard() {
        val restored = PrivacySettingsEntity(
            profileId = "profile_a",
            thirdPartyCookiesAllowed = true,
            trackingProtection = "removed_value",
            doNotTrack = true,
            javascriptEnabled = false,
            popupsAllowed = true,
            safeBrowsingEnabled = false,
        ).toDomain()

        assertEquals(TrackingProtectionLevel.STANDARD, restored.trackingProtection)
        assertTrue(restored.doNotTrack)
        assertTrue(restored.popupsAllowed)
    }
}
