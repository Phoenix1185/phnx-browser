package com.phoenix.phnx.search

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchEngineManagerTest {
    @Test
    fun enginesUseQueryUrlTemplates() {
        val engines = listOf(
            SearchEngine("google", "Google", "https://www.google.com/search?q="),
            SearchEngine("bing", "Bing", "https://www.bing.com/search?q="),
        )

        assertEquals("https://www.google.com/search?q=", engines.first().searchUrl)
        assertEquals("bing", engines.last().id)
    }
}
