package com.phoenix.phnx.resources

enum class MemoryPressure {
    NORMAL,
    LOW,
    MODERATE,
    HIGH,
    CRITICAL,
}

enum class CpuPressure {
    LOW,
    NORMAL,
    HIGH,
    CRITICAL,
}

enum class ThermalLevel {
    THERMAL_NORMAL,
    THERMAL_LIGHT,
    THERMAL_MODERATE,
    THERMAL_SEVERE,
    THERMAL_CRITICAL,
}

data class ResourceSnapshot(
    val memoryPressure: MemoryPressure = MemoryPressure.NORMAL,
    val cpuPressure: CpuPressure = CpuPressure.NORMAL,
    val thermalLevel: ThermalLevel = ThermalLevel.THERMAL_NORMAL,
    val thermalSupported: Boolean = false,
    val batteryPercent: Int = 100,
    val batterySaver: Boolean = false,
)

data class ProfileResourceDecision(
    val profileId: String,
    val from: ProfileLifecycleState,
    val to: ProfileLifecycleState,
    val reason: String,
)

class ResourcePolicy {
    fun decide(profile: ProfileResourceState, snapshot: ResourceSnapshot): ProfileResourceDecision {
        val target = when {
            profile.lifecycleState == ProfileLifecycleState.CLOSED -> ProfileLifecycleState.CLOSED
            profile.foreground -> ProfileLifecycleState.ACTIVE
            pressure(snapshot) >= Pressure.CRITICAL -> ProfileLifecycleState.SUSPENDED
            pressure(snapshot) >= Pressure.HIGH -> ProfileLifecycleState.FROZEN
            pressure(snapshot) >= Pressure.MODERATE -> ProfileLifecycleState.IDLE
            else -> ProfileLifecycleState.IDLE
        }
        return ProfileResourceDecision(
            profileId = profile.profileId,
            from = profile.lifecycleState,
            to = target,
            reason = reasonFor(target),
        )
    }

    private fun pressure(snapshot: ResourceSnapshot): Pressure {
        var result = maxOf(
            memoryPressure(snapshot.memoryPressure),
            cpuPressure(snapshot.cpuPressure),
            thermalPressure(snapshot.thermalLevel),
        )
        if (snapshot.batterySaver && snapshot.batteryPercent <= 20) result = maxOf(result, Pressure.HIGH)
        return result
    }

    private fun memoryPressure(value: MemoryPressure): Pressure = when (value) {
        MemoryPressure.NORMAL, MemoryPressure.LOW -> Pressure.NORMAL
        MemoryPressure.MODERATE -> Pressure.MODERATE
        MemoryPressure.HIGH -> Pressure.HIGH
        MemoryPressure.CRITICAL -> Pressure.CRITICAL
    }

    private fun cpuPressure(value: CpuPressure): Pressure = when (value) {
        CpuPressure.LOW, CpuPressure.NORMAL -> Pressure.NORMAL
        CpuPressure.HIGH -> Pressure.HIGH
        CpuPressure.CRITICAL -> Pressure.CRITICAL
    }

    private fun thermalPressure(value: ThermalLevel): Pressure = when (value) {
        ThermalLevel.THERMAL_NORMAL, ThermalLevel.THERMAL_LIGHT -> Pressure.NORMAL
        ThermalLevel.THERMAL_MODERATE -> Pressure.MODERATE
        ThermalLevel.THERMAL_SEVERE -> Pressure.HIGH
        ThermalLevel.THERMAL_CRITICAL -> Pressure.CRITICAL
    }

    private fun reasonFor(target: ProfileLifecycleState): String = when (target) {
        ProfileLifecycleState.ACTIVE -> "Foreground profile remains active."
        ProfileLifecycleState.IDLE -> "Background activity is reduced."
        ProfileLifecycleState.FROZEN -> "Background profile is frozen under resource pressure."
        ProfileLifecycleState.SUSPENDED -> "Background profile is suspended under severe resource pressure."
        ProfileLifecycleState.RECREATING -> "Profile view is being recreated from persisted session state."
        ProfileLifecycleState.CLOSED -> "Profile is closed and retains persistent data."
    }

    private enum class Pressure {
        NORMAL,
        MODERATE,
        HIGH,
        CRITICAL,
    }
}
