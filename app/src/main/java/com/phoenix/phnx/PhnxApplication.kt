package com.phoenix.phnx

import android.app.Application
import android.os.Build
import android.webkit.WebView
import com.phoenix.phnx.profiles.ProfileManager

class PhnxApplication : Application() {
    lateinit var profileManager: ProfileManager
        private set

    override fun onCreate() {
        super.onCreate()
        PhnxPreferences.applyTheme(this)
        profileManager = ProfileManager(this)
        val activeProfile = profileManager.ensureDefaultProfile()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WebView.setDataDirectorySuffix(activeProfile.id)
        }
    }
}
