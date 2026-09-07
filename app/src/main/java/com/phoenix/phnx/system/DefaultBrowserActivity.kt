package com.phoenix.phnx.system

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.phoenix.phnx.R

class DefaultBrowserActivity : AppCompatActivity() {
    private val browserManager by lazy { DefaultBrowserManager(this) }
    private lateinit var status: TextView
    private lateinit var action: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.default_browser)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(28), dp(24), dp(32))
            setBackgroundColor(getColor(R.color.phnx_cream))
        }
        content.addView(TextView(this).apply {
            text = getString(R.string.default_browser)
            textSize = 28f
            setTextColor(getColor(R.color.phnx_blue))
        })
        content.addView(TextView(this).apply {
            text = getString(R.string.default_browser_summary)
            textSize = 15f
            setTextColor(getColor(R.color.phnx_text))
            setPadding(0, dp(12), 0, dp(18))
        })
        status = TextView(this).apply {
            textSize = 17f
            setTextColor(getColor(R.color.phnx_text))
        }
        content.addView(status)
        action = Button(this)
        action.setOnClickListener { requestDefaultBrowser() }
        content.addView(action)
        content.addView(Button(this).apply {
            text = getString(R.string.open_default_browser_settings)
            setOnClickListener { openDefaultBrowserSettings() }
        })
        content.addView(Button(this).apply {
            text = getString(R.string.open_app_settings)
            setOnClickListener { openAppSettings() }
        })
        setContentView(ScrollView(this).apply {
            setBackgroundColor(getColor(R.color.phnx_cream))
            addView(content)
        })
        refreshStatus()
    }

    override fun onResume() {
        super.onResume()
        if (::status.isInitialized) refreshStatus()
    }

    private fun refreshStatus() {
        when (browserManager.state()) {
            DefaultBrowserState.DEFAULT -> {
                status.text = getString(R.string.default_browser_status_default)
                action.text = getString(R.string.manage_default_browser)
                action.isEnabled = false
            }
            DefaultBrowserState.NOT_DEFAULT -> {
                status.text = getString(R.string.default_browser_status_not_default)
                action.text = getString(R.string.set_as_default_browser)
                action.isEnabled = true
            }
            DefaultBrowserState.UNKNOWN -> {
                status.text = getString(R.string.default_browser_status_unknown)
                action.text = getString(R.string.open_default_browser_settings)
                action.isEnabled = true
            }
            DefaultBrowserState.UNAVAILABLE -> {
                status.text = getString(R.string.default_browser_status_unavailable)
                action.text = getString(R.string.open_default_browser_settings)
                action.isEnabled = true
            }
        }
    }

    private fun requestDefaultBrowser() {
        browserManager.requestRoleIntent()?.let(::startActivity)
            ?: openDefaultBrowserSettings()
    }

    private fun openDefaultBrowserSettings() {
        startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
    }

    private fun openAppSettings() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        })
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
