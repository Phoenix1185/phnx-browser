package com.phoenix.phnx.resources

class ProfileScheduler {
    fun selectActiveProfiles(profiles: List<ProfileResourceState>, limit: Int): List<ProfileResourceState> =
        profiles.sortedWith(profileComparator).take(limit.coerceAtLeast(0))

    fun selectProfilesToFreeze(profiles: List<ProfileResourceState>, count: Int): List<ProfileResourceState> =
        profiles
            .filter {
                !it.foreground && it.lifecycleState in setOf(
                    ProfileLifecycleState.ACTIVE,
                    ProfileLifecycleState.IDLE,
                )
            }
            .sortedWith(reductionComparator)
            .take(count.coerceAtLeast(0))

    fun selectProfilesToSuspend(profiles: List<ProfileResourceState>, count: Int): List<ProfileResourceState> =
        profiles
            .filter {
                !it.foreground && it.lifecycleState in setOf(
                    ProfileLifecycleState.ACTIVE,
                    ProfileLifecycleState.IDLE,
                    ProfileLifecycleState.FROZEN,
                )
            }
            .sortedWith(reductionComparator)
            .take(count.coerceAtLeast(0))

    fun selectProfilesToClose(profiles: List<ProfileResourceState>, count: Int): List<ProfileResourceState> =
        profiles
            .filter { !it.foreground && it.lifecycleState != ProfileLifecycleState.CLOSED }
            .sortedWith(reductionComparator)
            .take(count.coerceAtLeast(0))

    private val profileComparator = compareByDescending<ProfileResourceState> { it.foreground }
        .thenByDescending { it.userPinned }
        .thenByDescending { it.activeTabCount }
        .thenByDescending { it.lastActiveTime }

    private val reductionComparator = compareBy<ProfileResourceState> { priorityScore(it.priority) }
        .thenBy { it.lastActiveTime }

    private fun priorityScore(priority: ResourcePriority): Int = when (priority) {
        ResourcePriority.BACKGROUND -> 0
        ResourcePriority.LOW -> 1
        ResourcePriority.NORMAL -> 2
        ResourcePriority.HIGH -> 3
        ResourcePriority.FOREGROUND -> 4
    }
}
