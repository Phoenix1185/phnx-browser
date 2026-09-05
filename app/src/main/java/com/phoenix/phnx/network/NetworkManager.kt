package com.phoenix.phnx.network

import android.content.Context
import androidx.room.Room

class NetworkManager(context: Context) {
    private val database = Room.databaseBuilder(
        context.applicationContext,
        NetworkDatabase::class.java,
        "network_configs.db",
    ).allowMainThreadQueries().build()
    private val dao = database.configDao()
    private val credentials = SecureCredentialStore(context)

    fun getConfig(profileId: String): ProfileNetworkConfig {
        return dao.getForProfile(profileId)?.toDomain() ?: directConfig(profileId).also { dao.upsert(it.toEntity()) }
    }

    fun saveConfig(config: ProfileNetworkConfig) {
        val errors = NetworkConfigValidator.validate(config)
        require(errors.isEmpty()) { errors.joinToString(" ") }
        dao.upsert(config.toEntity())
    }

    fun clearConfig(profileId: String) {
        dao.getForProfile(profileId)?.let(dao::delete)
    }

    fun saveProxyCredential(profileId: String, secret: String): String {
        val reference = "proxy_$profileId"
        require(credentials.saveCredential(reference, secret)) { "Could not save proxy credential securely." }
        return reference
    }

    fun getProxyCredential(reference: String): String? = credentials.getCredential(reference)

    fun deleteProxyCredential(reference: String) = credentials.deleteCredential(reference)

    fun close() {
        database.close()
    }

    private fun directConfig(profileId: String) = ProfileNetworkConfig(
        id = "network_$profileId",
        profileId = profileId,
    )
}
