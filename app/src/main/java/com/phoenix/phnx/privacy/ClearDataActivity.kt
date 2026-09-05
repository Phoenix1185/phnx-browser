package com.phoenix.phnx.privacy

import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.phoenix.phnx.PhnxApplication
import com.phoenix.phnx.R

class ClearDataActivity : AppCompatActivity() {
    private val app by lazy { application as PhnxApplication }
    private lateinit var cookies: CheckBox
    private lateinit var siteStorage: CheckBox
    private lateinit var cache: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.clear_browsing_data)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(24))
            setBackgroundColor(getColor(R.color.phnx_cream))
        }
        content.addView(TextView(this).apply {
            text = getString(R.string.clear_browsing_data)
            textSize = 30f
            setTextColor(getColor(R.color.phnx_blue))
        })
        content.addView(TextView(this).apply {
            text = getString(R.string.clear_browsing_data_summary)
            textSize = 14f
            setTextColor(getColor(R.color.phnx_muted))
            setPadding(0, dp(8), 0, dp(16))
        })
        cookies = checkBox(R.string.clear_cookies, R.string.clear_cookies_summary, true)
        siteStorage = checkBox(R.string.clear_site_storage, R.string.clear_site_storage_summary, true)
        cache = checkBox(R.string.clear_cache, R.string.clear_cache_summary, true)
        content.addView(cookies)
        content.addView(siteStorage)
        content.addView(cache)
        content.addView(Button(this).apply {
            text = getString(R.string.clear_selected_data)
            setOnClickListener { confirmClear() }
        })
        setContentView(ScrollView(this).apply {
            setBackgroundColor(getColor(R.color.phnx_cream))
            addView(content)
        })
    }

    private fun confirmClear() {
        if (!cookies.isChecked && !siteStorage.isChecked && !cache.isChecked) {
            Toast.makeText(this, R.string.clear_nothing_selected, Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.clear_selected_data)
            .setMessage(R.string.clear_data_warning)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.clear) { _, _ -> clearSelected() }
            .show()
    }

    private fun clearSelected() {
        val request = ClearDataRequest(cookies.isChecked, siteStorage.isChecked, cache.isChecked)
        val views = buildList {
            app.profileViewPool.forEachView { add(it) }
        }
        app.clearDataManager.clear(request, views) {
            runOnUiThread {
                Toast.makeText(this, R.string.clear_complete, Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun checkBox(title: Int, summary: Int, checked: Boolean) = CheckBox(this).apply {
        text = "${getString(title)}\n${getString(summary)}"
        isChecked = checked
        setPadding(0, dp(10), 0, dp(10))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
