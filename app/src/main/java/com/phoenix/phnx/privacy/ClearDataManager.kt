package com.phoenix.phnx.privacy

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewDatabase

data class ClearDataRequest(
    val cookies: Boolean = true,
    val siteStorage: Boolean = true,
    val cache: Boolean = true,
)

data class ClearDataResult(
    val cookies: Boolean,
    val siteStorage: Boolean,
    val cache: Boolean,
)

class ClearDataManager(context: Context) {
    private val appContext = context.applicationContext

    fun clear(
        request: ClearDataRequest,
        views: Collection<WebView>,
        onComplete: (ClearDataResult) -> Unit,
    ) {
        if (request.cache) {
            views.forEach { view ->
                view.clearCache(true)
                view.clearHistory()
                view.clearFormData()
            }
            WebViewDatabase.getInstance(appContext).clearHttpAuthUsernamePassword()
        }
        if (request.siteStorage) WebStorage.getInstance().deleteAllData()
        if (request.cookies) {
            CookieManager.getInstance().removeAllCookies {
                CookieManager.getInstance().flush()
                onComplete(ClearDataResult(request.cookies, request.siteStorage, request.cache))
            }
        } else {
            onComplete(ClearDataResult(request.cookies, request.siteStorage, request.cache))
        }
    }
}
