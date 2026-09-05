package com.phoenix.phnx.bookmarks

import org.junit.Assert.assertEquals
import org.junit.Test

class BookmarkManagerTest {
    @Test
    fun entityKeepsProfileOwnership() {
        val bookmark = BookmarkEntity(
            id = "bookmark_1",
            profileId = "profile_work",
            title = "Work",
            url = "https://example.com",
            folderId = null,
            createdAt = 10L,
        ).toDomain()

        assertEquals("profile_work", bookmark.profileId)
        assertEquals("https://example.com", bookmark.url)
    }
}
