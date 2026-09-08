package com.phoenix.phnx

import android.app.Application
import android.webkit.WebView
import com.phoenix.phnx.adblock.AdBlockManager
import com.phoenix.phnx.bookmarks.BookmarkManager
import com.phoenix.phnx.browser.ProfileViewPool
import com.phoenix.phnx.history.HistoryManager
import com.phoenix.phnx.identity.DeviceProfileManager
import com.phoenix.phnx.downloads.DownloadManager
import com.phoenix.phnx.network.NetworkManager
import com.phoenix.phnx.chromium.network.ChromiumProxyAdapter
import com.phoenix.phnx.permissions.PermissionManager
import com.phoenix.phnx.profiles.ProfileManager
import com.phoenix.phnx.privacy.ClearDataManager
import com.phoenix.phnx.privacy.PrivacyManager
import com.phoenix.phnx.resources.AndroidResourceMonitor
import com.phoenix.phnx.resources.CrashRecoveryManager
import com.phoenix.phnx.resources.ResourceManager
import com.phoenix.phnx.search.SearchEngineManager
import com.phoenix.phnx.tabs.TabPreviewStore

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
    lateinit var crashRecoveryManager: CrashRecoveryManager
        private set
    lateinit var profileViewPool: ProfileViewPool
        private set
    lateinit var privacyManager: PrivacyManager
        private set
    lateinit var permissionManager: PermissionManager
        private set
    lateinit var clearDataManager: ClearDataManager
        private set
    lateinit var historyManager: HistoryManager
        private set
    lateinit var bookmarkManager: BookmarkManager
        private set
    lateinit var searchEngineManager: SearchEngineManager
        private set
    lateinit var downloadManager: DownloadManager
        private set
    lateinit var adBlockManager: AdBlockManager
        private set
    lateinit var tabPreviewStore: TabPreviewStore
        private set

    override fun onCreate() {
        super.onCreate()
        if (!AppIntegrityVerifier.verify(this)) return
        PhnxPreferences.applyTheme(this)
        crashRecoveryManager = CrashRecoveryManager(this)
        crashRecoveryManager.beginLaunch()
        profileManager = ProfileManager(this)
        val activeProfile = profileManager.ensureDefaultProfile()
        WebView.setDataDirectorySuffix(activeProfile.id)
        networkManager = NetworkManager(this)
        networkManager.getConfig(activeProfile.id)
        privacyManager = PrivacyManager(this)
        privacyManager.getSettings(activeProfile.id)
        permissionManager = PermissionManager(this)
        clearDataManager = ClearDataManager(this)
        historyManager = HistoryManager(this)
        bookmarkManager = BookmarkManager(this)
        searchEngineManager = SearchEngineManager(this)
        downloadManager = DownloadManager(this)
        adBlockManager = AdBlockManager(this)
        tabPreviewStore = TabPreviewStore(this)
        deviceProfileManager = DeviceProfileManager(this)
        deviceProfileManager.getProfileConfiguration(activeProfile.id)
        resourceMonitor = AndroidResourceMonitor(this)
        profileViewPool = ProfileViewPool(this)
        resourceManager = ResourceManager(
            snapshotProvider = resourceMonitor::currentSnapshot,
            lifecycleAdapter = profileViewPool::apply,
        )
        networkManager.applyConfig(activeProfile.id, ChromiumProxyAdapter())
    }
}
