package com.phoenix.phnx.identity

import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.phoenix.phnx.PhnxApplication
import com.phoenix.phnx.R

class IdentityActivity : AppCompatActivity() {
    private val app by lazy { application as PhnxApplication }
    private val profileId by lazy {
        intent.getStringExtra(EXTRA_PROFILE_ID) ?: app.profileManager.activeProfile().id
    }
    private lateinit var presetSpinner: Spinner
    private lateinit var details: TextView
    private lateinit var result: TextView
    private lateinit var presets: List<DevicePreset>
    private var selectedPreset: DevicePreset? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.identity)
        setContentView(buildLayout())
        loadConfiguration()
    }

    private fun buildLayout(): View {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(24))
            setBackgroundColor(getColor(R.color.phnx_cream))
        }
        content.addView(TextView(this).apply {
            text = getString(R.string.identity)
            textSize = 30f
            setTextColor(getColor(R.color.phnx_blue))
        })
        content.addView(TextView(this).apply {
            text = "Profile: ${app.profileManager.getAllProfiles().firstOrNull { it.id == profileId }?.name ?: profileId}"
            textSize = 15f
            setTextColor(getColor(R.color.phnx_muted))
            setPadding(0, dp(8), 0, dp(16))
        })
        content.addView(TextView(this).apply {
            text = getString(R.string.identity_summary)
            textSize = 14f
            setTextColor(getColor(R.color.phnx_muted))
            setPadding(0, 0, 0, dp(12))
        })

        presets = app.deviceProfileManager.getAvailablePresets()
        presetSpinner = Spinner(this)
        presetSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            presets.map { it.name },
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        presetSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedPreset = presets.getOrNull(position)
                selectedPreset?.forProfile(profileId)?.let(::showDetails)
            }
        }
        content.addView(presetSpinner, LinearLayout.LayoutParams(-1, dp(48)))

        details = TextView(this).apply {
            textSize = 15f
            setTextColor(getColor(R.color.phnx_text))
            setPadding(0, dp(16), 0, dp(16))
        }
        content.addView(details)
        result = TextView(this).apply {
            textSize = 14f
            setTextColor(getColor(R.color.phnx_muted))
            setPadding(0, 0, 0, dp(12))
        }
        content.addView(result)

        val actions = LinearLayout(this).apply {
            gravity = Gravity.END
            orientation = LinearLayout.HORIZONTAL
        }
        actions.addView(Button(this).apply {
            text = getString(R.string.reset)
            setOnClickListener {
                selectedPreset = presets.firstOrNull { it.id == DevicePresets.SYSTEM_DEFAULT }
                presetSpinner.setSelection(presets.indexOfFirst { it.id == selectedPreset?.id })
                saveSelectedPreset()
            }
        })
        actions.addView(Button(this).apply {
            text = getString(R.string.save)
            setOnClickListener { saveSelectedPreset() }
        })
        content.addView(actions)
        content.addView(Button(this).apply {
            text = "Open runtime diagnostics"
            setOnClickListener {
                startActivity(Intent(this@IdentityActivity, IdentityDiagnosticActivity::class.java).apply {
                    putExtra(EXTRA_PROFILE_ID, profileId)
                })
            }
        })
        return ScrollView(this).apply {
            setBackgroundColor(getColor(R.color.phnx_cream))
            addView(content)
        }
    }

    private fun loadConfiguration() {
        val config = app.deviceProfileManager.getProfileConfiguration(profileId)
        val index = presets.indexOfFirst { it.id == config.presetId }.coerceAtLeast(0)
        presetSpinner.setSelection(index)
        showDetails(config)
        result.text = getString(R.string.identity_saved)
    }

    private fun saveSelectedPreset() {
        val preset = selectedPreset ?: return
        runCatching {
            val config = app.deviceProfileManager.applyPreset(profileId, preset.id)
            showDetails(config)
            val status = DeviceProfileCapabilities.overallStatus(config)
            result.text = "Saved. Application assessment: ${DeviceProfileCapabilities.statusLabel(status)}."
            if (profileId == app.profileManager.activeProfile().id) {
                result.text = "Saved. Restarting PHNX to recreate WebView with the selected profile."
                restartBrowser()
            }
        }.onFailure { error ->
            result.text = error.message ?: getString(R.string.identity_save_failed)
        }
    }

    private fun showDetails(config: BrowserIdentityConfig) {
        val capabilityLines = DeviceProfileCapabilities.forProfile(config).map {
            "${it.label}: ${DeviceProfileCapabilities.modeLabel(it.mode)}. ${it.detail}"
        }
        details.text = listOf(
            "Profile: ${config.name} (${config.presetId})",
            "User-Agent: ${config.userAgent}",
            "Operating system target: ${config.operatingSystem}",
            "Viewport target: ${config.viewportWidth} x ${config.viewportHeight}",
            "JavaScript screen target: ${config.screenWidth} x ${config.screenHeight}",
            "Device scale target: ${config.deviceScaleFactor}",
            "Locale target: ${config.locale} (${config.language})",
            "Timezone target: ${config.timezone}",
            "Touch target: ${config.touchSupport}; mobile mode: ${config.mobileMode}",
            "Client hints target (not applied by WebView): ${config.clientHints.platform}, mobile=${config.clientHints.mobile}",
            "Overall status: ${DeviceProfileCapabilities.statusLabel(DeviceProfileCapabilities.overallStatus(config))}",
            "",
            "Field capabilities:",
            *capabilityLines.toTypedArray(),
        ).joinToString("\n")
    }

    private fun restartBrowser() {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(launchIntent)
        finishAffinity()
        Process.killProcess(Process.myPid())
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val EXTRA_PROFILE_ID = "profile_id"
    }
}
