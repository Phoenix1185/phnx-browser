package com.phoenix.phnx.browser

import com.phoenix.phnx.tabs.Tab
import com.phoenix.phnx.resources.ProfileLifecycleState
import com.phoenix.phnx.resources.ProfileResourceDecision

class BrowserController(private val pool: ProfileViewPool) {
    fun getOrCreate(tab: Tab): BrowserView = pool.acquire(tab)

    fun apply(decision: ProfileResourceDecision) = pool.apply(decision)

    fun transition(profileId: String, state: ProfileLifecycleState) =
        pool.transition(profileId, state)

    fun seed(profileId: String, state: ProfileLifecycleState) =
        pool.seed(profileId, state)

    fun state(profileId: String): ProfileLifecycleState? = pool.state(profileId)

    fun suspendProfile(profileId: String) = pool.transition(profileId, ProfileLifecycleState.SUSPENDED)

    fun setSessionSaver(saver: (String) -> Unit) = pool.setSessionSaver(saver)

    fun forEachView(action: (BrowserView) -> Unit) = pool.forEachView(action)

    fun close(tab: Tab) = pool.close(tab)

    fun clear() = pool.clear()
}
