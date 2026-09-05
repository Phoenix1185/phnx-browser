package com.phoenix.phnx

import android.app.Application
import android.webkit.WebView
import com.phoenix.phnx.identity.DeviceProfileManager
import com.phoenix.phnx.network.NetworkManager
import com.phoenix.phnx.profiles.ProfileManager
import com.phoenix.phnx.resources.AndroidResourceMonitor
import com.phoenix.phnx.resources.ResourceManager

class PhnxApplication : Application() {
    lateinit var profileManager: ProfileManager
        private set
    lateinit var networkManager: NetworkManager
        private set
    lateinit var deviceProfileManager: DeviceProfileManager
        private set
    lateinit var resourceMonitor: AndroidResourceMonitor
        private set
    lateinit var resourceManager: ResourceManager
        private set

    override fun onCreate() {
        super.onCreate()
        PhnxPreferences.applyTheme(this)
        profileManager = ProfileManager(this)
        val activeProfile = profileManager.ensureDefaultProfile()
        networkManager = NetworkManager(this)
        networkManager.getConfig(activeProfile.id)
        deviceProfileManager = DeviceProfileManager(this)
        deviceProfileManager.getProfileConfiguration(activeProfile.id)
        resourceMonitor = AndroidResourceMonitor(this)
        resourceManager = ResourceManager(snapshotProvider = resourceMonitor::currentSnapshot)
        WebView.setDataDirectorySuffix(activeProfile.id)
    }
}
