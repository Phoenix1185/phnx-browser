package com.phoenix.phnx.downloads

import java.net.URI

data class DownloadSecurityAssessment(
    val allowed: Boolean,
    val requiresConfirmation: Boolean,
    val message: String,
)

class DownloadSecurityManager {
    fun assess(url: String, contentDisposition: String, mimeType: String): DownloadSecurityAssessment {
        val uri = runCatching { URI(url) }.getOrNull()
        if (uri?.scheme?.lowercase() !in SUPPORTED_SCHEMES || uri.host.isNullOrBlank()) {
            return DownloadSecurityAssessment(false, false, "Only downloads from valid HTTP or HTTPS URLs are allowed.")
        }

        val name = listOf(uri.path.orEmpty(), contentDisposition, mimeType)
            .joinToString(" ")
            .lowercase()
        val extension = DANGEROUS_EXTENSIONS.firstOrNull { name.contains(".$it") }
        return if (extension == null) {
            DownloadSecurityAssessment(true, false, "Download allowed.")
        } else {
            DownloadSecurityAssessment(
                allowed = true,
                requiresConfirmation = true,
                message = "This download may contain executable content (.$extension). Review it before opening.",
            )
        }
    }

    private companion object {
        val SUPPORTED_SCHEMES = setOf("http", "https")
        val DANGEROUS_EXTENSIONS = setOf(
            "apk", "aab", "bat", "cmd", "deb", "dll", "dmg", "exe", "jar", "js", "msi", "pkg", "ps1", "rpm", "sh",
        )
    }
}
