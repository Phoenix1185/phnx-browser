package com.phoenix.phnx.tabs

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import android.webkit.WebView
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.math.min

/**
 * Keeps tab previews separate from tab/session state. WebViews can be destroyed and recreated
 * without losing the last useful visual snapshot for a regular tab.
 */
class TabPreviewStore(context: android.content.Context) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val io = Executors.newSingleThreadExecutor()
    private val diskRoot = File(appContext.cacheDir, "tab-previews")
    private val previews = object : LruCache<String, Bitmap>(maxCacheKilobytes()) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }
    private val favicons = object : LruCache<String, Bitmap>(512) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }
    private val pendingWrites = ConcurrentHashMap.newKeySet<String>()
    private val removedKeys = ConcurrentHashMap.newKeySet<String>()

    fun capture(tab: Tab, view: WebView) {
        if (tab.isPrivate) return
        if (view.width <= 0 || view.height <= 0) return

        val scale = min(MAX_WIDTH.toFloat() / view.width, MAX_HEIGHT.toFloat() / view.height).coerceAtMost(1f)
        val width = (view.width * scale).toInt().coerceAtLeast(1)
        val height = (view.height * scale).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        Canvas(bitmap).apply {
            scale(scale, scale)
            view.draw(this)
        }
        val key = key(tab.profileId, tab.id)
        removedKeys.remove(key)
        previews.put(key, bitmap)
        if (!pendingWrites.add(key)) return
        val file = previewFile(tab.profileId, tab.id)
        io.execute {
            try {
                if (!removedKeys.contains(key)) {
                    file.parentFile?.mkdirs()
                    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 82, it) }
                }
            } finally {
                pendingWrites.remove(key)
            }
        }
    }

    fun setFavicon(tab: Tab, favicon: Bitmap?) {
        if (tab.isPrivate) return
        if (favicon != null) favicons.put(key(tab.profileId, tab.id), favicon)
    }

    fun loadPreview(item: TabOverviewItem, callback: (Bitmap?) -> Unit) {
        val cacheKey = key(item.profileId, item.id)
        previews.get(cacheKey)?.let { cached ->
            mainHandler.post { callback(cached) }
            return
        }
        if (item.isPrivate) {
            mainHandler.post { callback(null) }
            return
        }
        io.execute {
            val file = previewFile(item.profileId, item.id)
            val decoded = if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
            if (decoded != null) previews.put(cacheKey, decoded)
            mainHandler.post { callback(decoded) }
        }
    }

    fun loadFavicon(item: TabOverviewItem, callback: (Bitmap?) -> Unit) {
        val favicon = favicons.get(key(item.profileId, item.id))
        mainHandler.post { callback(favicon) }
    }

    fun remove(tab: Tab) {
        val cacheKey = key(tab.profileId, tab.id)
        removedKeys.add(cacheKey)
        previews.remove(cacheKey)
        favicons.remove(cacheKey)
        if (!tab.isPrivate) io.execute { previewFile(tab.profileId, tab.id).delete() }
    }

    fun clearProfile(profileId: String) {
        val prefix = "$profileId/"
        previews.snapshot().keys.filter { it.startsWith(prefix) }.forEach(previews::remove)
        favicons.snapshot().keys.filter { it.startsWith(prefix) }.forEach(favicons::remove)
        io.execute { File(diskRoot, profileId).deleteRecursively() }
    }

    fun shutdown() {
        io.shutdownNow()
    }

    private fun previewFile(profileId: String, tabId: String): File = File(File(diskRoot, profileId), "$tabId.jpg")

    private fun key(profileId: String, tabId: String): String = "$profileId/$tabId"

    private fun maxCacheKilobytes(): Int =
        (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt().coerceIn(4 * 1024, 32 * 1024)

    private companion object {
        const val MAX_WIDTH = 640
        const val MAX_HEIGHT = 400
    }
}
