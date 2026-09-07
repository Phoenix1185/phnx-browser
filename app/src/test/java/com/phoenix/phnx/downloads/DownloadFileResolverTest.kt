package com.phoenix.phnx.downloads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DownloadFileResolverTest {
    @Test
    fun preservesContentDispositionFilenameAndInfersMimeFromOctetStream() {
        val result = resolve(
            url = "https://example.com/download?id=123",
            contentDisposition = "attachment; filename=\"song.mp3\"",
            mimeType = "application/octet-stream",
        )

        assertEquals("song.mp3", result.filename)
        assertEquals("audio/mpeg", result.mimeType)
    }

    @Test
    fun decodesUtf8ContentDispositionFilename() {
        val result = resolve(
            url = "https://example.com/download",
            contentDisposition = "attachment; filename*=UTF-8''song%20name%20%C3%A9.mp3",
            mimeType = "audio/mpeg",
        )

        assertEquals("song name é.mp3", result.filename)
        assertEquals("audio/mpeg", result.mimeType)
    }

    @Test
    fun usesUrlPathBeforeMimeAndIgnoresQueryText() {
        val result = resolve(
            url = "https://example.com/media/song.mp3?download=1&token=abc",
            contentDisposition = "",
            mimeType = "application/octet-stream",
        )

        assertEquals("song.mp3", result.filename)
        assertEquals("audio/mpeg", result.mimeType)
    }

    @Test
    fun recognizesQueryFormatWithoutMakingQueryPartOfFilename() {
        val result = resolve(
            url = "https://example.com/download?id=123&format=mp3",
            contentDisposition = "",
            mimeType = "application/octet-stream",
        )

        assertEquals("download.mp3", result.filename)
        assertEquals("audio/mpeg", result.mimeType)
    }

    @Test
    fun resolvesCommonMimeTypes() {
        val cases = mapOf(
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

        cases.forEach { (mimeType, extension) ->
            val result = resolve("https://example.com/download", "", mimeType)
            assertEquals("download.$extension", result.filename)
        }
    }

    @Test
    fun sanitizesTraversalAndInvalidCharacters() {
        val result = resolve(
            url = "https://example.com/download",
            contentDisposition = "attachment; filename=\"../../song:name.mp3\"",
            mimeType = "audio/mpeg",
        )

        assertEquals("song_name.mp3", result.filename)
        assertFalse(result.filename.contains('/'))
        assertFalse(result.filename.contains('\\'))
    }

    @Test
    fun fallsBackToBinOnlyWithoutFilenameOrUsefulMime() {
        val result = resolve(
            url = "https://example.com/download?id=123",
            contentDisposition = null,
            mimeType = "application/octet-stream",
        )

        assertEquals("download.bin", result.filename)
        assertEquals("application/octet-stream", result.mimeType)
    }

    private fun resolve(url: String, contentDisposition: String?, mimeType: String?) =
        DownloadFileResolver.resolve(url, contentDisposition, mimeType)
}
