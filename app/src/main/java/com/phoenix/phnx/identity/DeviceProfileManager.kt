package com.phoenix.phnx.identity

import android.content.Context
import androidx.room.Room
import java.util.concurrent.ConcurrentHashMap

class DeviceProfileManager(context: Context) {
    private val appContext = context.applicationContext
    private val database = Room.databaseBuilder(
        appContext,
        IdentityDatabase::class.java,
        "profile_identities.db",
    ).allowMainThreadQueries().build()
    private val dao = database.identityDao()
    private val profileCache = ConcurrentHashMap<String, BrowserIdentityConfig>()

    fun getAvailablePresets(): List<DevicePreset> = listOf(DevicePresets.systemDefault(appContext)) + DevicePresets.all()

    fun getPreset(id: String): DevicePreset? =
        if (id == DevicePresets.SYSTEM_DEFAULT) DevicePresets.systemDefault(appContext) else DevicePresets.get(id)

    fun getProfileConfiguration(profileId: String): BrowserIdentityConfig {
        return profileCache[profileId] ?: synchronized(this) {
            profileCache[profileId] ?: (dao.get(profileId)?.toDomain()
                ?: DevicePresets.systemDefault(appContext).forProfile(profileId).also(::save))
                .also { profileCache[profileId] = it }
        }
    }

    fun updateProfileConfiguration(config: BrowserIdentityConfig) {
        save(config)
        profileCache[config.profileId] = config
    }

    fun applyPreset(profileId: String, presetId: String): BrowserIdentityConfig {
        val config = getPreset(presetId)?.forProfile(profileId)
            ?: throw IllegalArgumentException("Unknown device preset: $presetId")
        save(config)
        profileCache[config.profileId] = config
        return config
    }

    fun validateProfileConfiguration(profileId: String): List<String> =
        DeviceProfileValidator.validate(getProfileConfiguration(profileId))

    fun resetProfileConfiguration(profileId: String): BrowserIdentityConfig =
        applyPreset(profileId, DevicePresets.SYSTEM_DEFAULT)

    fun clearProfileConfiguration(profileId: String) {
        dao.get(profileId)?.let(dao::delete)
        profileCache.remove(profileId)
    }

    fun close() {
        database.close()
    }

    private fun save(config: BrowserIdentityConfig) {
        val errors = DeviceProfileValidator.validate(config)
        require(errors.isEmpty()) { errors.joinToString(" ") }
        dao.upsert(config.toEntity())
    }
}
