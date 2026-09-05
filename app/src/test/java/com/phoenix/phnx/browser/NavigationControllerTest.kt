package com.phoenix.phnx.browser

import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationControllerTest {
    @Test
    fun preservesHttpAndHttpsUrls() {
        assertEquals("https://example.com/path", NavigationController.resolveInput("https://example.com/path"))
        assertEquals("http://localhost:8080", NavigationController.resolveInput("http://localhost:8080"))
    }

    @Test
    fun addsHttpsToHostLikeInput() {
        assertEquals("https://example.com", NavigationController.resolveInput("example.com"))
    }

    @Test
    fun routesTextToConfiguredSearchEngine() {
        assertEquals(
            "https://search.test/?q=hello%20world",
            NavigationController.resolveInput("hello world", "https://search.test/?q="),
        )
    }
}
