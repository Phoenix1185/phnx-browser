package com.phoenix.phnx.downloads

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.phoenix.phnx.PhnxApplication
import com.phoenix.phnx.R

class DownloadsActivity : AppCompatActivity() {
    private data class ProgressSample(val bytes: Long, val atMillis: Long)

    private val app by lazy { application as PhnxApplication }
    private val profileId by lazy { app.profileManager.activeProfile().id }
    private val refreshHandler = Handler(Looper.getMainLooper())
    private val progressSamples = mutableMapOf<Long, ProgressSample>()
    private val refreshRunnable = object : Runnable {
        override fun run() {
            if (!isFinishing && !isDestroyed) {
                refresh()
                refreshHandler.postDelayed(this, REFRESH_INTERVAL_MILLIS)
            }
        }
    }
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
        if (::list.isInitialized) {
            refreshHandler.removeCallbacks(refreshRunnable)
            refreshRunnable.run()
        }
    }

    override fun onPause() {
        refreshHandler.removeCallbacks(refreshRunnable)
        super.onPause()
    }

    private fun refresh() {
        list.removeAllViews()
        val downloads = app.downloadManager.getForProfile(profileId)
        if (downloads.isEmpty()) {
            progressSamples.clear()
            list.addView(TextView(this).apply {
                text = getString(R.string.no_downloads)
                textSize = 16f
                setTextColor(getColor(R.color.phnx_muted))
                setPadding(0, dp(18), 0, 0)
            })
            return
        }
        progressSamples.keys.retainAll(downloads.mapTo(mutableSetOf()) { it.downloadId })
        downloads.forEach { download ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(12), 0, dp(12))
            }
            val status = app.downloadManager.query(download.downloadId)
            val progress = app.downloadManager.progress(download.downloadId)
            row.addView(TextView(this).apply {
                text = buildDownloadLabel(download.downloadId, download.filename, status, progress)
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

    private fun buildDownloadLabel(
        downloadId: Long,
        filename: String,
        status: DownloadStatus,
        progress: DownloadProgress?,
    ): String {
        val progressLabel = progress?.let { current ->
            val bytesLabel = formatBytes(current.downloadedBytes)
            if (current.totalBytes > 0) {
                val percent = ((current.downloadedBytes * 100L) / current.totalBytes)
                    .coerceIn(0L, 100L)
                    .toInt()
                val details = getString(
                    R.string.download_progress,
                    percent,
                    bytesLabel,
                    formatBytes(current.totalBytes),
                )
                val estimate = estimate(downloadId = downloadId, progress = current, status = status)
                if (estimate != null) "$details\n$estimate" else details
            } else {
                getString(R.string.download_progress_unknown, bytesLabel)
            }
        }
        return buildString {
            append(filename)
            append('\n')
            append(statusLabel(status))
            if (!progressLabel.isNullOrBlank()) {
                append('\n')
                append(progressLabel)
            }
        }
    }

    private fun estimate(downloadId: Long, progress: DownloadProgress, status: DownloadStatus): String? {
        val now = SystemClock.elapsedRealtime()
        val previous = progressSamples.put(
            downloadId,
            ProgressSample(progress.downloadedBytes, now),
        )
        if (status == DownloadStatus.PAUSED) return getString(R.string.download_eta_paused)
        if (status != DownloadStatus.DOWNLOADING) return null
        val sample = previous ?: return getString(R.string.download_eta_calculating)
        val elapsed = now - sample.atMillis
        val bytesPerSecond = if (elapsed > 0 && progress.downloadedBytes > sample.bytes) {
            ((progress.downloadedBytes - sample.bytes) * 1000L / elapsed).coerceAtLeast(1L)
        } else {
            0L
        }
        if (bytesPerSecond == 0L) return getString(R.string.download_eta_calculating)
        val remainingBytes = (progress.totalBytes - progress.downloadedBytes).coerceAtLeast(0L)
        val remainingSeconds = (remainingBytes + bytesPerSecond - 1L) / bytesPerSecond
        val eta = getString(R.string.download_eta, formatDuration(remainingSeconds))
        return "$eta\n${getString(R.string.download_speed, formatBytes(bytesPerSecond))}"
    }

    private fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600L
        val minutes = (seconds % 3600L) / 60L
        val remainingSeconds = seconds % 60L
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${remainingSeconds}s"
            else -> "${remainingSeconds}s"
        }
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes >= 1024L * 1024L -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
        bytes >= 1024L -> "%.1f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }

    private companion object {
        const val REFRESH_INTERVAL_MILLIS = 1000L
    }
}
