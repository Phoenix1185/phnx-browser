package com.phoenix.phnx.about

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.phoenix.phnx.BuildConfig
import com.phoenix.phnx.R

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.about_phnx)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(28), dp(24), dp(32))
            setBackgroundColor(getColor(R.color.phnx_cream))
        }
        content.addView(text("PHNX Browser", 30f, true))
        content.addView(text("A fast, private, and powerful Chromium-based browser built for modern browsing.", 16f, false))
        content.addView(text(getString(R.string.builder_attribution), 15f, false))

        content.addView(sectionTitle("Version"))
        content.addView(infoRow("PHNX Browser", BuildConfig.VERSION_NAME))
        content.addView(infoRow("Build", BuildConfig.VERSION_CODE.toString()))
        content.addView(infoRow("Channel", releaseChannel()))
        content.addView(infoRow("Chromium runtime", ChromiumVersionProvider.get(this)))

        content.addView(sectionTitle("Features"))
        listOf(
            "Chromium-based browsing",
            "Multiple isolated browser profiles",
            "Profile-specific cookies, storage, permissions, history, and sessions",
            "Per-profile network configuration",
            "Privacy and security controls",
            "Resource and profile lifecycle management",
            "Download and bookmark management",
            "Private browsing",
            "Modern browser interface",
        ).forEach { feature ->
            content.addView(text("• $feature", 15f, false))
        }

        content.addView(sectionTitle("Updates"))
        content.addView(text("Current version: ${BuildConfig.VERSION_NAME}\nPHNX checks the official Phoenix release feed and lets you review releases before opening them.", 15f, false))
        content.addView(actionButton("Check for updates") {
            startActivity(Intent(this, UpdateActivity::class.java))
        })

        content.addView(sectionTitle("Legal and notices"))
        content.addView(linkButton("Privacy Policy", LegalActivity.PRIVACY))
        content.addView(linkButton("Terms of Use", LegalActivity.TERMS))
        content.addView(linkButton("Open Source Licenses", LegalActivity.LICENSES))
        content.addView(linkButton("Third-Party Notices", LegalActivity.NOTICES))

        content.addView(sectionTitle("Developer"))
        content.addView(text("PHOENIX\nBuilt & engineered with a focus on performance, privacy, and control.\n\n© 2026 Phoenix. All rights reserved.", 15f, false))

        val scroll = ScrollView(this).apply {
            setBackgroundColor(getColor(R.color.phnx_cream))
            addView(content)
        }
        setContentView(scroll)
    }

    private fun sectionTitle(value: String): TextView = text(value, 19f, true).apply {
        setPadding(0, dp(28), 0, dp(4))
    }

    private fun infoRow(label: String, value: String): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(0, dp(8), 0, dp(8))
        addView(text(label, 15f, false), LinearLayout.LayoutParams(0, -2, 1f))
        addView(text(value, 15f, false).apply {
            setTextColor(getColor(R.color.phnx_muted))
        })
    }

    private fun actionButton(label: String, action: () -> Unit): Button = Button(this).apply {
        text = label
        setOnClickListener { action() }
        setTextColor(getColor(R.color.phnx_blue))
    }

    private fun linkButton(label: String, page: String): Button = actionButton(label) {
        startActivity(Intent(this, LegalActivity::class.java).putExtra(LegalActivity.EXTRA_PAGE, page))
    }

    private fun text(value: String, size: Float, prominent: Boolean): TextView = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(getColor(R.color.phnx_text))
        setPadding(0, if (prominent) 0 else dp(8), 0, 0)
        if (prominent) setTextColor(getColor(R.color.phnx_blue))
    }

    private fun releaseChannel(): String = when (BuildConfig.BUILD_TYPE.lowercase()) {
        "release" -> "Stable"
        "debug" -> "Debug"
        else -> BuildConfig.BUILD_TYPE.replaceFirstChar { it.uppercase() }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
