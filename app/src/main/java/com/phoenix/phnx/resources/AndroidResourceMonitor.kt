package com.phoenix.phnx.resources

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.Build
import android.os.Debug
import android.os.PowerManager
import android.os.Process
import android.os.SystemClock
import com.phoenix.phnx.PhnxPreferences
import com.phoenix.phnx.network.NetworkState

class AndroidResourceMonitor(context: Context) {
    private val appContext = context.applicationContext
    private val activityManager = appContext.getSystemService(ActivityManager::class.java)
    private val connectivityManager = appContext.getSystemService(ConnectivityManager::class.java)
    private val powerManager = appContext.getSystemService(PowerManager::class.java)
    private val cpuMonitor = CpuMonitor()
    private val processUid = Process.myUid()
    private val sessionStartMillis = SystemClock.elapsedRealtime()
    private val initialRxBytes = TrafficStats.getUidRxBytes(processUid)
    private val initialTxBytes = TrafficStats.getUidTxBytes(processUid)
    private var peakAppPssMb: Int? = null
    private var peakCpuPercent: Double? = null
    private var highCpuSinceMillis: Long? = null

    @Synchronized
    fun currentSnapshot(): ResourceSnapshot {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val battery = appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = battery?.getIntExtra("level", -1) ?: -1
        val scale = battery?.getIntExtra("scale", -1) ?: -1
        val batteryPercent = if (level >= 0 && scale > 0) (level * 100 / scale).coerceIn(0, 100) else 100
        val cpuPercent = cpuMonitor.samplePercent()
        val processMemory = Debug.MemoryInfo()
        Debug.getMemoryInfo(processMemory)
        val appPssMb = processMemory.totalPss / 1024
        peakAppPssMb = maxOf(peakAppPssMb ?: 0, appPssMb)
        if (cpuPercent != null) peakCpuPercent = maxOf(peakCpuPercent ?: 0.0, cpuPercent)
        val now = SystemClock.elapsedRealtime()
        if (cpuPercent != null && cpuPercent >= HIGH_CPU_PERCENT) {
            highCpuSinceMillis = highCpuSinceMillis ?: now
        } else {
            highCpuSinceMillis = null
        }
        val memoryPressure = memoryPressure(memoryInfo)
        val thermalLevel = thermalLevel()
        val processImportance = processImportance()
        val processState = processState(processImportance)
        val totalRamMb = memoryInfo.totalMem / BYTES_PER_MB
        val currentNetworkRxBytes = TrafficStats.getUidRxBytes(processUid)
        val currentNetworkTxBytes = TrafficStats.getUidTxBytes(processUid)
        return ResourceSnapshot(
            memoryPressure = memoryPressure,
            cpuPressure = cpuPressure(cpuPercent),
            cpuPercent = cpuPercent,
            thermalLevel = thermalLevel,
            thermalSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
            batteryPercent = batteryPercent,
            batterySaver = powerManager.isPowerSaveMode,
            performanceMode = PhnxPreferences.performanceMode(appContext),
            appPssMb = appPssMb,
            peakAppPssMb = peakAppPssMb,
            peakCpuPercent = peakCpuPercent,
            sessionUptimeMillis = now - sessionStartMillis,
            availableRamMb = memoryInfo.availMem / BYTES_PER_MB,
            totalRamMb = totalRamMb,
            appDalvikPssMb = processMemory.dalvikPss / 1024,
            appNativePssMb = processMemory.nativePss / 1024,
            appOtherPssMb = processMemory.otherPss / 1024,
            processState = processState,
            processImportance = processImportance,
            networkState = networkState(),
            networkRxBytes = sessionBytes(currentNetworkRxBytes, initialRxBytes),
            networkTxBytes = sessionBytes(currentNetworkTxBytes, initialTxBytes),
            warnings = warnings(
                now = now,
                memoryPressure = memoryPressure,
                appPssMb = appPssMb,
                totalRamMb = totalRamMb,
                cpuPercent = cpuPercent,
                thermalLevel = thermalLevel,
                batteryPercent = batteryPercent,
            ),
        )
    }

    private fun memoryPressure(info: ActivityManager.MemoryInfo): MemoryPressure {
        if (info.availMem <= info.threshold / 2) return MemoryPressure.CRITICAL
        if (info.availMem <= info.threshold) return MemoryPressure.HIGH
        if (info.lowMemory || info.availMem <= info.threshold * 2) return MemoryPressure.MODERATE
        return MemoryPressure.NORMAL
    }

    private fun thermalLevel(): ThermalLevel {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return ThermalLevel.THERMAL_NORMAL
        return when (powerManager.currentThermalStatus) {
            PowerManager.THERMAL_STATUS_LIGHT -> ThermalLevel.THERMAL_LIGHT
            PowerManager.THERMAL_STATUS_MODERATE -> ThermalLevel.THERMAL_MODERATE
            PowerManager.THERMAL_STATUS_SEVERE -> ThermalLevel.THERMAL_SEVERE
            PowerManager.THERMAL_STATUS_CRITICAL,
            PowerManager.THERMAL_STATUS_EMERGENCY,
            PowerManager.THERMAL_STATUS_SHUTDOWN,
            -> ThermalLevel.THERMAL_CRITICAL
            else -> ThermalLevel.THERMAL_NORMAL
        }
    }

    private fun processImportance(): Int {
        val processInfo = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(processInfo)
        return processInfo.importance
    }

    private fun processState(importance: Int): ProcessState = when {
        importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND -> ProcessState.FOREGROUND
        importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE -> ProcessState.VISIBLE
        importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE -> ProcessState.SERVICE
        importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED -> ProcessState.CACHED
        else -> ProcessState.UNKNOWN
    }

    private fun networkState(): NetworkState = runCatching {
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            ?: return@runCatching NetworkState.DISCONNECTED
        if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        ) {
            NetworkState.CONNECTED
        } else {
            NetworkState.CONNECTING
        }
    }.getOrDefault(NetworkState.DISCONNECTED)

    private fun sessionBytes(current: Long, initial: Long): Long? =
        if (current >= 0 && initial >= 0) (current - initial).coerceAtLeast(0) else null

    private fun warnings(
        now: Long,
        memoryPressure: MemoryPressure,
        appPssMb: Int,
        totalRamMb: Long,
        cpuPercent: Double?,
        thermalLevel: ThermalLevel,
        batteryPercent: Int,
    ): List<ResourceWarning> = buildList {
        val highCpuDuration = highCpuSinceMillis?.let { now - it } ?: 0L
        if (highCpuDuration >= HIGH_CPU_WARNING_DURATION_MILLIS && cpuPercent != null) {
            add(
                ResourceWarning(
                    type = ResourceWarningType.HIGH_CPU,
                    severity = if (cpuPercent >= CRITICAL_CPU_PERCENT) {
                        ResourceWarningSeverity.CRITICAL
                    } else {
                        ResourceWarningSeverity.WARNING
                    },
                ),
            )
        }

        val highMemoryThresholdMb = maxOf(MIN_HIGH_MEMORY_MB, totalRamMb / 4)
        val criticalMemoryThresholdMb = maxOf(MIN_CRITICAL_MEMORY_MB, totalRamMb / 2)
        if (appPssMb.toLong() >= highMemoryThresholdMb) {
            add(
                ResourceWarning(
                    type = ResourceWarningType.HIGH_MEMORY,
                    severity = if (appPssMb.toLong() >= criticalMemoryThresholdMb) {
                        ResourceWarningSeverity.CRITICAL
                    } else {
                        ResourceWarningSeverity.WARNING
                    },
                ),
            )
        }

        if (memoryPressure >= MemoryPressure.HIGH) {
            add(
                ResourceWarning(
                    type = ResourceWarningType.MEMORY_PRESSURE,
                    severity = if (memoryPressure == MemoryPressure.CRITICAL) {
                        ResourceWarningSeverity.CRITICAL
                    } else {
                        ResourceWarningSeverity.WARNING
                    },
                ),
            )
        }

        if (thermalLevel >= ThermalLevel.THERMAL_MODERATE) {
            add(
                ResourceWarning(
                    type = ResourceWarningType.THERMAL,
                    severity = if (thermalLevel >= ThermalLevel.THERMAL_CRITICAL) {
                        ResourceWarningSeverity.CRITICAL
                    } else {
                        ResourceWarningSeverity.WARNING
                    },
                ),
            )
        }

        if (batteryPercent <= LOW_BATTERY_PERCENT) {
            add(
                ResourceWarning(
                    type = ResourceWarningType.LOW_BATTERY,
                    severity = if (batteryPercent <= CRITICAL_BATTERY_PERCENT) {
                        ResourceWarningSeverity.CRITICAL
                    } else {
                        ResourceWarningSeverity.WARNING
                    },
                ),
            )
        }
    }

    private fun cpuPressure(percent: Double?): CpuPressure = when {
        percent == null -> CpuPressure.NORMAL
        percent >= 85.0 -> CpuPressure.CRITICAL
        percent >= 60.0 -> CpuPressure.HIGH
        else -> CpuPressure.NORMAL
    }

    private companion object {
        const val BYTES_PER_MB = 1024L * 1024L
        const val HIGH_CPU_PERCENT = 60.0
        const val CRITICAL_CPU_PERCENT = 85.0
        const val HIGH_CPU_WARNING_DURATION_MILLIS = 15_000L
        const val MIN_HIGH_MEMORY_MB = 256L
        const val MIN_CRITICAL_MEMORY_MB = 512L
        const val LOW_BATTERY_PERCENT = 15
        const val CRITICAL_BATTERY_PERCENT = 5
    }
}
