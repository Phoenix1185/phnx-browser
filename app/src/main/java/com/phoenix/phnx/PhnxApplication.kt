package com.phoenix.phnx

import android.app.Application
import com.phoenix.phnx.profiles.ProfileManager

class PhnxApplication : Application() {
    lateinit var profileManager: ProfileManager
        private set

    override fun onCreate() {
        super.onCreate()
        PhnxPreferences.applyTheme(this)
        profileManager = ProfileManager(this)
        profileManager.ensureDefaultProfile()
    }
}
