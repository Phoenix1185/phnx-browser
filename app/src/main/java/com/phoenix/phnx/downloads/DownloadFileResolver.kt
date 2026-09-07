package com.phoenix.phnx.downloads

import android.webkit.MimeTypeMap
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.Charset
import java.util.Locale

data class DownloadResolution(
    val filename: String,
    val mimeType: String,
)

/** Resolves the name and type used by Android DownloadManager for a WebView download. */
object DownloadFileResolver {
    private const val GENERIC_MIME_TYPE = "application/octet-stream"
    private const val FALLBACK_BASENAME = "download"

    private val extendedFilename = Regex(
        "(?:^|;)\\s*filename\\*\\s*=\\s*(?:\"([^\"]*)\"|([^;]*))",
        RegexOption.IGNORE_CASE,
    )
    private val regularFilename = Regex(
        "(?:^|;)\\s*filename\\s*=\\s*(?:\"([^\"]*)\"|([^;]*))",
        RegexOption.IGNORE_CASE,
    )
    private val safeExtension = Regex("[a-z0-9]{1,16}")
    private val queryFilenameKeys = setOf("filename", "file", "name", "format", "extension")

    private val explicitMimeExtensions = mapOf(
        "audio/mpeg" to "mp3",
        "audio/mp3" to "mp3",
        "audio/wav" to "wav",
        "audio/x-wav" to "wav",
        "audio/ogg" to "ogg",
        "audio/opus" to "opus",
        "audio/flac" to "flac",
        "audio/aac" to "aac",
        "video/mp4" to "mp4",
        "video/webm" to "webm",
        "image/jpeg" to "jpg",
        "image/png" to "png",
        "image/webp" to "webp",
        "application/pdf" to "pdf",
        "text/plain" to "txt",
        "application/zip" to "zip",
    )

    private val explicitExtensionMimeTypes = mapOf(
        "mp3" to "audio/mpeg",
        "wav" to "audio/wav",
        "ogg" to "audio/ogg",
        "opus" to "audio/opus",
        "flac" to "audio/flac",
        "aac" to "audio/aac",
        "mp4" to "video/mp4",
        "webm" to "video/webm",
        "jpg" to "image/jpeg",
        "jpeg" to "image/jpeg",
        "png" to "image/png",
        "webp" to "image/webp",
        "pdf" to "application/pdf",
        "txt" to "text/plain",
        "zip" to "application/zip",
    )

    fun resolve(url: String, contentDisposition: String?, mimeType: String?): DownloadResolution {
        val reportedMimeType = normalizeMimeType(mimeType)
        val dispositionName = contentDispositionFilename(contentDisposition)
        val urlName = urlPathFilename(url)
        val queryExtension = queryExtension(url)
        val typeExtension = mimeTypeToExtension(reportedMimeType)

        val preferredName = dispositionName ?: urlName ?: FALLBACK_BASENAME
        val preferredExtension = extensionOf(preferredName)
        val fallbackExtension = extensionOf(urlName) ?: queryExtension ?: typeExtension
        val extension = when {
            // A generic .bin is only a fallback. Prefer another reliable source instead.
            preferredExtension != null && !preferredExtension.equals("bin", ignoreCase = true) -> preferredExtension
            fallbackExtension != null -> fallbackExtension
            preferredExtension != null -> preferredExtension
            else -> "bin"
        }
        val filename = appendExtension(
            sanitizeFilename(preferredName) ?: FALLBACK_BASENAME,
            extension,
        )

        return DownloadResolution(
            filename = filename,
            mimeType = mimeTypeFor(filename, reportedMimeType),
        )
    }

    private fun contentDispositionFilename(header: String?): String? {
        if (header.isNullOrBlank()) return null
        val match = extendedFilename.find(header)
        if (match != null) {
            return decodeExtendedFilename(match.groupValues[1].ifBlank { match.groupValues[2] })
        }
        val regular = regularFilename.find(header) ?: return null
        return regular.groupValues[1].ifBlank { regular.groupValues[2] }
    }

    private fun decodeExtendedFilename(value: String): String? {
        val trimmed = value.trim()
        val separator = trimmed.indexOf("''")
        val charsetName = if (separator >= 0) trimmed.substring(0, separator) else "UTF-8"
        val encoded = if (separator >= 0) trimmed.substring(separator + 2) else trimmed
        val charset = runCatching { Charset.forName(charsetName.ifBlank { "UTF-8" }) }
            .getOrDefault(Charsets.UTF_8)
        return runCatching {
            // RFC 5987 uses percent encoding; a literal '+' is not a space.
            URLDecoder.decode(encoded.replace("+", "%2B"), charset.name())
        }.getOrNull()
    }

    private fun urlPathFilename(url: String): String? {
        val path = runCatching { URI(url).rawPath }.getOrNull() ?: return null
        val segment = path.substringAfterLast('/').takeIf { it.isNotBlank() } ?: return null
        return decodePathSegment(segment)
    }

    private fun queryExtension(url: String): String? {
        val query = runCatching { URI(url).rawQuery }.getOrNull() ?: return null
        return query.split('&').asSequence()
            .mapNotNull { part ->
                val separator = part.indexOf('=')
                if (separator <= 0) return@mapNotNull null
                val key = decodePathSegment(part.substring(0, separator))?.lowercase(Locale.US)
                if (key !in queryFilenameKeys) return@mapNotNull null
                val value = decodePathSegment(part.substring(separator + 1)) ?: return@mapNotNull null
                val sanitized = sanitizeFilename(value) ?: return@mapNotNull null
                extensionOf(sanitized) ?: sanitized.lowercase(Locale.US).takeIf { safeExtension.matches(it) }
            }
            .firstOrNull { isRecognizedExtension(it) }
    }

    private fun decodePathSegment(value: String): String? = runCatching {
        URLDecoder.decode(value.replace("+", "%2B"), Charsets.UTF_8.name())
    }.getOrNull()

    private fun sanitizeFilename(value: String?): String? {
        val candidate = value?.trim().orEmpty()
        if (candidate.isEmpty()) return null
        val basename = candidate.substringAfterLast('/').substringAfterLast('\\')
        val sanitized = basename
            .replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_")
            .trim()
            .take(255)
        return sanitized.takeUnless { it.isBlank() || it == "." || it == ".." }
    }

    private fun extensionOf(filename: String?): String? {
        val name = filename ?: return null
        val dot = name.lastIndexOf('.')
        if (dot <= 0 || dot == name.lastIndex) return null
        val extension = name.substring(dot + 1).lowercase(Locale.US)
        return extension.takeIf { safeExtension.matches(it) }
    }

    private fun appendExtension(filename: String, extension: String): String {
        val current = extensionOf(filename)
        if (current != null) {
            if (current.equals(extension, ignoreCase = true)) return filename
            if (!current.equals("bin", ignoreCase = true)) return filename
            return filename.substring(0, filename.lastIndexOf('.')) + "." + extension
        }
        return "$filename.$extension"
    }

    private fun isRecognizedExtension(extension: String?): Boolean {
        if (extension.isNullOrBlank() || extension == "bin") return false
        return explicitExtensionMimeTypes.containsKey(extension) ||
            standardMimeTypeFromExtension(extension) != null
    }

    private fun mimeTypeToExtension(mimeType: String): String? {
        if (mimeType.isBlank() || mimeType == GENERIC_MIME_TYPE) return null
        return explicitMimeExtensions[mimeType]
            ?: standardExtensionFromMimeType(mimeType)
                ?.lowercase(Locale.US)
                ?.takeIf { it != "bin" }
    }

    private fun mimeTypeFor(filename: String, reportedMimeType: String): String {
        val normalized = when (reportedMimeType) {
            "audio/mp3" -> "audio/mpeg"
            else -> reportedMimeType
        }
        if (normalized.isNotBlank() && normalized != GENERIC_MIME_TYPE) return normalized
        val extension = extensionOf(filename) ?: return GENERIC_MIME_TYPE
        return explicitExtensionMimeTypes[extension]
            ?: standardMimeTypeFromExtension(extension)
            ?: GENERIC_MIME_TYPE
    }

    private fun standardExtensionFromMimeType(mimeType: String): String? = runCatching {
        MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
    }.getOrNull()

    private fun standardMimeTypeFromExtension(extension: String): String? = runCatching {
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }.getOrNull()

    private fun normalizeMimeType(value: String?): String =
        value.orEmpty().substringBefore(';').trim().lowercase(Locale.US)
}
