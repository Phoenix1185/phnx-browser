package com.phoenix.phnx

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Color
import android.graphics.PointF
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.Environment
import android.os.Process
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.text.Editable
import android.text.TextWatcher
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.GeolocationPermissions
import android.webkit.MimeTypeMap
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebChromeClient.CustomViewCallback
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.RenderProcessGoneDetail
import android.webkit.SafeBrowsingResponse
import android.webkit.WebSettings
import android.webkit.SslErrorHandler
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.phoenix.phnx.bookmarks.BookmarksActivity
import com.phoenix.phnx.history.HistoryActivity
import com.phoenix.phnx.about.AboutActivity
import com.phoenix.phnx.browser.BrowserController
import com.phoenix.phnx.browser.BrowserView
import com.phoenix.phnx.browser.NavigationController
import com.phoenix.phnx.downloads.DownloadsActivity
import com.phoenix.phnx.downloads.DownloadFileResolver
import com.phoenix.phnx.downloads.DownloadSecurityManager
import com.phoenix.phnx.identity.BrowserIdentityConfig
import com.phoenix.phnx.identity.DevicePresets
import com.phoenix.phnx.identity.WebViewIdentityCompatibility
import com.phoenix.phnx.identity.WebViewIdentityAdapter
import com.phoenix.phnx.menu.BrowserMenu
import com.phoenix.phnx.network.NetworkApplyStatus
import com.phoenix.phnx.network.NetworkActivity
import com.phoenix.phnx.network.NetworkState
import com.phoenix.phnx.network.NetworkStateListener
import com.phoenix.phnx.chromium.network.ChromiumProxyAdapter
import com.phoenix.phnx.pages.FindInPageResult
import com.phoenix.phnx.permissions.SitePermissionDecision
import com.phoenix.phnx.permissions.SitePermission
import com.phoenix.phnx.permissions.SitePermissionType
import com.phoenix.phnx.profiles.TabSessionEntity
import com.phoenix.phnx.profiles.ProfileStatus
import com.phoenix.phnx.resources.ProfileLifecycleState
import com.phoenix.phnx.resources.ProfileResourceState
import com.phoenix.phnx.resources.TabDiagnostics
import com.phoenix.phnx.security.BrowserSecurityState
import com.phoenix.phnx.security.SecurityStateResolver
import com.phoenix.phnx.settings.SettingsActivity
import com.phoenix.phnx.system.DefaultBrowserManager
import com.phoenix.phnx.system.UrlIntentParser
import com.phoenix.phnx.tabs.Tab
import com.phoenix.phnx.tabs.TabManager
import com.phoenix.phnx.tabs.TabOverviewDialog
import com.phoenix.phnx.tabs.TabOverviewItem
import com.phoenix.phnx.tabs.toOverviewItem
import com.phoenix.phnx.update.UpdateInstaller
import com.phoenix.phnx.update.UpdateService
import org.json.JSONArray
import org.json.JSONTokener
import java.io.File
import kotlin.math.abs
import java.io.ByteArrayInputStream
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), BrowserMenu.Callbacks {
    private val tabManager = TabManager()
    private val tabDiagnosticsOwner = Any()
    private val app by lazy { application as PhnxApplication }
    private val browserController by lazy { BrowserController(app.profileViewPool) }
    private val profileManager by lazy { app.profileManager }
    private val deviceProfileManager by lazy { app.deviceProfileManager }
    private val resourceManager by lazy { app.resourceManager }
    private val privacyManager by lazy { app.privacyManager }
    private val permissionManager by lazy { app.permissionManager }
    private val adBlockManager by lazy { app.adBlockManager }
    private val identityAdapter = WebViewIdentityAdapter()
    private val previewStore by lazy { app.tabPreviewStore }

    private lateinit var browserContainer: FrameLayout
    private lateinit var addressBar: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var securityIndicator: TextView
    private lateinit var tabCount: TextView
    private lateinit var bookmarkButton: TextView
    private lateinit var refreshButton: TextView
    private var errorView: View? = null
    private var desktopSiteEnabled = false
    private var dataSaverEnabled = false
    private var pageZoomPercent = 100
    private var textScalePercent = 100
    private var activityVisible = false
    private var mediaCheckInFlight = false
    private var thermalListenerRegistered = false
    @SuppressLint("NewApi")
    private val thermalStatusListener = PowerManager.OnThermalStatusChangedListener { status ->
        updateThermalDisplayPolicy()
        if (activityVisible && status >= PowerManager.THERMAL_STATUS_MODERATE) {
            reconcileResources()
            trimInactiveTabs(aggressive = true)
        }
    }
    private var integrityRejected = false
    private var attachedTabId: String? = null
    private var appliedNetworkConfigHash: Int? = null
    private var customView: View? = null
    private var customViewCallback: CustomViewCallback? = null
    private var customViewTabId: String? = null
    private var securityState = BrowserSecurityState.UNKNOWN
    private val downloadSecurityManager by lazy { DownloadSecurityManager() }
    private var findQuery = ""
    private var findDialogWebView: WebView? = null
    private var findCountView: TextView? = null
    private var tabOverview: TabOverviewDialog? = null
    private val previewHandler = Handler(Looper.getMainLooper())
    private val networkRecoveryHandler = Handler(Looper.getMainLooper())
    private val updateExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val updateCheckInFlight = AtomicBoolean(false)
    private val pendingPreviewCaptures = mutableMapOf<String, Runnable>()
    private val longPressPoints = WeakHashMap<WebView, PointF>()
    private var bookmarkStateUrl: String? = null
    private var bookmarkState = false
    private var lastNetworkState: NetworkState? = null
    private var networkRecoveryPending = false
    private var networkRecoveryAttempt = 0
    private var networkRecoveryInFlight = false
    private var networkRecoveryRunnable: Runnable? = null
    private val networkStateListener = NetworkStateListener { state ->
        runOnUiThread { handleNetworkState(state) }
    }

    private val networkRecoveryDelaysMs = longArrayOf(250L, 1_000L, 3_000L)

    private val swipeDetector by lazy {
        GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(event: MotionEvent): Boolean = true

            override fun onFling(
                start: MotionEvent?,
                end: MotionEvent,
                velocityX: Float,
                velocityY: Float,
            ): Boolean {
                val first = start ?: return false
                val distanceX = end.x - first.x
                val distanceY = end.y - first.y
                if (abs(distanceX) < dp(72) || abs(distanceX) < abs(distanceY) * 1.2f || abs(velocityX) < dp(240)) {
                    return false
                }

                val view = currentBrowserView() ?: return false
                return if (distanceX > 0 && view.canGoBack()) {
                    view.goBack()
                    true
                } else if (distanceX < 0 && view.canGoForward()) {
                    view.goForward()
                    true
                } else {
                    false
                }
            }
        })
    }

    private var pendingPermissionRequest: PermissionRequest? = null
    private var pendingPermissionResources: Array<String> = emptyArray()
    private var pendingGeolocationOrigin: String? = null
    private var pendingGeolocationCallback: GeolocationPermissions.Callback? = null
    private var pendingWebTaskTabId: String? = null
    private var pendingDownload: PendingDownload? = null
    private var pendingFilePathCallback: ValueCallback<Array<Uri>>? = null

    private val webPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        val request = pendingPermissionRequest ?: return@registerForActivityResult
        pendingPermissionRequest = null
        val resources = pendingPermissionResources
        pendingPermissionResources = emptyArray()
        if (results.values.all { it }) request.grant(resources) else request.deny()
        clearPendingWebTask()
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        val origin = pendingGeolocationOrigin
        val callback = pendingGeolocationCallback
        pendingGeolocationOrigin = null
        pendingGeolocationCallback = null
        if (origin != null && callback != null) {
            callback.invoke(origin, results.values.any { it }, false)
        }
        clearPendingWebTask()
    }

    private val downloadPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val download = pendingDownload
        pendingDownload = null
        if (granted && download != null) enqueueDownloadAfterPermissions(download)
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        val download = pendingDownload
        pendingDownload = null
        if (download != null) enqueueDownload(download)
    }

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        val callback = pendingFilePathCallback
        pendingFilePathCallback = null
        callback?.onReceiveValue(uris.toTypedArray().takeIf { it.isNotEmpty() })
        clearPendingWebTask()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!AppIntegrityVerifier.verify(this)) {
            integrityRejected = true
            showIntegrityFailure()
            return
        }
        if (restartForShortcutProfile(intent)) return
        app.profileViewPool.attachHostContext(this)
        val activeProfileId = profileManager.activeProfile().id
        val preferences = PhnxPreferences.store(this)
        dataSaverEnabled = preferences.getBoolean(PhnxPreferences.DATA_SAVER_ENABLED, false)
        desktopSiteEnabled = PhnxPreferences.profileDesktopSiteEnabled(this, activeProfileId)
        pageZoomPercent = PhnxPreferences.profilePageZoomPercent(this, activeProfileId)
        textScalePercent = PhnxPreferences.profileTextScalePercent(this, activeProfileId)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = getColor(R.color.phnx_navy)
        window.navigationBarColor = getColor(R.color.phnx_navy)
        registerThermalListener()

        val layout = buildLayout()
        setContentView(layout)
        applySystemBarInsets(layout)

        appliedNetworkConfigHash = app.networkManager.getConfig(activeProfileId).hashCode()
        val savedTabs = profileManager.loadTabSessions(activeProfileId)
        if (savedTabs.isEmpty()) {
            tabManager.createTab(profileId = activeProfileId)
        } else {
            tabManager.restoreTabs(
                savedTabs.map { saved ->
                    Tab(
                        id = saved.tabId,
                        profileId = saved.profileId,
                        title = saved.title,
                        url = saved.url,
                        isPrivate = saved.isPrivate,
                        groupId = saved.groupId,
                        groupTitle = saved.groupTitle,
                        groupCreatedAt = saved.groupCreatedAt,
                    )
                },
                savedTabs.firstOrNull { it.isActive }?.tabId,
            )
        }
        app.setTabDiagnosticsProvider(tabDiagnosticsOwner) {
            val tabs = tabManager.getTabs()
            TabDiagnostics(
                openTabs = tabs.size,
                loadedPages = tabs.count { it.url.isNotBlank() && !it.isLoading },
                available = true,
            )
        }
        browserController.setSessionSaver(::saveProfileSession)
        attachCurrentTab()
        openIncomingPage(intent)
        app.crashRecoveryManager.markHealthy()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (customView != null) {
                    hideCustomView()
                    return
                }
                val view = currentBrowserView()
                if (view?.canGoBack() == true) view.goBack() else finish()
            }
        })
    }

    override fun onStart() {
        super.onStart()
        if (!integrityRejected) {
            lastNetworkState = app.networkManager.observeConnection(networkStateListener)
        }
    }

    override fun onResume() {
        super.onResume()
        if (integrityRejected) return
        activityVisible = true
        updateThermalDisplayPolicy()
        val savedDataSaver = PhnxPreferences.store(this).getBoolean(PhnxPreferences.DATA_SAVER_ENABLED, false)
        if (savedDataSaver != dataSaverEnabled) {
            dataSaverEnabled = savedDataSaver
            browserController.forEachView(::applyBrowserModes)
            currentBrowserView()?.reload()
        }
        val networkConfig = app.networkManager.getConfig(profileManager.activeProfile().id)
        if (networkConfig.hashCode() != appliedNetworkConfigHash) {
            val apply = app.networkManager.applyConfig(networkConfig.profileId, ChromiumProxyAdapter())
            appliedNetworkConfigHash = networkConfig.hashCode()
            if (apply.status == NetworkApplyStatus.APPLIED) currentBrowserView()?.reload()
            else Toast.makeText(this, apply.message, Toast.LENGTH_LONG).show()
        }
        if (networkRecoveryPending && app.networkManager.reportConnectionState() == NetworkState.CONNECTED) {
            scheduleNetworkRecovery()
        }
        reconcileResources()
        attachCurrentTab()
        trimInactiveTabs()
        checkForAutomaticUpdate()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (integrityRejected) return
        if (restartForShortcutProfile(intent)) return
        setIntent(intent)
        openIncomingPage(intent)
    }

    override fun onStop() {
        if (integrityRejected) {
            super.onStop()
            return
        }
        app.adBlockManager.flushStats()
        activityVisible = false
        networkRecoveryRunnable?.let(networkRecoveryHandler::removeCallbacks)
        networkRecoveryRunnable = null
        if (!integrityRejected) app.networkManager.stopObservingConnection(networkStateListener)
        trimInactiveTabs()
        reconcileResources()
        super.onStop()
    }

    private fun handleNetworkState(state: NetworkState) {
        val previous = lastNetworkState
        lastNetworkState = state
        if (state != NetworkState.CONNECTED) {
            networkRecoveryRunnable?.let(networkRecoveryHandler::removeCallbacks)
            networkRecoveryRunnable = null
            networkRecoveryAttempt = 0
            if (previous == NetworkState.CONNECTED) networkRecoveryPending = true
            return
        }
        if (previous != null && previous != NetworkState.CONNECTED) {
            networkRecoveryPending = true
            scheduleNetworkRecovery()
        }
    }

    private fun scheduleNetworkRecovery() {
        if (!activityVisible || !networkRecoveryPending || networkRecoveryInFlight || networkRecoveryRunnable != null) return
        val delay = networkRecoveryDelaysMs[networkRecoveryAttempt.coerceAtMost(networkRecoveryDelaysMs.lastIndex)]
        networkRecoveryRunnable = Runnable {
            networkRecoveryRunnable = null
            recoverNetwork()
        }.also { networkRecoveryHandler.postDelayed(it, delay) }
    }

    private fun recoverNetwork() {
        if (!activityVisible || !networkRecoveryPending ||
            app.networkManager.reportConnectionState() != NetworkState.CONNECTED
        ) return
        networkRecoveryInFlight = true
        val outcome = runCatching {
            app.networkManager.applyConfig(profileManager.activeProfile().id, ChromiumProxyAdapter())
        }
        networkRecoveryInFlight = false
        outcome.onSuccess { apply ->
            if (apply.status == NetworkApplyStatus.APPLIED) {
                networkRecoveryPending = false
                networkRecoveryAttempt = 0
                appliedNetworkConfigHash = app.networkManager
                    .getConfig(profileManager.activeProfile().id)
                    .hashCode()
                browserController.forEachView { it.setNetworkAvailable(true) }
                currentBrowserView()?.reload()
            } else {
                retryNetworkRecovery(apply.message)
            }
        }.onFailure { error ->
            retryNetworkRecovery(error.message ?: error.javaClass.simpleName)
        }
    }

    private fun retryNetworkRecovery(message: String) {
        if (networkRecoveryAttempt < networkRecoveryDelaysMs.lastIndex) {
            networkRecoveryAttempt++
            scheduleNetworkRecovery()
        } else {
            networkRecoveryPending = false
            Toast.makeText(this, "Could not restore network routing. $message", Toast.LENGTH_LONG).show()
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (integrityRejected) return
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            trimInactiveTabs(aggressive = level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL)
        }
        reconcileResources()
    }

    private fun saveCurrentProfileSession() {
        if (!::browserContainer.isInitialized) return
        saveProfileSession(profileManager.activeProfile().id)
    }

    private fun saveProfileSession(profileId: String) {
        if (!::browserContainer.isInitialized) return
        val tabs = tabManager.persistedTabs(profileId)
        if (tabs.isEmpty() && profileId != profileManager.activeProfile().id) return
        val activeTabId = tabManager.activeTabId()
        val sessions = tabs.mapIndexed { index, tab ->
            TabSessionEntity(
                profileId = profileId,
                tabId = tab.id,
                title = tab.title,
                url = tab.url,
                isPrivate = tab.isPrivate,
                position = index,
                isActive = tab.id == activeTabId,
                groupId = tab.groupId,
                groupTitle = tab.groupTitle,
                groupCreatedAt = tab.groupCreatedAt,
            )
        }
        profileManager.saveTabSessions(profileId, sessions)
    }

    private fun buildLayout(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(getColor(R.color.phnx_navy))
        }

        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(6), dp(4), dp(6), dp(4))
            setBackgroundColor(getColor(R.color.phnx_navy))
        }
        val back = toolbarButton("‹", "Back")
        back.setOnClickListener { currentBrowserView()?.goBack() }
        toolbar.addView(back)
        val forward = toolbarButton("›", "Forward")
        forward.setOnClickListener { currentBrowserView()?.goForward() }
        toolbar.addView(forward)

        securityIndicator = TextView(this).apply {
            gravity = Gravity.CENTER
            textSize = 12f
            setTextColor(getColor(R.color.phnx_toolbar_content))
            contentDescription = "Security state"
        }
        toolbar.addView(securityIndicator, LinearLayout.LayoutParams(dp(42), dp(48)))

        addressBar = EditText(this).apply {
            hint = getString(R.string.address_hint)
            isSingleLine = true
            textSize = 15f
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_GO
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
            setTextColor(getColor(R.color.phnx_toolbar_content))
            setHintTextColor(getColor(R.color.phnx_toolbar_hint))
            setPadding(dp(10), 0, dp(10), 0)
            setOnEditorActionListener { _, _, _ -> navigateFromAddressBar(); true }
        }
        toolbar.addView(addressBar, LinearLayout.LayoutParams(0, dp(48), 1f))

        val menuButton = toolbarButton("⋮", "Browser menu")
        menuButton.setOnClickListener { BrowserMenu.show(menuButton, this) }
        toolbar.addView(menuButton)

        bookmarkButton = toolbarButton("☆", "Bookmark current page")
        bookmarkButton.setOnClickListener { toggleCurrentBookmark() }
        toolbar.addView(bookmarkButton)
        refreshButton = toolbarButton("↻", getString(R.string.refresh))
        refreshButton.setOnClickListener {
            if (tabManager.currentTab()?.isLoading == true) currentBrowserView()?.stopLoading()
            else currentBrowserView()?.reload()
        }
        toolbar.addView(refreshButton)
        root.addView(toolbar)

        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
            visibility = View.GONE
        }
        root.addView(progressBar, LinearLayout.LayoutParams(-1, dp(3)))

        browserContainer = FrameLayout(this).apply {
            setBackgroundColor(Color.WHITE)
        }
        root.addView(browserContainer, LinearLayout.LayoutParams(-1, 0, 1f))

        val bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(getColor(R.color.phnx_navy))
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }
        val newTab = toolbarButton("+", "New tab")
        newTab.setOnClickListener { onNewTab() }
        bottomBar.addView(newTab)
        tabCount = TextView(this).apply {
            gravity = Gravity.CENTER
            textSize = 15f
            setTextColor(getColor(R.color.phnx_toolbar_content))
            setOnClickListener { showTabSwitcher() }
            contentDescription = "Open tabs"
        }
        bottomBar.addView(tabCount, LinearLayout.LayoutParams(0, dp(48), 1f))
        val bottomMenu = toolbarButton("⋮", "Browser menu")
        bottomMenu.setOnClickListener { BrowserMenu.show(bottomMenu, this) }
        bottomBar.addView(bottomMenu)
        root.addView(bottomBar)
        return root
    }

    private fun applySystemBarInsets(root: View) {
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val safeInsets = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
            )
            view.setPadding(view.paddingLeft, safeInsets.top, view.paddingRight, safeInsets.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

    private fun attachCurrentTab() {
        val tab = tabManager.currentTab() ?: return
        val webView = browserController.getOrCreate(tab)
        val tabChanged = attachedTabId != tab.id
        configureWebView(webView, tab)
        val restored = browserController.restoreStateIfNeeded(tab.id, webView)
        (webView.parent as? ViewGroup)?.removeView(webView)
        browserContainer.removeAllViews()
        browserContainer.addView(webView, FrameLayout.LayoutParams(-1, -1))
        errorView = null
        updateTabChrome(tab, webView)

        attachedTabId = tab.id
        if (!restored && (tabChanged || webView.url == null)) {
            if (tab.url.isBlank()) {
                webView.loadDataWithBaseURL(START_PAGE_BASE, startPageHtml(), "text/html", "UTF-8", null)
            } else if (webView.url != tab.url) {
                webView.loadUrl(tab.url)
            }
        }
    }

    private fun configureWebView(webView: BrowserView, tab: Tab) {
        val config = WebViewConfiguration(
            tabId = tab.id,
            profileId = tab.profileId,
            identity = effectiveIdentityConfig(tab.profileId),
            privacy = privacyManager.getSettings(tab.profileId),
        )
        val previous = webView.tag as? WebViewConfiguration
        if (previous?.matches(config) == true) {
            applyBrowserModes(webView)
            applyPageControls(webView)
            return
        }
        webView.tag = config
        webView.setFindListener { activeMatchOrdinal, numberOfMatches, isDoneCounting ->
            if (findDialogWebView === webView) {
                findCountView?.text = FindInPageResult(
                    activeMatchOrdinal = activeMatchOrdinal,
                    numberOfMatches = numberOfMatches,
                    isDoneCounting = isDoneCounting,
                ).summary()
            }
        }
        applyBrowserModes(webView)
        applyProfileIdentity(webView, config.identity)
        applyPageControls(webView)
        privacyManager.applyTo(webView, tab.profileId, config.privacy)
        webView.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                longPressPoints[webView] = PointF(event.x, event.y)
            }
            swipeDetector.onTouchEvent(event)
            false
        }
        webView.setOnLongClickListener { handleWebViewLongPress(webView, tab) }
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val inspection = UrlIntentParser.inspect(request.url.toString())
                if (inspection.classification == UrlIntentParser.Classification.NORMAL_WEB) return false
                if (inspection.classification != UrlIntentParser.Classification.ANDROID_APP_LINK) return true

                val externalIntent = Intent(Intent.ACTION_VIEW, request.url)
                if (externalIntent.resolveActivity(packageManager) != null) {
                    runCatching { startActivity(externalIntent) }
                }
                return true
            }

            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                // WebView invokes this callback off the main thread; never read WebView state here.
                val decision = adBlockManager.evaluate(
                    profileId = tab.profileId,
                    url = request.url.toString(),
                    firstPartyUrl = tab.url,
                )
                if (!decision.blocked) return super.shouldInterceptRequest(view, request)
                return WebResourceResponse(
                    "text/plain",
                    "UTF-8",
                    ByteArrayInputStream(ByteArray(0)),
                )
            }

            override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                tab.isLoading = true
                tab.url = url
                tab.title = tabTitleForUrl(url)
                if (tabManager.currentTab()?.id == tab.id) updateSecurityState(SecurityStateResolver.fromUrl(url))
                updateTabChrome(tab, view)
            }

            override fun onPageFinished(view: WebView, url: String) {
                tab.isLoading = false
                if (url != START_PAGE_BASE) tab.url = url
                tab.title = view.title?.takeIf { it.isNotBlank() } ?: tabTitleForUrl(url)
                if (tabManager.currentTab()?.id == tab.id) updateSecurityState(SecurityStateResolver.fromUrl(url))
                applyProfileCompatibility(view, tab.profileId)
                app.historyManager.recordVisit(tab.profileId, url, tab.title, tab.isPrivate)
                captureTabPreview(tab, view)
                updateTabChrome(tab, view)
                hideError()
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) {
                    tab.isLoading = false
                    updateTabChrome(tab, view)
                    showError(error.description?.toString() ?: "The page could not be loaded.")
                }
            }

            override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
                if (request.isForMainFrame && errorResponse.statusCode >= 400) {
                    showError("The page returned an error (${errorResponse.statusCode}).")
                }
            }

            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: android.net.http.SslError) {
                handler.cancel()
                if (tabManager.currentTab()?.id == tab.id) updateSecurityState(BrowserSecurityState.CERTIFICATE_ERROR)
                if (view.url == error.url) {
                    showError("The secure connection could not be verified.")
                }
            }

            override fun onSafeBrowsingHit(
                view: WebView,
                request: WebResourceRequest,
                threatType: Int,
                callback: SafeBrowsingResponse,
            ) {
                callback.backToSafety(true)
                if (tabManager.currentTab()?.id == tab.id) {
                    updateSecurityState(BrowserSecurityState.SAFE_BROWSING_WARNING)
                    showError("Android Safe Browsing blocked this page.")
                }
            }

            override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                val isCurrentTab = tabManager.currentTab()?.id == tab.id
                browserController.suspendProfile(tab.profileId)
                val recovery = app.crashRecoveryManager.recordRendererCrash()
                attachedTabId = null
                tab.isLoading = false
                if (isCurrentTab) {
                    val message = if (recovery.recoveryMode) {
                        "The page renderer has stopped repeatedly. Retry after closing other tabs or restarting PHNX."
                    } else {
                        "The page renderer stopped unexpectedly. Retry to reopen this tab."
                    }
                    showError(message)
                }
                return true
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                if (tabManager.currentTab()?.id != tab.id) return
                progressBar.progress = newProgress
                progressBar.visibility = if (newProgress in 1..99) View.VISIBLE else View.GONE
            }

            override fun onReceivedTitle(view: WebView, title: String) {
                tab.title = title.ifBlank { "New tab" }
                if (tabManager.currentTab()?.id == tab.id) updateTabChrome(tab, view)
            }

            override fun onReceivedIcon(view: WebView, icon: android.graphics.Bitmap) {
                previewStore.setFavicon(tab, icon)
            }

            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                if (customView != null) {
                    callback.onCustomViewHidden()
                    return
                }
                customView = view
                customViewCallback = callback
                customViewTabId = tab.id
                tab.hasActiveMedia = true
                browserContainer.addView(view, FrameLayout.LayoutParams(-1, -1))
                WindowCompat.getInsetsController(window, window.decorView)
                    .hide(WindowInsetsCompat.Type.systemBars())
            }

            override fun onHideCustomView() = hideCustomView()

            override fun onCreateWindow(
                view: WebView,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message,
            ): Boolean {
                if (!isUserGesture) return false
                val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false
                val popupTab = tabManager.createTab(profileId = tab.profileId)
                val popupView = browserController.getOrCreate(popupTab)
                configureWebView(popupView, popupTab)
                transport.webView = popupView
                resultMsg.sendToTarget()
                attachCurrentTab()
                return true
            }

            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams,
            ): Boolean {
                pendingFilePathCallback?.onReceiveValue(null)
                pendingFilePathCallback = filePathCallback
                clearPendingWebTask()
                pendingWebTaskTabId = tab.id
                tab.hasPendingWebTask = true
                val acceptTypes = fileChooserParams.acceptTypes
                    .filter { it.isNotBlank() }
                    .ifEmpty { listOf("*/*") }
                    .toTypedArray()
                fileChooserLauncher.launch(acceptTypes)
                return true
            }

            override fun onPermissionRequest(request: PermissionRequest) {
                runOnUiThread { handleWebPermissionRequest(request, tab.profileId, tab.id) }
            }

            override fun onPermissionRequestCanceled(request: PermissionRequest) {
                if (pendingPermissionRequest === request) {
                    pendingPermissionRequest = null
                    pendingPermissionResources = emptyArray()
                    clearPendingWebTask()
                }
            }

            override fun onGeolocationPermissionsShowPrompt(
                origin: String,
                callback: GeolocationPermissions.Callback,
            ) {
                runOnUiThread { handleLocationRequest(origin, callback, tab.profileId) }
            }
        }
        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            requestDownload(url, userAgent, contentDisposition, mimeType)
        }
    }

    private fun handleWebPermissionRequest(request: PermissionRequest, profileId: String, tabId: String) {
        val types = buildList {
            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE in request.resources) add(SitePermissionType.CAMERA)
            if (PermissionRequest.RESOURCE_AUDIO_CAPTURE in request.resources) add(SitePermissionType.MICROPHONE)
        }
        if (types.isEmpty()) {
            request.deny()
            return
        }
        val origin = request.origin.toString()
        val supportedResources = request.resources.filter {
            it == PermissionRequest.RESOURCE_VIDEO_CAPTURE || it == PermissionRequest.RESOURCE_AUDIO_CAPTURE
        }.toTypedArray()
        val decisions = types.map { permissionManager.get(profileId, origin, it) }
        if (decisions.any { it == SitePermissionDecision.BLOCK }) {
            request.deny()
            return
        }
        if (decisions.all { it == SitePermissionDecision.ALLOW }) {
            requestAndroidPermissions(request, supportedResources, tabId)
            return
        }
        pendingPermissionRequest?.deny()
        pendingPermissionRequest = request
        pendingPermissionResources = supportedResources
        clearPendingWebTask()
        pendingWebTaskTabId = tabId
        tabManager.getTabs().firstOrNull { it.id == tabId }?.hasPendingWebTask = true
        AlertDialog.Builder(this)
            .setTitle("Permission request")
            .setMessage("$origin wants to use ${types.joinToString { it.name.lowercase() }}.")
            .setNegativeButton("Block") { _, _ ->
                types.forEach { type ->
                    permissionManager.save(SitePermission(profileId, origin, type, SitePermissionDecision.BLOCK))
                }
                pendingPermissionRequest = null
                pendingPermissionResources = emptyArray()
                clearPendingWebTask()
                request.deny()
            }
            .setPositiveButton("Allow") { _, _ ->
                types.forEach { type ->
                    permissionManager.save(SitePermission(profileId, origin, type, SitePermissionDecision.ALLOW))
                }
                requestAndroidPermissions(request, supportedResources, tabId)
            }
            .setOnCancelListener {
                pendingPermissionRequest = null
                pendingPermissionResources = emptyArray()
                clearPendingWebTask()
                request.deny()
            }
            .show()
    }

    private fun requestAndroidPermissions(request: PermissionRequest, resources: Array<String>, tabId: String? = pendingWebTaskTabId) {
        val androidPermissions = buildList {
            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE in resources) add(Manifest.permission.CAMERA)
            if (PermissionRequest.RESOURCE_AUDIO_CAPTURE in resources) add(Manifest.permission.RECORD_AUDIO)
        }
        val missing = androidPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            pendingPermissionRequest = null
            pendingPermissionResources = emptyArray()
            clearPendingWebTask()
            request.grant(resources)
        } else {
            pendingPermissionRequest = request
            pendingPermissionResources = resources
            pendingWebTaskTabId = tabId
            webPermissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun handleLocationRequest(origin: String, callback: GeolocationPermissions.Callback, profileId: String) {
        when (permissionManager.get(profileId, origin, SitePermissionType.LOCATION)) {
            SitePermissionDecision.BLOCK -> {
                callback.invoke(origin, false, false)
                return
            }
            SitePermissionDecision.ASK -> {
                AlertDialog.Builder(this)
                    .setTitle("Location permission")
                    .setMessage("$origin wants to access your location.")
                    .setNegativeButton("Block") { _, _ ->
                        permissionManager.save(SitePermission(profileId, origin, SitePermissionType.LOCATION, SitePermissionDecision.BLOCK))
                        callback.invoke(origin, false, false)
                    }
                    .setPositiveButton("Allow") { _, _ ->
                        permissionManager.save(SitePermission(profileId, origin, SitePermissionType.LOCATION, SitePermissionDecision.ALLOW))
                        requestAndroidLocation(origin, callback)
                    }
                    .setOnCancelListener { callback.invoke(origin, false, false) }
                    .show()
                return
            }
            SitePermissionDecision.ALLOW -> Unit
        }
        requestAndroidLocation(origin, callback)
    }

    private fun requestAndroidLocation(origin: String, callback: GeolocationPermissions.Callback) {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            callback.invoke(origin, true, false)
            return
        }
        pendingGeolocationOrigin = origin
        pendingGeolocationCallback = callback
        clearPendingWebTask()
        pendingWebTaskTabId = tabManager.currentTab()?.id
        tabManager.currentTab()?.hasPendingWebTask = true
        locationPermissionLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
        )
    }

    private fun requestDownload(url: String, userAgent: String, contentDisposition: String, mimeType: String) {
        val request = PendingDownload(url, userAgent, contentDisposition, mimeType)
        val assessment = downloadSecurityManager.assess(url, contentDisposition, mimeType)
        if (!assessment.allowed) {
            Toast.makeText(this, assessment.message, Toast.LENGTH_LONG).show()
            return
        }
        if (assessment.requiresConfirmation) {
            AlertDialog.Builder(this)
                .setTitle("Review download")
                .setMessage(assessment.message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("Download") { _, _ -> beginDownload(request) }
                .show()
            return
        }
        beginDownload(request)
    }

    private fun handleWebViewLongPress(view: WebView, tab: Tab): Boolean {
        // Let WebView handle ordinary text long presses so its native selection action mode remains available.
        val hitTarget = contextTargetFromHitTest(view.hitTestResult) ?: return false
        val point = longPressPoints[view]?.let { PointF(it.x, it.y) }
        resolveContextTarget(view, tab, hitTarget, point)
        return true
    }

    private fun resolveContextTarget(
        view: WebView,
        tab: Tab,
        seed: WebContextTarget,
        point: PointF?,
    ) {
        if (point == null) {
            showWebContextMenu(view, tab, seed)
            return
        }
        val script = contextTargetScript(view, point)
        try {
            view.evaluateJavascript(script) { raw ->
                val target = mergeContextTargets(seed, parseDomContextTarget(raw))
                showWebContextMenu(view, tab, target ?: seed)
            }
        } catch (_: RuntimeException) {
            showWebContextMenu(view, tab, seed)
        }
    }

    private fun showWebContextMenu(view: WebView, tab: Tab, target: WebContextTarget) {
        val actions = mutableListOf<WebContextAction>()
        target.linkUrl?.let { linkUrl ->
            if (isHttpUrl(linkUrl)) {
                actions += WebContextAction(getString(R.string.context_open_new_tab)) {
                    openContextUrl(tab, linkUrl, background = false)
                }
                if (!tab.isPrivate) {
                    actions += WebContextAction(getString(R.string.context_open_group_tab)) {
                        openContextInGroup(tab, linkUrl)
                    }
                }
                actions += WebContextAction(getString(R.string.context_open_background_tab)) {
                    openContextUrl(tab, linkUrl, background = true)
                }
                actions += WebContextAction(getString(R.string.context_open_private_tab)) {
                    openContextPrivateTab(tab, linkUrl)
                }
                actions += WebContextAction(getString(R.string.context_preview_page)) {
                    previewContextUrl(tab, linkUrl, target.linkText)
                }
            }
            actions += WebContextAction(getString(R.string.context_copy_link)) {
                copyContextText(getString(R.string.context_copy_link), linkUrl)
            }
            actions += WebContextAction(getString(R.string.context_copy_link_text)) {
                copyContextText(getString(R.string.context_copy_link_text), target.linkText.ifBlank { linkUrl })
            }
            if (isHttpUrl(linkUrl)) {
                actions += WebContextAction(getString(R.string.context_download_link)) {
                    downloadContextUrl(view, linkUrl)
                }
                actions += WebContextAction(getString(R.string.context_reading_list)) {
                    addToReadingList(tab, linkUrl, target.linkText)
                }
            }
            actions += WebContextAction(getString(R.string.context_share_link)) {
                shareContextText(target.linkText, linkUrl)
            }
        }
        target.imageUrl?.let { imageUrl ->
            actions += WebContextAction(getString(R.string.context_open_image_new_tab)) {
                openContextUrl(tab, imageUrl, background = false)
            }
            actions += WebContextAction(getString(R.string.context_open_image_background_tab)) {
                openContextUrl(tab, imageUrl, background = true)
            }
            actions += WebContextAction(getString(R.string.context_copy_image_address)) {
                copyContextText(getString(R.string.context_copy_image_address), imageUrl)
            }
            actions += WebContextAction(getString(R.string.context_share_image)) {
                shareContextText(getString(R.string.context_image), imageUrl)
            }
            actions += WebContextAction(getString(R.string.context_download_image)) {
                downloadContextUrl(view, imageUrl)
            }
        }
        if (actions.isEmpty()) return

        val summary = target.linkText.ifBlank { target.linkUrl ?: target.imageUrl.orEmpty() }
            .replace('\n', ' ')
            .trim()
            .take(160)
        AlertDialog.Builder(this)
            .setTitle(if (target.linkUrl != null) R.string.context_link else R.string.context_image)
            .setMessage(summary)
            .setItems(actions.map { it.label }.toTypedArray()) { _, which -> actions[which].action() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun openContextUrl(sourceTab: Tab, url: String, background: Boolean) {
        if (!isHttpUrl(url)) return
        val newTab = tabManager.createTab(
            profileId = sourceTab.profileId,
            isPrivate = sourceTab.isPrivate,
            activate = !background,
        ).apply {
            this.url = url
            title = tabTitleForUrl(url)
            isLoading = true
        }
        if (background) {
            val backgroundView = browserController.getOrCreate(newTab)
            configureWebView(backgroundView, newTab)
            backgroundView.loadUrl(url)
        } else {
            attachCurrentTab()
        }
        saveProfileSession(sourceTab.profileId)
        if (background) currentBrowserView()?.let { updateTabChrome(sourceTab, it) }
        refreshTabOverview()
    }

    private fun openContextInGroup(sourceTab: Tab, url: String) {
        if (sourceTab.isPrivate || !isHttpUrl(url)) return
        val newTab = tabManager.createTab(
            profileId = sourceTab.profileId,
            activate = true,
        ).apply {
            this.url = url
            title = tabTitleForUrl(url)
            isLoading = true
        }
        val groupId = sourceTab.groupId
        if (groupId != null) {
            tabManager.addToGroup(sourceTab.profileId, newTab.id, groupId)
        } else {
            tabManager.createGroup(
                profileId = sourceTab.profileId,
                title = getString(R.string.context_tab_group),
                tabIds = listOf(sourceTab.id, newTab.id),
            )
        }
        attachCurrentTab()
        saveProfileSession(sourceTab.profileId)
        refreshTabOverview()
    }

    private fun openContextPrivateTab(sourceTab: Tab, url: String) {
        if (!isHttpUrl(url)) return
        tabManager.createTab(
            profileId = sourceTab.profileId,
            isPrivate = true,
            activate = true,
        ).apply {
            this.url = url
            title = tabTitleForUrl(url)
            isLoading = true
        }
        attachCurrentTab()
        refreshTabOverview()
        Toast.makeText(this, getString(R.string.context_private_tab_opened), Toast.LENGTH_SHORT).show()
    }

    private fun previewContextUrl(sourceTab: Tab, url: String, linkText: String) {
        if (!isHttpUrl(url)) return
        val preview = BrowserView(this, sourceTab.isPrivate).apply {
            settings.userAgentString = currentBrowserView()?.settings?.userAgentString
                ?: settings.userAgentString
            applyBrowserModes(this)
            applyPageControls(this)
            applyProfileIdentity(this, effectiveIdentityConfig(sourceTab.profileId))
            privacyManager.applyTo(this, sourceTab.profileId)
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest,
                ): WebResourceResponse? {
                    val decision = adBlockManager.evaluate(
                        profileId = sourceTab.profileId,
                        url = request.url.toString(),
                        firstPartyUrl = url,
                    )
                    if (!decision.blocked) return super.shouldInterceptRequest(view, request)
                    return WebResourceResponse(
                        "text/plain",
                        "UTF-8",
                        ByteArrayInputStream(ByteArray(0)),
                    )
                }
            }
            webChromeClient = WebChromeClient()
            loadUrl(url)
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle(linkText.ifBlank { tabTitleForUrl(url) })
            .setView(preview)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.context_open_new_tab) { _, _ ->
                openContextUrl(sourceTab, url, background = false)
            }
            .create()
        dialog.setOnDismissListener {
            preview.stopLoading()
            preview.destroy()
        }
        dialog.show()
    }

    private fun addToReadingList(sourceTab: Tab, url: String, linkText: String) {
        val folder = app.bookmarkManager.getFolders(sourceTab.profileId)
            .firstOrNull { it.name == getString(R.string.context_reading_list) }
            ?: app.bookmarkManager.createFolder(sourceTab.profileId, getString(R.string.context_reading_list))
        if (app.bookmarkManager.getForProfile(sourceTab.profileId).none { it.url == url }) {
            app.bookmarkManager.add(
                profileId = sourceTab.profileId,
                title = linkText.ifBlank { tabTitleForUrl(url) },
                url = url,
                folderId = folder?.id,
            )
        }
        Toast.makeText(this, getString(R.string.context_reading_list_saved), Toast.LENGTH_SHORT).show()
    }

    private fun copyContextText(label: String, value: String) {
        val clipboard = getSystemService(ClipboardManager::class.java) ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
        Toast.makeText(this, label, Toast.LENGTH_SHORT).show()
    }

    private fun shareContextText(title: String, value: String) {
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, value)
            putExtra(Intent.EXTRA_TITLE, title.ifBlank { value })
        }, getString(R.string.share)))
    }

    private fun downloadContextUrl(view: WebView, url: String) {
        val extension = MimeTypeMap.getFileExtensionFromUrl(url).lowercase()
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension).orEmpty()
        requestDownload(url, view.settings.userAgentString, "", mimeType)
    }

    private fun contextTargetFromHitTest(result: WebView.HitTestResult): WebContextTarget? {
        val extra = result.extra?.trim().orEmpty()
        if (extra.isBlank()) return null
        return when (result.type) {
            WebView.HitTestResult.ANCHOR_TYPE,
            WebView.HitTestResult.SRC_ANCHOR_TYPE,
            -> WebContextTarget(linkUrl = safeLinkUrl(extra))
            WebView.HitTestResult.IMAGE_TYPE,
            WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE,
            -> WebContextTarget(imageUrl = safeHttpUrl(extra))
            WebView.HitTestResult.PHONE_TYPE,
            WebView.HitTestResult.GEO_TYPE,
            WebView.HitTestResult.EMAIL_TYPE,
            -> WebContextTarget(linkUrl = safeLinkUrl(extra))
            else -> null
        }?.takeIf { it.linkUrl != null || it.imageUrl != null }
    }

    private fun parseDomContextTarget(raw: String): WebContextTarget? {
        val values = runCatching { JSONArray(raw) }.getOrNull()
            ?: runCatching { JSONArray(JSONTokener(raw).nextValue().toString()) }.getOrNull()
            ?: return null
        val target = WebContextTarget(
            linkUrl = safeLinkUrl(values.optString(0)),
            linkText = values.optString(1).trim(),
            imageUrl = safeHttpUrl(values.optString(2)),
        )
        return target.takeIf { it.linkUrl != null || it.imageUrl != null }
    }

    private fun mergeContextTargets(first: WebContextTarget?, second: WebContextTarget?): WebContextTarget? {
        val target = WebContextTarget(
            linkUrl = first?.linkUrl ?: second?.linkUrl,
            linkText = second?.linkText.orEmpty().ifBlank { first?.linkText.orEmpty() },
            imageUrl = first?.imageUrl ?: second?.imageUrl,
        )
        return target.takeIf { it.linkUrl != null || it.imageUrl != null }
    }

    private fun contextTargetScript(view: WebView, point: PointF): String {
        val density = view.resources.displayMetrics.density
        val x = (point.x / density).coerceAtLeast(0f)
        val y = (point.y / density).coerceAtLeast(0f)
        return """
            (function(x,y){
                var element=document.elementFromPoint(x,y);
                if(!element)return ['','',''];
                var anchor=element.closest&&element.closest('a[href]');
                var roleLink=element.closest&&element.closest('[role="link"],[data-href],[data-url]');
                var link=anchor?anchor.href:(roleLink?(roleLink.href||roleLink.getAttribute('data-href')||roleLink.getAttribute('data-url')||''):'');
                try{if(link)link=new URL(link,document.baseURI).href;}catch(e){}
                var text=anchor?(anchor.innerText||anchor.textContent||anchor.getAttribute('aria-label')||anchor.title||''):(roleLink?(roleLink.innerText||roleLink.textContent||roleLink.getAttribute('aria-label')||''):'');
                var image=element.closest&&element.closest('img');
                var imageUrl=image?(image.currentSrc||image.src||''):'';
                return [link,(text||'').trim(),imageUrl];
            })($x,$y)
        """.trimIndent()
    }

    private fun safeLinkUrl(value: String?): String? {
        val candidate = value?.trim().orEmpty()
        val scheme = Uri.parse(candidate).scheme?.lowercase() ?: return null
        return candidate.takeIf { scheme in setOf("http", "https", "mailto", "tel", "geo") }
    }

    private fun safeHttpUrl(value: String?): String? = safeLinkUrl(value)
        ?.takeIf { isHttpUrl(it) }

    private fun isHttpUrl(value: String): Boolean {
        val scheme = Uri.parse(value).scheme?.lowercase()
        return scheme == "http" || scheme == "https"
    }

    private fun beginDownload(request: PendingDownload) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingDownload = request
            downloadPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            enqueueDownloadAfterPermissions(request)
        }
    }

    private fun enqueueDownloadAfterPermissions(download: PendingDownload) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingDownload = download
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            enqueueDownload(download)
        }
    }

    private fun enqueueDownload(download: PendingDownload) {
        val resolution = DownloadFileResolver.resolve(
            url = download.url,
            contentDisposition = download.contentDisposition,
            mimeType = download.mimeType,
        )
        val request = DownloadManager.Request(Uri.parse(download.url))
            .setTitle(resolution.filename)
            .setDescription("Downloading with PHNX Browser")
            .setMimeType(resolution.mimeType)
            .addRequestHeader("User-Agent", download.userAgent)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, resolution.filename)
        val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = manager.enqueue(request)
        val profileId = tabManager.currentTab()?.profileId ?: profileManager.activeProfile().id
        app.downloadManager.record(
            profileId,
            downloadId,
            download.url,
            resolution.filename,
            resolution.mimeType,
        )
        Toast.makeText(this, "Downloading: ${resolution.filename}", Toast.LENGTH_SHORT).show()
    }

    private fun navigateFromAddressBar() {
        val tab = tabManager.currentTab() ?: return
        val destination = NavigationController.resolveInput(
            addressBar.text.toString(),
            app.searchEngineManager.current().searchUrl,
        )
        tab.url = destination
        tab.title = destination
        tab.isLoading = true
        hideError()
        currentBrowserView()?.loadUrl(destination)
    }

    private fun updateTabChrome(tab: Tab, view: WebView) {
        val isCurrent = tabManager.currentTab()?.id == tab.id
        if (isCurrent) {
            if (addressBar.text.toString() != tab.url && !addressBar.hasFocus()) addressBar.setText(tab.url)
            progressBar.visibility = if (tab.isLoading) View.VISIBLE else View.GONE
            if (::bookmarkButton.isInitialized) {
                if (bookmarkStateUrl != tab.url) {
                    bookmarkStateUrl = tab.url
                    bookmarkState = app.bookmarkManager.isBookmarked(tab.profileId, tab.url)
                }
                bookmarkButton.text = if (bookmarkState) "★" else "☆"
                bookmarkButton.contentDescription = if (bookmarkState) "Remove bookmark" else "Bookmark current page"
            }
            if (::refreshButton.isInitialized) {
                refreshButton.text = if (tab.isLoading) "×" else "↻"
                refreshButton.contentDescription = if (tab.isLoading) getString(R.string.stop) else getString(R.string.refresh)
            }
        }
        tab.canGoBack = view.canGoBack()
        tab.canGoForward = view.canGoForward()
        if (!isCurrent) return
        val profileTabCount = tabManager.tabCount(tab.profileId)
        val profileGroupCount = tabManager.getGroups(tab.profileId).size
        val profileName = profileManager.getAllProfiles().firstOrNull { it.id == tab.profileId }?.name ?: "Profile"
        val groupSummary = if (profileGroupCount == 0) "" else " · $profileGroupCount group${if (profileGroupCount == 1) "" else "s"}"
        tabCount.text = "$profileName · $profileTabCount tab${if (profileTabCount == 1) "" else "s"}$groupSummary"
    }

    private fun updateSecurityState(state: BrowserSecurityState) {
        securityState = state
        if (!::securityIndicator.isInitialized) return
        securityIndicator.text = when (state) {
            BrowserSecurityState.SECURE -> "LOCK"
            BrowserSecurityState.NOT_SECURE -> "HTTP"
            BrowserSecurityState.CERTIFICATE_ERROR -> "CERT"
            BrowserSecurityState.SAFE_BROWSING_WARNING -> "SAFE"
            BrowserSecurityState.UNKNOWN -> ""
        }
        securityIndicator.contentDescription = "Security state: ${state.name.lowercase().replace('_', ' ')}"
    }

    private fun currentBrowserView(): BrowserView? = tabManager.currentTab()?.let(browserController::getOrCreate)

    private fun showError(message: String) {
        if (tabManager.currentTab() == null) return
        hideError()
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(32), dp(24), dp(32), dp(24))
            setBackgroundColor(getColor(R.color.phnx_cream))
        }
        content.addView(TextView(this).apply {
            text = "PHNX could not load this page"
            textSize = 22f
            gravity = Gravity.CENTER
            setTextColor(getColor(R.color.phnx_text))
        })
        content.addView(TextView(this).apply {
            text = message
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(getColor(R.color.phnx_muted))
            setPadding(0, dp(10), 0, dp(12))
        })
        content.addView(Button(this).apply {
            text = getString(R.string.retry)
            setOnClickListener {
                hideError()
                val tab = tabManager.currentTab()
                if (attachedTabId == null) {
                    attachCurrentTab()
                    if (!tab?.url.isNullOrBlank()) currentBrowserView()?.loadUrl(tab?.url.orEmpty())
                } else {
                    currentBrowserView()?.reload()
                }
            }
        })
        if (message.contains("connection", ignoreCase = true) || message.contains("closed", ignoreCase = true)) {
            content.addView(Button(this).apply {
                text = getString(R.string.network_settings)
                setOnClickListener { startActivity(Intent(this@MainActivity, NetworkActivity::class.java)) }
            })
        }
        val failedSearch = tabManager.currentTab()?.url
            ?.takeIf { it.startsWith("https://www.google.com/search") }
        if (failedSearch != null) {
            content.addView(Button(this).apply {
                text = getString(R.string.try_alternate_search)
                setOnClickListener {
                    val query = Uri.parse(failedSearch).getQueryParameter("q").orEmpty()
                    val tab = tabManager.currentTab() ?: return@setOnClickListener
                    val destination = NavigationController.resolveInput(query, "https://duckduckgo.com/?q=")
                    tab.url = destination
                    hideError()
                    currentBrowserView()?.loadUrl(destination)
                }
            })
        }
        errorView = content
        browserContainer.addView(content, FrameLayout.LayoutParams(-1, -1))
    }

    private fun hideError() {
        errorView?.let(browserContainer::removeView)
        errorView = null
    }

    private fun tabTitleForUrl(url: String): String {
        if (url == START_PAGE_BASE) return "New tab"
        return Uri.parse(url).host?.removePrefix("www.").takeUnless { it.isNullOrBlank() } ?: "Loading"
    }

    private fun captureTabPreview(tab: Tab, view: WebView) {
        if (tab.isPrivate || tabManager.currentTab()?.id != tab.id) return
        pendingPreviewCaptures.remove(tab.id)?.let(previewHandler::removeCallbacks)
        val task = Runnable {
            pendingPreviewCaptures.remove(tab.id)
            if (tabManager.currentTab()?.id == tab.id) previewStore.capture(tab, view)
        }
        pendingPreviewCaptures[tab.id] = task
        previewHandler.postDelayed(task, PREVIEW_CAPTURE_DELAY_MS)
    }

    private fun showTabSwitcher() {
        val profileId = profileManager.activeProfile().id
        tabManager.currentTab()?.takeIf { it.profileId == profileId }?.let { current ->
            currentBrowserView()?.let { previewStore.capture(current, it) }
        }
        val dialog = TabOverviewDialog(
            context = this,
            profileId = profileId,
            profileName = profileManager.activeProfile().name,
            previewStore = previewStore,
            onSelect = ::selectTabFromOverview,
            onClose = ::closeTabFromOverview,
            onGroup = { item -> showGroupPicker(item) },
            onNewTab = ::onNewTab,
            onRecentTabs = ::onRecentTabs,
        )
        tabOverview = dialog
        dialog.setOnDismissListener {
            if (tabOverview === dialog) tabOverview = null
        }
        dialog.show()
        dialog.setTabs(tabManager.getTabs(profileId).map { it.toOverviewItem(tabManager.activeTabId().orEmpty()) })
    }

    private fun selectTabFromOverview(item: TabOverviewItem) {
        val activeProfileId = profileManager.activeProfile().id
        if (item.profileId != activeProfileId) return
        if (!tabManager.switchTab(item.id, profileId = activeProfileId)) return
        saveProfileSession(activeProfileId)
        tabOverview?.dismiss()
        attachCurrentTab()
    }

    private fun closeTabFromOverview(item: TabOverviewItem) {
        val activeProfileId = profileManager.activeProfile().id
        if (item.profileId != activeProfileId) return
        val wasCurrent = tabManager.activeTabId() == item.id
        val closed = tabManager.closeTab(item.id) ?: return
        browserController.close(closed)
        pendingPreviewCaptures.remove(closed.id)?.let(previewHandler::removeCallbacks)
        previewStore.remove(closed)
        if (tabManager.tabCount(activeProfileId) == 0) tabManager.createTab(profileId = activeProfileId)
        if (wasCurrent) attachCurrentTab()
        saveProfileSession(activeProfileId)
        refreshTabOverview()
    }

    private fun refreshTabOverview() {
        val profileId = profileManager.activeProfile().id
        tabOverview?.setTabs(tabManager.getTabs(profileId).map { it.toOverviewItem(tabManager.activeTabId().orEmpty()) })
    }

    private fun showGroupPicker(item: TabOverviewItem) {
        val tab = tabManager.getTabs(item.profileId).firstOrNull { it.id == item.id } ?: return
        if (tab.groupId != null) {
            tabManager.removeFromGroup(tab.profileId, tab.id)
            refreshTabOverview()
            return
        }
        val groups = tabManager.getGroups(tab.profileId).filterNot { it.id == tab.groupId }
        val options = listOf(getString(R.string.new_tab_group)) + groups.map { it.title }
        AlertDialog.Builder(this)
            .setTitle(R.string.group_tab)
            .setItems(options.toTypedArray()) { _, which ->
                if (which == 0) {
                    showGroupEditor(tab)
                } else if (tabManager.addToGroup(tab.profileId, tab.id, groups[which - 1].id)) {
                    refreshTabOverview()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showGroupEditor(tab: Tab) {
        val input = EditText(this).apply {
            hint = getString(R.string.tab_group_name)
            setSingleLine(true)
            setPadding(dp(20), dp(8), dp(20), dp(8))
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.new_tab_group)
            .setView(input)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.create) { _, _ ->
                val created = tabManager.createGroup(tab.profileId, input.text.toString(), listOf(tab.id))
                if (created == null) {
                    Toast.makeText(this, getString(R.string.tab_group_name_required), Toast.LENGTH_SHORT).show()
                } else {
                    refreshTabOverview()
                }
            }
            .show()
    }

    private fun toolbarButton(label: String, description: String): TextView = TextView(this).apply {
        text = label
        textSize = 25f
        gravity = Gravity.CENTER
        contentDescription = description
        setTextColor(getColor(R.color.phnx_toolbar_content))
        isClickable = true
        isFocusable = true
        setPadding(dp(6), 0, dp(6), 0)
        layoutParams = LinearLayout.LayoutParams(dp(44), dp(48))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onNewTab() {
        tabManager.createTab(profileId = profileManager.activeProfile().id)
        attachCurrentTab()
    }

    override fun onNewPrivateTab() {
        tabManager.createTab(profileId = profileManager.activeProfile().id, isPrivate = true)
        attachCurrentTab()
        Toast.makeText(this, "Private tab opened. It will not be restored or added to history.", Toast.LENGTH_LONG).show()
    }

    override fun onBookmarks() {
        val tab = tabManager.currentTab() ?: return
        startActivity(Intent(this, BookmarksActivity::class.java).apply {
            putExtra("url", tab.url)
            putExtra("title", tab.title)
        })
    }

    private fun toggleCurrentBookmark() {
        val tab = tabManager.currentTab() ?: return
        if (!tab.url.startsWith("http://") && !tab.url.startsWith("https://")) {
            Toast.makeText(this, "Open a web page before bookmarking it.", Toast.LENGTH_SHORT).show()
            return
        }
        val existing = app.bookmarkManager.getForProfile(tab.profileId).firstOrNull { it.url == tab.url }
        if (existing == null) {
            app.bookmarkManager.add(tab.profileId, tab.title, tab.url)
            Toast.makeText(this, "Bookmark saved", Toast.LENGTH_SHORT).show()
        } else {
            app.bookmarkManager.delete(tab.profileId, existing.id)
            Toast.makeText(this, "Bookmark removed", Toast.LENGTH_SHORT).show()
        }
        bookmarkStateUrl = null
        updateTabChrome(tab, currentBrowserView() ?: return)
    }

    override fun onHistory() = startActivity(Intent(this, HistoryActivity::class.java))

    override fun onDownloads() = startActivity(Intent(this, DownloadsActivity::class.java))

    override fun onRecentTabs() {
        val profileId = profileManager.activeProfile().id
        val recent = tabManager.recentlyClosed(profileId)
        if (recent.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_recent_tabs), Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.recent_tabs)
            .setItems(recent.map { it.title.ifBlank { it.url }.take(60) }.toTypedArray()) { dialog, which ->
                if (tabManager.restoreRecentlyClosed(profileId, recent[which].id) != null) {
                    dialog.dismiss()
                    attachCurrentTab()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onReload() {
        currentBrowserView()?.reload()
    }

    override fun onShare() {
        val tab = tabManager.currentTab() ?: return
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, tab.url)
            putExtra(Intent.EXTRA_TITLE, tab.title)
        }, getString(R.string.share)))
    }

    override fun onFindInPage() {
        val view = currentBrowserView() ?: return
        val input = EditText(this).apply {
            hint = "Find text on this page"
            isSingleLine = true
            setText(findQuery)
            setSelection(text.length)
        }
        val matchCount = TextView(this).apply {
            text = FindInPageResult(0, 0, true).summary()
            setTextColor(getColor(R.color.phnx_muted))
            setPadding(0, dp(8), 0, dp(4))
            contentDescription = "Find match count"
        }
        val previous = Button(this).apply { text = "Previous" }
        val next = Button(this).apply { text = "Next" }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(4), dp(20), 0)
            addView(input)
            addView(matchCount)
            addView(LinearLayout(this@MainActivity).apply {
                gravity = Gravity.END
                addView(previous, LinearLayout.LayoutParams(0, dp(48), 1f))
                addView(next, LinearLayout.LayoutParams(0, dp(48), 1f))
            })
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.find_in_page))
            .setView(content)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Done", null)
            .create()
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
                findQuery = text?.toString().orEmpty()
                val hasQuery = findQuery.isNotBlank()
                previous.isEnabled = hasQuery
                next.isEnabled = hasQuery
                if (hasQuery) {
                    matchCount.text = FindInPageResult(0, 0, false).summary()
                    view.findAllAsync(findQuery)
                } else {
                    view.clearMatches()
                    matchCount.text = FindInPageResult(0, 0, true).summary()
                }
            }

            override fun afterTextChanged(editable: Editable?) = Unit
        }
        input.addTextChangedListener(watcher)
        previous.setOnClickListener { view.findNext(false) }
        next.setOnClickListener { view.findNext(true) }
        dialog.setOnShowListener {
            findDialogWebView = view
            findCountView = matchCount
            previous.isEnabled = findQuery.isNotBlank()
            next.isEnabled = findQuery.isNotBlank()
            if (findQuery.isBlank()) {
                matchCount.text = FindInPageResult(0, 0, true).summary()
            } else {
                matchCount.text = FindInPageResult(0, 0, false).summary()
                view.findAllAsync(findQuery)
            }
        }
        dialog.setOnDismissListener {
            input.removeTextChangedListener(watcher)
            view.clearMatches()
            if (findDialogWebView === view) {
                findDialogWebView = null
                findCountView = null
            }
        }
        dialog.show()
    }

    override fun onPageZoom() {
        val view = currentBrowserView() ?: return
        val levels = intArrayOf(50, 75, 90, 100, 110, 125, 150, 175, 200)
        AlertDialog.Builder(this)
            .setTitle(R.string.page_zoom)
            .setSingleChoiceItems(levels.map { "$it%" }.toTypedArray(), levels.indexOf(pageZoomPercent).coerceAtLeast(0)) { dialog, which ->
                pageZoomPercent = levels[which]
                PhnxPreferences.setProfilePageZoomPercent(this, profileManager.activeProfile().id, pageZoomPercent)
                applyPageControls(view)
                tabManager.currentTab()?.let { applyProfileIdentity(view, effectiveIdentityConfig(it.profileId)) }
                view.reload()
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onTextSize() {
        val view = currentBrowserView() ?: return
        val levels = intArrayOf(80, 90, 100, 115, 130, 150, 175, 200)
        AlertDialog.Builder(this)
            .setTitle(R.string.text_size)
            .setSingleChoiceItems(levels.map { "$it%" }.toTypedArray(), levels.indexOf(textScalePercent).coerceAtLeast(0)) { dialog, which ->
                textScalePercent = levels[which]
                PhnxPreferences.setProfileTextScalePercent(this, profileManager.activeProfile().id, textScalePercent)
                applyPageControls(view)
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onDesktopSite() {
        desktopSiteEnabled = !desktopSiteEnabled
        PhnxPreferences.setProfileDesktopSiteEnabled(this, profileManager.activeProfile().id, desktopSiteEnabled)
        attachCurrentTab()
        currentBrowserView()?.reload()
        Toast.makeText(
            this,
            if (desktopSiteEnabled) "Desktop site and device agent applied" else "Mobile site and device agent applied",
            Toast.LENGTH_SHORT,
        ).show()
    }

    override fun isDesktopSiteEnabled(): Boolean = desktopSiteEnabled

    override fun onDataSaver() {
        dataSaverEnabled = !dataSaverEnabled
        PhnxPreferences.store(this).edit()
            .putBoolean(PhnxPreferences.DATA_SAVER_ENABLED, dataSaverEnabled)
            .apply()
        browserController.forEachView(::applyBrowserModes)
        currentBrowserView()?.reload()
        Toast.makeText(this, if (dataSaverEnabled) "Data Saver enabled" else "Data Saver disabled", Toast.LENGTH_SHORT).show()
    }

    override fun isDataSaverEnabled(): Boolean = dataSaverEnabled

    override fun onAddToHomeScreen() {
        val tab = tabManager.currentTab() ?: return
        if (!tab.url.startsWith("http://") && !tab.url.startsWith("https://")) {
            Toast.makeText(this, getString(R.string.shortcut_requires_page), Toast.LENGTH_SHORT).show()
            return
        }
        val shortcuts = getSystemService(ShortcutManager::class.java)
        if (!shortcuts.isRequestPinShortcutSupported) {
            Toast.makeText(this, getString(R.string.shortcut_unsupported), Toast.LENGTH_SHORT).show()
            return
        }
        val label = tab.title.trim().ifBlank { tab.url }.take(60)
        val shortcut = ShortcutInfo.Builder(this, "page_${tab.profileId}_${tab.url.hashCode()}")
            .setShortLabel(label.take(25))
            .setLongLabel(label)
            .setIcon(Icon.createWithResource(this, R.drawable.phnx_browser_icon))
            .setIntent(Intent(Intent.ACTION_VIEW, Uri.parse(tab.url)).apply {
                setClass(this@MainActivity, MainActivity::class.java)
                putExtra(EXTRA_PROFILE_ID, tab.profileId)
            })
            .build()
        shortcuts.requestPinShortcut(shortcut, null)
        Toast.makeText(this, getString(R.string.shortcut_requested), Toast.LENGTH_SHORT).show()
    }

    override fun onSettings() = startActivity(Intent(this, SettingsActivity::class.java))

    override fun onAbout() = startActivity(Intent(this, AboutActivity::class.java))

    private fun showPlanned(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    private fun restartForShortcutProfile(incomingIntent: Intent?): Boolean {
        if (incomingIntent?.action != Intent.ACTION_VIEW) return false
        val requestedProfileId = incomingIntent.getStringExtra(EXTRA_PROFILE_ID) ?: return false
        val activeProfileId = profileManager.activeProfile().id
        if (requestedProfileId == activeProfileId) return false
        if (profileManager.getAllProfiles().none { it.id == requestedProfileId }) return false
        if (profileManager.switchProfile(requestedProfileId) == null) return false

        startActivity(Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = incomingIntent.data
            putExtra(EXTRA_PROFILE_ID, requestedProfileId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
        finishAffinity()
        Process.killProcess(Process.myPid())
        return true
    }

    private fun openIncomingPage(incomingIntent: Intent?) {
        val requestedProfileId = incomingIntent?.getStringExtra(EXTRA_PROFILE_ID)
        if (requestedProfileId != null) {
            if (profileManager.getAllProfiles().none { it.id == requestedProfileId } ||
                profileManager.activeProfile().id != requestedProfileId
            ) {
                Toast.makeText(this, getString(R.string.profile_unavailable), Toast.LENGTH_LONG).show()
                return
            }
        }
        if (incomingIntent?.action != Intent.ACTION_VIEW) return
        val inspection = UrlIntentParser.inspect(incomingIntent.dataString)
        Log.d(
            INTENT_LOG_TAG,
            "Incoming URI ${UrlIntentParser.summary(incomingIntent.dataString)} " +
                "phnxDefault=${DefaultBrowserManager(this).state() == com.phoenix.phnx.system.DefaultBrowserState.DEFAULT} " +
                "action=${if (inspection.classification == UrlIntentParser.Classification.NORMAL_WEB) "OPEN_IN_PHNX" else "ANDROID_INTENT_RESOLUTION"}",
        )
        if (inspection.classification != UrlIntentParser.Classification.NORMAL_WEB) return
        val url = inspection.normalizedUrl ?: return
        val tab = tabManager.currentTab() ?: return
        tab.url = url
        tab.title = url
        tab.isLoading = true
        hideError()
        currentBrowserView()?.loadUrl(url)
    }

    override fun onDestroy() {
        updateExecutor.shutdownNow()
        if (integrityRejected) {
            super.onDestroy()
            return
        }
        unregisterThermalListener()
        app.clearTabDiagnosticsProvider(tabDiagnosticsOwner)
        pendingPreviewCaptures.values.forEach(previewHandler::removeCallbacks)
        pendingPreviewCaptures.clear()
        longPressPoints.clear()
        previewHandler.removeCallbacksAndMessages(null)
        pendingPermissionRequest?.deny()
        pendingPermissionRequest = null
        pendingPermissionResources = emptyArray()
        pendingFilePathCallback?.onReceiveValue(null)
        pendingFilePathCallback = null
        pendingGeolocationCallback?.invoke(pendingGeolocationOrigin.orEmpty(), false, false)
        pendingGeolocationCallback = null
        pendingGeolocationOrigin = null
        pendingDownload = null
        clearPendingWebTask()
        networkRecoveryHandler.removeCallbacksAndMessages(null)
        app.networkManager.stopObservingConnection(networkStateListener)
        if (::browserContainer.isInitialized) hideCustomView()
        tabOverview?.dismiss()
        if (isFinishing) clearHistoryOnClose()
        browserController.clear()
        app.profileViewPool.detachHostContext(this)
        super.onDestroy()
    }

    private fun showIntegrityFailure() {
        AlertDialog.Builder(this)
            .setTitle(AppIntegrityVerifier.FAILURE_MESSAGE)
            .setMessage("This PHNX package is not an official release and cannot run.")
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok) { _, _ -> finishAndRemoveTask() }
            .show()
    }

    private fun checkForAutomaticUpdate() {
        if (!PhnxPreferences.automaticUpdates(this)) return
        val preferences = PhnxPreferences.store(this)
        val now = System.currentTimeMillis()
        val lastCheck = preferences.getLong(PhnxPreferences.UPDATE_LAST_CHECK_MS, 0L)
        if (now - lastCheck < AUTOMATIC_UPDATE_CHECK_INTERVAL_MS) return
        if (!updateCheckInFlight.compareAndSet(false, true)) return
        preferences.edit().putLong(PhnxPreferences.UPDATE_LAST_CHECK_MS, now).apply()
        updateExecutor.execute {
            val result = runCatching {
                val release = UpdateService.fetchLatestRelease()
                val manifest = release.manifest
                if (!UpdateService.isNewer(release) || manifest == null) {
                    null
                } else {
                    release to UpdateService.downloadAndVerify(this@MainActivity, manifest)
                }
            }
            runOnUiThread {
                updateCheckInFlight.set(false)
                if (isFinishing || isDestroyed) return@runOnUiThread
                result.onSuccess { candidate ->
                    candidate?.let { showAutomaticUpdate(it.first, it.second) }
                }
            }
        }
    }

    private fun showAutomaticUpdate(release: UpdateService.ReleaseInfo, apk: File) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.update_available_title, release.name))
            .setMessage(R.string.update_automatic_ready)
            .setNegativeButton(R.string.update_later, null)
            .setPositiveButton(R.string.update_install_now) { _, _ -> launchPreparedUpdate(apk) }
            .show()
    }

    private fun launchPreparedUpdate(apk: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManager.canRequestPackageInstalls()) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:$packageName"),
                ),
            )
            Toast.makeText(this, getString(R.string.update_allow_installs), Toast.LENGTH_LONG).show()
            return
        }
        val intent = UpdateInstaller.installIntent(this, apk)
        if (intent == null) {
            Toast.makeText(this, getString(R.string.update_install_failed), Toast.LENGTH_LONG).show()
            return
        }
        runCatching { startActivity(intent) }.onFailure {
            Toast.makeText(this, getString(R.string.update_install_failed), Toast.LENGTH_LONG).show()
        }
    }

    private fun clearHistoryOnClose() {
        if (PhnxPreferences.historyRetention(this) == PhnxPreferences.HISTORY_CLEAR_ON_CLOSE) {
            app.historyManager.clearProfile(profileManager.activeProfile().id)
        }
    }

    private fun registerThermalListener() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || thermalListenerRegistered) return
        getSystemService(PowerManager::class.java).addThermalStatusListener(
            mainExecutor,
            thermalStatusListener,
        )
        thermalListenerRegistered = true
        updateThermalDisplayPolicy()
    }

    private fun unregisterThermalListener() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || !thermalListenerRegistered) return
        getSystemService(PowerManager::class.java).removeThermalStatusListener(thermalStatusListener)
        thermalListenerRegistered = false
    }

    private fun updateThermalDisplayPolicy() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val powerManager = getSystemService(PowerManager::class.java)
        val reduceDisplayWork = powerManager.currentThermalStatus >= PowerManager.THERMAL_STATUS_MODERATE ||
            powerManager.isPowerSaveMode
        val targetRefreshRate = if (reduceDisplayWork) THERMAL_REFRESH_RATE_HZ else 0f
        val attributes = window.attributes
        if (attributes.preferredRefreshRate != targetRefreshRate) {
            attributes.preferredRefreshRate = targetRefreshRate
            window.attributes = attributes
        }
    }

    private fun reconcileResources() {
        if (!::browserContainer.isInitialized) return
        val activeProfileId = profileManager.activeProfile().id
        val decisions = resourceManager.reconcile(resourceStates(activeProfileId))
        decisions.forEach { decision ->
            val status = when (decision.to) {
                ProfileLifecycleState.ACTIVE,
                ProfileLifecycleState.IDLE,
                ProfileLifecycleState.RECREATING,
                -> if (decision.profileId == activeProfileId) ProfileStatus.ACTIVE else ProfileStatus.IDLE
                ProfileLifecycleState.FROZEN -> ProfileStatus.FROZEN
                ProfileLifecycleState.SUSPENDED -> ProfileStatus.SUSPENDED
                ProfileLifecycleState.CLOSED -> ProfileStatus.CLOSED
            }
            if (profileManager.getAllProfiles().firstOrNull { it.id == decision.profileId }?.status != status) {
                profileManager.updateStatus(decision.profileId, status)
            }
        }
    }

    private fun resourceStates(activeProfileId: String): List<ProfileResourceState> =
        profileManager.getAllProfiles().map { profile ->
            val persistedState = profile.status.toLifecycleState()
            browserController.seed(profile.id, persistedState)
            ProfileResourceState(
                profileId = profile.id,
                lifecycleState = browserController.state(profile.id) ?: persistedState,
                lastActiveTime = profile.lastUsedAt,
                activeTabCount = tabManager.tabCount(profile.id),
                foreground = activityVisible && profile.id == activeProfileId,
                userPinned = false,
            )
        }

    private fun ProfileStatus.toLifecycleState(): ProfileLifecycleState = when (this) {
        ProfileStatus.ACTIVE -> ProfileLifecycleState.ACTIVE
        ProfileStatus.IDLE -> ProfileLifecycleState.IDLE
        ProfileStatus.FROZEN -> ProfileLifecycleState.FROZEN
        ProfileStatus.SUSPENDED -> ProfileLifecycleState.SUSPENDED
        ProfileStatus.RECREATING -> ProfileLifecycleState.SUSPENDED
        ProfileStatus.CLOSED -> ProfileLifecycleState.CLOSED
    }

    private data class PendingDownload(
        val url: String,
        val userAgent: String,
        val contentDisposition: String,
        val mimeType: String,
    )

    private data class WebContextTarget(
        val linkUrl: String? = null,
        val linkText: String = "",
        val imageUrl: String? = null,
    )

    private data class WebContextAction(
        val label: String,
        val action: () -> Unit,
    )

    companion object {
        const val EXTRA_PROFILE_ID = "profile_id"
        private const val INTENT_LOG_TAG = "PhnxIntent"
        private const val AUTOMATIC_UPDATE_CHECK_INTERVAL_MS = 6 * 60 * 60 * 1000L
        private const val START_PAGE_BASE = "https://phnx.local/"
        private const val PREVIEW_CAPTURE_DELAY_MS = 120L
        private const val THERMAL_REFRESH_RATE_HZ = 60f
        private const val ACTIVE_MEDIA_CHECK_SCRIPT =
            "(function(){try{return Array.from(document.querySelectorAll('audio,video')).some(function(m){return !m.paused && !m.ended && m.readyState >= 2;});}catch(_){return true;}})()"
    }

    private fun startPageHtml(): String {
        val background = colorHex(R.color.phnx_navy)
        val foreground = colorHex(R.color.phnx_text)
        val muted = colorHex(R.color.phnx_muted)
        val blue = colorHex(R.color.phnx_blue)
        val profileId = profileManager.activeProfile().id
        val bookmarks = app.bookmarkManager.getRecentForProfile(profileId, 6)
        val history = app.historyManager.getRecentForProfile(profileId, 6)
        val bookmarkLinks = bookmarks.joinToString("") { bookmark ->
            "<a href='${html(bookmark.url)}' style='display:block;color:$blue;padding:8px 0'>${html(bookmark.title.ifBlank { bookmark.url })}</a>"
        }.ifBlank { "<p style='color:$muted'>No bookmarks yet.</p>" }
        val historyLinks = history.joinToString("") { entry ->
            "<a href='${html(entry.url)}' style='display:block;color:$blue;padding:8px 0'>${html(entry.title.ifBlank { entry.host })}</a>"
        }.ifBlank { "<p style='color:$muted'>No recently visited pages.</p>" }
        return """
            <!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'></head>
            <body style='margin:0;background:$background;color:$foreground;font-family:sans-serif;min-height:100vh'>
            <main style='padding:32px;max-width:620px;margin:auto'><div style='color:$blue;font-size:18px;font-weight:700;letter-spacing:.2em'>PHNX</div>
            <h1 style='font-size:42px;margin:12px 0'>A clearer way to browse.</h1>
            <p style='color:$muted;font-size:17px;line-height:1.6'>Use the address bar above to search or enter a web address.</p>
            <section><h2>Bookmarks</h2>$bookmarkLinks</section>
            <section><h2>Recently visited</h2>$historyLinks</section>
            </main></body></html>
        """.trimIndent()
    }

    private fun html(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("'", "&#39;")
        .replace("\"", "&quot;")

    private fun hideCustomView() {
        customView?.let(browserContainer::removeView)
        customView = null
        customViewCallback?.onCustomViewHidden()
        customViewCallback = null
        customViewTabId?.let { id -> tabManager.getTabs().firstOrNull { it.id == id }?.hasActiveMedia = false }
        customViewTabId = null
        WindowCompat.getInsetsController(window, window.decorView)
            .show(WindowInsetsCompat.Type.systemBars())
    }

    private fun colorHex(@androidx.annotation.ColorRes colorRes: Int): String =
        String.format("#%06X", 0xFFFFFF and getColor(colorRes))

    private fun applyBrowserModes(view: WebView) {
        view.settings.apply {
            cacheMode = if (dataSaverEnabled) WebSettings.LOAD_CACHE_ELSE_NETWORK else WebSettings.LOAD_DEFAULT
            blockNetworkImage = dataSaverEnabled
            mediaPlaybackRequiresUserGesture = true
        }
    }

    private fun applyPageControls(view: WebView) {
        view.settings.textZoom = textScalePercent
    }

    private fun applyProfileIdentity(view: WebView, config: BrowserIdentityConfig) {
        identityAdapter.apply(view, config, pageZoomPercent)
    }

    private fun applyProfileCompatibility(view: WebView, profileId: String) {
        WebViewIdentityCompatibility.install(view, effectiveIdentityConfig(profileId))
    }

    private fun identityConfig(profileId: String): BrowserIdentityConfig =
        deviceProfileManager.getProfileConfiguration(profileId)

    private fun effectiveIdentityConfig(profileId: String): BrowserIdentityConfig {
        val selected = identityConfig(profileId)
        if (!desktopSiteEnabled) return selected

        // Desktop Site is a deliberate transient runtime override; it never replaces the
        // profile-owned device selection or its persisted configuration.
        return DevicePresets.get(DevicePresets.DESKTOP)?.forProfile(profileId) ?: selected
    }

    private fun trimInactiveTabs(aggressive: Boolean = false) {
        if (mediaCheckInFlight) return
        val current = tabManager.currentTab() ?: return
        val candidates = tabManager.getTabs(current.profileId)
            .filter { it.id != current.id && !it.isLoading && !it.hasPendingWebTask }
            .mapNotNull { tab -> browserController.viewForTab(tab.id)?.let { tab to it } }
        if (candidates.isEmpty()) {
            trimInactiveTabsNow(aggressive)
            return
        }

        mediaCheckInFlight = true
        val remaining = AtomicInteger(candidates.size)
        candidates.forEach { (tab, view) ->
            val complete = {
                if (remaining.decrementAndGet() == 0) {
                    mediaCheckInFlight = false
                    trimInactiveTabsNow(aggressive)
                }
            }
            try {
                view.evaluateJavascript(ACTIVE_MEDIA_CHECK_SCRIPT) { result ->
                    // A failed or unexpected result is treated as active to avoid interrupting media.
                    tab.hasActiveMedia = result != "false"
                    complete()
                }
            } catch (_: RuntimeException) {
                tab.hasActiveMedia = true
                complete()
            }
        }
    }

    private fun trimInactiveTabsNow(aggressive: Boolean) {
        val current = tabManager.currentTab() ?: return
        browserController.trimInactiveTabs(
            tabs = tabManager.getTabs(current.profileId),
            activeTabId = current.id,
            mode = PhnxPreferences.performanceMode(this),
            aggressive = aggressive,
        )
    }

    private fun clearPendingWebTask() {
        pendingWebTaskTabId?.let { id -> tabManager.getTabs().firstOrNull { it.id == id }?.hasPendingWebTask = false }
        pendingWebTaskTabId = null
    }

    private data class WebViewConfiguration(
        val tabId: String,
        val profileId: String,
        val identity: BrowserIdentityConfig,
        val privacy: com.phoenix.phnx.privacy.PrivacySettings,
    ) {
        fun matches(other: WebViewConfiguration): Boolean =
            tabId == other.tabId && profileId == other.profileId && identity == other.identity && privacy == other.privacy
    }

}
