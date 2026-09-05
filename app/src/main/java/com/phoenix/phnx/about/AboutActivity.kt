package com.phoenix.phnx.about

import android.os.Bundle
import android.view.Gravity
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
            setPadding(dp(24), dp(28), dp(24), dp(28))
        }
        content.addView(text("PHNX Browser", 30f, true))
        content.addView(text(getString(R.string.builder_attribution), 16f, false))
        content.addView(text("Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", 15f, false))
        content.addView(text("Chromium runtime\n${ChromiumVersionProvider.get(this)}", 15f, false))
        content.addView(text("\nCopyright Phoenix\n\nOpen Source Licenses\nThird-Party Notices\nPrivacy Policy\nTerms of Service", 15f, false))

        val scroll = ScrollView(this).apply { addView(content) }
        setContentView(scroll)
    }

    private fun text(value: String, size: Float, prominent: Boolean): TextView = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(getColor(R.color.phnx_text))
        gravity = Gravity.START
        setPadding(0, dp(if (prominent) 0 else 14), 0, 0)
        if (prominent) setTextColor(getColor(R.color.phnx_blue))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
