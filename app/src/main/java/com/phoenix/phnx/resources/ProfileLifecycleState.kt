package com.phoenix.phnx.resources

enum class ProfileLifecycleState {
    ACTIVE,
    IDLE,
    FROZEN,
    SUSPENDED,
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
