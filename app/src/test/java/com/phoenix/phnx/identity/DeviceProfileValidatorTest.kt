package com.phoenix.phnx.identity

import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceProfileValidatorTest {
    @Test
    fun supportedPresetsAreInternallyConsistent() {
        DevicePresets.all().forEach { preset ->
            assertTrue(
                "${preset.id}: ${DeviceProfileValidator.validate(preset.forProfile("profile_a"))}",
                DeviceProfileValidator.validate(preset.forProfile("profile_a")).isEmpty(),
            )
        }
    }

    @Test
    fun rejectsAndroidUserAgentOnWindowsProfile() {
        val config = DevicePresets.get(DevicePresets.DESKTOP)!!.forProfile("profile_a").copy(
            userAgent = DevicePresets.get(DevicePresets.ANDROID_PHONE)!!.userAgent,
        )

        assertTrue(DeviceProfileValidator.validate(config).any { it.contains("Windows User-Agent") })
    }

    @Test
    fun rejectsMismatchedClientHints() {
        val config = DevicePresets.get(DevicePresets.ANDROID_PHONE)!!.forProfile("profile_a").copy(
            clientHints = ClientHintsConfig("Windows", false, emptyList()),
        )

        assertTrue(DeviceProfileValidator.validate(config).isNotEmpty())
    }
}
