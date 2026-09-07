package com.phoenix.phnx.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.phoenix.phnx.BuildConfig
import com.phoenix.phnx.R
import com.phoenix.phnx.update.UpdateManifest
import com.phoenix.phnx.update.UpdateManifestParser
import com.phoenix.phnx.update.VersionComparator
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class UpdateActivity : AppCompatActivity() {
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private lateinit var status: TextView
    private lateinit var releaseButton: Button
    private var releaseUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Updates"

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(28), dp(24), dp(32))
            setBackgroundColor(getColor(R.color.phnx_cream))
        }
        content.addView(text("PHNX Updates", 28f, true))
        content.addView(text("Current version: ${BuildConfig.VERSION_NAME}\nThis checker reads release metadata from the official Phoenix repository. It does not silently install software.", 15f, false))
        status = text("Checking for updates...", 16f, false)
        content.addView(status)
        releaseButton = Button(this).apply {
            text = "Open official release page"
            isEnabled = false
            setOnClickListener { releaseUrl?.let(::openRelease) }
        }
        content.addView(releaseButton)
        content.addView(text("Only download builds from a source you trust. Review Android's install and permission prompts before installing an update.", 14f, false))

        setContentView(ScrollView(this).apply {
            setBackgroundColor(getColor(R.color.phnx_cream))
            addView(content)
        })
        checkForUpdates()
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun checkForUpdates() {
        executor.execute {
            val result = runCatching { fetchLatestRelease() }
            runOnUiThread {
                result.onSuccess { release -> showRelease(release) }
                    .onFailure { error ->
                        status.text = "Could not check for updates: ${error.message ?: error.javaClass.simpleName}."
                    }
            }
        }
    }

    private fun fetchLatestRelease(): ReleaseInfo {
        val connection = URL(LATEST_RELEASE_API).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = TIMEOUT_MILLIS
            connection.readTimeout = TIMEOUT_MILLIS
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "PHNX-Browser/${BuildConfig.VERSION_NAME}")
            if (connection.responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                return ReleaseInfo("No published release", "", REPOSITORY_URL)
            }
            if (connection.responseCode !in 200..299) {
                error("Release service returned HTTP ${connection.responseCode}")
            }
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            ReleaseInfo(
                name = json.optString("name").ifBlank { json.optString("tag_name") },
                tag = json.optString("tag_name"),
                url = json.optString("html_url"),
                manifest = fetchManifest(json.optJSONArray("assets")),
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun fetchManifest(assets: JSONArray?): UpdateManifest? {
        for (index in 0 until (assets?.length() ?: 0)) {
            val asset = assets?.optJSONObject(index) ?: continue
            if (asset.optString("name") != MANIFEST_ASSET_NAME) continue
            val url = asset.optString("browser_download_url").takeIf { it.startsWith("https://") }
                ?: return null
            val connection = URL(url).openConnection() as HttpURLConnection
            return try {
                connection.connectTimeout = TIMEOUT_MILLIS
                connection.readTimeout = TIMEOUT_MILLIS
                connection.setRequestProperty("Accept", "application/octet-stream")
                connection.setRequestProperty("User-Agent", "PHNX-Browser/${BuildConfig.VERSION_NAME}")
                if (connection.responseCode !in 200..299) return null
                UpdateManifestParser.parse(connection.inputStream.bufferedReader().use { it.readText() })
            } finally {
                connection.disconnect()
            }
        }
        return null
    }

    private fun showRelease(release: ReleaseInfo) {
        releaseUrl = release.url.takeIf { it.startsWith("https://") }
        if (release.tag.isBlank()) {
            status.text = "No published release is available yet. Check the official repository for project updates."
            releaseButton.text = "Open Phoenix repository"
            releaseButton.isEnabled = releaseUrl != null
            return
        }
        if (release.manifest == null) {
            status.text = "Published release has no valid signed update manifest."
            releaseButton.text = "Open official release page"
            releaseButton.isEnabled = releaseUrl != null
            return
        }
        val current = BuildConfig.VERSION_NAME.removeSuffix("-debug").removePrefix("v")
        val latest = release.manifest.latestVersion.removePrefix("v")
        releaseButton.text = "Open official release page"
        status.text = if (!VersionComparator.isNewer(latest, current)) {
            "You are up to date (${release.name})."
        } else {
            "Latest published release: ${release.name}\nA different release is available for review."
        }
        releaseButton.isEnabled = releaseUrl != null
    }

    private fun openRelease(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun text(value: String, size: Float, prominent: Boolean): TextView = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(if (prominent) getColor(R.color.phnx_blue) else getColor(R.color.phnx_text))
        setPadding(0, if (prominent) 0 else dp(12), 0, 0)
        setTextIsSelectable(true)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private data class ReleaseInfo(
        val name: String,
        val tag: String,
        val url: String,
        val manifest: UpdateManifest? = null,
    )

    private companion object {
        const val MANIFEST_ASSET_NAME = "phnx-update-manifest.json"
        const val LATEST_RELEASE_API = "https://api.github.com/repos/Phoenix1185/phnx-browser/releases/latest"
        const val REPOSITORY_URL = "https://github.com/Phoenix1185/phnx-browser"
        const val TIMEOUT_MILLIS = 10_000
    }
}
