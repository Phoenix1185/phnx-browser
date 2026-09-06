package com.phoenix.phnx.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object UpdateInstaller {
    fun installIntent(context: Context, apk: File): Intent? {
        if (!apk.isFile || !apk.name.endsWith(".apk", ignoreCase = true)) return null
        val uri = runCatching {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        }.getOrNull() ?: return null
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, APK_MIME_TYPE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
}
