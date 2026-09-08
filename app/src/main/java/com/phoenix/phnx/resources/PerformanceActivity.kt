package com.phoenix.phnx.resources

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.phoenix.phnx.PhnxApplication
import com.phoenix.phnx.R
import com.phoenix.phnx.profiles.ProfileStatus
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class PerformanceActivity : AppCompatActivity() {
    private val app by lazy { application as PhnxApplication }
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshInFlight = AtomicBoolean(false)
    private var started = false
    private val refreshTask = object : Runnable {
        override fun run() {
            if (started && !isFinishing) {
                refreshAsync()
                refreshHandler.postDelayed(this, REFRESH_INTERVAL_MILLIS)
            }
        }
    }
    private lateinit var content: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.performance)
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(24))
            setBackgroundColor(getColor(R.color.phnx_cream))
        }
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
        executor.execute {
            try {
                val profiles = app.profileManager.getAllProfiles()
                val snapshot = app.resourceMonitor.currentSnapshot()
                val profileStatuses = profiles.map { it.name to it.status }
                runOnUiThread {
                    if (started && !isFinishing) renderSnapshot(snapshot, profileStatuses)
                }
            } finally {
                refreshInFlight.set(false)
            }
        }
    }

    private fun renderSnapshot(snapshot: ResourceSnapshot, profileStatuses: List<Pair<String, ProfileStatus>>) {
        content.removeAllViews()
        content.addView(TextView(this).apply {
            text = getString(R.string.resource_snapshot)
            textSize = 30f
            setTextColor(getColor(R.color.phnx_blue))
        })
        content.addView(Button(this).apply {
            text = getString(R.string.refresh)
            setOnClickListener { refreshAsync() }
        })
        content.addView(metric(getString(R.string.resource_updated, DateFormat.getTimeInstance().format(Date()))))

        content.addView(metric(getString(R.string.memory_pressure, snapshot.memoryPressure)))
        snapshot.appPssMb?.let { content.addView(metric(getString(R.string.app_memory, it))) }
        content.addView(metric(
            if (snapshot.thermalSupported) {
                getString(R.string.thermal_status, snapshot.thermalLevel)
            } else {
                getString(R.string.thermal_unsupported)
            },
        ))
        content.addView(metric(getString(R.string.battery_status, snapshot.batteryPercent)))
        content.addView(metric(if (snapshot.batterySaver) getString(R.string.battery_saver_on) else getString(R.string.battery_saver_off)))
        content.addView(metric(getString(R.string.performance_mode_status, snapshot.performanceMode)))
        content.addView(metric(snapshot.cpuPercent?.let {
            getString(R.string.cpu_usage, String.format(Locale.US, "%.1f", it))
        } ?: getString(R.string.cpu_sampling)))

        content.addView(TextView(this).apply {
            text = getString(R.string.profile_lifecycle)
            textSize = 22f
            setTextColor(getColor(R.color.phnx_blue))
            setPadding(0, dp(24), 0, dp(6))
        })
        profileStatuses.forEach { (name, status) ->
            content.addView(metric(getString(R.string.profile_state, name, status)))
        }
    }

    private fun metric(value: String): TextView = TextView(this).apply {
        text = value
        textSize = 16f
        setTextColor(getColor(R.color.phnx_text))
        setPadding(0, dp(8), 0, dp(8))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val REFRESH_INTERVAL_MILLIS = 2_500L
    }
}
