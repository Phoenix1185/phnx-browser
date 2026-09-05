package com.phoenix.phnx

import android.app.Application
import android.webkit.WebView
import com.phoenix.phnx.browser.ProfileViewPool
import com.phoenix.phnx.identity.DeviceProfileManager
import com.phoenix.phnx.network.NetworkManager
import com.phoenix.phnx.permissions.PermissionManager
import com.phoenix.phnx.profiles.ProfileManager
import com.phoenix.phnx.privacy.PrivacyManager
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
    lateinit var profileViewPool: ProfileViewPool
        private set
    lateinit var privacyManager: PrivacyManager
        private set
    lateinit var permissionManager: PermissionManager
        private set
    lateinit var clearDataManager: ClearDataManager
        private set

    override fun onCreate() {
        super.onCreate()
        PhnxPreferences.applyTheme(this)
        profileManager = ProfileManager(this)
        val activeProfile = profileManager.ensureDefaultProfile()
        networkManager = NetworkManager(this)
        networkManager.getConfig(activeProfile.id)
        privacyManager = PrivacyManager(this)
        privacyManager.getSettings(activeProfile.id)
        permissionManager = PermissionManager(this)
        clearDataManager = ClearDataManager(this)
        deviceProfileManager = DeviceProfileManager(this)
        deviceProfileManager.getProfileConfiguration(activeProfile.id)
        resourceMonitor = AndroidResourceMonitor(this)
        profileViewPool = ProfileViewPool(this)
        resourceManager = ResourceManager(
            snapshotProvider = resourceMonitor::currentSnapshot,
            lifecycleAdapter = profileViewPool::apply,
        )
        WebView.setDataDirectorySuffix(activeProfile.id)
    }
}
