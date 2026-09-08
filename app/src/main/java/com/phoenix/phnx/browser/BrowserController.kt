package com.phoenix.phnx.browser

import com.phoenix.phnx.tabs.Tab
import com.phoenix.phnx.resources.PerformanceMode
import com.phoenix.phnx.resources.ProfileLifecycleState
import com.phoenix.phnx.resources.ProfileResourceDecision

class BrowserController(private val pool: ProfileViewPool) {
    fun getOrCreate(tab: Tab): BrowserView = pool.acquire(tab)

    fun apply(decision: ProfileResourceDecision) = pool.apply(decision)

    fun transition(profileId: String, state: ProfileLifecycleState) =
        pool.transition(profileId, state)

    fun seed(profileId: String, state: ProfileLifecycleState) =
        pool.seed(profileId, state)

    fun restoreStateIfNeeded(tabId: String, view: BrowserView): Boolean =
        pool.restoreStateIfNeeded(tabId, view)

    fun trimInactiveTabs(
        tabs: List<Tab>,
        activeTabId: String?,
        mode: PerformanceMode,
        aggressive: Boolean = false,
    ): List<String> = pool.trimInactiveTabs(tabs, activeTabId, mode, aggressive)

    fun state(profileId: String): ProfileLifecycleState? = pool.state(profileId)

    fun suspendProfile(profileId: String) = pool.transition(profileId, ProfileLifecycleState.SUSPENDED)

    fun setSessionSaver(saver: (String) -> Unit) = pool.setSessionSaver(saver)

    fun forEachView(action: (BrowserView) -> Unit) = pool.forEachView(action)

    fun close(tab: Tab) = pool.close(tab)

    fun clear() = pool.clear()

    fun clearProfile(profileId: String) = pool.clearProfile(profileId)
}
