package com.phoenix.phnx.resources

import android.content.Context

data class CrashRecoveryState(
    val startupFailures: Int,
    val rendererCrashes: Int,
    val recoveryMode: Boolean,
)

class CrashRecoveryManager(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(STORE, Context.MODE_PRIVATE)

    fun beginLaunch(now: Long = System.currentTimeMillis()): CrashRecoveryState {
        val wasInProgress = preferences.getBoolean(KEY_IN_PROGRESS, false)
        val previousLaunch = preferences.getLong(KEY_LAST_LAUNCH, 0L)
        val previousFailures = preferences.getInt(KEY_STARTUP_FAILURES, 0)
        val failures = if (wasInProgress && now - previousLaunch < FAILURE_WINDOW_MILLIS) {
            previousFailures + 1
        } else {
            0
        }
        preferences.edit()
            .putBoolean(KEY_IN_PROGRESS, true)
            .putLong(KEY_LAST_LAUNCH, now)
            .putInt(KEY_STARTUP_FAILURES, failures)
            .apply()
        return state()
    }

    fun markHealthy() {
        preferences.edit().putBoolean(KEY_IN_PROGRESS, false).putInt(KEY_STARTUP_FAILURES, 0).apply()
    }

    fun recordRendererCrash(now: Long = System.currentTimeMillis()): CrashRecoveryState {
        val previous = preferences.getLong(KEY_LAST_RENDERER_CRASH, 0L)
        val count = if (now - previous < FAILURE_WINDOW_MILLIS) {
            preferences.getInt(KEY_RENDERER_CRASHES, 0) + 1
        } else {
            1
        }
        preferences.edit().putLong(KEY_LAST_RENDERER_CRASH, now).putInt(KEY_RENDERER_CRASHES, count).apply()
        return state()
    }

    fun state(): CrashRecoveryState = CrashRecoveryState(
        startupFailures = preferences.getInt(KEY_STARTUP_FAILURES, 0),
        rendererCrashes = preferences.getInt(KEY_RENDERER_CRASHES, 0),
        recoveryMode = preferences.getInt(KEY_STARTUP_FAILURES, 0) >= MAX_FAILURES ||
            preferences.getInt(KEY_RENDERER_CRASHES, 0) >= MAX_FAILURES,
    )

    private companion object {
        const val STORE = "phnx_crash_recovery"
        const val KEY_IN_PROGRESS = "launch_in_progress"
        const val KEY_LAST_LAUNCH = "last_launch"
        const val KEY_STARTUP_FAILURES = "startup_failures"
        const val KEY_LAST_RENDERER_CRASH = "last_renderer_crash"
        const val KEY_RENDERER_CRASHES = "renderer_crashes"
        const val FAILURE_WINDOW_MILLIS = 5 * 60 * 1000L
        const val MAX_FAILURES = 3
    }
}
