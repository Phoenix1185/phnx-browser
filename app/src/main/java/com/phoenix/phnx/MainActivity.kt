package com.phoenix.phnx

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
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
import com.phoenix.phnx.about.AboutActivity
import com.phoenix.phnx.browser.BrowserController
import com.phoenix.phnx.browser.BrowserView
import com.phoenix.phnx.browser.NavigationController
import com.phoenix.phnx.menu.BrowserMenu
import com.phoenix.phnx.settings.SettingsActivity
import com.phoenix.phnx.tabs.Tab
import com.phoenix.phnx.tabs.TabManager

class MainActivity : AppCompatActivity(), BrowserMenu.Callbacks {
    private val tabManager = TabManager()
    private val browserController by lazy { BrowserController(this) }

    private lateinit var browserContainer: FrameLayout
    private lateinit var addressBar: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var tabCount: TextView
    private var errorView: View? = null
    private var desktopSiteEnabled = false

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
        setContentView(buildLayout())

        tabManager.createTab()
        attachCurrentTab()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val view = currentBrowserView()
                if (view?.canGoBack() == true) view.goBack() else finish()
            }
        })
    }

    private fun buildLayout(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(getColor(R.color.phnx_cream))
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
            setTextColor(Color.WHITE)
            setHintTextColor(0xFFB8C2D9.toInt())
            setPadding(dp(10), 0, dp(10), 0)
            setOnEditorActionListener { _, _, _ -> navigateFromAddressBar(); true }
        }
        toolbar.addView(addressBar, LinearLayout.LayoutParams(0, dp(48), 1f))

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
            setTextColor(Color.WHITE)
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

    private fun attachCurrentTab() {
        val tab = tabManager.currentTab() ?: return
        val webView = browserController.getOrCreate(tab)
        configureWebView(webView, tab)
        (webView.parent as? ViewGroup)?.removeView(webView)
        browserContainer.removeAllViews()
        browserContainer.addView(webView, FrameLayout.LayoutParams(-1, -1))
        errorView = null
        updateTabChrome(tab, webView)

        if (webView.url == null) {
            if (tab.url.isBlank()) {
                webView.loadDataWithBaseURL(START_PAGE_BASE, START_PAGE_HTML, "text/html", "UTF-8", null)
            } else {
                webView.loadUrl(tab.url)
            }
        }
    }

    private fun configureWebView(webView: BrowserView, tab: Tab) {
        if (webView.tag == tab.id) return
        webView.tag = tab.id
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                tab.isLoading = true
                tab.url = url
                updateTabChrome(tab, view)
            }

            override fun onPageFinished(view: WebView, url: String) {
                tab.isLoading = false
                if (url != START_PAGE_BASE) tab.url = url
                tab.title = view.title?.takeIf { it.isNotBlank() } ?: tab.title
                updateTabChrome(tab, view)
                hideError()
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) showError(error.description?.toString() ?: "The page could not be loaded.")
            }

            override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
                if (request.isForMainFrame && errorResponse.statusCode >= 400) {
                    showError("The page returned an error (${errorResponse.statusCode}).")
                }
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
        val androidPermissions = buildList {
            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE in request.resources) add(Manifest.permission.CAMERA)
            if (PermissionRequest.RESOURCE_AUDIO_CAPTURE in request.resources) add(Manifest.permission.RECORD_AUDIO)
        }
        if (androidPermissions.isEmpty()) {
            request.deny()
            return
        }
        pendingPermissionRequest?.deny()
        pendingPermissionRequest = request
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
        manager.enqueue(request)
        Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show()
    }

    private fun navigateFromAddressBar() {
        val tab = tabManager.currentTab() ?: return
        val destination = NavigationController.resolveInput(addressBar.text.toString())
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
        }
        tab.canGoBack = view.canGoBack()
        tab.canGoForward = view.canGoForward()
        tabCount.text = "${tabManager.tabCount()} tab${if (tabManager.tabCount() == 1) "" else "s"}"
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

    private fun showTabSwitcher() {
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(4), dp(18), 0)
        }
        val dialog = AlertDialog.Builder(this).setTitle("Open tabs").setView(list).setNegativeButton("Close", null).create()
        tabManager.getTabs().forEach { tab ->
            val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
            val select = Button(this).apply {
                text = tab.title.ifBlank { "New tab" }.take(32)
                setOnClickListener {
                    tabManager.switchTab(tab.id)
                    dialog.dismiss()
                    attachCurrentTab()
                }
            }
            row.addView(select, LinearLayout.LayoutParams(0, dp(52), 1f))
            row.addView(Button(this).apply {
                text = "Close"
                contentDescription = "Close tab"
                setOnClickListener {
                    browserController.remove(tab.id)
                    tabManager.closeTab(tab.id)
                    if (tabManager.tabCount() == 0) tabManager.createTab()
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
        setTextColor(Color.WHITE)
        isClickable = true
        isFocusable = true
        setPadding(dp(6), 0, dp(6), 0)
        layoutParams = LinearLayout.LayoutParams(dp(44), dp(48))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onNewTab() {
        tabManager.createTab()
        attachCurrentTab()
    }

    override fun onNewPrivateTab() {
        tabManager.createTab(isPrivate = true)
        attachCurrentTab()
        Toast.makeText(this, "Private tab entry point opened; private isolation is planned for Phase 6.", Toast.LENGTH_LONG).show()
    }

    override fun onBookmarks() = showPlanned("Bookmarks are planned for a later phase.")

    override fun onHistory() = showPlanned("History is planned for a later phase.")

    override fun onDownloads() = showPlanned("Downloads are available through Android's Downloads app.")

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

    override fun onDesktopSite() {
        val view = currentBrowserView() ?: return
        desktopSiteEnabled = !desktopSiteEnabled
        view.settings.userAgentString = if (desktopSiteEnabled) DESKTOP_USER_AGENT else WebSettings.getDefaultUserAgent(this)
        view.reload()
        Toast.makeText(this, if (desktopSiteEnabled) "Desktop site enabled" else "Mobile site enabled", Toast.LENGTH_SHORT).show()
    }

    override fun onAddToHomeScreen() = showPlanned("Home-screen shortcuts will be available in a later phase.")

    override fun onSettings() = startActivity(Intent(this, SettingsActivity::class.java))

    override fun onAbout() = startActivity(Intent(this, AboutActivity::class.java))

    private fun showPlanned(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    override fun onDestroy() {
        browserController.clear()
        super.onDestroy()
    }

    private data class PendingDownload(
        val url: String,
        val userAgent: String,
        val contentDisposition: String,
        val mimeType: String,
    )

    private companion object {
        const val START_PAGE_BASE = "https://phnx.local/"
        const val DESKTOP_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/131.0 Safari/537.36"
        const val START_PAGE_HTML = """
            <!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'></head>
            <body style='margin:0;background:#111a2e;color:#f7f8fc;font-family:sans-serif;display:grid;place-items:center;min-height:100vh'>
            <main style='padding:32px;max-width:520px'><div style='color:#326bff;font-size:18px;font-weight:700;letter-spacing:.2em'>PHNX</div>
            <h1 style='font-size:42px;margin:12px 0'>A clearer way to browse.</h1>
            <p style='color:#b8c2d9;font-size:17px;line-height:1.6'>Enter a web address or search term above to get started.</p></main></body></html>
        """
    }
}
