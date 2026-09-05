package com.phoenix.phnx.browser

import android.content.Context
import com.phoenix.phnx.tabs.Tab

class BrowserController(private val context: Context) {
    private val views = mutableMapOf<String, BrowserView>()

    fun getOrCreate(tab: Tab): BrowserView = views.getOrPut(tab.id) { BrowserView(context) }

    fun remove(tabId: String) {
        views.remove(tabId)?.apply {
            stopLoading()
            loadUrl("about:blank")
            removeAllViews()
            destroy()
        }
    }

    fun forEachView(action: (BrowserView) -> Unit) {
        views.values.forEach(action)
    }

    fun clear() {
        views.keys.toList().forEach(::remove)
    }
}
