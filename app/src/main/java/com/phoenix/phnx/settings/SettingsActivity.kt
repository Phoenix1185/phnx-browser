package com.phoenix.phnx.settings

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.phoenix.phnx.R

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.settings)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(24))
        }
        content.addView(header("Settings"))
        addSection(content, "Appearance", "Theme and toolbar options are planned for a later phase.")
        addSection(content, "Privacy & Security", "Site permissions, cookies, tracking protection, and clear-data controls are planned for a later phase.")
        addSection(content, "Site Settings", "JavaScript is enabled by default for modern websites. Per-site controls are planned.")
        addSection(content, "Downloads", "Downloads use Android's Downloads provider.")
        addSection(content, "Language and Search", "The default search engine is Google. Search-engine selection is planned.")
        addSection(content, "Profiles", "Multiple isolated profiles are planned for Phase 2.")
        addSection(content, "Network", "Per-profile network configuration is planned for Phase 3.")
        addSection(content, "Performance", "Resource management is planned for Phase 5.")
        addSection(content, "About PHNX", "Open the About screen from the browser menu for version and runtime details.")

        setContentView(ScrollView(this).apply { addView(content) })
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
