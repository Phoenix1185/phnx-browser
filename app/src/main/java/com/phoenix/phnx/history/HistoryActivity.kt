package com.phoenix.phnx.history

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.phoenix.phnx.PhnxApplication
import com.phoenix.phnx.R
import java.text.DateFormat
import java.util.Date

class HistoryActivity : AppCompatActivity() {
    private val app by lazy { application as PhnxApplication }
    private val profileId by lazy { app.profileManager.activeProfile().id }
    private lateinit var list: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.history)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(24))
            setBackgroundColor(getColor(R.color.phnx_cream))
        }
        content.addView(TextView(this).apply {
            text = getString(R.string.history)
            textSize = 30f
            setTextColor(getColor(R.color.phnx_blue))
        })
        content.addView(Button(this).apply {
            text = getString(R.string.clear_history)
            setOnClickListener { confirmClear() }
        })
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(list)
        setContentView(ScrollView(this).apply {
            setBackgroundColor(getColor(R.color.phnx_cream))
            addView(content)
        })
    }

    override fun onResume() {
        super.onResume()
        if (::list.isInitialized) refresh()
    }

    private fun refresh() {
        list.removeAllViews()
        val entries = app.historyManager.getForProfile(profileId)
        if (entries.isEmpty()) {
            list.addView(TextView(this).apply {
                text = getString(R.string.no_history)
                textSize = 16f
                setTextColor(getColor(R.color.phnx_muted))
                setPadding(0, dp(18), 0, 0)
            })
            return
        }
        entries.forEach { entry ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(12), 0, dp(12))
            }
            row.addView(TextView(this).apply {
                text = "${entry.title}\n${entry.host}\n${DateFormat.getDateTimeInstance().format(Date(entry.visitedAt))}"
                textSize = 15f
                setTextColor(getColor(R.color.phnx_text))
            }, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(Button(this).apply {
                text = getString(R.string.delete)
                setOnClickListener {
                    app.historyManager.delete(profileId, entry.id)
                    refresh()
                }
            })
            list.addView(row)
        }
    }

    private fun confirmClear() {
        AlertDialog.Builder(this)
            .setTitle(R.string.clear_history)
            .setMessage(R.string.clear_history_warning)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.clear) { _, _ ->
                app.historyManager.clearProfile(profileId)
                refresh()
            }
            .show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
