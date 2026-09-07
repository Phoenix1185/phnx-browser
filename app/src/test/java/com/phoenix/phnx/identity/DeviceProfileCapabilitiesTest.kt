package com.phoenix.phnx.identity

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceProfileCapabilitiesTest {
    @Test
    fun compatibilityScriptDoesNotPretendToControlClientHints() {
        val profile = DevicePresets.get(DevicePresets.IPHONE_15)!!.forProfile("profile_a")
        val script = WebViewIdentityCompatibility.scriptFor(profile)

        assertTrue(script.contains("innerWidth"))
        assertTrue(script.contains("devicePixelRatio"))
        assertFalse(script.contains("userAgentData"))
        assertFalse(script.contains("Sec-CH-UA"))
    }

    @Test
    fun nativeAndLimitedStatusAreDifferentForSystemAndIosProfiles() {
        val system = DevicePresets.get(DevicePresets.ANDROID_PHONE)!!.forProfile("profile_a").copy(
            presetId = DevicePresets.SYSTEM_DEFAULT,
            name = "System Default",
        )
        val iphone = DevicePresets.get(DevicePresets.IPHONE_15)!!.forProfile("profile_a")

        assertTrue(DeviceProfileCapabilities.overallStatus(system) == IdentityApplyStatus.APPLIED)
        assertTrue(DeviceProfileCapabilities.overallStatus(iphone) == IdentityApplyStatus.WEBVIEW_LIMITED)
    }

    @Test
    fun everyBuiltInPresetPassesProfileValidation() {
        DevicePresets.all().forEach { preset ->
            val errors = DeviceProfileValidator.validate(preset.forProfile("profile_a"))
            assertTrue("${preset.id}: ${errors.joinToString()}", errors.isEmpty())
        }
    }
}
