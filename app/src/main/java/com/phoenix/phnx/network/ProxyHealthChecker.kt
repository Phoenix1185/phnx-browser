package com.phoenix.phnx.network

import java.util.concurrent.Callable
import java.util.concurrent.Executors

class ProxyHealthChecker(
    private val tester: ConnectionTester = ConnectionTester(timeoutMillis = DEFAULT_TIMEOUT_MILLIS),
) {
    fun check(endpoint: ProxyEndpoint): ProxyHealthResult {
        val checkedAt = System.currentTimeMillis()
        val result = tester.testProxy(
            ProfileNetworkConfig(
                id = "health-check",
                profileId = "health-check",
                mode = NetworkMode.FREE_PUBLIC_PROXY,
            ),
            endpoint,
        )
        val status = when {
            result.state != ConnectionTestState.SUCCESS -> ProxyHealthStatus.FAILED
            (result.latencyMs ?: Int.MAX_VALUE) > SLOW_THRESHOLD_MILLIS -> ProxyHealthStatus.SLOW
            else -> ProxyHealthStatus.HEALTHY
        }
        return ProxyHealthResult(
            endpoint = endpoint,
            status = status,
            latencyMs = result.latencyMs,
            checkedAt = checkedAt,
            failureCount = if (status == ProxyHealthStatus.FAILED) endpoint.failureCount + 1 else 0,
            message = result.message,
        )
    }

    fun checkAll(endpoints: List<ProxyEndpoint>): List<ProxyEndpoint> {
        if (endpoints.isEmpty()) return emptyList()
        val executor = Executors.newFixedThreadPool(MAX_PARALLEL_CHECKS)
        return try {
            endpoints.map { endpoint ->
                executor.submit(Callable { check(endpoint) })
            }.map { it.get().updatedEndpoint() }
        } finally {
            executor.shutdownNow()
        }
    }

    companion object {
        const val DEFAULT_TIMEOUT_MILLIS = 4_000
        const val SLOW_THRESHOLD_MILLIS = 1_500
        const val MAX_PARALLEL_CHECKS = 4
    }
}
