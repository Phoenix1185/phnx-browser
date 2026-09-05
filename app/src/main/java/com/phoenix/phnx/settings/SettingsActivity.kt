package com.phoenix.phnx.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.phoenix.phnx.PhnxPreferences
import com.phoenix.phnx.R
import com.phoenix.phnx.network.NetworkActivity
import com.phoenix.phnx.privacy.PrivacyActivity
import com.phoenix.phnx.permissions.PermissionActivity
import com.phoenix.phnx.profiles.ProfilesActivity
import com.phoenix.phnx.resources.PerformanceActivity
import com.phoenix.phnx.search.SearchEngineActivity

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.settings)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(24))
            setBackgroundColor(getColor(R.color.phnx_cream))
        }
        content.addView(header("Settings"))
        addThemeSection(content)
        addDataSaverSection(content)
        addPrivacySection(content)
        addSiteSettingsSection(content)
        addHistorySection(content)
        addSection(content, "Downloads", "Downloads use Android's Downloads provider.")
        addSearchSection(content)
        addProfileSection(content)
        addNetworkSection(content)
        addPerformanceSection(content)
        addSection(content, "About PHNX", "Open the About screen from the browser menu for version and runtime details.")

        setContentView(ScrollView(this).apply {
            setBackgroundColor(getColor(R.color.phnx_cream))
            addView(content)
        })
    }

    private fun addThemeSection(parent: LinearLayout) {
        val row = optionRow(getString(R.string.theme), themeLabel())
        row.setOnClickListener { showThemeDialog() }
        parent.addView(row)
    }

    private fun addProfileSection(parent: LinearLayout) {
        val row = optionRow(getString(R.string.profiles), getString(R.string.profiles_summary))
        row.setOnClickListener { startActivity(Intent(this, ProfilesActivity::class.java)) }
        parent.addView(row)
    }

    private fun addNetworkSection(parent: LinearLayout) {
        val row = optionRow(getString(R.string.network), getString(R.string.network_summary))
        row.setOnClickListener { startActivity(Intent(this, NetworkActivity::class.java)) }
        parent.addView(row)
    }

    private fun addPerformanceSection(parent: LinearLayout) {
        val row = optionRow(getString(R.string.performance), getString(R.string.performance_summary))
        row.setOnClickListener { startActivity(Intent(this, PerformanceActivity::class.java)) }
        parent.addView(row)
    }

    private fun addPrivacySection(parent: LinearLayout) {
        val row = optionRow(getString(R.string.privacy_security), getString(R.string.privacy_security_summary))
        row.setOnClickListener { startActivity(Intent(this, PrivacyActivity::class.java)) }
        parent.addView(row)
    }

    private fun addSiteSettingsSection(parent: LinearLayout) {
        val row = optionRow(getString(R.string.site_permissions), getString(R.string.site_permissions_summary))
        row.setOnClickListener { startActivity(Intent(this, PermissionActivity::class.java)) }
        parent.addView(row)
    }

    private fun addHistorySection(parent: LinearLayout) {
        val row = optionRow(getString(R.string.history_retention), historyRetentionLabel())
        row.setOnClickListener { showHistoryRetentionDialog() }
        parent.addView(row)
    }

    private fun addSearchSection(parent: LinearLayout) {
        val row = optionRow(getString(R.string.search_engine), getString(R.string.search_engine_summary))
        row.setOnClickListener { startActivity(Intent(this, SearchEngineActivity::class.java)) }
        parent.addView(row)
    }

    private fun addDataSaverSection(parent: LinearLayout) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, dp(16), 0, dp(16))
            contentDescription = getString(R.string.data_saver)
        }
        val labels = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        labels.addView(TextView(this).apply {
            text = getString(R.string.data_saver)
            textSize = 18f
            setTextColor(getColor(R.color.phnx_text))
        })
        labels.addView(TextView(this).apply {
            text = getString(R.string.data_saver_summary)
            textSize = 14f
            setTextColor(getColor(R.color.phnx_muted))
            setPadding(0, dp(6), 0, 0)
        })
        row.addView(labels, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(SwitchCompat(this).apply {
            isChecked = PhnxPreferences.store(this@SettingsActivity)
                .getBoolean(PhnxPreferences.DATA_SAVER_ENABLED, false)
            setOnCheckedChangeListener { _, enabled ->
                PhnxPreferences.store(this@SettingsActivity).edit()
                    .putBoolean(PhnxPreferences.DATA_SAVER_ENABLED, enabled)
                    .apply()
            }
        })
        parent.addView(row)
    }

    private fun optionRow(title: String, summary: String): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, dp(16), 0, dp(16))
        isClickable = true
        isFocusable = true
        contentDescription = title
        addView(TextView(this@SettingsActivity).apply {
            text = title
            textSize = 18f
            setTextColor(getColor(R.color.phnx_text))
        })
        addView(TextView(this@SettingsActivity).apply {
            text = summary
            textSize = 14f
            setTextColor(getColor(R.color.phnx_muted))
            setPadding(0, dp(6), 0, 0)
        })
    }

    private fun showThemeDialog() {
        val modes = arrayOf(
            getString(R.string.theme_system),
            getString(R.string.theme_light),
            getString(R.string.theme_dark),
        )
        val values = arrayOf(
            PhnxPreferences.THEME_SYSTEM,
            PhnxPreferences.THEME_LIGHT,
            PhnxPreferences.THEME_DARK,
        )
        val selected = values.indexOf(PhnxPreferences.themeMode(this)).coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle(R.string.theme)
            .setSingleChoiceItems(modes, selected) { dialog, which ->
                PhnxPreferences.setTheme(this, values[which])
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showHistoryRetentionDialog() {
        val labels = arrayOf(
            getString(R.string.history_remember),
            getString(R.string.history_clear_on_close),
        )
        val values = arrayOf(
            PhnxPreferences.HISTORY_REMEMBER,
            PhnxPreferences.HISTORY_CLEAR_ON_CLOSE,
        )
        val selected = values.indexOf(PhnxPreferences.historyRetention(this)).coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle(R.string.history_retention)
            .setSingleChoiceItems(labels, selected) { dialog, which ->
                PhnxPreferences.store(this).edit()
                    .putString(PhnxPreferences.HISTORY_RETENTION, values[which])
                    .apply()
                dialog.dismiss()
                recreate()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun themeLabel(): String = when (PhnxPreferences.themeMode(this)) {
        PhnxPreferences.THEME_LIGHT -> getString(R.string.theme_light)
        PhnxPreferences.THEME_DARK -> getString(R.string.theme_dark)
        else -> getString(R.string.theme_system)
    }

    private fun historyRetentionLabel(): String = when (PhnxPreferences.historyRetention(this)) {
        PhnxPreferences.HISTORY_CLEAR_ON_CLOSE -> getString(R.string.history_clear_on_close)
        else -> getString(R.string.history_remember)
    }

    private fun addSection(parent: LinearLayout, title: String, summary: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(16), 0, dp(16))
            contentDescription = title
        }
        row.addView(TextView(this).apply {
            text = title
            textSize = 18f
            setTextColor(getColor(R.color.phnx_text))
        })
        row.addView(TextView(this).apply {
            text = summary
            textSize = 14f
            setTextColor(getColor(R.color.phnx_muted))
            setPadding(0, dp(6), 0, 0)
        })
        parent.addView(row)
    }

    private fun header(value: String): View = TextView(this).apply {
        text = value
        textSize = 30f
        setTextColor(getColor(R.color.phnx_blue))
        setPadding(0, 0, 0, dp(8))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
