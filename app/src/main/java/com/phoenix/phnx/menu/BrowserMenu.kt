package com.phoenix.phnx.menu

import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.PopupMenu
import com.phoenix.phnx.R

object BrowserMenu {
    private const val NEW_TAB = 1
    private const val PRIVATE_TAB = 2
    private const val BOOKMARKS = 3
    private const val HISTORY = 4
    private const val DOWNLOADS = 5
    private const val SHARE = 6
    private const val FIND = 7
    private const val DESKTOP = 8
    private const val HOME = 9
    private const val SETTINGS = 10
    private const val ABOUT = 11
    private const val DATA_SAVER = 12
    private const val RELOAD = 13
    private const val PAGE_ZOOM = 14
    private const val TEXT_SIZE = 15

    interface Callbacks {
        fun onNewTab()
        fun onNewPrivateTab()
        fun onBookmarks()
        fun onHistory()
        fun onDownloads()
        fun onReload()
        fun onShare()
        fun onFindInPage()
        fun onPageZoom()
        fun onTextSize()
        fun onDesktopSite()
        fun isDesktopSiteEnabled(): Boolean
        fun onDataSaver()
        fun isDataSaverEnabled(): Boolean
        fun onAddToHomeScreen()
        fun onSettings()
        fun onAbout()
    }

    fun show(anchor: View, callbacks: Callbacks) {
        val popup = PopupMenu(anchor.context, anchor)
        popup.menu.apply {
            add(0, NEW_TAB, 0, R.string.new_tab)
            add(0, PRIVATE_TAB, 1, R.string.new_private_tab)
            add(0, BOOKMARKS, 2, R.string.bookmarks)
            add(0, HISTORY, 3, R.string.history)
            add(0, DOWNLOADS, 4, R.string.downloads)
            add(0, RELOAD, 5, R.string.reload)
            add(0, SHARE, 6, R.string.share)
            add(0, FIND, 7, R.string.find_in_page)
            add(0, PAGE_ZOOM, 8, R.string.page_zoom)
            add(0, TEXT_SIZE, 9, R.string.text_size)
            add(
                0,
                DESKTOP,
                10,
                if (callbacks.isDesktopSiteEnabled()) R.string.desktop_site_enabled else R.string.desktop_site,
            ).apply {
                isCheckable = true
                isChecked = callbacks.isDesktopSiteEnabled()
            }
            add(
                0,
                DATA_SAVER,
                11,
                if (callbacks.isDataSaverEnabled()) R.string.data_saver_enabled else R.string.data_saver,
            ).apply {
                isCheckable = true
                isChecked = callbacks.isDataSaverEnabled()
            }
            add(0, HOME, 12, R.string.add_to_home_screen)
            add(0, SETTINGS, 13, R.string.settings)
            add(0, ABOUT, 14, R.string.about_phnx)
        }
        popup.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                NEW_TAB -> callbacks.onNewTab()
                PRIVATE_TAB -> callbacks.onNewPrivateTab()
                BOOKMARKS -> callbacks.onBookmarks()
                HISTORY -> callbacks.onHistory()
                DOWNLOADS -> callbacks.onDownloads()
                RELOAD -> callbacks.onReload()
                SHARE -> callbacks.onShare()
                FIND -> callbacks.onFindInPage()
                PAGE_ZOOM -> callbacks.onPageZoom()
                TEXT_SIZE -> callbacks.onTextSize()
                DESKTOP -> callbacks.onDesktopSite()
                DATA_SAVER -> callbacks.onDataSaver()
                HOME -> callbacks.onAddToHomeScreen()
                SETTINGS -> callbacks.onSettings()
                ABOUT -> callbacks.onAbout()
                else -> return@setOnMenuItemClickListener false
            }
            true
        }
        popup.show()
    }
}
