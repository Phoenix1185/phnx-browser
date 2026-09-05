package com.phoenix.phnx.browser

import android.content.Context
import com.phoenix.phnx.resources.LifecycleTransition
import com.phoenix.phnx.resources.ProfileLifecycleState
import com.phoenix.phnx.resources.ProfileLifecycleTracker
import com.phoenix.phnx.resources.ProfileResourceDecision

class ProfileViewPool(context: Context) {
    private val appContext = context.applicationContext
    private val tracker = ProfileLifecycleTracker()
    private val entries = mutableMapOf<String, Entry>()
    private var sessionSaver: ((String) -> Unit)? = null

    fun setSessionSaver(saver: (String) -> Unit) {
        sessionSaver = saver
    }

    fun acquire(profileId: String): BrowserView {
        val entry = entries.getOrPut(profileId) { Entry() }
        val view = entry.view ?: recreate(profileId, entry)
        view.onResume()
        tracker.transition(profileId, ProfileLifecycleState.ACTIVE)
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
                acquire(profileId)
                tracker.transition(profileId, target)
            }
            ProfileLifecycleState.IDLE -> {
                if (current == ProfileLifecycleState.SUSPENDED) {
                    // A cold profile is recreated only when its runtime entry is known in this process.
                    entries[profileId]?.let { recreate(profileId, it) }
                }
                entries[profileId]?.view?.let {
                    saveBeforeRelease(profileId)
                    it.onPause()
                    detach(it)
                }
                tracker.transition(profileId, target)
            }
            ProfileLifecycleState.FROZEN -> {
                entries[profileId]?.view?.let {
                    saveBeforeRelease(profileId)
                    it.onPause()
                    detach(it)
                }
                tracker.transition(profileId, target)
            }
            ProfileLifecycleState.SUSPENDED -> {
                saveBeforeRelease(profileId)
                entries[profileId]?.view?.let(::destroy)
                entries[profileId]?.view = null
                tracker.transition(profileId, target)
            }
            ProfileLifecycleState.RECREATING -> {
                val entry = entries.getOrPut(profileId) { Entry() }
                recreate(profileId, entry)
                tracker.transition(profileId, ProfileLifecycleState.ACTIVE)
            }
            ProfileLifecycleState.CLOSED -> {
                saveBeforeRelease(profileId)
                entries.remove(profileId)?.view?.let(::destroy)
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

    fun clear() {
        entries.values.mapNotNull { it.view }.forEach(::destroy)
        entries.clear()
        tracker.clear()
    }

    private fun recreate(profileId: String, entry: Entry): BrowserView {
        entry.view?.let(::destroy)
        tracker.transition(profileId, ProfileLifecycleState.RECREATING)
        return BrowserView(appContext).also { entry.view = it }
    }

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

    private class Entry(var view: BrowserView? = null)
}
