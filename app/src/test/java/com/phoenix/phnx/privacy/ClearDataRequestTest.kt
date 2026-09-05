package com.phoenix.phnx.privacy

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClearDataRequestTest {
    @Test
    fun requestCanSelectOnlySiteStorage() {
        val request = ClearDataRequest(cookies = false, siteStorage = true, cache = false)

        assertFalse(request.cookies)
        assertTrue(request.siteStorage)
        assertFalse(request.cache)
    }
}
