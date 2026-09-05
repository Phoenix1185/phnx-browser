package com.phoenix.phnx.downloads

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.phoenix.phnx.PhnxApplication
import com.phoenix.phnx.R

class DownloadsActivity : AppCompatActivity() {
    private val app by lazy { application as PhnxApplication }
    private val profileId by lazy { app.profileManager.activeProfile().id }
    private lateinit var list: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.downloads)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(24))
            setBackgroundColor(getColor(R.color.phnx_cream))
        }
        content.addView(TextView(this).apply {
            text = getString(R.string.downloads)
            textSize = 30f
            setTextColor(getColor(R.color.phnx_blue))
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
        val downloads = app.downloadManager.getForProfile(profileId)
        if (downloads.isEmpty()) {
            list.addView(TextView(this).apply {
                text = getString(R.string.no_downloads)
                textSize = 16f
                setTextColor(getColor(R.color.phnx_muted))
                setPadding(0, dp(18), 0, 0)
            })
            return
        }
        downloads.forEach { download ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(12), 0, dp(12))
            }
            val status = app.downloadManager.query(download.downloadId)
            val progress = app.downloadManager.progress(download.downloadId)
            row.addView(TextView(this).apply {
                val progressLabel = progress?.takeIf { it.totalBytes > 0 }?.let {
                    "\n${formatBytes(it.downloadedBytes)} / ${formatBytes(it.totalBytes)}"
                }.orEmpty()
                text = "${download.filename}\n${statusLabel(status)}$progressLabel"
                textSize = 15f
                setTextColor(getColor(R.color.phnx_text))
            }, LinearLayout.LayoutParams(0, -2, 1f))
            if (status == DownloadStatus.COMPLETED && app.downloadManager.uri(download.downloadId) != null) {
                row.addView(Button(this).apply {
                    text = getString(R.string.download_open)
                    setOnClickListener {
                        val uri = app.downloadManager.uri(download.downloadId) ?: return@setOnClickListener
                        startActivity(Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, download.mimeType.ifBlank { "*/*" })
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        })
                    }
                })
                row.addView(Button(this).apply {
                    text = getString(R.string.download_share)
                    setOnClickListener {
                        val uri = app.downloadManager.uri(download.downloadId) ?: return@setOnClickListener
                        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                            type = download.mimeType.ifBlank { "*/*" }
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }, getString(R.string.share)))
                    }
                })
            }
            row.addView(Button(this).apply {
                text = getString(R.string.download_remove)
                setOnClickListener {
                    app.downloadManager.remove(profileId, download)
                    refresh()
                }
            })
            list.addView(row)
        }
    }

    private fun statusLabel(status: DownloadStatus): String = when (status) {
        DownloadStatus.QUEUED -> getString(R.string.download_queued)
        DownloadStatus.DOWNLOADING -> getString(R.string.download_downloading)
        DownloadStatus.PAUSED -> getString(R.string.download_paused)
        DownloadStatus.COMPLETED -> getString(R.string.download_completed)
        DownloadStatus.MISSING -> getString(R.string.download_missing)
        is DownloadStatus.FAILED -> getString(R.string.download_failed)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun formatBytes(bytes: Long): String = when {
        bytes >= 1024L * 1024L -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
        bytes >= 1024L -> "%.1f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }
}
