package com.phoenix.phnx

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.phoenix.phnx.resources.PerformanceMode

object PhnxPreferences {
    const val STORE = "phnx_preferences"
    const val DATA_SAVER_ENABLED = "data_saver_enabled"
    const val DESKTOP_SITE_ENABLED = "desktop_site_enabled"
    const val PAGE_ZOOM_PERCENT = "page_zoom_percent"
    const val TEXT_SCALE_PERCENT = "text_scale_percent"
    const val THEME_MODE = "theme_mode"
    const val THEME_SYSTEM = "system"
    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"
    const val HISTORY_RETENTION = "history_retention"
    const val HISTORY_REMEMBER = "remember"
    const val HISTORY_CLEAR_ON_CLOSE = "clear_on_close"
    const val PERFORMANCE_MODE = "performance_mode"
    const val UPDATE_MODE = "update_mode"
    const val UPDATE_MODE_AUTOMATIC = "automatic"
    const val UPDATE_MODE_MANUAL = "manual"
    const val UPDATE_LAST_CHECK_MS = "update_last_check_ms"

    fun store(context: Context) = context.getSharedPreferences(STORE, Context.MODE_PRIVATE)

    fun profileDesktopSiteEnabled(context: Context, profileId: String): Boolean {
        val preferences = store(context)
        return preferences.getBoolean(
            profileKey(DESKTOP_SITE_ENABLED, profileId),
            preferences.getBoolean(DESKTOP_SITE_ENABLED, false),
        )
    }

    fun setProfileDesktopSiteEnabled(context: Context, profileId: String, enabled: Boolean) {
        store(context).edit().putBoolean(profileKey(DESKTOP_SITE_ENABLED, profileId), enabled).apply()
    }

    fun profilePageZoomPercent(context: Context, profileId: String): Int =
        profileInt(context, PAGE_ZOOM_PERCENT, profileId, 100, 50..200)

    fun setProfilePageZoomPercent(context: Context, profileId: String, percent: Int) {
        store(context).edit().putInt(profileKey(PAGE_ZOOM_PERCENT, profileId), percent.coerceIn(50, 200)).apply()
    }

    fun profileTextScalePercent(context: Context, profileId: String): Int =
        profileInt(context, TEXT_SCALE_PERCENT, profileId, 100, 50..200)

    fun setProfileTextScalePercent(context: Context, profileId: String, percent: Int) {
        store(context).edit().putInt(profileKey(TEXT_SCALE_PERCENT, profileId), percent.coerceIn(50, 200)).apply()
    }

    fun clearProfilePageSettings(context: Context, profileId: String) {
        store(context).edit()
            .remove(profileKey(DESKTOP_SITE_ENABLED, profileId))
            .remove(profileKey(PAGE_ZOOM_PERCENT, profileId))
            .remove(profileKey(TEXT_SCALE_PERCENT, profileId))
            .apply()
    }

    fun themeMode(context: Context): String = store(context).getString(THEME_MODE, THEME_SYSTEM) ?: THEME_SYSTEM

    fun applyTheme(context: Context) {
        AppCompatDelegate.setDefaultNightMode(themeNightMode(themeMode(context)))
    }

    fun setTheme(context: Context, mode: String) {
        store(context).edit().putString(THEME_MODE, mode).apply()
        AppCompatDelegate.setDefaultNightMode(themeNightMode(mode))
    }

    fun historyRetention(context: Context): String =
        store(context).getString(HISTORY_RETENTION, HISTORY_REMEMBER) ?: HISTORY_REMEMBER

    fun performanceMode(context: Context): PerformanceMode = runCatching {
        PerformanceMode.valueOf(store(context).getString(PERFORMANCE_MODE, PerformanceMode.BALANCED.name).orEmpty())
    }.getOrDefault(PerformanceMode.BALANCED)

    fun setPerformanceMode(context: Context, mode: PerformanceMode) {
        store(context).edit().putString(PERFORMANCE_MODE, mode.name).apply()
    }

    fun updateMode(context: Context): String = store(context)
        .getString(UPDATE_MODE, UPDATE_MODE_AUTOMATIC)
        ?.takeIf { it == UPDATE_MODE_AUTOMATIC || it == UPDATE_MODE_MANUAL }
        ?: UPDATE_MODE_AUTOMATIC

    fun automaticUpdates(context: Context): Boolean = updateMode(context) == UPDATE_MODE_AUTOMATIC

    fun setUpdateMode(context: Context, mode: String) {
        val value = if (mode == UPDATE_MODE_MANUAL) UPDATE_MODE_MANUAL else UPDATE_MODE_AUTOMATIC
        store(context).edit().putString(UPDATE_MODE, value).apply()
    }

    private fun themeNightMode(mode: String): Int = when (mode) {
        THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
        THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }

    private fun profileInt(
        context: Context,
        setting: String,
        profileId: String,
        default: Int,
        validRange: IntRange,
    ): Int = store(context).getInt(profileKey(setting, profileId), default).takeIf { it in validRange } ?: default

    private fun profileKey(setting: String, profileId: String): String = "$setting.profile.$profileId"
}
