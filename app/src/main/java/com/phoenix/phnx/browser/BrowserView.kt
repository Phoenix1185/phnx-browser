package com.phoenix.phnx.browser

import android.content.Context
import android.os.Build
import android.webkit.WebView
import android.webkit.WebSettings

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
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            importantForAutofill = if (isPrivateTab) {
                IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
            } else {
                IMPORTANT_FOR_AUTOFILL_YES
            }
        }
        isLongClickable = true
        isHapticFeedbackEnabled = true
        isVerticalScrollBarEnabled = true
    }
}
