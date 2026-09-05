package com.phoenix.phnx.network

import android.content.Context
import androidx.room.Room

class NetworkManager(context: Context) {
    private val database = Room.databaseBuilder(
        context.applicationContext,
        NetworkDatabase::class.java,
        "network_configs.db",
    ).addMigrations(NetworkDatabase.MIGRATION_1_2).allowMainThreadQueries().build()
    private val dao = database.configDao()
    private val credentials = SecureCredentialStore(context)
    private val monitor = NetworkMonitor(context)
    private val connectionTester = ConnectionTester(credentials::getCredential)
    private val freeProxyProvider = FreeProxyProvider()

    fun getConfig(profileId: String): ProfileNetworkConfig {
        return dao.getForProfile(profileId)?.toDomain() ?: directConfig(profileId).also { dao.upsert(it.toEntity()) }
    }

    fun saveConfig(config: ProfileNetworkConfig) {
        val errors = NetworkConfigValidator.validate(config)
        require(errors.isEmpty()) { errors.joinToString(" ") }
        val previous = dao.getForProfile(config.profileId)
        if (previous?.credentialReference != config.credentialReference) {
            previous?.credentialReference?.let(credentials::deleteCredential)
        }
        dao.upsert(config.toEntity())
    }

    fun clearConfig(profileId: String) {
        dao.getForProfile(profileId)?.let { config ->
            config.credentialReference?.let(credentials::deleteCredential)
            dao.delete(config)
        }
    }

    fun testConfig(profileId: String): ConnectionTestResult =
        connectionTester.test(getConfig(profileId))

    fun applyConfig(profileId: String, adapter: ChromiumNetworkAdapter): NetworkApplyResult =
        adapter.apply(getConfig(profileId))

    fun fetchFreeProxyFallbacks(limit: Int = FreeProxyProvider.DEFAULT_LIMIT): List<ProxyEndpoint> =
        freeProxyProvider.fetch(limit)

    fun observeConnection(listener: NetworkStateListener): NetworkState =
        monitor.observeConnection(listener)

    fun detectNetworkChanges(): NetworkState = monitor.detectNetworkChanges()

    fun reportConnectionState(): NetworkState = monitor.reportConnectionState()

    fun stopObservingConnection() = monitor.stop()

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
