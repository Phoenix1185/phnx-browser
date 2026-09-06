package com.phoenix.phnx.update

import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCoreTest {
    @Test
    fun comparesNumericVersionsWithoutAllowingDowngrades() {
        assertTrue(VersionComparator.isNewer("1.10.0", "1.9.9"))
        assertFalse(VersionComparator.isNewer("1.0.0", "1.0.0"))
        assertFalse(VersionComparator.isNewer("v1.0.0-beta", "1.0.0"))
        assertEquals(0, VersionComparator.compare("1.0", "1.0.0"))
    }

    @Test
    fun verifiesSha256AndRejectsInvalidChecksums() {
        val payload = "PHNX update artifact"
        val checksum = "ea2859cdca33d2ed412d98b7e0856503560f45f0311549cc8fa4aa5505795066"

        assertTrue(ChecksumVerifier.verify(ByteArrayInputStream(payload.toByteArray()), checksum))
        assertFalse(ChecksumVerifier.verify(ByteArrayInputStream(payload.toByteArray()), "not-a-checksum"))
    }

}
