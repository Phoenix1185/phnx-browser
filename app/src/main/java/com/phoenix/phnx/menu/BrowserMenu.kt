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

    interface Callbacks {
        fun onNewTab()
        fun onNewPrivateTab()
        fun onBookmarks()
        fun onHistory()
        fun onDownloads()
        fun onShare()
        fun onFindInPage()
        fun onDesktopSite()
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
            add(0, SHARE, 5, R.string.share)
            add(0, FIND, 6, R.string.find_in_page)
            add(0, DESKTOP, 7, R.string.desktop_site)
            add(0, HOME, 8, R.string.add_to_home_screen)
            add(0, SETTINGS, 9, R.string.settings)
            add(0, ABOUT, 10, R.string.about_phnx)
        }
        popup.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                NEW_TAB -> callbacks.onNewTab()
                PRIVATE_TAB -> callbacks.onNewPrivateTab()
                BOOKMARKS -> callbacks.onBookmarks()
                HISTORY -> callbacks.onHistory()
                DOWNLOADS -> callbacks.onDownloads()
                SHARE -> callbacks.onShare()
                FIND -> callbacks.onFindInPage()
                DESKTOP -> callbacks.onDesktopSite()
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
