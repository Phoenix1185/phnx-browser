package com.phoenix.phnx

import android.app.Application
import android.webkit.WebView
import com.phoenix.phnx.network.NetworkManager
import com.phoenix.phnx.profiles.ProfileManager

class PhnxApplication : Application() {
    lateinit var profileManager: ProfileManager
        private set
    lateinit var networkManager: NetworkManager
        private set

    override fun onCreate() {
        super.onCreate()
        PhnxPreferences.applyTheme(this)
        profileManager = ProfileManager(this)
        val activeProfile = profileManager.ensureDefaultProfile()
        networkManager = NetworkManager(this)
        networkManager.getConfig(activeProfile.id)
        WebView.setDataDirectorySuffix(activeProfile.id)
    }
}
