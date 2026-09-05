package com.phoenix.phnx.history

import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryManagerTest {
    @Test
    fun entityRestoresHostAndVisitCount() {
        val entry = HistoryEntryEntity(
            id = "history_1",
            profileId = "profile_a",
            url = "https://example.com/article",
            title = "Article",
            visitedAt = 10L,
            visitCount = 3,
        ).toDomain()

        assertEquals("example.com", entry.host)
        assertEquals(3, entry.visitCount)
    }

    @Test
    fun hostOmitsPathAndQuery() {
        val entry = HistoryEntryEntity(
            id = "history_2",
            profileId = "profile_work",
            url = "https://example.com/path?q=1",
            title = "Example",
            visitedAt = 20L,
            visitCount = 2,
        ).toDomain()

        assertEquals("example.com", entry.host)
    }
}
