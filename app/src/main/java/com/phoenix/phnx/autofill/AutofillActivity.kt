package com.phoenix.phnx.autofill

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.autofill.AutofillManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.phoenix.phnx.R

class AutofillActivity : AppCompatActivity() {
    private lateinit var serviceSummary: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.autofill_settings)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(24))
            setBackgroundColor(getColor(R.color.phnx_cream))
        }
        content.addView(backRow { finish() })
        content.addView(header(getString(R.string.autofill_settings)))
        content.addView(description(getString(R.string.autofill_description)))

        val serviceRow = optionRow(
            getString(R.string.autofill_android_service),
            getString(R.string.autofill_android_service_summary),
        )
        serviceSummary = serviceRow.getChildAt(1) as TextView
        content.addView(serviceRow)

        val openSettings = optionRow(
            getString(R.string.autofill_open_android_settings),
            getString(R.string.autofill_open_android_settings_summary),
        )
        openSettings.setOnClickListener { openAndroidAutofillSettings() }
        content.addView(openSettings)

        setContentView(ScrollView(this).apply {
            setBackgroundColor(getColor(R.color.phnx_cream))
            addView(content)
        })
    }

    override fun onResume() {
        super.onResume()
        if (::serviceSummary.isInitialized) {
            serviceSummary.text = currentServiceSummary()
        }
    }

    private fun currentServiceSummary(): String {
        val manager = getSystemService(AutofillManager::class.java)
        val component = manager?.autofillServiceComponentName
        return if (component == null) {
            getString(R.string.autofill_no_service)
        } else {
            getString(R.string.autofill_current_service, component.packageName)
        }
    }

    private fun openAndroidAutofillSettings() {
        try {
            startActivity(Intent(Settings.ACTION_AUTOFILL_SETTINGS))
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun optionRow(title: String, summary: String): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, dp(18), 0, dp(18))
        isClickable = true
        isFocusable = true
        addView(TextView(this@AutofillActivity).apply {
            text = title
            textSize = 18f
            setTextColor(getColor(R.color.phnx_text))
        })
        addView(TextView(this@AutofillActivity).apply {
            text = summary
            textSize = 14f
            setTextColor(getColor(R.color.phnx_muted))
            setPadding(0, dp(6), 0, 0)
        })
    }

    private fun description(value: String): View = TextView(this).apply {
        text = value
        textSize = 16f
        setTextColor(getColor(R.color.phnx_text))
        setPadding(0, 0, 0, dp(12))
    }

    private fun header(value: String): View = TextView(this).apply {
        text = value
        textSize = 30f
        setTextColor(getColor(R.color.phnx_blue))
        setPadding(0, 0, 0, dp(12))
    }

    private fun backRow(action: () -> Unit): View = TextView(this).apply {
        text = "‹  ${getString(R.string.back)}"
        textSize = 16f
        setTextColor(getColor(R.color.phnx_blue))
        setPadding(0, 0, 0, dp(16))
        isClickable = true
        isFocusable = true
        setOnClickListener { action() }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
