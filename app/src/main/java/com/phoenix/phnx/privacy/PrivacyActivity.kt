package com.phoenix.phnx.privacy

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.phoenix.phnx.PhnxApplication
import com.phoenix.phnx.R

class PrivacyActivity : AppCompatActivity() {
    private val app by lazy { application as PhnxApplication }
    private val profileId by lazy { app.profileManager.activeProfile().id }
    private lateinit var settings: PrivacySettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = app.privacyManager.getSettings(profileId)
        title = getString(R.string.privacy_security)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(24))
            setBackgroundColor(getColor(R.color.phnx_cream))
        }
        content.addView(header(getString(R.string.privacy_security)))
        content.addView(TextView(this).apply {
            text = getString(R.string.privacy_profile_summary, profileId)
            textSize = 14f
            setTextColor(getColor(R.color.phnx_muted))
            setPadding(0, 0, 0, dp(12))
        })
        addSwitch(content, getString(R.string.privacy_javascript), getString(R.string.privacy_javascript_summary), settings.javascriptEnabled) {
            settings = settings.copy(javascriptEnabled = it)
            save()
        }
        addSwitch(content, getString(R.string.privacy_third_party_cookies), getString(R.string.privacy_third_party_cookies_summary), settings.thirdPartyCookiesAllowed) {
            settings = settings.copy(thirdPartyCookiesAllowed = it)
            save()
        }
        addSwitch(content, getString(R.string.privacy_popups), getString(R.string.privacy_popups_summary), settings.popupsAllowed) {
            settings = settings.copy(popupsAllowed = it)
            save()
        }
        addSwitch(content, getString(R.string.privacy_safe_browsing), getString(R.string.privacy_safe_browsing_summary), settings.safeBrowsingEnabled) {
            settings = settings.copy(safeBrowsingEnabled = it)
            save()
        }
        addSwitch(content, getString(R.string.privacy_do_not_track), getString(R.string.privacy_do_not_track_summary), settings.doNotTrack, enabled = false) {}
        addTrackingProtection(content)
        content.addView(TextView(this).apply {
            text = getString(R.string.privacy_support_note)
            textSize = 13f
            setTextColor(getColor(R.color.phnx_muted))
            setPadding(0, dp(14), 0, 0)
        })

        setContentView(ScrollView(this).apply {
            setBackgroundColor(getColor(R.color.phnx_cream))
            addView(content)
        })
    }

    private fun addTrackingProtection(parent: LinearLayout) {
        val row = optionRow(
            getString(R.string.privacy_tracking_protection),
            settings.trackingProtection.name.lowercase().replace('_', ' '),
        )
        row.setOnClickListener {
            val values = TrackingProtectionLevel.values()
            AlertDialog.Builder(this)
                .setTitle(R.string.privacy_tracking_protection)
                .setSingleChoiceItems(values.map { it.name }.toTypedArray(), values.indexOf(settings.trackingProtection)) { dialog, which ->
                    settings = settings.copy(trackingProtection = values[which])
                    save()
                    dialog.dismiss()
                    recreate()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
        parent.addView(row)
    }

    private fun addSwitch(
        parent: LinearLayout,
        title: String,
        summary: String,
        checked: Boolean,
        enabled: Boolean = true,
        onChanged: (Boolean) -> Unit,
    ) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(12), 0, dp(12))
            contentDescription = title
        }
        val labels = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        labels.addView(TextView(this).apply {
            text = title
            textSize = 17f
            setTextColor(getColor(R.color.phnx_text))
        })
        labels.addView(TextView(this).apply {
            text = summary
            textSize = 13f
            setTextColor(getColor(R.color.phnx_muted))
            setPadding(0, dp(4), 0, 0)
        })
        row.addView(labels, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(SwitchCompat(this).apply {
            isChecked = checked
            isEnabled = enabled
            setOnCheckedChangeListener { _, value -> onChanged(value) }
        })
        parent.addView(row)
    }

    private fun save() = app.privacyManager.saveSettings(settings)

    private fun optionRow(title: String, summary: String): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, dp(14), 0, dp(14))
        isClickable = true
        isFocusable = true
        contentDescription = title
        addView(TextView(this@PrivacyActivity).apply {
            text = title
            textSize = 17f
            setTextColor(getColor(R.color.phnx_text))
        })
        addView(TextView(this@PrivacyActivity).apply {
            text = summary
            textSize = 13f
            setTextColor(getColor(R.color.phnx_muted))
            setPadding(0, dp(4), 0, 0)
        })
    }

    private fun header(value: String): View = TextView(this).apply {
        text = value
        textSize = 30f
        setTextColor(getColor(R.color.phnx_blue))
        setPadding(0, 0, 0, dp(8))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
