package com.phoenix.phnx.profiles

import android.content.Context
import androidx.room.Room
import java.io.File
import java.util.UUID

class ProfileManager(context: Context) {
    private val appContext = context.applicationContext
    private val database = Room.databaseBuilder(appContext, ProfileDatabase::class.java, "profiles.db")
        .allowMainThreadQueries()
        .build()
    private val dao = database.profileDao()

    fun ensureDefaultProfile(): ProfileEntity {
        val profiles = dao.getAll()
        if (profiles.isEmpty()) return createProfileInternal("Phoenix", id = DEFAULT_PROFILE_ID)

        val active = profiles.firstOrNull { it.status == ProfileStatus.ACTIVE } ?: profiles.first()
        if (active.status != ProfileStatus.ACTIVE) {
            val promoted = active.copy(status = ProfileStatus.ACTIVE, lastUsedAt = System.currentTimeMillis())
            dao.upsert(promoted)
            return promoted
        }
        return active
    }

    fun activeProfile(): ProfileEntity =
        dao.getAll().firstOrNull { it.status == ProfileStatus.ACTIVE } ?: ensureDefaultProfile()

    fun getAllProfiles(): List<ProfileEntity> = dao.getAll()

    fun createProfile(name: String): ProfileEntity = createProfileInternal(name.trim())

    fun renameProfile(id: String, name: String): ProfileEntity? {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return null
        val profile = dao.getById(id) ?: return null
        return profile.copy(name = cleanName).also(dao::upsert)
    }

    fun switchProfile(id: String): ProfileEntity? {
        val target = dao.getById(id) ?: return null
        val now = System.currentTimeMillis()
        dao.getAll().filter { it.status == ProfileStatus.ACTIVE && it.id != id }.forEach {
            dao.upsert(it.copy(status = ProfileStatus.IDLE, lastUsedAt = now))
        }
        return target.copy(status = ProfileStatus.ACTIVE, lastUsedAt = now).also(dao::upsert)
    }

    fun deleteProfile(id: String): Boolean {
        val profile = dao.getById(id) ?: return false
        if (profile.status == ProfileStatus.ACTIVE) return false
        dao.delete(profile)
        File(profile.storagePath).deleteRecursively()
        return true
    }

    fun close() {
        database.close()
    }

    private fun createProfileInternal(name: String, id: String = "profile_${UUID.randomUUID()}"): ProfileEntity {
        val now = System.currentTimeMillis()
        val storage = File(appContext.filesDir, "profiles/$id").apply { mkdirs() }
        val status = if (dao.getAll().isEmpty()) ProfileStatus.ACTIVE else ProfileStatus.IDLE
        return ProfileEntity(id, name.ifBlank { "Phoenix" }, now, now, status, storage.absolutePath)
            .also { dao.upsert(it) }
    }

    private companion object {
        const val DEFAULT_PROFILE_ID = "profile_default"
    }
}
