package com.phoenix.phnx.resources

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.phoenix.phnx.BuildConfig
import com.phoenix.phnx.PhnxApplication
import com.phoenix.phnx.R
import com.phoenix.phnx.profiles.ProfileStatus
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

class PerformanceActivity : AppCompatActivity() {
    private val app by lazy { application as PhnxApplication }
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshInFlight = AtomicBoolean(false)
    private var started = false
    private var diagnosticsExpanded = false
    private var latestSnapshot: ResourceSnapshot? = null
    private lateinit var statusView: TextView
    private lateinit var updatedView: TextView
    private lateinit var warningsContainer: LinearLayout
    private lateinit var coreMetrics: List<TextView>
    private lateinit var sessionMetrics: List<TextView>
    private lateinit var deviceMetrics: List<TextView>
    private lateinit var profileContainer: LinearLayout
    private lateinit var diagnosticsButton: Button
    private lateinit var diagnosticsView: TextView
    private val refreshTask = object : Runnable {
        override fun run() {
            if (started && !isFinishing) {
                refreshAsync()
                refreshHandler.postDelayed(this, REFRESH_INTERVAL_MILLIS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.performance)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(24))
            setBackgroundColor(getColor(R.color.phnx_cream))
        }
        content.addView(sectionTitle(R.string.resource_snapshot, 30f))
        statusView = metric("").apply { setTypeface(null, android.graphics.Typeface.BOLD) }
        content.addView(statusView)

        content.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(Button(this@PerformanceActivity).apply {
                text = getString(R.string.refresh)
                setOnClickListener { refreshAsync() }
            })
        })
        updatedView = metric("")
        content.addView(updatedView)

        warningsContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(warningsContainer)

        content.addView(sectionTitle(R.string.current_resources))
        coreMetrics = listOf(
            metric(""),
            metric(""),
            metric(""),
            metric(""),
            metric(""),
            metric(""),
            metric(""),
            metric(""),
        )
        coreMetrics.forEach(content::addView)

        content.addView(sectionTitle(R.string.session_diagnostics))
        sessionMetrics = listOf(metric(""), metric(""), metric(""))
        sessionMetrics.forEach(content::addView)

        content.addView(sectionTitle(R.string.device_state))
        deviceMetrics = listOf(metric(""), metric(""))
        deviceMetrics.forEach(content::addView)

        content.addView(sectionTitle(R.string.profile_lifecycle))
        profileContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(profileContainer)

        diagnosticsButton = Button(this).apply {
            text = getString(R.string.detailed_diagnostics)
            setAllCaps(false)
            setOnClickListener {
                diagnosticsExpanded = !diagnosticsExpanded
                diagnosticsView.visibility = if (diagnosticsExpanded) android.view.View.VISIBLE else android.view.View.GONE
                text = getString(if (diagnosticsExpanded) R.string.hide_detailed_diagnostics else R.string.detailed_diagnostics)
                if (diagnosticsExpanded) latestSnapshot?.let { diagnosticsView.text = diagnosticsText(it) }
            }
        }
        content.addView(diagnosticsButton)
        diagnosticsView = metric("").apply {
            setTextColor(getColor(R.color.phnx_muted))
            visibility = android.view.View.GONE
        }
        content.addView(diagnosticsView)

        setContentView(ScrollView(this).apply {
            setBackgroundColor(getColor(R.color.phnx_cream))
            addView(content)
        })
    }

    override fun onStart() {
        super.onStart()
        started = true
        refreshHandler.post(refreshTask)
    }

    override fun onStop() {
        started = false
        refreshHandler.removeCallbacks(refreshTask)
        super.onStop()
    }

    override fun onDestroy() {
        started = false
        refreshHandler.removeCallbacksAndMessages(null)
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun refreshAsync() {
        if (!refreshInFlight.compareAndSet(false, true)) return
        val tabDiagnostics = app.currentTabDiagnostics()
        executor.execute {
            try {
                val profiles = app.profileManager.getAllProfiles()
                val snapshot = app.resourceMonitor.currentSnapshot()
                val profileStatuses = profiles.map { it.name to it.status }
                runOnUiThread {
                    if (started && !isFinishing) renderSnapshot(snapshot, profileStatuses, tabDiagnostics)
                }
            } finally {
                refreshInFlight.set(false)
            }
        }
    }

    private fun renderSnapshot(
        snapshot: ResourceSnapshot,
        profileStatuses: List<Pair<String, ProfileStatus>>,
        tabDiagnostics: TabDiagnostics,
    ) {
        latestSnapshot = snapshot
        val overallSeverity = snapshot.warnings.maxOfOrNull { it.severity.ordinal }
        val overallState = when (overallSeverity) {
            ResourceWarningSeverity.CRITICAL.ordinal -> getString(R.string.critical)
            ResourceWarningSeverity.WARNING.ordinal -> getString(R.string.warning)
            else -> getString(R.string.normal)
        }
        statusView.text = getString(R.string.resource_state, overallState)
        statusView.setTextColor(getColor(statusColor(overallSeverity)))
        updatedView.text = getString(R.string.resource_updated, DateFormat.getTimeInstance().format(Date()))

        coreMetrics[0].text = getString(R.string.memory_pressure, snapshot.memoryPressure)
        coreMetrics[1].text = snapshot.appPssMb?.let { current ->
            getString(R.string.app_memory_range, current, snapshot.peakAppPssMb ?: current)
        } ?: getString(R.string.app_memory_unavailable)
        coreMetrics[2].text = snapshot.cpuPercent?.let { current ->
            getString(
                R.string.cpu_usage_range,
                formatDecimal(current),
                formatDecimal(snapshot.peakCpuPercent ?: current),
            )
        } ?: getString(R.string.cpu_sampling)
        coreMetrics[3].text = if (snapshot.thermalSupported) {
            getString(R.string.thermal_status, snapshot.thermalLevel)
        } else {
            getString(R.string.thermal_unsupported)
        }
        coreMetrics[4].text = getString(R.string.battery_status, snapshot.batteryPercent)
        coreMetrics[5].text = if (snapshot.batterySaver) {
            getString(R.string.battery_saver_on)
        } else {
            getString(R.string.battery_saver_off)
        }
        coreMetrics[6].text = getString(R.string.performance_mode_status, snapshot.performanceMode)
        coreMetrics[7].text = networkLabel(snapshot)

        sessionMetrics[0].text = getString(R.string.session_uptime, formatDuration(snapshot.sessionUptimeMillis))
        sessionMetrics[1].text = if (tabDiagnostics.available) {
            getString(R.string.open_tabs_diagnostics, tabDiagnostics.openTabs)
        } else {
            getString(R.string.open_tabs_unavailable)
        }
        sessionMetrics[2].text = if (tabDiagnostics.available) {
            getString(R.string.loaded_pages, tabDiagnostics.loadedPages)
        } else {
            getString(R.string.loaded_pages_unavailable)
        }

        deviceMetrics[0].text = getString(R.string.process_state, processStateLabel(snapshot))
        deviceMetrics[1].text = getString(R.string.memory_available, snapshot.availableRamMb ?: 0, snapshot.totalRamMb ?: 0)

        renderWarnings(snapshot)
        profileContainer.removeAllViews()
        profileStatuses.forEach { (name, status) ->
            profileContainer.addView(metric(getString(R.string.profile_state, name, status)))
        }
        if (diagnosticsExpanded) diagnosticsView.text = diagnosticsText(snapshot)
    }

    private fun renderWarnings(snapshot: ResourceSnapshot) {
        warningsContainer.removeAllViews()
        if (snapshot.warnings.isEmpty()) {
            warningsContainer.visibility = android.view.View.GONE
            return
        }
        warningsContainer.visibility = android.view.View.VISIBLE
        warningsContainer.addView(sectionTitle(R.string.warnings))
        snapshot.warnings.forEach { warning ->
            val message = when (warning.type) {
                ResourceWarningType.HIGH_CPU -> getString(R.string.warning_high_cpu)
                ResourceWarningType.HIGH_MEMORY -> getString(R.string.warning_high_memory)
                ResourceWarningType.MEMORY_PRESSURE -> getString(R.string.warning_memory_pressure)
                ResourceWarningType.THERMAL -> getString(R.string.warning_thermal)
                ResourceWarningType.LOW_BATTERY -> getString(R.string.warning_low_battery)
            }
            warningsContainer.addView(metric(
                getString(R.string.warning_item, warning.severity, message),
            ).apply {
                setTextColor(getColor(statusColor(warning.severity.ordinal)))
            })
        }
    }

    private fun diagnosticsText(snapshot: ResourceSnapshot): String {
        val packageInfo = runCatching { packageManager.getPackageInfo(packageName, 0) }.getOrNull()
        val webViewPackage = WebView.getCurrentWebViewPackage()
        val processMemory = getString(
            R.string.diagnostic_process_memory,
            snapshot.appPssMb ?: 0,
            snapshot.appDalvikPssMb ?: 0,
            snapshot.appNativePssMb ?: 0,
            snapshot.appOtherPssMb ?: 0,
        )
        return listOf(
            getString(R.string.diagnostic_android, Build.VERSION.RELEASE),
            getString(R.string.diagnostic_api, Build.VERSION.SDK_INT),
            getString(
                R.string.diagnostic_app,
                BuildConfig.VERSION_NAME,
                packageInfo?.longVersionCode ?: 0L,
            ),
            getString(
                R.string.diagnostic_webview,
                webViewPackage?.packageName ?: getString(R.string.unavailable),
                webViewPackage?.versionName ?: getString(R.string.unavailable),
            ),
            getString(R.string.diagnostic_abis, Build.SUPPORTED_ABIS.joinToString()),
            getString(R.string.diagnostic_ram, snapshot.availableRamMb ?: 0, snapshot.totalRamMb ?: 0),
            processMemory,
            getString(R.string.diagnostic_thermal, snapshot.thermalLevel),
            getString(R.string.diagnostic_performance, snapshot.performanceMode),
            getString(R.string.diagnostic_process, processStateLabel(snapshot), snapshot.processImportance ?: -1),
        ).joinToString("\n")
    }

    private fun networkLabel(snapshot: ResourceSnapshot): String {
        val state = snapshot.networkState?.name?.lowercase(Locale.US) ?: getString(R.string.unavailable)
        return if (snapshot.networkRxBytes != null && snapshot.networkTxBytes != null) {
            getString(
                R.string.network_activity,
                state,
                formatBytes(snapshot.networkRxBytes),
                formatBytes(snapshot.networkTxBytes),
            )
        } else {
            getString(R.string.network_activity_unavailable, state)
        }
    }

    private fun processStateLabel(snapshot: ResourceSnapshot): String =
        snapshot.processState.name.lowercase(Locale.US)

    private fun statusColor(severity: Int?): Int = when (severity) {
        ResourceWarningSeverity.CRITICAL.ordinal -> R.color.phnx_error
        ResourceWarningSeverity.WARNING.ordinal -> R.color.phnx_warning
        else -> R.color.phnx_blue
    }

    private fun formatDecimal(value: Double): String = String.format(Locale.US, "%.1f", value)

    private fun formatDuration(durationMillis: Long): String {
        var seconds = max(0L, durationMillis / 1000)
        val days = seconds / 86_400
        seconds %= 86_400
        val hours = seconds / 3_600
        seconds %= 3_600
        val minutes = seconds / 60
        seconds %= 60
        return when {
            days > 0 -> String.format(Locale.US, "%dd %02dh %02dm", days, hours, minutes)
            hours > 0 -> String.format(Locale.US, "%dh %02dm %02ds", hours, minutes, seconds)
            minutes > 0 -> String.format(Locale.US, "%dm %02ds", minutes, seconds)
            else -> String.format(Locale.US, "%ds", seconds)
        }
    }

    private fun formatBytes(value: Long): String = when {
        value < 1024L -> "$value B"
        value < 1024L * 1024L -> String.format(Locale.US, "%.1f KB", value / 1024.0)
        value < 1024L * 1024L * 1024L -> String.format(Locale.US, "%.1f MB", value / (1024.0 * 1024.0))
        else -> String.format(Locale.US, "%.1f GB", value / (1024.0 * 1024.0 * 1024.0))
    }

    private fun sectionTitle(@androidx.annotation.StringRes resource: Int, size: Float = 22f): TextView =
        TextView(this).apply {
            text = getString(resource)
            textSize = size
            setTextColor(getColor(R.color.phnx_blue))
            setPadding(0, dp(if (size >= 30f) 0 else 20), 0, dp(6))
        }

    private fun metric(value: String): TextView = TextView(this).apply {
        text = value
        textSize = 16f
        setTextColor(getColor(R.color.phnx_text))
        setPadding(0, dp(7), 0, dp(7))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val REFRESH_INTERVAL_MILLIS = 2_500L
    }
}
