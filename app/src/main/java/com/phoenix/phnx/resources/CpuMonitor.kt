package com.phoenix.phnx.resources

import java.io.File
import android.os.Debug
import android.os.SystemClock

class CpuMonitor {
    private var previousProcessTicks: Long? = null
    private var previousTotalTicks: Long? = null
    private var previousThreadCpuNanos: Long? = null
    private var previousWallNanos: Long? = null
    private var lastPercent: Double? = null

    init {
        previousProcessTicks = readProcessTicks()
        previousTotalTicks = readTotalTicks()
        previousThreadCpuNanos = Debug.threadCpuTimeNanos()
        previousWallNanos = SystemClock.elapsedRealtimeNanos()
    }

    @Synchronized
    fun samplePercent(): Double? {
        val processTicks = readProcessTicks()
        val totalTicks = readTotalTicks()
        val previousProcess = previousProcessTicks
        val previousTotal = previousTotalTicks
        if (processTicks != null && totalTicks != null) {
            previousProcessTicks = processTicks
            previousTotalTicks = totalTicks
            if (previousProcess != null && previousTotal != null) {
                val processDelta = processTicks - previousProcess
                val totalDelta = totalTicks - previousTotal
                if (processDelta >= 0 && totalDelta > 0) {
                    lastPercent = (processDelta.toDouble() / totalDelta * Runtime.getRuntime().availableProcessors() * 100.0)
                        .coerceIn(0.0, 100.0)
                    return lastPercent
                }
            }
        }
        return sampleThreadFallback()?.also { lastPercent = it } ?: lastPercent
    }

    private fun sampleThreadFallback(): Double? {
        val cpuNanos = Debug.threadCpuTimeNanos()
        val wallNanos = SystemClock.elapsedRealtimeNanos()
        val previousCpu = previousThreadCpuNanos
        val previousWall = previousWallNanos
        previousThreadCpuNanos = cpuNanos
        previousWallNanos = wallNanos
        if (previousCpu == null || previousWall == null || wallNanos <= previousWall) return null
        return ((cpuNanos - previousCpu).toDouble() / (wallNanos - previousWall) * 100.0)
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
