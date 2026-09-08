package com.phoenix.phnx.tabs

import com.phoenix.phnx.resources.PerformanceMode

class TabResourcePolicy {
    fun selectForSuspension(
        tabs: List<Tab>,
        activeTabId: String?,
        mode: PerformanceMode,
        nowMillis: Long,
        aggressive: Boolean = false,
    ): List<String> {
        val candidates = tabs.filter { tab ->
            tab.id != activeTabId &&
                !tab.isLoading &&
                !tab.hasActiveMedia &&
                !tab.hasPendingWebTask
        }
        if (candidates.isEmpty()) return emptyList()

        val timeout = if (aggressive) 0L else mode.inactiveTimeoutMillis
        val old = candidates.filter { nowMillis - it.lastActivatedAt >= timeout }
        val overflowCount = (tabs.count { it.id != activeTabId } - mode.maxLiveTabs).coerceAtLeast(0)
        val overflow = candidates.sortedBy(Tab::lastActivatedAt).take(overflowCount)

        return (old + overflow)
            .distinctBy(Tab::id)
            .sortedBy(Tab::lastActivatedAt)
            .map(Tab::id)
    }

    private val PerformanceMode.inactiveTimeoutMillis: Long
        get() = when (this) {
            PerformanceMode.BALANCED -> 15 * MINUTE_MILLIS
            PerformanceMode.BATTERY_SAVER -> 5 * MINUTE_MILLIS
            PerformanceMode.MAXIMUM_PERFORMANCE -> 30 * MINUTE_MILLIS
        }

    private val PerformanceMode.maxLiveTabs: Int
        get() = when (this) {
            PerformanceMode.BALANCED -> 8
            PerformanceMode.BATTERY_SAVER -> 4
            PerformanceMode.MAXIMUM_PERFORMANCE -> 16
        }

    private companion object {
        const val MINUTE_MILLIS = 60_000L
    }
}
