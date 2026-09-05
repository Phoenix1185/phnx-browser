package com.phoenix.phnx.bookmarks

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.phoenix.phnx.PhnxApplication
import com.phoenix.phnx.R

class BookmarksActivity : AppCompatActivity() {
    private val app by lazy { application as PhnxApplication }
    private val profileId by lazy { app.profileManager.activeProfile().id }
    private lateinit var list: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.bookmarks)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(24))
            setBackgroundColor(getColor(R.color.phnx_cream))
        }
        content.addView(TextView(this).apply {
            text = getString(R.string.bookmarks)
            textSize = 30f
            setTextColor(getColor(R.color.phnx_blue))
        })
        val currentUrl = intent.getStringExtra(EXTRA_URL).orEmpty()
        val currentTitle = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        if (currentUrl.startsWith("http://") || currentUrl.startsWith("https://")) {
            content.addView(Button(this).apply {
                text = getString(R.string.add_current_bookmark)
                setOnClickListener {
                    app.bookmarkManager.add(profileId, currentTitle, currentUrl)
                    refresh()
                }
            })
        }
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
        val bookmarks = app.bookmarkManager.getForProfile(profileId)
        if (bookmarks.isEmpty()) {
            list.addView(TextView(this).apply {
                text = getString(R.string.no_bookmarks)
                textSize = 16f
                setTextColor(getColor(R.color.phnx_muted))
                setPadding(0, dp(18), 0, 0)
            })
            return
        }
        bookmarks.forEach { bookmark ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(12), 0, dp(12))
            }
            row.addView(TextView(this).apply {
                text = "${bookmark.title}\n${bookmark.url}"
                textSize = 15f
                setTextColor(getColor(R.color.phnx_text))
            }, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(Button(this).apply {
                text = getString(R.string.delete)
                setOnClickListener {
                    app.bookmarkManager.delete(profileId, bookmark.id)
                    refresh()
                }
            })
            list.addView(row)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val EXTRA_URL = "url"
        const val EXTRA_TITLE = "title"
    }
}
