package com.phoenix.phnx.security

import org.junit.Assert.assertEquals
import org.junit.Test

class SecurityStateTest {
    @Test
    fun resolvesOnlyValidatedWebSchemes() {
        assertEquals(BrowserSecurityState.SECURE, SecurityStateResolver.fromUrl("https://example.com"))
        assertEquals(BrowserSecurityState.NOT_SECURE, SecurityStateResolver.fromUrl("http://example.com"))
        assertEquals(BrowserSecurityState.UNKNOWN, SecurityStateResolver.fromUrl("file:///tmp/page"))
    }
}
