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
}
