package com.phoenix.phnx.resources

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.phoenix.phnx.PhnxApplication
import com.phoenix.phnx.R

class PerformanceActivity : AppCompatActivity() {
    private val app by lazy { application as PhnxApplication }
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
        refresh()
    }

    private fun refresh() {
        content.removeAllViews()
        content.addView(TextView(this).apply {
            text = getString(R.string.resource_snapshot)
            textSize = 30f
            setTextColor(getColor(R.color.phnx_blue))
        })
        content.addView(Button(this).apply {
            text = getString(R.string.refresh)
            setOnClickListener { refresh() }
        })

        val snapshot = app.resourceMonitor.currentSnapshot()
        content.addView(metric(getString(R.string.memory_pressure, snapshot.memoryPressure)))
        content.addView(metric(
            if (snapshot.thermalSupported) {
                getString(R.string.thermal_status, snapshot.thermalLevel)
            } else {
                getString(R.string.thermal_unsupported)
            },
        ))
        content.addView(metric(getString(R.string.battery_status, snapshot.batteryPercent)))
        content.addView(metric(if (snapshot.batterySaver) getString(R.string.battery_saver_on) else getString(R.string.battery_saver_off)))
        content.addView(metric(getString(R.string.cpu_status)))

        content.addView(TextView(this).apply {
            text = getString(R.string.profile_lifecycle)
            textSize = 22f
            setTextColor(getColor(R.color.phnx_blue))
            setPadding(0, dp(24), 0, dp(6))
        })
        app.profileManager.getAllProfiles().forEach { profile ->
            content.addView(metric(getString(R.string.profile_state, profile.name, profile.status)))
        }
    }

    private fun metric(value: String): TextView = TextView(this).apply {
        text = value
        textSize = 16f
        setTextColor(getColor(R.color.phnx_text))
        setPadding(0, dp(8), 0, dp(8))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
