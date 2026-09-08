package com.phoenix.phnx.identity

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.phoenix.phnx.PhnxApplication
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertTrue
import org.junit.Test

class IdentityProfileSwitchTest {
    @Test
    fun observesEveryBuiltInPresetCompatibilityValue() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val app = context.applicationContext as PhnxApplication
        val profileId = app.profileManager.activeProfile().id
        val presetIds = DevicePresets.all().map { it.id }

        try {
            presetIds.forEach { presetId ->
                val expected = DevicePresets.get(presetId)!!.forProfile(profileId)
                app.deviceProfileManager.applyPreset(profileId, presetId)
                val intent = Intent(context, IdentityDiagnosticActivity::class.java)
                    .putExtra(IdentityDiagnosticActivity.EXTRA_PROFILE_ID, profileId)
                val observed = AtomicReference<String>()
                val ready = CountDownLatch(1)

                ActivityScenario.launch<IdentityDiagnosticActivity>(intent).use { scenario ->
                    scenario.onActivity { activity ->
                        activity.whenDiagnosticsReady { webView ->
                            webView.evaluateJavascript(
                                "JSON.stringify({ua:navigator.userAgent,platform:navigator.platform,language:navigator.language,languages:navigator.languages,width:window.innerWidth,height:window.innerHeight,dpr:window.devicePixelRatio,timezone:Intl.DateTimeFormat().resolvedOptions().timeZone})",
                            ) {
                                observed.set(it)
                                ready.countDown()
                            }
                        }
                    }
                    assertTrue("Diagnostic page did not become ready for $presetId", ready.await(20, TimeUnit.SECONDS))
                }

                val result = observed.get().orEmpty()
                assertTrue("User-Agent was not applied for $presetId: $result", result.contains(expected.userAgent))
                assertTrue("Platform was not applied for $presetId: $result", result.contains(expected.platform))
                assertTrue("Viewport width was not observed for $presetId: $result", result.contains(expected.viewportWidth.toString()))
                assertTrue("Viewport height was not observed for $presetId: $result", result.contains(expected.viewportHeight.toString()))
                val expectedDpr = expected.deviceScaleFactor.toString().removeSuffix(".0")
                assertTrue("Device scale was not observed for $presetId: $result", result.contains(expectedDpr))
                assertTrue("Language was not observed for $presetId: $result", result.contains(expected.language))
                assertTrue("Timezone was not observed for $presetId: $result", result.contains(expected.timezone))
            }
        } finally {
            app.deviceProfileManager.resetProfileConfiguration(profileId)
        }
    }
}
