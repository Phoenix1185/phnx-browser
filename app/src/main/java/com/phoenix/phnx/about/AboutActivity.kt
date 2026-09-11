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
import com.phoenix.phnx.system.BrowserNavigationIntent

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.about_phnx)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(28), dp(24), dp(32))
            setBackgroundColor(getColor(R.color.phnx_cream))
        }
        content.addView(backButton())
        content.addView(text(getString(R.string.about_phnx_title), 30f, true))
        content.addView(text(getString(R.string.about_phnx_description), 16f, false))
        content.addView(text(getString(R.string.builder_attribution), 15f, false))

        content.addView(sectionTitle(getString(R.string.about_version)))
        content.addView(infoRow(getString(R.string.about_browser), BuildConfig.VERSION_NAME))
        content.addView(infoRow(getString(R.string.about_build), BuildConfig.VERSION_CODE.toString()))
        content.addView(infoRow(getString(R.string.about_channel), releaseChannel()))
        content.addView(infoRow(getString(R.string.about_chromium_webview), ChromiumVersionProvider.get(this)))

        content.addView(sectionTitle(getString(R.string.about_features)))
        listOf(
            R.string.about_feature_chromium,
            R.string.about_feature_profiles,
            R.string.about_feature_profile_data,
            R.string.about_feature_network,
            R.string.about_feature_privacy,
            R.string.about_feature_lifecycle,
            R.string.about_feature_saved_pages,
            R.string.about_feature_private,
            R.string.about_feature_browser_tools,
        ).forEach { feature ->
            content.addView(text("• ${getString(feature)}", 15f, false))
        }

        content.addView(sectionTitle(getString(R.string.about_updates)))
        content.addView(text(getString(R.string.about_updates_copy, BuildConfig.VERSION_NAME), 15f, false))
        content.addView(actionButton(getString(R.string.about_check_for_updates)) {
            startActivity(Intent(this, UpdateActivity::class.java))
        })

        content.addView(sectionTitle(getString(R.string.about_phnx_online)))
        content.addView(websiteButton(getString(R.string.about_official_website), PhnxWebsite.HOME))
        content.addView(websiteButton(getString(R.string.about_documentation), PhnxWebsite.DOCS))
        content.addView(websiteButton(getString(R.string.about_releases), PhnxWebsite.RELEASES))
        content.addView(websiteButton(getString(R.string.about_changelog), PhnxWebsite.CHANGELOG))
        content.addView(websiteButton(getString(R.string.about_privacy_policy), PhnxWebsite.PRIVACY))
        content.addView(websiteButton(getString(R.string.about_security), PhnxWebsite.SECURITY))
        content.addView(websiteButton(getString(R.string.about_github), PhnxWebsite.GITHUB))

        content.addView(sectionTitle(getString(R.string.about_legal_notices)))
        content.addView(linkButton(getString(R.string.about_privacy_policy), LegalActivity.PRIVACY))
        content.addView(linkButton(getString(R.string.about_terms), LegalActivity.TERMS))
        content.addView(linkButton(getString(R.string.about_licenses), LegalActivity.LICENSES))
        content.addView(linkButton(getString(R.string.about_third_party_notices), LegalActivity.NOTICES))

        content.addView(sectionTitle(getString(R.string.about_developer)))
        content.addView(text(getString(R.string.about_developer_copy), 15f, false))

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
        addView(text(label, 15f, false), LinearLayout.LayoutParams(0, -2, 0.55f))
        addView(text(value, 15f, false).apply {
            setTextColor(getColor(R.color.phnx_muted))
            gravity = android.view.Gravity.END
            maxLines = 2
            setTextIsSelectable(true)
        }, LinearLayout.LayoutParams(0, -2, 0.45f))
    }

    private fun actionButton(label: String, action: () -> Unit): Button = Button(this).apply {
        text = label
        isAllCaps = false
        setOnClickListener { action() }
        setTextColor(getColor(R.color.phnx_blue))
    }

    private fun linkButton(label: String, page: String): Button = actionButton(label) {
        startActivity(Intent(this, LegalActivity::class.java).putExtra(LegalActivity.EXTRA_PAGE, page))
    }

    private fun websiteButton(label: String, url: String): Button = actionButton(label) {
        BrowserNavigationIntent.forCurrentProfile(this, url)?.let(::startActivity)
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

    private fun backButton(): Button = Button(this).apply {
        text = getString(R.string.back)
        setOnClickListener { finish() }
        setTextColor(getColor(R.color.phnx_blue))
        isAllCaps = false
        minWidth = 0
        minimumWidth = 0
        setPadding(0, 0, dp(16), dp(8))
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
