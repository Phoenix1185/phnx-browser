package com.phoenix.phnx.identity

import android.content.Context
import androidx.room.Room

class DeviceProfileManager(context: Context) {
    private val appContext = context.applicationContext
    private val database = Room.databaseBuilder(
        appContext,
        IdentityDatabase::class.java,
        "profile_identities.db",
    ).allowMainThreadQueries().build()
    private val dao = database.identityDao()

    fun getAvailablePresets(): List<DevicePreset> = listOf(DevicePresets.systemDefault(appContext)) + DevicePresets.all()

    fun getPreset(id: String): DevicePreset? =
        if (id == DevicePresets.SYSTEM_DEFAULT) DevicePresets.systemDefault(appContext) else DevicePresets.get(id)

    fun getProfileConfiguration(profileId: String): BrowserIdentityConfig {
        return dao.get(profileId)?.toDomain() ?: DevicePresets.systemDefault(appContext).forProfile(profileId).also(::save)
    }

    fun updateProfileConfiguration(config: BrowserIdentityConfig) {
        save(config)
    }

    fun applyPreset(profileId: String, presetId: String): BrowserIdentityConfig {
        val config = getPreset(presetId)?.forProfile(profileId)
            ?: throw IllegalArgumentException("Unknown device preset: $presetId")
        save(config)
        return config
    }

    fun validateProfileConfiguration(profileId: String): List<String> =
        DeviceProfileValidator.validate(getProfileConfiguration(profileId))

    fun resetProfileConfiguration(profileId: String): BrowserIdentityConfig =
        applyPreset(profileId, DevicePresets.SYSTEM_DEFAULT)

    fun clearProfileConfiguration(profileId: String) {
        dao.get(profileId)?.let(dao::delete)
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
