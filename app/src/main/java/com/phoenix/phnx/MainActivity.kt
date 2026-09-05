package com.phoenix.phnx

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Color
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.RenderProcessGoneDetail
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
import com.phoenix.phnx.identity.DevicePresets
import com.phoenix.phnx.identity.WebViewIdentityAdapter
import com.phoenix.phnx.menu.BrowserMenu
import com.phoenix.phnx.network.NetworkApplyStatus
import com.phoenix.phnx.chromium.network.ChromiumProxyAdapter
import com.phoenix.phnx.permissions.SitePermissionDecision
import com.phoenix.phnx.permissions.SitePermission
import com.phoenix.phnx.permissions.SitePermissionType
import com.phoenix.phnx.profiles.TabSessionEntity
import com.phoenix.phnx.profiles.ProfileStatus
import com.phoenix.phnx.resources.ProfileLifecycleState
import com.phoenix.phnx.resources.ProfileResourceState
import com.phoenix.phnx.settings.SettingsActivity
import com.phoenix.phnx.tabs.Tab
import com.phoenix.phnx.tabs.TabManager
import kotlin.math.abs

class MainActivity : AppCompatActivity(), BrowserMenu.Callbacks {
    private val tabManager = TabManager()
    private val app by lazy { application as PhnxApplication }
    private val browserController by lazy { BrowserController(app.profileViewPool) }
    private val profileManager by lazy { app.profileManager }
    private val deviceProfileManager by lazy { app.deviceProfileManager }
    private val resourceManager by lazy { app.resourceManager }
    private val privacyManager by lazy { app.privacyManager }
    private val permissionManager by lazy { app.permissionManager }
    private val identityAdapter = WebViewIdentityAdapter()

    private lateinit var browserContainer: FrameLayout
    private lateinit var addressBar: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var tabCount: TextView
    private lateinit var bookmarkButton: TextView
    private lateinit var refreshButton: TextView
    private var errorView: View? = null
    private var desktopSiteEnabled = false
    private var dataSaverEnabled = false
    private var pageZoomPercent = 100
    private var textScalePercent = 100
    private var activityVisible = false
    private var attachedTabId: String? = null
    private var appliedNetworkConfigHash: Int? = null

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
    private var pendingGeolocationOrigin: String? = null
    private var pendingGeolocationCallback: GeolocationPermissions.Callback? = null
    private var pendingDownload: PendingDownload? = null

    private val webPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        val request = pendingPermissionRequest ?: return@registerForActivityResult
        pendingPermissionRequest = null
        if (results.values.all { it }) request.grant(request.resources) else request.deny()
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
    }

    private val downloadPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val download = pendingDownload
        pendingDownload = null
        if (granted && download != null) enqueueDownload(download)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferences = PhnxPreferences.store(this)
        dataSaverEnabled = preferences.getBoolean(PhnxPreferences.DATA_SAVER_ENABLED, false)
        desktopSiteEnabled = preferences.getBoolean(PhnxPreferences.DESKTOP_SITE_ENABLED, false)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = getColor(R.color.phnx_navy)
        window.navigationBarColor = getColor(R.color.phnx_navy)

        val layout = buildLayout()
        setContentView(layout)
        applySystemBarInsets(layout)

        val activeProfileId = profileManager.activeProfile().id
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
                    )
                },
                savedTabs.firstOrNull { it.isActive }?.tabId,
            )
        }
        browserController.setSessionSaver(::saveProfileSession)
        attachCurrentTab()
        openIncomingPage(intent)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val view = currentBrowserView()
                if (view?.canGoBack() == true) view.goBack() else finish()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        activityVisible = true
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
        reconcileResources()
        attachCurrentTab()
    }

    override fun onStop() {
        saveCurrentProfileSession()
        activityVisible = false
        reconcileResources()
        super.onStop()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
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

        bookmarkButton = toolbarButton("☆", "Bookmark current page")
        bookmarkButton.setOnClickListener { toggleCurrentBookmark() }
        toolbar.addView(bookmarkButton)
        refreshButton = toolbarButton("↻", getString(R.string.refresh))
        refreshButton.setOnClickListener { currentBrowserView()?.reload() }
        toolbar.addView(refreshButton)
        val menuButton = toolbarButton("⋮", "Browser menu")
        menuButton.setOnClickListener { BrowserMenu.show(menuButton, this) }
        toolbar.addView(menuButton)
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
        (webView.parent as? ViewGroup)?.removeView(webView)
        browserContainer.removeAllViews()
        browserContainer.addView(webView, FrameLayout.LayoutParams(-1, -1))
        errorView = null
        updateTabChrome(tab, webView)

        attachedTabId = tab.id
        if (tabChanged || webView.url == null) {
            if (tab.url.isBlank()) {
                webView.loadDataWithBaseURL(START_PAGE_BASE, startPageHtml(), "text/html", "UTF-8", null)
            } else if (webView.url != tab.url) {
                webView.loadUrl(tab.url)
            }
        }
    }

    private fun configureWebView(webView: BrowserView, tab: Tab) {
        if (webView.tag == tab.id) {
            applyBrowserModes(webView)
            applyProfileIdentity(webView, tab.profileId)
            applyPageControls(webView)
            privacyManager.applyTo(webView, tab.profileId)
            return
        }
        webView.tag = tab.id
        applyBrowserModes(webView)
        applyProfileIdentity(webView, tab.profileId)
        applyPageControls(webView)
        privacyManager.applyTo(webView, tab.profileId)
        webView.setOnTouchListener { _, event ->
            swipeDetector.onTouchEvent(event)
            false
        }
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                tab.isLoading = true
                tab.url = url
                tab.title = tabTitleForUrl(url)
                updateTabChrome(tab, view)
            }

            override fun onPageFinished(view: WebView, url: String) {
                tab.isLoading = false
                if (url != START_PAGE_BASE) tab.url = url
                tab.title = view.title?.takeIf { it.isNotBlank() } ?: tabTitleForUrl(url)
                privacyManager.applyTo(view, tab.profileId)
                app.historyManager.recordVisit(tab.profileId, url, tab.title, tab.isPrivate)
                updateTabChrome(tab, view)
                hideError()
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) {
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
                if (view.url == error.url) {
                    showError("The secure connection could not be verified.")
                }
            }

            override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                val isCurrentTab = tabManager.currentTab()?.id == tab.id
                browserController.suspendProfile(tab.profileId)
                attachedTabId = null
                if (isCurrentTab) {
                    showError("The page renderer stopped unexpectedly. Retry to reopen this tab.")
                }
                return true
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progressBar.progress = newProgress
                progressBar.visibility = if (newProgress in 1..99) View.VISIBLE else View.GONE
            }

            override fun onReceivedTitle(view: WebView, title: String) {
                tab.title = title.ifBlank { "New tab" }
                updateTabChrome(tab, view)
            }

            override fun onPermissionRequest(request: PermissionRequest) {
                runOnUiThread { handleWebPermissionRequest(request) }
            }

            override fun onGeolocationPermissionsShowPrompt(
                origin: String,
                callback: GeolocationPermissions.Callback,
            ) {
                runOnUiThread { handleLocationRequest(origin, callback) }
            }
        }
        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            requestDownload(url, userAgent, contentDisposition, mimeType)
        }
    }

    private fun handleWebPermissionRequest(request: PermissionRequest) {
        val types = buildList {
            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE in request.resources) add(SitePermissionType.CAMERA)
            if (PermissionRequest.RESOURCE_AUDIO_CAPTURE in request.resources) add(SitePermissionType.MICROPHONE)
        }
        if (types.isEmpty()) {
            request.deny()
            return
        }
        val profileId = tabManager.currentTab()?.profileId ?: profileManager.activeProfile().id
        val origin = request.origin.toString()
        val decisions = types.map { permissionManager.get(profileId, origin, it) }
        if (decisions.any { it == SitePermissionDecision.BLOCK }) {
            request.deny()
            return
        }
        if (decisions.all { it == SitePermissionDecision.ALLOW }) {
            requestAndroidPermissions(request)
            return
        }
        pendingPermissionRequest?.deny()
        pendingPermissionRequest = request
        AlertDialog.Builder(this)
            .setTitle("Permission request")
            .setMessage("$origin wants to use ${types.joinToString { it.name.lowercase() }}.")
            .setNegativeButton("Block") { _, _ ->
                types.forEach { type ->
                    permissionManager.save(SitePermission(profileId, origin, type, SitePermissionDecision.BLOCK))
                }
                pendingPermissionRequest = null
                request.deny()
            }
            .setPositiveButton("Allow") { _, _ ->
                types.forEach { type ->
                    permissionManager.save(SitePermission(profileId, origin, type, SitePermissionDecision.ALLOW))
                }
                requestAndroidPermissions(request)
            }
            .setOnCancelListener {
                pendingPermissionRequest = null
                request.deny()
            }
            .show()
    }

    private fun requestAndroidPermissions(request: PermissionRequest) {
        val androidPermissions = buildList {
            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE in request.resources) add(Manifest.permission.CAMERA)
            if (PermissionRequest.RESOURCE_AUDIO_CAPTURE in request.resources) add(Manifest.permission.RECORD_AUDIO)
        }
        val missing = androidPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            pendingPermissionRequest = null
            request.grant(request.resources)
        } else {
            webPermissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun handleLocationRequest(origin: String, callback: GeolocationPermissions.Callback) {
        val profileId = tabManager.currentTab()?.profileId ?: profileManager.activeProfile().id
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
        locationPermissionLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
        )
    }

    private fun requestDownload(url: String, userAgent: String, contentDisposition: String, mimeType: String) {
        val request = PendingDownload(url, userAgent, contentDisposition, mimeType)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingDownload = request
            downloadPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            enqueueDownload(request)
        }
    }

    private fun enqueueDownload(download: PendingDownload) {
        val filename = URLUtil.guessFileName(download.url, download.contentDisposition, download.mimeType)
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val request = DownloadManager.Request(Uri.parse(download.url))
            .setTitle(filename)
            .setDescription("Downloading with PHNX Browser")
            .setMimeType(download.mimeType)
            .addRequestHeader("User-Agent", download.userAgent)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
        val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = manager.enqueue(request)
        val profileId = tabManager.currentTab()?.profileId ?: profileManager.activeProfile().id
        app.downloadManager.record(profileId, downloadId, download.url, filename, download.mimeType)
        Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show()
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
        if (tabManager.currentTab()?.id == tab.id) {
            if (addressBar.text.toString() != tab.url && !addressBar.hasFocus()) addressBar.setText(tab.url)
            progressBar.visibility = if (tab.isLoading) View.VISIBLE else View.GONE
            if (::bookmarkButton.isInitialized) {
                val isBookmarked = app.bookmarkManager.getForProfile(tab.profileId).any { it.url == tab.url }
                bookmarkButton.text = if (isBookmarked) "★" else "☆"
                bookmarkButton.contentDescription = if (isBookmarked) "Remove bookmark" else "Bookmark current page"
            }
        }
        tab.canGoBack = view.canGoBack()
        tab.canGoForward = view.canGoForward()
        val profileTabCount = tabManager.tabCount(tab.profileId)
        val profileName = profileManager.getAllProfiles().firstOrNull { it.id == tab.profileId }?.name ?: "Profile"
        tabCount.text = "$profileName · $profileTabCount tab${if (profileTabCount == 1) "" else "s"}"
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
                currentBrowserView()?.reload()
            }
        })
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

    private fun showTabSwitcher() {
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(4), dp(18), 0)
        }
        val dialog = AlertDialog.Builder(this).setTitle("Open tabs").setView(list).setNegativeButton("Close", null).create()
        val profileId = profileManager.activeProfile().id
        tabManager.getTabs(profileId).forEach { tab ->
            val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
            val select = Button(this).apply {
                text = tab.title.ifBlank { "New tab" }.take(32)
                setOnClickListener {
                    tabManager.switchTab(tab.id, profileId = profileId)
                    dialog.dismiss()
                    attachCurrentTab()
                }
            }
            row.addView(select, LinearLayout.LayoutParams(0, dp(52), 1f))
            row.addView(Button(this).apply {
                text = "Close"
                contentDescription = "Close tab"
                setOnClickListener {
                    tabManager.closeTab(tab.id)
                    if (tabManager.tabCount(profileId) == 0) {
                        tabManager.createTab(profileId = profileId)
                    }
                    dialog.dismiss()
                    attachCurrentTab()
                }
            }, LinearLayout.LayoutParams(dp(80), dp(52)))
            list.addView(row)
        }
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setOnClickListener { dialog.dismiss() }
        }
        dialog.show()
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
        updateTabChrome(tab, currentBrowserView() ?: return)
    }

    override fun onHistory() = startActivity(Intent(this, HistoryActivity::class.java))

    override fun onDownloads() = startActivity(Intent(this, DownloadsActivity::class.java))

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
        val input = EditText(this).apply { hint = "Find text on this page"; isSingleLine = true }
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.find_in_page))
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Find") { _, _ -> currentBrowserView()?.findAllAsync(input.text.toString()) }
            .show()
    }

    override fun onPageZoom() {
        val view = currentBrowserView() ?: return
        val levels = intArrayOf(50, 75, 90, 100, 110, 125, 150, 175, 200)
        AlertDialog.Builder(this)
            .setTitle(R.string.page_zoom)
            .setSingleChoiceItems(levels.map { "$it%" }.toTypedArray(), levels.indexOf(pageZoomPercent).coerceAtLeast(0)) { dialog, which ->
                pageZoomPercent = levels[which]
                applyPageControls(view)
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
                applyPageControls(view)
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onDesktopSite() {
        desktopSiteEnabled = !desktopSiteEnabled
        PhnxPreferences.store(this).edit()
            .putBoolean(PhnxPreferences.DESKTOP_SITE_ENABLED, desktopSiteEnabled)
            .apply()
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
        val shortcut = ShortcutInfo.Builder(this, "page_${tab.url.hashCode()}")
            .setShortLabel(label.take(25))
            .setLongLabel(label)
            .setIcon(Icon.createWithResource(this, R.drawable.ic_launcher))
            .setIntent(Intent(Intent.ACTION_VIEW, Uri.parse(tab.url)).setClass(this, MainActivity::class.java))
            .build()
        shortcuts.requestPinShortcut(shortcut, null)
        Toast.makeText(this, getString(R.string.shortcut_requested), Toast.LENGTH_SHORT).show()
    }

    override fun onSettings() = startActivity(Intent(this, SettingsActivity::class.java))

    override fun onAbout() = startActivity(Intent(this, AboutActivity::class.java))

    private fun showPlanned(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    private fun openIncomingPage(incomingIntent: Intent?) {
        if (incomingIntent?.action != Intent.ACTION_VIEW) return
        val url = incomingIntent.dataString ?: return
        if (!url.startsWith("http://") && !url.startsWith("https://")) return
        val tab = tabManager.currentTab() ?: return
        tab.url = url
        tab.title = url
        currentBrowserView()?.loadUrl(url)
    }

    override fun onDestroy() {
        if (isFinishing) clearHistoryOnClose()
        browserController.clear()
        super.onDestroy()
    }

    private fun clearHistoryOnClose() {
        if (PhnxPreferences.historyRetention(this) == PhnxPreferences.HISTORY_CLEAR_ON_CLOSE) {
            app.historyManager.clearProfile(profileManager.activeProfile().id)
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
            profileManager.updateStatus(decision.profileId, status)
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

    private companion object {
        const val START_PAGE_BASE = "https://phnx.local/"
    }

    private fun startPageHtml(): String {
        val background = colorHex(R.color.phnx_navy)
        val foreground = colorHex(R.color.phnx_text)
        val muted = colorHex(R.color.phnx_muted)
        val blue = colorHex(R.color.phnx_blue)
        return """
            <!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'></head>
            <body style='margin:0;background:$background;color:$foreground;font-family:sans-serif;display:grid;place-items:center;min-height:100vh'>
            <main style='padding:32px;max-width:520px'><div style='color:$blue;font-size:18px;font-weight:700;letter-spacing:.2em'>PHNX</div>
            <h1 style='font-size:42px;margin:12px 0'>A clearer way to browse.</h1>
            <p style='color:$muted;font-size:17px;line-height:1.6'>Enter a web address or search term above to get started.</p></main></body></html>
        """.trimIndent()
    }

    private fun colorHex(@androidx.annotation.ColorRes colorRes: Int): String =
        String.format("#%06X", 0xFFFFFF and getColor(colorRes))

    private fun applyBrowserModes(view: WebView) {
        view.settings.apply {
            useWideViewPort = desktopSiteEnabled
            loadWithOverviewMode = desktopSiteEnabled
            cacheMode = if (dataSaverEnabled) WebSettings.LOAD_CACHE_ELSE_NETWORK else WebSettings.LOAD_DEFAULT
            blockNetworkImage = dataSaverEnabled
            mediaPlaybackRequiresUserGesture = true
        }
    }

    private fun applyPageControls(view: WebView) {
        view.setInitialScale(pageZoomPercent)
        view.settings.textZoom = textScalePercent
    }

    private fun applyProfileIdentity(view: WebView, profileId: String) {
        val config = if (desktopSiteEnabled) {
            DevicePresets.get(DevicePresets.DESKTOP)?.forProfile(profileId)
                ?: deviceProfileManager.getProfileConfiguration(profileId)
        } else {
            deviceProfileManager.getProfileConfiguration(profileId)
        }
        identityAdapter.apply(view, config)
    }
}
