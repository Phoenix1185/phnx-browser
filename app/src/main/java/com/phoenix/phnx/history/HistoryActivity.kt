package com.phoenix.phnx.history

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.phoenix.phnx.PhnxApplication
import com.phoenix.phnx.R
import com.phoenix.phnx.system.BrowserNavigationIntent
import java.text.DateFormat
import java.util.Date

class HistoryActivity : AppCompatActivity() {
    private val app by lazy { application as PhnxApplication }
    private val profileId by lazy { app.profileManager.activeProfile().id }
    private lateinit var list: LinearLayout
    private lateinit var searchInput: EditText

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
        searchInput = EditText(this).apply {
            hint = getString(R.string.search_history)
            isSingleLine = true
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = refresh()
                override fun afterTextChanged(s: Editable?) = Unit
            })
        }
        content.addView(searchInput)
        content.addView(Button(this).apply {
            text = getString(R.string.clear_history)
            setOnClickListener { confirmClear() }
        })
        content.addView(Button(this).apply {
            text = getString(R.string.clear_old_history)
            setOnClickListener { confirmClearOld() }
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
        val entries = app.historyManager.search(profileId, searchInput.text.toString())
        if (entries.isEmpty()) {
            list.addView(TextView(this).apply {
                text = getString(R.string.no_history)
                textSize = 16f
                setTextColor(getColor(R.color.phnx_muted))
                setPadding(0, dp(18), 0, 0)
            })
            return
        }
        var lastSection: String? = null
        entries.forEach { entry ->
            val section = sectionFor(entry.visitedAt)
            if (section != lastSection) {
                list.addView(TextView(this).apply {
                    text = section
                    textSize = 18f
                    setTextColor(getColor(R.color.phnx_blue))
                    setPadding(0, dp(16), 0, dp(4))
                })
                lastSection = section
            }
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(12), 0, dp(12))
                isClickable = true
                isFocusable = true
                setBackgroundResource(android.R.drawable.list_selector_background)
            }
            val label = entry.title.ifBlank { entry.host.ifBlank { entry.url } }
            row.contentDescription = getString(R.string.open_history_entry, label)
            row.setOnClickListener { openSavedUrl(entry.url) }
            row.addView(TextView(this).apply {
                text = "$label\n${entry.host}\n${DateFormat.getDateTimeInstance().format(Date(entry.visitedAt))}"
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

    private fun openSavedUrl(rawUrl: String) {
        val intent = BrowserNavigationIntent.forSavedUrl(this, profileId, rawUrl)
        if (intent == null) {
            Toast.makeText(this, getString(R.string.unable_to_open_saved_page), Toast.LENGTH_LONG).show()
        } else {
            startActivity(intent)
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

    private fun confirmClearOld() {
        AlertDialog.Builder(this)
            .setTitle(R.string.clear_old_history)
            .setMessage(R.string.clear_old_history_warning)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.clear) { _, _ ->
                app.historyManager.deleteBefore(profileId, System.currentTimeMillis() - THIRTY_DAYS_MILLIS)
                refresh()
            }
            .show()
    }

    private fun sectionFor(timestamp: Long): String {
        val today = java.util.Calendar.getInstance()
        val date = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
        if (today.get(java.util.Calendar.ERA) == date.get(java.util.Calendar.ERA) &&
            today.get(java.util.Calendar.YEAR) == date.get(java.util.Calendar.YEAR) &&
            today.get(java.util.Calendar.DAY_OF_YEAR) == date.get(java.util.Calendar.DAY_OF_YEAR)
        ) return getString(R.string.history_today)
        today.add(java.util.Calendar.DAY_OF_YEAR, -1)
        return if (today.get(java.util.Calendar.YEAR) == date.get(java.util.Calendar.YEAR) &&
            today.get(java.util.Calendar.DAY_OF_YEAR) == date.get(java.util.Calendar.DAY_OF_YEAR)
        ) getString(R.string.history_yesterday) else getString(R.string.history_earlier)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val THIRTY_DAYS_MILLIS = 30L * 24L * 60L * 60L * 1000L
    }
}
