package com.phoenix.phnx.resources

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.PowerManager

class AndroidResourceMonitor(context: Context) {
    private val appContext = context.applicationContext
    private val activityManager = appContext.getSystemService(ActivityManager::class.java)
    private val powerManager = appContext.getSystemService(PowerManager::class.java)
    private val cpuMonitor = CpuMonitor()

    fun currentSnapshot(): ResourceSnapshot {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val battery = appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = battery?.getIntExtra("level", -1) ?: -1
        val scale = battery?.getIntExtra("scale", -1) ?: -1
        val batteryPercent = if (level >= 0 && scale > 0) (level * 100 / scale).coerceIn(0, 100) else 100
        val cpuPercent = cpuMonitor.samplePercent()
        return ResourceSnapshot(
            memoryPressure = memoryPressure(memoryInfo),
            cpuPressure = cpuPressure(cpuPercent),
            cpuPercent = cpuPercent,
            thermalLevel = thermalLevel(),
            thermalSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
            batteryPercent = batteryPercent,
            batterySaver = powerManager.isPowerSaveMode,
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

    private fun cpuPressure(percent: Double?): CpuPressure = when {
        percent == null -> CpuPressure.NORMAL
        percent >= 85.0 -> CpuPressure.CRITICAL
        percent >= 60.0 -> CpuPressure.HIGH
        else -> CpuPressure.NORMAL
    }
}
