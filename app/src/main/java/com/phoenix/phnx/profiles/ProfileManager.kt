package com.phoenix.phnx.profiles

import android.content.Context
import androidx.room.Room
import java.io.File
import java.util.UUID

class ProfileManager(context: Context) {
    private val appContext = context.applicationContext
    private val database = Room.databaseBuilder(appContext, ProfileDatabase::class.java, "profiles.db")
        .addMigrations(ProfileDatabase.MIGRATION_1_2, ProfileDatabase.MIGRATION_2_3)
        .allowMainThreadQueries()
        .build()
    private val dao = database.profileDao()
    private val tabSessionDao = database.tabSessionDao()
    @Volatile
    private var profilesCache: List<ProfileEntity>? = null

    fun ensureDefaultProfile(): ProfileEntity {
        val profiles = getAllProfiles()
        if (profiles.isEmpty()) return createProfileInternal("Phoenix", id = DEFAULT_PROFILE_ID)

        val active = profiles.firstOrNull { it.status == ProfileStatus.ACTIVE } ?: profiles.first()
        if (active.status != ProfileStatus.ACTIVE) {
            val promoted = active.copy(status = ProfileStatus.ACTIVE, lastUsedAt = System.currentTimeMillis())
            dao.upsert(promoted)
            replaceCached(promoted)
            return promoted
        }
        return active
    }

    fun activeProfile(): ProfileEntity =
        getAllProfiles().firstOrNull { it.status == ProfileStatus.ACTIVE } ?: ensureDefaultProfile()

    @Synchronized
    fun getAllProfiles(): List<ProfileEntity> = profilesCache ?: dao.getAll().also { profilesCache = it }

    fun updateStatus(id: String, status: ProfileStatus): ProfileEntity? {
        val profile = dao.getById(id) ?: return null
        val lastUsedAt = if (status == ProfileStatus.ACTIVE) System.currentTimeMillis() else profile.lastUsedAt
        return profile.copy(status = status, lastUsedAt = lastUsedAt).also {
            dao.upsert(it)
            replaceCached(it)
        }
    }

    fun loadTabSessions(profileId: String): List<TabSessionEntity> = tabSessionDao.getForProfile(profileId)

    fun saveTabSessions(profileId: String, sessions: List<TabSessionEntity>) {
        database.runInTransaction {
            tabSessionDao.deleteForProfile(profileId)
            if (sessions.isNotEmpty()) tabSessionDao.insertAll(sessions)
        }
    }

    fun createProfile(name: String): ProfileEntity = createProfileInternal(name.trim())

    fun renameProfile(id: String, name: String): ProfileEntity? {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return null
        val profile = dao.getById(id) ?: return null
        return profile.copy(name = cleanName).also {
            dao.upsert(it)
            replaceCached(it)
        }
    }

    fun switchProfile(id: String): ProfileEntity? {
        val before = getAllProfiles()
        val target = before.firstOrNull { it.id == id } ?: return null
        val now = System.currentTimeMillis()
        val updated = before.map {
            when {
                it.status == ProfileStatus.ACTIVE && it.id != id -> it.copy(status = ProfileStatus.IDLE, lastUsedAt = now)
                it.id == id -> target.copy(status = ProfileStatus.ACTIVE, lastUsedAt = now)
                else -> it
            }
        }
        database.runInTransaction {
            updated.filterIndexed { index, profile -> profile != before[index] }.forEach(dao::upsert)
        }
        profilesCache = updated
        return updated.firstOrNull { it.id == id }
    }

    fun deleteProfile(id: String): Boolean {
        val profile = getAllProfiles().firstOrNull { it.id == id } ?: return false
        if (profile.status == ProfileStatus.ACTIVE) return false
        val storage = File(profile.storagePath)
        if (storage.exists() && !storage.deleteRecursively()) return false
        database.runInTransaction {
            tabSessionDao.deleteForProfile(id)
            dao.delete(profile)
        }
        profilesCache = getAllProfiles().filterNot { it.id == id }
        return true
    }

    fun close() {
        profilesCache = null
        database.close()
    }

    private fun createProfileInternal(name: String, id: String = "profile_${UUID.randomUUID()}"): ProfileEntity {
        val now = System.currentTimeMillis()
        val storage = File(appContext.applicationInfo.dataDir, "app_webview_$id").apply { mkdirs() }
        val status = if (getAllProfiles().isEmpty()) ProfileStatus.ACTIVE else ProfileStatus.IDLE
        return ProfileEntity(id, name.ifBlank { "Phoenix" }, now, now, status, storage.absolutePath)
            .also {
                dao.upsert(it)
                replaceCached(it)
            }
    }

    @Synchronized
    private fun replaceCached(profile: ProfileEntity) {
        profilesCache = (profilesCache ?: dao.getAll())
            .filterNot { it.id == profile.id }
            .plus(profile)
            .sortedBy { it.createdAt }
    }

    private companion object {
        const val DEFAULT_PROFILE_ID = "profile_default"
    }
}
