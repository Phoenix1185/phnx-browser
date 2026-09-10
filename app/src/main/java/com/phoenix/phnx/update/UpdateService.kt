package com.phoenix.phnx.update

import android.content.Context
import com.phoenix.phnx.BuildConfig
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

object UpdateService {
    data class ReleaseInfo(
        val name: String,
        val tag: String,
        val url: String,
        val manifest: UpdateManifest? = null,
    )

    fun fetchLatestRelease(): ReleaseInfo {
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

    fun isNewer(release: ReleaseInfo): Boolean = release.manifest?.let {
        VersionComparator.isNewer(it.latestVersion, currentVersion())
    } == true

    fun downloadAndVerify(context: Context, manifest: UpdateManifest): File {
        require(manifest.updateType == UpdateType.FULL) { "Only full APK updates are supported" }
        val directory = File(context.cacheDir, "updates").apply { mkdirs() }
        val safeVersion = manifest.latestVersion.replace(UNSAFE_FILENAME, "_")
        val destination = File(directory, "phnx-update-$safeVersion.apk")
        if (destination.isFile && FileInputStream(destination).use { input ->
                ChecksumVerifier.verify(input, manifest.fullApkSha256)
            }) {
            return destination
        }

        val partial = File(directory, "$safeVersion.apk.part")
        val connection = URL(manifest.fullApkUrl).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = TIMEOUT_MILLIS
            connection.readTimeout = DOWNLOAD_TIMEOUT_MILLIS
            connection.setRequestProperty("Accept", "application/vnd.android.package-archive")
            connection.setRequestProperty("User-Agent", "PHNX-Browser/${BuildConfig.VERSION_NAME}")
            if (connection.responseCode !in 200..299) {
                error("Update service returned HTTP ${connection.responseCode}")
            }
            connection.inputStream.use { input ->
                partial.outputStream().use { output -> input.copyTo(output) }
            }
            val verified = FileInputStream(partial).use { input ->
                ChecksumVerifier.verify(input, manifest.fullApkSha256)
            }
            if (!verified) error("Downloaded update checksum did not match the signed manifest")
            if (destination.exists() && !destination.delete()) error("Could not replace the previous update")
            if (!partial.renameTo(destination)) error("Could not stage the verified update")
            destination
        } catch (error: Throwable) {
            partial.delete()
            throw error
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

    private fun currentVersion(): String = BuildConfig.VERSION_NAME
        .removeSuffix("-debug")
        .removePrefix("v")

    private const val MANIFEST_ASSET_NAME = "phnx-update-manifest.json"
    private const val LATEST_RELEASE_API = "https://api.github.com/repos/Phoenix1185/phnx-browser/releases/latest"
    private const val REPOSITORY_URL = "https://github.com/Phoenix1185/phnx-browser"
    private const val TIMEOUT_MILLIS = 10_000
    private const val DOWNLOAD_TIMEOUT_MILLIS = 60_000
    private val UNSAFE_FILENAME = Regex("[^A-Za-z0-9._-]")
}
