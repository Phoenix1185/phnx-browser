package com.phoenix.phnx.permissions

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

enum class SitePermissionType {
    CAMERA,
    MICROPHONE,
    LOCATION,
    NOTIFICATIONS,
}

enum class SitePermissionDecision {
    ASK,
    ALLOW,
    BLOCK,
}

data class SitePermission(
    val profileId: String,
    val origin: String,
    val type: SitePermissionType,
    val decision: SitePermissionDecision,
)

@Entity(
    tableName = "site_permissions",
    primaryKeys = ["profileId", "origin", "type"],
)
data class SitePermissionEntity(
    val profileId: String,
    val origin: String,
    val type: String,
    val decision: String,
) {
    fun toDomain(): SitePermission = SitePermission(
        profileId = profileId,
        origin = origin,
        type = runCatching { SitePermissionType.valueOf(type) }.getOrDefault(SitePermissionType.CAMERA),
        decision = runCatching { SitePermissionDecision.valueOf(decision) }
            .getOrDefault(SitePermissionDecision.ASK),
    )
}

private fun SitePermission.toEntity() = SitePermissionEntity(
    profileId = profileId,
    origin = origin,
    type = type.name,
    decision = decision.name,
)

@Dao
interface PermissionDao {
    @Query("SELECT * FROM site_permissions WHERE profileId = :profileId AND origin = :origin")
    fun getForOrigin(profileId: String, origin: String): List<SitePermissionEntity>

    @Query("SELECT * FROM site_permissions WHERE profileId = :profileId ORDER BY origin, type")
    fun getForProfile(profileId: String): List<SitePermissionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(permission: SitePermissionEntity)

    @Query("DELETE FROM site_permissions WHERE profileId = :profileId AND origin = :origin")
    fun clearOrigin(profileId: String, origin: String)

    @Query("DELETE FROM site_permissions WHERE profileId = :profileId")
    fun clearProfile(profileId: String)
}

@Database(entities = [SitePermissionEntity::class], version = 1, exportSchema = false)
abstract class PermissionDatabase : RoomDatabase() {
    abstract fun permissionDao(): PermissionDao
}

class PermissionManager(context: Context) {
    private val database = Room.databaseBuilder(
        context.applicationContext,
        PermissionDatabase::class.java,
        "site_permissions.db",
    ).allowMainThreadQueries().build()
    private val dao = database.permissionDao()

    fun get(profileId: String, origin: String, type: SitePermissionType): SitePermissionDecision =
        dao.getForOrigin(profileId, canonicalOrigin(origin))
            .firstOrNull { it.type == type.name }
            ?.toDomain()
            ?.decision
            ?: SitePermissionDecision.ASK

    fun getForProfile(profileId: String): List<SitePermission> =
        dao.getForProfile(profileId).map(SitePermissionEntity::toDomain)

    fun save(permission: SitePermission) = dao.upsert(permission.copy(origin = canonicalOrigin(permission.origin)).toEntity())

    fun clearOrigin(profileId: String, origin: String) = dao.clearOrigin(profileId, canonicalOrigin(origin))

    fun clearProfile(profileId: String) = dao.clearProfile(profileId)

    fun close() = database.close()

    private fun canonicalOrigin(origin: String): String = origin.trim().trimEnd('/')
}
