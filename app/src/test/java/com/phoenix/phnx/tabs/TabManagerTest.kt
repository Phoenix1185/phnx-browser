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
}
