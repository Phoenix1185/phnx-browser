package com.phoenix.phnx.tabs

class TabManager {
    private val tabs = mutableListOf<Tab>()
    private var activeTabId: String? = null

    fun createTab(isPrivate: Boolean = false): Tab {
        val tab = Tab(isPrivate = isPrivate)
        tabs += tab
        activeTabId = tab.id
        return tab
    }

    fun closeTab(tabId: String): Tab? {
        val index = tabs.indexOfFirst { it.id == tabId }
        if (index < 0) return null

        val removed = tabs.removeAt(index)
        if (removed.id == activeTabId) {
            activeTabId = tabs.getOrNull(index.coerceAtMost(tabs.lastIndex))?.id
        }
        return removed
    }

    fun switchTab(tabId: String): Boolean {
        if (tabs.none { it.id == tabId }) return false
        activeTabId = tabId
        return true
    }

    fun currentTab(): Tab? = tabs.firstOrNull { it.id == activeTabId }

    fun getTabs(): List<Tab> = tabs.toList()

    fun tabCount(): Int = tabs.size
}
