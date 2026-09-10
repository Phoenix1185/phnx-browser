package com.phoenix.phnx.search

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.phoenix.phnx.PhnxApplication
import com.phoenix.phnx.R

class SearchEngineActivity : AppCompatActivity() {
    private val app by lazy { application as PhnxApplication }
    private lateinit var summary: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.search_engine)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(24))
            setBackgroundColor(getColor(R.color.phnx_cream))
        }
        content.addView(TextView(this).apply {
            text = getString(R.string.search_engine)
            textSize = 30f
            setTextColor(getColor(R.color.phnx_blue))
        })
        summary = TextView(this).apply {
            textSize = 16f
            setTextColor(getColor(R.color.phnx_text))
            setPadding(0, dp(12), 0, dp(12))
        }
        content.addView(summary)
        content.addView(TextView(this).apply {
            text = getString(R.string.search_engine_summary)
            textSize = 14f
            setTextColor(getColor(R.color.phnx_muted))
        })
        content.addView(selectorRow())
        setContentView(ScrollView(this).apply {
            setBackgroundColor(getColor(R.color.phnx_cream))
            addView(content)
        })
        updateSummary()
    }

    private fun showChoices() {
        val engines = app.searchEngineManager.available
        val current = app.searchEngineManager.current()
        AlertDialog.Builder(this)
            .setTitle(R.string.search_engine)
            .setSingleChoiceItems(engines.map { it.name }.toTypedArray(), engines.indexOf(current)) { dialog, which ->
                app.searchEngineManager.setCurrent(engines[which].id)
                updateSummary()
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun updateSummary() {
        summary.text = getString(R.string.search_engine_selected, app.searchEngineManager.current().name)
    }

    private fun selectorRow(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, dp(24), 0, dp(16))
        isClickable = true
        isFocusable = true
        contentDescription = getString(R.string.search_engine_choose)
        addView(TextView(this@SearchEngineActivity).apply {
            text = getString(R.string.search_engine_choose)
            textSize = 18f
            setTextColor(getColor(R.color.phnx_blue))
        })
        addView(TextView(this@SearchEngineActivity).apply {
            text = getString(
                R.string.search_engine_available,
                app.searchEngineManager.available.joinToString(", ") { it.name },
            )
            textSize = 14f
            setTextColor(getColor(R.color.phnx_muted))
            setPadding(0, dp(6), 0, 0)
        })
        setOnClickListener { showChoices() }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
