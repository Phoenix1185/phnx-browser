package com.phoenix.phnx

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object PhnxPreferences {
    const val STORE = "phnx_preferences"
    const val DATA_SAVER_ENABLED = "data_saver_enabled"
    const val DESKTOP_SITE_ENABLED = "desktop_site_enabled"
    const val THEME_MODE = "theme_mode"
    const val THEME_SYSTEM = "system"
    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"

    fun store(context: Context) = context.getSharedPreferences(STORE, Context.MODE_PRIVATE)

    fun themeMode(context: Context): String = store(context).getString(THEME_MODE, THEME_SYSTEM) ?: THEME_SYSTEM

    fun applyTheme(context: Context) {
        AppCompatDelegate.setDefaultNightMode(themeNightMode(themeMode(context)))
    }

    fun setTheme(context: Context, mode: String) {
        store(context).edit().putString(THEME_MODE, mode).apply()
        AppCompatDelegate.setDefaultNightMode(themeNightMode(mode))
    }

    private fun themeNightMode(mode: String): Int = when (mode) {
        THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
        THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }
}
