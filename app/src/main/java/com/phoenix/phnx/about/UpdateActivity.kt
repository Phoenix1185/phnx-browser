package com.phoenix.phnx.about

import android.content.Intent
import android.os.Build
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.phoenix.phnx.BuildConfig
import com.phoenix.phnx.R
import com.phoenix.phnx.update.UpdateManifest
import com.phoenix.phnx.update.UpdateInstaller
import com.phoenix.phnx.update.UpdateService
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class UpdateActivity : AppCompatActivity() {
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private lateinit var status: TextView
    private lateinit var releaseButton: Button
    private lateinit var installButton: Button
    private var releaseUrl: String? = null
    private var latestManifest: UpdateManifest? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.update_title)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(28), dp(24), dp(32))
            setBackgroundColor(getColor(R.color.phnx_cream))
        }
        content.addView(text(getString(R.string.update_title), 28f, true))
        content.addView(text(getString(R.string.update_current_version, BuildConfig.VERSION_NAME), 15f, false))
        status = text(getString(R.string.update_checking), 16f, false)
        content.addView(status)
        installButton = Button(this).apply {
            text = getString(R.string.update_download_install)
            isEnabled = false
            isAllCaps = false
            setOnClickListener { downloadAndInstall() }
        }
        content.addView(installButton)
        releaseButton = Button(this).apply {
            text = getString(R.string.update_open_release)
            isEnabled = false
            isAllCaps = false
            setOnClickListener { releaseUrl?.let(::openRelease) }
        }
        content.addView(releaseButton)
        content.addView(text(getString(R.string.update_only_trusted), 14f, false))

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
            val result = runCatching { UpdateService.fetchLatestRelease() }
            runOnUiThread {
                result.onSuccess { release -> showRelease(release) }
                    .onFailure { error ->
                        status.text = getString(
                            R.string.update_check_failed,
                            error.message ?: error.javaClass.simpleName,
                        )
                    }
            }
        }
    }

    private fun showRelease(release: UpdateService.ReleaseInfo) {
        releaseUrl = release.url.takeIf { it.startsWith("https://") }
        latestManifest = null
        releaseButton.isEnabled = releaseUrl != null
        if (release.tag.isBlank()) {
            status.text = getString(R.string.update_no_release)
            return
        }
        if (release.manifest == null) {
            status.text = getString(R.string.update_no_manifest)
            return
        }
        status.text = if (!UpdateService.isNewer(release)) {
            getString(R.string.update_up_to_date, release.name)
        } else {
            latestManifest = release.manifest
            installButton.isEnabled = true
            getString(R.string.update_available, release.name)
        }
    }

    private fun downloadAndInstall() {
        val manifest = latestManifest ?: return
        installButton.isEnabled = false
        status.text = getString(R.string.update_downloading)
        executor.execute {
            val result = runCatching { UpdateService.downloadAndVerify(this@UpdateActivity, manifest) }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                result.onSuccess { apk ->
                    status.text = getString(R.string.update_ready)
                    if (!launchInstaller(apk)) installButton.isEnabled = true
                }.onFailure { error ->
                    status.text = getString(
                        R.string.update_download_failed,
                        error.message ?: error.javaClass.simpleName,
                    )
                    installButton.isEnabled = true
                }
            }
        }
    }

    private fun openRelease(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun launchInstaller(apk: File): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManager.canRequestPackageInstalls()) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:$packageName"),
                ),
            )
            status.text = getString(R.string.update_allow_installs)
            return false
        }
        val intent = UpdateInstaller.installIntent(this, apk) ?: run {
            status.text = getString(R.string.update_install_failed)
            return false
        }
        return runCatching { startActivity(intent) }.onFailure {
            status.text = getString(R.string.update_install_failed)
        }.isSuccess
    }

    private fun text(value: String, size: Float, prominent: Boolean): TextView = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(if (prominent) getColor(R.color.phnx_blue) else getColor(R.color.phnx_text))
        setPadding(0, if (prominent) 0 else dp(12), 0, 0)
        setTextIsSelectable(true)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

}
