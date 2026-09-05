package com.phoenix.phnx.resources

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResourcePolicyTest {
    @Test
    fun foregroundProfileRemainsActiveUnderCriticalPressure() {
        val profile = profile("foreground", foreground = true, state = ProfileLifecycleState.ACTIVE)
        val decision = ResourceManager().evaluate(
            listOf(profile),
            ResourceSnapshot(memoryPressure = MemoryPressure.CRITICAL),
        ).single()

        assertEquals(ProfileLifecycleState.ACTIVE, decision.to)
    }

    @Test
    fun criticalPressureSuspendsBackgroundProfiles() {
        val profile = profile("background", foreground = false, state = ProfileLifecycleState.IDLE)
        val decision = ResourceManager().evaluate(
            listOf(profile),
            ResourceSnapshot(thermalLevel = ThermalLevel.THERMAL_CRITICAL),
        ).single()

        assertEquals(ProfileLifecycleState.SUSPENDED, decision.to)
    }

    @Test
    fun schedulerSelectsLowestPriorityProfileFirst() {
        val foreground = profile("foreground", foreground = true, state = ProfileLifecycleState.ACTIVE)
        val pinned = profile("pinned", pinned = true, state = ProfileLifecycleState.IDLE)
        val background = profile("background", state = ProfileLifecycleState.IDLE)

        val selected = ProfileScheduler().selectProfilesToSuspend(listOf(foreground, pinned, background), 1)

        assertEquals("background", selected.single().profileId)
        assertTrue(selected.none { it.foreground })
    }

    private fun profile(
        id: String,
        foreground: Boolean = false,
        pinned: Boolean = false,
        state: ProfileLifecycleState,
    ) = ProfileResourceState(
        profileId = id,
        lifecycleState = state,
        lastActiveTime = 1L,
        activeTabCount = 0,
        foreground = foreground,
        userPinned = pinned,
    )
}
