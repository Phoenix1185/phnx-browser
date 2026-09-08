package com.phoenix.phnx.resources

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.phoenix.phnx.tabs.Tab
import com.phoenix.phnx.tabs.TabResourcePolicy

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
    fun managerCanEvaluateUsingSnapshotProvider() {
        val profile = profile("background", foreground = false, state = ProfileLifecycleState.IDLE)
        val decision = ResourceManager(
            snapshotProvider = { ResourceSnapshot(memoryPressure = MemoryPressure.CRITICAL) },
        ).evaluate(listOf(profile)).single()

        assertEquals(ProfileLifecycleState.SUSPENDED, decision.to)
    }

    @Test
    fun suspendedProfileRecoversToIdleWhenPressureClears() {
        val profile = profile("background", foreground = false, state = ProfileLifecycleState.SUSPENDED)
        val decision = ResourceManager().evaluate(
            listOf(profile),
            ResourceSnapshot(),
        ).single()

        assertEquals(ProfileLifecycleState.IDLE, decision.to)
    }

    @Test
    fun lifecycleTransitionsAreIdempotent() {
        val tracker = ProfileLifecycleTracker()

        val first = tracker.transition("profile", ProfileLifecycleState.FROZEN)
        val second = tracker.transition("profile", ProfileLifecycleState.FROZEN)

        assertTrue(first.changed)
        assertFalse(second.changed)
        assertEquals(ProfileLifecycleState.FROZEN, tracker.state("profile"))
    }

    @Test
    fun batterySaverFreezesBackgroundProfilesWithoutAffectingForeground() {
        val foreground = profile("foreground", foreground = true, state = ProfileLifecycleState.ACTIVE)
        val background = profile("background", state = ProfileLifecycleState.IDLE)
        val decisions = ResourceManager().evaluate(
            listOf(foreground, background),
            ResourceSnapshot(performanceMode = PerformanceMode.BATTERY_SAVER),
        )

        assertEquals(ProfileLifecycleState.ACTIVE, decisions[0].to)
        assertEquals(ProfileLifecycleState.FROZEN, decisions[1].to)
    }

    @Test
    fun reconcileAppliesDecisionsToLifecycleAdapter() {
        val applied = mutableListOf<ProfileResourceDecision>()
        val profile = profile("background", foreground = false, state = ProfileLifecycleState.IDLE)

        ResourceManager(lifecycleAdapter = { applied += it }).reconcile(
            listOf(profile),
            ResourceSnapshot(memoryPressure = MemoryPressure.CRITICAL),
        )

        assertEquals(ProfileLifecycleState.SUSPENDED, applied.single().to)
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

    @Test
    fun schedulerOnlyFreezesEligibleBackgroundProfiles() {
        val alreadyFrozen = profile("frozen", state = ProfileLifecycleState.FROZEN)
        val closed = profile("closed", state = ProfileLifecycleState.CLOSED)
        val idle = profile("idle", state = ProfileLifecycleState.IDLE)

        val selected = ProfileScheduler().selectProfilesToFreeze(listOf(alreadyFrozen, closed, idle), 3)

        assertEquals(listOf("idle"), selected.map { it.profileId })
    }

    @Test
    fun schedulerDoesNotReselectSuspendedOrClosedProfiles() {
        val suspended = profile("suspended", state = ProfileLifecycleState.SUSPENDED)
        val closed = profile("closed", state = ProfileLifecycleState.CLOSED)
        val idle = profile("idle", state = ProfileLifecycleState.IDLE)

        val selected = ProfileScheduler().selectProfilesToSuspend(listOf(suspended, closed, idle), 3)

        assertEquals(listOf("idle"), selected.map { it.profileId })
    }

    @Test
    fun tabPolicySkipsActiveMediaAndPendingWebTasks() {
        val now = 1_000_000L
        val active = Tab(id = "active", lastActivatedAt = now)
        val media = Tab(id = "media", lastActivatedAt = now - 60 * 60_000L, hasActiveMedia = true)
        val pending = Tab(id = "pending", lastActivatedAt = now - 60 * 60_000L, hasPendingWebTask = true)
        val old = Tab(id = "old", lastActivatedAt = now - 60 * 60_000L)

        val selected = TabResourcePolicy().selectForSuspension(
            tabs = listOf(active, media, pending, old),
            activeTabId = active.id,
            mode = PerformanceMode.BALANCED,
            nowMillis = now,
        )

        assertEquals(listOf("old"), selected)
    }

    @Test
    fun schedulerClosesLowestPriorityNonForegroundProfiles() {
        val foreground = profile("foreground", foreground = true, state = ProfileLifecycleState.ACTIVE)
        val idle = profile("idle", state = ProfileLifecycleState.IDLE)
        val pinned = profile("pinned", pinned = true, state = ProfileLifecycleState.IDLE)

        val selected = ProfileScheduler().selectProfilesToClose(listOf(foreground, pinned, idle), 1)

        assertEquals(listOf("idle"), selected.map { it.profileId })
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
