package com.phoenix.phnx.browser

import android.content.Context
import com.phoenix.phnx.resources.LifecycleTransition
import com.phoenix.phnx.resources.ProfileLifecycleState
import com.phoenix.phnx.resources.ProfileLifecycleTracker
import com.phoenix.phnx.resources.ProfileResourceDecision
import com.phoenix.phnx.tabs.Tab

class ProfileViewPool(context: Context) {
    private val appContext = context.applicationContext
    private val tracker = ProfileLifecycleTracker()
    private val entries = mutableMapOf<String, Entry>()
    private var sessionSaver: ((String) -> Unit)? = null

    fun setSessionSaver(saver: (String) -> Unit) {
        sessionSaver = saver
    }

    fun acquire(tab: Tab): BrowserView {
        val entry = entries.getOrPut(tab.id) { Entry(tab.profileId, tab.isPrivate) }
        val view = entry.view ?: recreate(entry)
        view.onResume()
        tracker.transition(tab.profileId, ProfileLifecycleState.ACTIVE)
        return view
    }

    fun apply(decision: ProfileResourceDecision) {
        transition(decision.profileId, decision.to)
    }

    fun transition(profileId: String, target: ProfileLifecycleState): LifecycleTransition {
        val current = tracker.state(profileId)
        if (current == target && !(target == ProfileLifecycleState.ACTIVE && entries[profileId]?.view == null)) {
            return tracker.transition(profileId, target)
        }

        return when (target) {
            ProfileLifecycleState.ACTIVE -> {
                tracker.transition(profileId, target)
            }
            ProfileLifecycleState.IDLE -> {
                entriesFor(profileId).forEach { entry ->
                    if (current == ProfileLifecycleState.SUSPENDED) recreate(entry.value)
                    val view = entry.value.view ?: return@forEach
                    saveBeforeRelease(profileId)
                    view.onPause()
                    detach(view)
                }
                tracker.transition(profileId, target)
            }
            ProfileLifecycleState.FROZEN -> {
                entriesFor(profileId).forEach { entry ->
                    val view = entry.value.view ?: return@forEach
                    saveBeforeRelease(profileId)
                    view.onPause()
                    detach(view)
                }
                tracker.transition(profileId, target)
            }
            ProfileLifecycleState.SUSPENDED -> {
                saveBeforeRelease(profileId)
                entriesFor(profileId).forEach { entry ->
                    entry.value.view?.let(::destroy)
                    entry.value.view = null
                }
                tracker.transition(profileId, target)
            }
            ProfileLifecycleState.RECREATING -> {
                entriesFor(profileId).forEach { entry -> recreate(entry.value) }
                tracker.transition(profileId, ProfileLifecycleState.ACTIVE)
            }
            ProfileLifecycleState.CLOSED -> {
                saveBeforeRelease(profileId)
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

    fun close(tab: Tab) {
        entries.remove(tab.id)?.view?.let(::destroy)
    }

    fun clear() {
        entries.values.mapNotNull { it.view }.forEach(::destroy)
        entries.clear()
        tracker.clear()
    }

    private fun recreate(entry: Entry): BrowserView {
        entry.view?.let(::destroy)
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
        view.removeAllViews()
        view.destroy()
    }

    private class Entry(
        val profileId: String,
        val isPrivate: Boolean,
        var view: BrowserView? = null,
    )
}
