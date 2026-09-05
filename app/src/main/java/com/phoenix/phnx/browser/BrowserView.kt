package com.phoenix.phnx.browser

import android.content.Context
import android.webkit.WebView

class BrowserView(
    context: Context,
    val isPrivateTab: Boolean,
) : WebView(context) {
    init {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.loadsImagesAutomatically = true
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.allowFileAccess = false
        settings.allowContentAccess = true
        isLongClickable = true
        isHapticFeedbackEnabled = true
        isVerticalScrollBarEnabled = true
    }
}
