package com.phoenix.phnx.system

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlIntentParserTest {
    @Test
    fun acceptsHttpAndHttpsUrls() {
        assertEquals("https://example.com/path", UrlIntentParser.parseUrl("https://example.com/path"))
        assertEquals("http://example.com", UrlIntentParser.parseUrl("http://example.com"))
    }

    @Test
    fun normalizesBareWebsiteAddresses() {
        assertEquals(
            "https://www.senseiphoenix.name.ng",
            UrlIntentParser.parseUrl("www.senseiphoenix.name.ng"),
        )
        assertEquals(
            "https://senseiphoenix.name.ng/path",
            UrlIntentParser.parseUrl("senseiphoenix.name.ng/path"),
        )
        assertEquals(
            UrlIntentParser.Classification.NORMAL_WEB,
            UrlIntentParser.inspect("www.senseiphoenix.name.ng").classification,
        )
    }

    @Test
    fun rejectsUnsupportedOrCredentialBearingUrls() {
        assertNull(UrlIntentParser.parseUrl("javascript:alert(1)"))
        assertNull(UrlIntentParser.parseUrl("file:///tmp/page.html"))
        assertNull(UrlIntentParser.parseUrl("market://details?id=com.example"))
        assertEquals(
            UrlIntentParser.Classification.INVALID,
            UrlIntentParser.inspect("javascript:alert(1)").classification,
        )
        assertNull(UrlIntentParser.parseUrl("https://user:password@example.com"))
        assertNull(UrlIntentParser.parseUrl("https://example.com\nunsafe"))
    }

    @Test
    fun ignoresNonViewIntents() {
        assertNull(UrlIntentParser.parse(Intent(Intent.ACTION_SEND)))
    }
}
