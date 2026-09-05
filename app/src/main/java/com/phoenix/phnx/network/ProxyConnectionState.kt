package com.phoenix.phnx.network

enum class ProxyHealthStatus {
    UNKNOWN,
    CHECKING,
    HEALTHY,
    SLOW,
    FAILED,
}

data class ProxyHealthResult(
    val endpoint: ProxyEndpoint,
    val status: ProxyHealthStatus,
    val latencyMs: Int?,
    val checkedAt: Long,
    val failureCount: Int,
    val message: String,
) {
    fun updatedEndpoint(): ProxyEndpoint = endpoint.copy(
        latencyMs = latencyMs,
        lastCheckedAt = checkedAt,
        health = status,
        failureCount = failureCount,
        lastError = if (status == ProxyHealthStatus.FAILED) message else "",
    )
}
