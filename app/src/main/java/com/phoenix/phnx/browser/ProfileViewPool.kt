package com.phoenix.phnx.browser

import android.content.Context
import android.os.Bundle
import com.phoenix.phnx.resources.PerformanceMode
import com.phoenix.phnx.resources.LifecycleTransition
import com.phoenix.phnx.resources.ProfileLifecycleState
import com.phoenix.phnx.resources.ProfileLifecycleTracker
import com.phoenix.phnx.resources.ProfileResourceDecision
import com.phoenix.phnx.tabs.Tab
import com.phoenix.phnx.tabs.TabResourcePolicy

class ProfileViewPool(context: Context) {
    private val appContext = context.applicationContext
    private val tracker = ProfileLifecycleTracker()
    private val tabResourcePolicy = TabResourcePolicy()
    private val entries = mutableMapOf<String, Entry>()
    private var sessionSaver: ((String) -> Unit)? = null

    fun setSessionSaver(saver: (String) -> Unit) {
        sessionSaver = saver
    }

    fun acquire(tab: Tab): BrowserView {
        val entry = entries.getOrPut(tab.id) { Entry(tab.profileId, tab.isPrivate) }
        val view = entry.view ?: recreate(entry)
        tab.lastActivatedAt = System.currentTimeMillis()
        view.onResume()
        tracker.transition(tab.profileId, ProfileLifecycleState.ACTIVE)
        return view
    }

    fun apply(decision: ProfileResourceDecision) {
        transition(decision.profileId, decision.to)
    }

    fun transition(profileId: String, target: ProfileLifecycleState): LifecycleTransition {
        val current = tracker.state(profileId)
        if (current == target && !(target == ProfileLifecycleState.ACTIVE && entriesFor(profileId).none { it.value.view != null })) {
            return tracker.transition(profileId, target)
        }

        return when (target) {
            ProfileLifecycleState.ACTIVE -> {
                tracker.transition(profileId, target)
            }
            ProfileLifecycleState.IDLE -> {
                if (current == ProfileLifecycleState.SUSPENDED) {
                    return tracker.transition(profileId, target)
                }
                if (entriesFor(profileId).any { it.value.view != null }) saveBeforeRelease(profileId)
                entriesFor(profileId).forEach { entry ->
                    val view = entry.value.view ?: return@forEach
                    view.onPause()
                    detach(view)
                }
                tracker.transition(profileId, target)
            }
            ProfileLifecycleState.FROZEN -> {
                if (entriesFor(profileId).any { it.value.view != null }) saveBeforeRelease(profileId)
                entriesFor(profileId).forEach { entry ->
                    val view = entry.value.view ?: return@forEach
                    view.onPause()
                    detach(view)
                }
                tracker.transition(profileId, target)
            }
            ProfileLifecycleState.SUSPENDED -> {
                if (entriesFor(profileId).any { it.value.view != null }) saveBeforeRelease(profileId)
                entriesFor(profileId).forEach { entry ->
                    entry.value.view?.let {
                        saveState(entry.value)
                        destroy(it)
                    }
                    entry.value.view = null
                }
                tracker.transition(profileId, target)
            }
            ProfileLifecycleState.RECREATING -> {
                entriesFor(profileId).forEach { entry -> recreate(entry.value) }
                tracker.transition(profileId, ProfileLifecycleState.ACTIVE)
            }
            ProfileLifecycleState.CLOSED -> {
                if (entriesFor(profileId).any { it.value.view != null }) saveBeforeRelease(profileId)
                entriesFor(profileId).forEach { (_, entry) -> entry.view?.let(::destroy) }
                entries.entries.removeIf { it.value.profileId == profileId }
                tracker.transition(profileId, target)
            }
        }
    }

    fun seed(profileId: String, state: ProfileLifecycleState) {
        tracker.seed(profileId, state)
    }

    fun state(profileId: String): ProfileLifecycleState? = tracker.stateOrNull(profileId)

    fun forEachView(action: (BrowserView) -> Unit) {
        entries.values.mapNotNull { it.view }.forEach(action)
    }

    fun restoreStateIfNeeded(tabId: String, view: BrowserView): Boolean {
        val entry = entries[tabId] ?: return false
        val savedState = entry.savedState ?: return false
        entry.savedState = null
        view.restoreState(savedState)
        return true
    }

    fun trimInactiveTabs(
        tabs: List<Tab>,
        activeTabId: String?,
        mode: PerformanceMode,
        aggressive: Boolean = false,
    ): List<String> {
        val selected = tabResourcePolicy.selectForSuspension(tabs, activeTabId, mode, System.currentTimeMillis(), aggressive)
            .filter { entries[it]?.view != null }
        selected.mapNotNull { tabs.firstOrNull { tab -> tab.id == it }?.profileId }
            .distinct()
            .forEach(::saveBeforeRelease)
        selected.forEach { tabId ->
            val entry = entries[tabId] ?: return@forEach
            entry.view?.let {
                saveState(entry)
                it.onPause()
                destroy(it)
                entry.view = null
            }
        }
        return selected
    }

    fun close(tab: Tab) {
        entries.remove(tab.id)?.view?.let(::destroy)
    }

    fun clearProfile(profileId: String) {
        entries.entries
            .filter { it.value.profileId == profileId }
            .forEach { (_, entry) -> entry.view?.let(::destroy) }
        entries.entries.removeIf { it.value.profileId == profileId }
        tracker.transition(profileId, ProfileLifecycleState.CLOSED)
    }

    fun clear() {
        entries.values.mapNotNull { it.view }.forEach(::destroy)
        entries.clear()
        tracker.clear()
        sessionSaver = null
    }

    private fun recreate(entry: Entry): BrowserView {
        entry.view?.let {
            saveState(entry)
            destroy(it)
        }
        tracker.transition(entry.profileId, ProfileLifecycleState.RECREATING)
        return BrowserView(appContext, entry.isPrivate).also { entry.view = it }
    }

    private fun entriesFor(profileId: String): List<Map.Entry<String, Entry>> =
        entries.entries.filter { it.value.profileId == profileId }

    private fun saveBeforeRelease(profileId: String) {
        sessionSaver?.invoke(profileId)
    }

    private fun detach(view: BrowserView) {
        (view.parent as? android.view.ViewGroup)?.removeView(view)
    }

    private fun destroy(view: BrowserView) {
        detach(view)
        view.stopLoading()
        view.webViewClient = null
        view.webChromeClient = null
        view.setDownloadListener(null)
        view.setFindListener(null)
        view.setOnTouchListener(null)
        view.removeAllViews()
        view.destroy()
    }

    private fun saveState(entry: Entry) {
        val view = entry.view ?: return
        val state = Bundle()
        if (view.saveState(state) != null) entry.savedState = state
    }

    private class Entry(
        val profileId: String,
        val isPrivate: Boolean,
        var view: BrowserView? = null,
        var savedState: Bundle? = null,
    )
}
