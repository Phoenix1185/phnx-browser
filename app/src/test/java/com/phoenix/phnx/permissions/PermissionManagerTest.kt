package com.phoenix.phnx.permissions

import org.junit.Assert.assertEquals
import org.junit.Test

class PermissionManagerTest {
    @Test
    fun permissionEntityRoundTripsDecision() {
        val permission = SitePermissionEntity(
            profileId = "profile_a",
            origin = "https://example.com",
            type = SitePermissionType.LOCATION.name,
            decision = SitePermissionDecision.BLOCK.name,
        ).toDomain()

        assertEquals("profile_a", permission.profileId)
        assertEquals("https://example.com", permission.origin)
        assertEquals(SitePermissionType.LOCATION, permission.type)
        assertEquals(SitePermissionDecision.BLOCK, permission.decision)
    }

    @Test
    fun invalidDecisionFallsBackToAsk() {
        val permission = SitePermissionEntity(
            profileId = "profile_a",
            origin = "https://example.com",
            type = SitePermissionType.CAMERA.name,
            decision = "removed_value",
        ).toDomain()

        assertEquals(SitePermissionDecision.ASK, permission.decision)
    }
}
