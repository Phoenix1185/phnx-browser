package com.phoenix.phnx.tabs

import java.util.ArrayDeque
import java.util.UUID

class TabManager {
    private val tabs = mutableListOf<Tab>()
    private val recentlyClosed = mutableMapOf<String, ArrayDeque<Tab>>()
    private var activeTabId: String? = null

    fun createTab(
        profileId: String = DEFAULT_PROFILE_ID,
        isPrivate: Boolean = false,
        activate: Boolean = true,
    ): Tab {
        val tab = Tab(profileId = profileId, isPrivate = isPrivate)
        tabs += tab
        if (activate) activeTabId = tab.id
        return tab
    }

    fun closeTab(tabId: String): Tab? {
        val index = tabs.indexOfFirst { it.id == tabId }
        if (index < 0) return null

        val removed = tabs.removeAt(index)
        if (!removed.isPrivate && removed.url.isNotBlank()) {
            val closed = recentlyClosed.getOrPut(removed.profileId) { ArrayDeque() }
            closed.addFirst(
                removed.copy(
                    id = UUID.randomUUID().toString(),
                    isLoading = false,
                    lastActivatedAt = System.currentTimeMillis(),
                    hasActiveMedia = false,
                    hasPendingWebTask = false,
                ),
            )
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
        tab.lastActivatedAt = System.currentTimeMillis()
        return true
    }

    fun currentTab(): Tab? = tabs.firstOrNull { it.id == activeTabId }

    fun restoreTabs(restoredTabs: List<Tab>, restoredActiveTabId: String?) {
        tabs.clear()
        tabs += restoredTabs
        activeTabId = restoredActiveTabId?.takeIf { id -> tabs.any { it.id == id } }
            ?: tabs.lastOrNull()?.id
        activeTabId?.let { id -> tabs.firstOrNull { it.id == id }?.lastActivatedAt = System.currentTimeMillis() }
    }

    fun activeTabId(): String? = activeTabId

    fun getTabs(): List<Tab> = tabs.toList()

    fun getTabs(profileId: String): List<Tab> = tabs.filter { it.profileId == profileId }

    fun getGroups(profileId: String): List<TabGroup> =
        getTabs(profileId)
            .filter { it.groupId != null && !it.isPrivate }
            .groupBy { it.groupId!! }
            .values
            .map { groupedTabs ->
                val first = groupedTabs.first()
                TabGroup(
                    id = first.groupId!!,
                    profileId = profileId,
                    title = first.groupTitle ?: "Tab group",
                    tabs = groupedTabs,
                    createdAt = first.groupCreatedAt ?: 0L,
                )
            }
            .sortedWith(compareBy<TabGroup> { it.createdAt }.thenBy { it.title })

    fun createGroup(profileId: String, title: String, tabIds: List<String>): TabGroup? {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return null
        val groupedTabs = tabIds.mapNotNull { id -> tabs.firstOrNull { it.id == id } }
            .filter { it.profileId == profileId && !it.isPrivate }
        if (groupedTabs.isEmpty()) return null
        val id = UUID.randomUUID().toString()
        val createdAt = System.currentTimeMillis()
        groupedTabs.forEach { tab ->
            tab.groupId = id
            tab.groupTitle = cleanTitle
            tab.groupCreatedAt = createdAt
        }
        return TabGroup(id, profileId, cleanTitle, groupedTabs, createdAt)
    }

    fun addToGroup(profileId: String, tabId: String, groupId: String): Boolean {
        val tab = tabs.firstOrNull { it.id == tabId } ?: return false
        if (tab.profileId != profileId || tab.isPrivate) return false
        val group = getGroups(profileId).firstOrNull { it.id == groupId } ?: return false
        tab.groupId = group.id
        tab.groupTitle = group.title
        tab.groupCreatedAt = group.createdAt
        return true
    }

    fun removeFromGroup(profileId: String, tabId: String): Boolean {
        val tab = tabs.firstOrNull { it.id == tabId } ?: return false
        if (tab.profileId != profileId || tab.groupId == null) return false
        tab.groupId = null
        tab.groupTitle = null
        tab.groupCreatedAt = null
        return true
    }

    fun recentlyClosed(profileId: String): List<Tab> = recentlyClosed[profileId].orEmpty().toList()

    fun restoreRecentlyClosed(profileId: String, tabId: String): Tab? {
        val closed = recentlyClosed[profileId] ?: return null
        val saved = closed.firstOrNull { it.id == tabId } ?: return null
        closed.remove(saved)
        if (closed.isEmpty()) recentlyClosed.remove(profileId)
        val restored = saved.copy(
            id = UUID.randomUUID().toString(),
            isLoading = false,
            hasActiveMedia = false,
            hasPendingWebTask = false,
        )
        restored.lastActivatedAt = System.currentTimeMillis()
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
