package com.phoenix.phnx.system

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.phoenix.phnx.MainActivity

/** Builds the same validated browser intent used by external HTTP/HTTPS navigation. */
object BrowserNavigationIntent {
    fun forSavedUrl(context: Context, profileId: String, rawUrl: String): Intent? {
        val url = UrlIntentParser.parseUrl(rawUrl) ?: return null
        return Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(url)
            putExtra(MainActivity.EXTRA_PROFILE_ID, profileId)
        }
    }
}
