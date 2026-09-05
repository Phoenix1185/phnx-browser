package com.phoenix.phnx.resources

import java.io.File

class CpuMonitor {
    private var previousProcessTicks: Long? = null
    private var previousTotalTicks: Long? = null

    @Synchronized
    fun samplePercent(): Double? {
        val processTicks = readProcessTicks() ?: return null
        val totalTicks = readTotalTicks() ?: return null
        val previousProcess = previousProcessTicks
        val previousTotal = previousTotalTicks
        previousProcessTicks = processTicks
        previousTotalTicks = totalTicks
        if (previousProcess == null || previousTotal == null) return null
        val processDelta = processTicks - previousProcess
        val totalDelta = totalTicks - previousTotal
        if (processDelta < 0 || totalDelta <= 0) return null
        return (processDelta.toDouble() / totalDelta * Runtime.getRuntime().availableProcessors() * 100.0)
            .coerceIn(0.0, 100.0)
    }

    private fun readProcessTicks(): Long? = runCatching {
        val stat = File("/proc/self/stat").readText()
        val fields = stat.substring(stat.lastIndexOf(')') + 1).trim().split(Regex("\\s+"))
        val userTicks = fields.getOrNull(11)?.toLongOrNull() ?: return@runCatching null
        val systemTicks = fields.getOrNull(12)?.toLongOrNull() ?: return@runCatching null
        userTicks + systemTicks
    }.getOrNull()

    private fun readTotalTicks(): Long? = runCatching {
        val line = File("/proc/stat").useLines { lines -> lines.firstOrNull { it.startsWith("cpu ") } }
            ?: return@runCatching null
        line.trim().split(Regex("\\s+")).drop(1).take(8).mapNotNull(String::toLongOrNull).sum()
    }.getOrNull()
}
