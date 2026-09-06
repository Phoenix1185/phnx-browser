package com.phoenix.phnx.system

import android.content.Intent
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlIntentParserTest {
    @Test
    fun acceptsHttpAndHttpsUrls() {
        assertEquals("https://example.com/path", UrlIntentParser.parseUrl("https://example.com/path"))
        assertEquals("http://example.com", UrlIntentParser.parseUrl(Uri.parse("http://example.com").toString()))
    }

    @Test
    fun rejectsUnsupportedOrCredentialBearingUrls() {
        assertNull(UrlIntentParser.parseUrl("javascript:alert(1)"))
        assertNull(UrlIntentParser.parseUrl("file:///tmp/page.html"))
        assertNull(UrlIntentParser.parseUrl("https://user:password@example.com"))
        assertNull(UrlIntentParser.parseUrl("https://example.com\nunsafe"))
    }

    @Test
    fun ignoresNonViewIntents() {
        assertNull(UrlIntentParser.parse(Intent(Intent.ACTION_SEND, Uri.parse("https://example.com"))))
    }
}
