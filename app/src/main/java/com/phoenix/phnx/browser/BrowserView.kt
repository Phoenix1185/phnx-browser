package com.phoenix.phnx.browser

import android.content.Context
import android.webkit.WebView

class BrowserView(context: Context) : WebView(context) {
    init {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.loadsImagesAutomatically = true
        settings.builtInZoomControls = false
        settings.displayZoomControls = false
        settings.allowFileAccess = false
        settings.allowContentAccess = true
        isVerticalScrollBarEnabled = true
    }
}
