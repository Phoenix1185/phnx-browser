package com.phoenix.phnx.resources

enum class ProfileLifecycleState {
    ACTIVE,
    IDLE,
    FROZEN,
    SUSPENDED,
    RECREATING,
    CLOSED,
}

enum class ResourcePriority {
    FOREGROUND,
    HIGH,
    NORMAL,
    LOW,
    BACKGROUND,
}

data class ProfileResourceState(
    val profileId: String,
    val lifecycleState: ProfileLifecycleState,
    val lastActiveTime: Long,
    val activeTabCount: Int,
    val foreground: Boolean,
    val userPinned: Boolean,
) {
    val priority: ResourcePriority
        get() = when {
            foreground -> ResourcePriority.FOREGROUND
            userPinned -> ResourcePriority.HIGH
            activeTabCount > 0 -> ResourcePriority.NORMAL
            else -> ResourcePriority.BACKGROUND
    }
}

data class LifecycleTransition(
    val profileId: String,
    val from: ProfileLifecycleState,
    val to: ProfileLifecycleState,
    val changed: Boolean,
)

class ProfileLifecycleTracker {
    private val states = mutableMapOf<String, ProfileLifecycleState>()

    fun state(profileId: String): ProfileLifecycleState =
        states[profileId] ?: ProfileLifecycleState.IDLE

    fun stateOrNull(profileId: String): ProfileLifecycleState? = states[profileId]

    fun seed(profileId: String, state: ProfileLifecycleState) {
        states.putIfAbsent(profileId, state)
    }

    fun transition(profileId: String, target: ProfileLifecycleState): LifecycleTransition {
        val current = state(profileId)
        if (current == target) return LifecycleTransition(profileId, current, target, changed = false)
        states[profileId] = target
        return LifecycleTransition(profileId, current, target, changed = true)
    }

    fun clear() = states.clear()
}
