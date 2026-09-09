package com.phoenix.phnx.network

import android.content.Context
import androidx.room.Room
import java.util.concurrent.ConcurrentHashMap

class NetworkManager(context: Context) {
    private val database = Room.databaseBuilder(
        context.applicationContext,
        NetworkDatabase::class.java,
        "network_configs.db",
    ).addMigrations(NetworkDatabase.MIGRATION_1_2).allowMainThreadQueries().build()
    private val dao = database.configDao()
    private val credentials = ProxyCredentialStore(context)
    private val monitor = NetworkMonitor(context)
    private val connectionTester = ConnectionTester(credentials::getCredential)
    private val proxyManager = ProxyManager()
    private val configCache = ConcurrentHashMap<String, ProfileNetworkConfig>()

    fun getConfig(profileId: String): ProfileNetworkConfig = configCache[profileId] ?: synchronized(this) {
        configCache[profileId] ?: (dao.getForProfile(profileId)?.toDomain()
            ?: directConfig(profileId).also { dao.upsert(it.toEntity()) })
            .also { configCache[profileId] = it }
    }

    fun saveConfig(config: ProfileNetworkConfig) {
        val errors = NetworkConfigValidator.validate(config)
        require(errors.isEmpty()) { errors.joinToString(" ") }
        val previous = dao.getForProfile(config.profileId)
        if (previous?.credentialReference != config.credentialReference) {
            previous?.credentialReference?.let(credentials::deleteCredential)
        }
        dao.upsert(config.toEntity())
        configCache[config.profileId] = config
    }

    fun clearConfig(profileId: String) {
        dao.getForProfile(profileId)?.let { config ->
            config.credentialReference?.let(credentials::deleteCredential)
            dao.delete(config)
        }
        configCache.remove(profileId)
    }

    fun testConfig(profileId: String): ConnectionTestResult {
        val config = getConfig(profileId)
        if (config.mode != NetworkMode.PROXY || config.proxyHost.isNotBlank() || !config.fallbackToFreeProxy) {
            return connectionTester.test(config)
        }

        val endpoints = config.freeProxyFallbacks.ifEmpty { proxyManager.fetchAndCheck() }
        if (endpoints.isEmpty()) {
            return ConnectionTestResult(
                state = ConnectionTestState.FAILURE,
                message = "No free proxy routes are available to test.",
            )
        }

        val attempts = endpoints.map { endpoint ->
            endpoint to connectionTester.testProxy(config, endpoint)
        }
        val successful = attempts.firstOrNull { (_, result) -> result.state == ConnectionTestState.SUCCESS }
        if (successful != null) {
            val (endpoint, result) = successful
            return result.copy(message = "Free proxy test succeeded via ${endpoint.host}:${endpoint.port}.")
        }

        val lastResult = attempts.last().second
        return lastResult.copy(
            message = "No free proxy route succeeded after ${attempts.size} attempt(s). " +
                "Last error: ${lastResult.message}",
        )
    }

    fun applyConfig(profileId: String, adapter: ChromiumNetworkAdapter): NetworkApplyResult =
        adapter.apply(getConfig(profileId))

    fun fetchPublicProxies(
        profileId: String,
        limit: Int = PublicProxyFetcher.DEFAULT_LIMIT,
        forceRefresh: Boolean = false,
    ): List<ProxyEndpoint> {
        val proxies = proxyManager.fetchAndCheck(limit, forceRefresh)
        val config = getConfig(profileId)
        saveConfig(config.copy(freeProxyFallbacks = proxies))
        return proxies
    }

    fun refreshProxyHealth(profileId: String, endpoint: ProxyEndpoint): ProxyEndpoint {
        val updated = proxyManager.check(endpoint)
        val config = getConfig(profileId)
        val proxies = config.freeProxyFallbacks.map {
            if (it.type == endpoint.type && it.host == endpoint.host && it.port == endpoint.port) updated else it
        }
        saveConfig(config.copy(freeProxyFallbacks = proxies))
        return updated
    }

    fun selectBestProxy(proxies: List<ProxyEndpoint>): ProxyEndpoint? = proxyManager.bestHealthy(proxies)

    fun testProxy(profileId: String, endpoint: ProxyEndpoint): ConnectionTestResult =
        connectionTester.testProxy(
            getConfig(profileId).copy(
                mode = NetworkMode.FREE_PUBLIC_PROXY,
                enabled = true,
            ),
            endpoint,
        )

    fun fetchFreeProxyFallbacks(limit: Int = PublicProxyFetcher.DEFAULT_LIMIT): List<ProxyEndpoint> =
        proxyManager.fetchAndCheck(limit, forceRefresh = true)

    fun observeConnection(listener: NetworkStateListener): NetworkState =
        monitor.observeConnection(listener)

    fun detectNetworkChanges(): NetworkState = monitor.detectNetworkChanges()

    fun reportConnectionState(): NetworkState = monitor.reportConnectionState()

    fun stopObservingConnection() = monitor.stop()

    fun stopObservingConnection(listener: NetworkStateListener) = monitor.stop(listener)

    fun saveProxyCredential(profileId: String, secret: String): String {
        val reference = "proxy_$profileId"
        require(credentials.saveCredential(reference, secret)) { "Could not save proxy credential securely." }
        return reference
    }

    fun getProxyCredential(reference: String): String? = credentials.getCredential(reference)

    fun deleteProxyCredential(reference: String) = credentials.deleteCredential(reference)

    fun close() {
        monitor.stop()
        database.close()
    }

    private fun directConfig(profileId: String) = ProfileNetworkConfig(
        id = "network_$profileId",
        profileId = profileId,
    )
}
