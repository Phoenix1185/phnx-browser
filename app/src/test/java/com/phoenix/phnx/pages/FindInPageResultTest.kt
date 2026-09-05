package com.phoenix.phnx.pages

import org.junit.Assert.assertEquals
import org.junit.Test

class FindInPageResultTest {
    @Test
    fun reportsCurrentMatchAndTotal() {
        assertEquals("Match 2 of 4", FindInPageResult(1, 4, true).summary())
    }

    @Test
    fun reportsCountingAndNoMatches() {
        assertEquals("Counting...", FindInPageResult(0, 0, false).summary())
        assertEquals("No matches", FindInPageResult(0, 0, true).summary())
    }

    @Test
    fun clampsUnexpectedActiveMatchOrdinal() {
        assertEquals("Match 1 of 3", FindInPageResult(-1, 3, true).summary())
        assertEquals("Match 3 of 3", FindInPageResult(8, 3, true).summary())
    }
}
