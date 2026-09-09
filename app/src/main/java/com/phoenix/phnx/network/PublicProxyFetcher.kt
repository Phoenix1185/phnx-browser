package com.phoenix.phnx.network

import java.net.HttpURLConnection
import java.net.URL
import java.io.Reader
import java.util.concurrent.Callable
import java.util.concurrent.CompletionService
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream

class PublicProxyFetcher(
    private val sources: List<ProxySource> = ProxySources.default,
    private val timeoutMillis: Int = DEFAULT_TIMEOUT_MILLIS,
) {
    fun fetch(limit: Int = DEFAULT_LIMIT): List<ProxyEndpoint> {
        val requestedLimit = limit.coerceIn(1, 30)
        if (sources.isEmpty()) return emptyList()

        val executor = Executors.newFixedThreadPool(minOf(MAX_PARALLEL_SOURCES, sources.size))
        val completion: CompletionService<List<ProxyEndpoint>> = ExecutorCompletionService(executor)
        val futures = sources.map { source ->
            completion.submit(Callable { fetchSource(source, requestedLimit) })
        }
        val endpoints = linkedMapOf<String, ProxyEndpoint>()
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(FETCH_WINDOW_MILLIS.toLong())

        try {
            var completed = 0
            while (completed < futures.size && endpoints.size < requestedLimit) {
                val remaining = deadline - System.nanoTime()
                if (remaining <= 0) break
                val future = completion.poll(remaining, TimeUnit.NANOSECONDS) ?: break
                completed++
                runCatching { future.get() }.getOrDefault(emptyList()).forEach { endpoint ->
                    endpoints.putIfAbsent("${endpoint.type}:${endpoint.host}:${endpoint.port}", endpoint)
                }
            }
            return endpoints.values.take(requestedLimit)
        } finally {
            futures.forEach { it.cancel(true) }
            executor.shutdownNow()
        }
    }

    private fun fetchSource(source: ProxySource, limit: Int): List<ProxyEndpoint> {
        val connection = runCatching { URL(source.endpoint).openConnection() as HttpURLConnection }.getOrNull()
            ?: return emptyList()
        return try {
            connection.connectTimeout = timeoutMillis
            connection.readTimeout = timeoutMillis
            connection.setRequestProperty("Accept", "application/json, text/plain;q=0.9, */*;q=0.8")
            connection.setRequestProperty("Accept-Encoding", "gzip")
            connection.setRequestProperty("User-Agent", "PHNX-Browser public proxy discovery")
            if (connection.responseCode !in 200..299) return emptyList()
            val input = if (connection.contentEncoding.equals("gzip", ignoreCase = true)) {
                GZIPInputStream(connection.inputStream)
            } else {
                connection.inputStream
            }
            val payload = input.bufferedReader().use(::readLimited) ?: return emptyList()
            PublicProxyParser.parse(source, payload, limit)
        } catch (_: Exception) {
            emptyList()
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val DEFAULT_LIMIT = 8
        const val DEFAULT_TIMEOUT_MILLIS = 5_000
        const val FETCH_WINDOW_MILLIS = 8_000
        const val MAX_PARALLEL_SOURCES = 6
        const val MAX_RESPONSE_CHARS = 512 * 1024
    }

    private fun readLimited(reader: Reader): String? {
        val output = StringBuilder()
        val buffer = CharArray(8 * 1024)
        while (true) {
            val read = reader.read(buffer)
            if (read < 0) return output.toString()
            output.append(buffer, 0, read)
            if (output.length > MAX_RESPONSE_CHARS) return null
        }
    }
}
