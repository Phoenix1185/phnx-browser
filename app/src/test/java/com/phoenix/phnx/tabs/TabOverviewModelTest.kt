package com.phoenix.phnx.tabs

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TabOverviewModelTest {
    @Test
    fun searchMatchesTitleDomainAndUrl() {
        val item = TabOverviewItem(
            id = "tab-1",
            profileId = "profile-work",
            title = "Project tracker",
            url = "https://example.com/projects?id=42",
            isPrivate = false,
            isActive = false,
            groupId = null,
            groupTitle = null,
        )

        assertTrue(item.matches("project"))
        assertTrue(item.matches("example.com"))
        assertTrue(item.matches("id=42"))
        assertFalse(item.matches("calendar"))
    }

    @Test
    fun blankSearchIncludesEveryItem() {
        val item = TabOverviewItem("tab-1", "profile", "New tab", "", false, false, null, null)

        assertTrue(item.matches("  "))
    }
}
