package com.phoenix.phnx.downloads

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadSecurityManagerTest {
    private val manager = DownloadSecurityManager()

    @Test
    fun rejectsNonWebDownloads() {
        assertFalse(manager.assess("file:///tmp/app.apk", "", "application/vnd.android.package-archive").allowed)
    }

    @Test
    fun requiresConfirmationForExecutableContent() {
        assertTrue(manager.assess("https://example.com/app.apk", "", "application/vnd.android.package-archive").requiresConfirmation)
    }

    @Test
    fun allowsOrdinaryWebDownloadsWithoutConfirmation() {
        val assessment = manager.assess("https://example.com/manual.pdf", "", "application/pdf")
        assertTrue(assessment.allowed)
        assertFalse(assessment.requiresConfirmation)
    }
}
