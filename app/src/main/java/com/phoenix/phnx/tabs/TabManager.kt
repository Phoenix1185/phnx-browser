package com.phoenix.phnx.tabs

import java.util.ArrayDeque
import java.util.UUID

class TabManager {
    private val tabs = mutableListOf<Tab>()
    private val recentlyClosed = mutableMapOf<String, ArrayDeque<Tab>>()
    private var activeTabId: String? = null

    fun createTab(profileId: String = DEFAULT_PROFILE_ID, isPrivate: Boolean = false): Tab {
        val tab = Tab(profileId = profileId, isPrivate = isPrivate)
        tabs += tab
        activeTabId = tab.id
        return tab
    }

    fun closeTab(tabId: String): Tab? {
        val index = tabs.indexOfFirst { it.id == tabId }
        if (index < 0) return null

        val removed = tabs.removeAt(index)
        if (!removed.isPrivate && removed.url.isNotBlank()) {
            val closed = recentlyClosed.getOrPut(removed.profileId) { ArrayDeque() }
            closed.addFirst(removed.copy(id = UUID.randomUUID().toString(), isLoading = false))
            while (closed.size > MAX_RECENTLY_CLOSED) closed.removeLast()
        }
        if (removed.id == activeTabId) {
            activeTabId = tabs.getOrNull(index.coerceAtMost(tabs.lastIndex))?.id
        }
        return removed
    }

    fun switchTab(tabId: String, profileId: String? = null): Boolean {
        val tab = tabs.firstOrNull { it.id == tabId } ?: return false
        if (profileId != null && tab.profileId != profileId) return false
        activeTabId = tabId
        return true
    }

    fun currentTab(): Tab? = tabs.firstOrNull { it.id == activeTabId }

    fun restoreTabs(restoredTabs: List<Tab>, restoredActiveTabId: String?) {
        tabs.clear()
        tabs += restoredTabs
        activeTabId = restoredActiveTabId?.takeIf { id -> tabs.any { it.id == id } }
            ?: tabs.lastOrNull()?.id
    }

    fun activeTabId(): String? = activeTabId

    fun getTabs(): List<Tab> = tabs.toList()

    fun getTabs(profileId: String): List<Tab> = tabs.filter { it.profileId == profileId }

    fun recentlyClosed(profileId: String): List<Tab> = recentlyClosed[profileId].orEmpty().toList()

    fun restoreRecentlyClosed(profileId: String, tabId: String): Tab? {
        val closed = recentlyClosed[profileId] ?: return null
        val saved = closed.firstOrNull { it.id == tabId } ?: return null
        closed.remove(saved)
        if (closed.isEmpty()) recentlyClosed.remove(profileId)
        val restored = saved.copy(id = UUID.randomUUID().toString(), isLoading = false)
        tabs += restored
        activeTabId = restored.id
        return restored
    }

    fun persistedTabs(profileId: String): List<Tab> = getTabs(profileId).filterNot { it.isPrivate }

    fun tabCount(): Int = tabs.size

    fun tabCount(profileId: String): Int = tabs.count { it.profileId == profileId }

    private companion object {
        const val MAX_RECENTLY_CLOSED = 10
    }
}
