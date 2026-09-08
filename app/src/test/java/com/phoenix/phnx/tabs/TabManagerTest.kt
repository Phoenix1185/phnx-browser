package com.phoenix.phnx.tabs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TabManagerTest {
    @Test
    fun createsAndSwitchesTabs() {
        val manager = TabManager()
        val first = manager.createTab()
        val second = manager.createTab(isPrivate = true)

        assertEquals(second.id, manager.currentTab()?.id)
        assertTrue(manager.switchTab(first.id))
        assertEquals(first.id, manager.currentTab()?.id)
        assertTrue(manager.getTabs().single { it.id == second.id }.isPrivate)
        assertNotEquals(first.id, second.id)
    }

    @Test
    fun backgroundTabDoesNotChangeActiveTab() {
        val manager = TabManager()
        val current = manager.createTab()
        val background = manager.createTab(activate = false)

        assertEquals(current.id, manager.currentTab()?.id)
        assertTrue(manager.getTabs().contains(background))
    }

    @Test
    fun closingActiveTabSelectsAnotherTab() {
        val manager = TabManager()
        val first = manager.createTab()
        val second = manager.createTab()

        assertEquals(second, manager.closeTab(second.id))
        assertEquals(first.id, manager.currentTab()?.id)
        assertNull(manager.closeTab("missing"))
        assertFalse(manager.switchTab("missing"))
    }

    @Test
    fun tabsKeepTheirProfileOwnership() {
        val manager = TabManager()
        val phoenix = manager.createTab(profileId = "profile_phoenix")
        val work = manager.createTab(profileId = "profile_work")

        assertEquals(1, manager.getTabs("profile_phoenix").size)
        assertEquals(phoenix.id, manager.getTabs("profile_phoenix").single().id)
        assertTrue(manager.switchTab(work.id, profileId = "profile_work"))
        assertFalse(manager.switchTab(phoenix.id, profileId = "profile_work"))
    }

    @Test
    fun restoresSavedTabsAndActiveTab() {
        val manager = TabManager()
        val first = manager.createTab(profileId = "profile_phoenix")
        val second = manager.createTab(profileId = "profile_phoenix")
        manager.restoreTabs(listOf(first, second), first.id)

        assertEquals(first.id, manager.currentTab()?.id)
        assertEquals(2, manager.tabCount("profile_phoenix"))
    }

    @Test
    fun privateTabsAreNotPersisted() {
        val manager = TabManager()
        val regular = manager.createTab(profileId = "profile_phoenix")
        manager.createTab(profileId = "profile_phoenix", isPrivate = true)

        assertEquals(listOf(regular), manager.persistedTabs("profile_phoenix"))
    }

    @Test
    fun regularClosedTabsCanBeRestoredPerProfile() {
        val manager = TabManager()
        val closed = manager.createTab(profileId = "profile_phoenix").apply {
            title = "Example"
            url = "https://example.com"
        }
        manager.closeTab(closed.id)

        val recent = manager.recentlyClosed("profile_phoenix")
        assertEquals(1, recent.size)
        val restored = manager.restoreRecentlyClosed("profile_phoenix", recent.single().id)

        assertEquals("https://example.com", restored?.url)
        assertEquals(1, manager.tabCount("profile_phoenix"))
        assertTrue(manager.recentlyClosed("profile_phoenix").isEmpty())
    }

    @Test
    fun privateTabsNeverEnterRecentlyClosed() {
        val manager = TabManager()
        val privateTab = manager.createTab(profileId = "profile_phoenix", isPrivate = true).apply {
            url = "https://example.com"
        }

        manager.closeTab(privateTab.id)

        assertTrue(manager.recentlyClosed("profile_phoenix").isEmpty())
    }

    @Test
    fun tabGroupsStayWithinTheirProfile() {
        val manager = TabManager()
        val first = manager.createTab(profileId = "profile_phoenix")
        val second = manager.createTab(profileId = "profile_phoenix")
        val otherProfile = manager.createTab(profileId = "profile_work")

        val group = manager.createGroup("profile_phoenix", "Work", listOf(first.id, otherProfile.id))
            ?: error("Expected a group")

        assertEquals(1, group.tabs.size)
        assertFalse(manager.addToGroup("profile_work", otherProfile.id, group.id))
        assertTrue(manager.addToGroup("profile_phoenix", second.id, group.id))
        assertEquals(2, manager.getGroups("profile_phoenix").single().tabs.size)
        assertTrue(manager.removeFromGroup("profile_phoenix", first.id))
        assertEquals(1, manager.getGroups("profile_phoenix").single().tabs.size)
    }
}
