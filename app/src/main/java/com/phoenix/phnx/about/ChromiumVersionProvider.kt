package com.phoenix.phnx.about

import android.content.Context
import android.os.Build
import android.webkit.WebView

object ChromiumVersionProvider {
    fun get(context: Context): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return "Unavailable on this Android version"
        val packageInfo = WebView.getCurrentWebViewPackage()
            ?: return "Unavailable"
        return packageInfo.versionName ?: "Unknown"
    }
}
