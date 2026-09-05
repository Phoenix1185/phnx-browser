package com.phoenix.phnx.resources

import android.os.Bundle
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
        val profiles = app.profileManager.getAllProfiles()
        val activeProfileId = app.profileManager.activeProfile().id
        val states = profiles.map { profile ->
            ProfileResourceState(
                profileId = profile.id,
                lifecycleState = profile.status.toLifecycleState(),
                lastActiveTime = profile.lastUsedAt,
                activeTabCount = if (profile.id == activeProfileId) 1 else 0,
                foreground = profile.id == activeProfileId,
                userPinned = false,
            )
        }
        app.resourceManager.evaluate(states).forEach { decision ->
            val status = when (decision.to) {
                ProfileLifecycleState.ACTIVE -> ProfileStatus.ACTIVE
                ProfileLifecycleState.IDLE -> ProfileStatus.IDLE
                ProfileLifecycleState.FROZEN -> ProfileStatus.FROZEN
                ProfileLifecycleState.SUSPENDED -> ProfileStatus.SUSPENDED
                ProfileLifecycleState.RECREATING -> ProfileStatus.RECREATING
                ProfileLifecycleState.CLOSED -> ProfileStatus.CLOSED
            }
            app.profileManager.updateStatus(decision.profileId, status)
        }
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
        content.addView(metric(getString(R.string.resource_updated, DateFormat.getTimeInstance().format(Date()))))

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
        content.addView(metric(snapshot.cpuPercent?.let {
            getString(R.string.cpu_usage, String.format(Locale.US, "%.1f", it))
        } ?: getString(R.string.cpu_sampling)))

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

    private fun ProfileStatus.toLifecycleState(): ProfileLifecycleState = when (this) {
        ProfileStatus.ACTIVE -> ProfileLifecycleState.ACTIVE
        ProfileStatus.IDLE -> ProfileLifecycleState.IDLE
        ProfileStatus.FROZEN -> ProfileLifecycleState.FROZEN
        ProfileStatus.SUSPENDED -> ProfileLifecycleState.SUSPENDED
        ProfileStatus.RECREATING -> ProfileLifecycleState.RECREATING
        ProfileStatus.CLOSED -> ProfileLifecycleState.CLOSED
    }

    private fun metric(value: String): TextView = TextView(this).apply {
        text = value
        textSize = 16f
        setTextColor(getColor(R.color.phnx_text))
        setPadding(0, dp(8), 0, dp(8))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
