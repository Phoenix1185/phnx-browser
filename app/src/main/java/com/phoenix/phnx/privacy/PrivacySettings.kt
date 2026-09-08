package com.phoenix.phnx.privacy

import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import java.util.concurrent.ConcurrentHashMap

enum class TrackingProtectionLevel {
    OFF,
    STANDARD,
    STRICT,
}

data class PrivacySettings(
    val profileId: String,
    val cookiesAllowed: Boolean = true,
    val thirdPartyCookiesAllowed: Boolean = false,
    val trackingProtection: TrackingProtectionLevel = TrackingProtectionLevel.STANDARD,
    val doNotTrack: Boolean = false,
    val javascriptEnabled: Boolean = true,
    val popupsAllowed: Boolean = false,
    val safeBrowsingEnabled: Boolean = true,
)

data class PrivacyApplyResult(
    val settings: PrivacySettings,
    val unsupported: List<String>,
)

@Entity(tableName = "privacy_settings")
data class PrivacySettingsEntity(
    @PrimaryKey val profileId: String,
    val cookiesAllowed: Boolean,
    val thirdPartyCookiesAllowed: Boolean,
    val trackingProtection: String,
    val doNotTrack: Boolean,
    val javascriptEnabled: Boolean,
    val popupsAllowed: Boolean,
    val safeBrowsingEnabled: Boolean,
) {
    fun toDomain(): PrivacySettings = PrivacySettings(
        profileId = profileId,
        cookiesAllowed = cookiesAllowed,
        thirdPartyCookiesAllowed = thirdPartyCookiesAllowed,
        trackingProtection = runCatching { TrackingProtectionLevel.valueOf(trackingProtection) }
            .getOrDefault(TrackingProtectionLevel.STANDARD),
        doNotTrack = doNotTrack,
        javascriptEnabled = javascriptEnabled,
        popupsAllowed = popupsAllowed,
        safeBrowsingEnabled = safeBrowsingEnabled,
    )
}

private fun PrivacySettings.toEntity() = PrivacySettingsEntity(
    profileId = profileId,
    cookiesAllowed = cookiesAllowed,
    thirdPartyCookiesAllowed = thirdPartyCookiesAllowed,
    trackingProtection = trackingProtection.name,
    doNotTrack = doNotTrack,
    javascriptEnabled = javascriptEnabled,
    popupsAllowed = popupsAllowed,
    safeBrowsingEnabled = safeBrowsingEnabled,
)

@Dao
interface PrivacyDao {
    @Query("SELECT * FROM privacy_settings WHERE profileId = :profileId LIMIT 1")
    fun get(profileId: String): PrivacySettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(settings: PrivacySettingsEntity)

    @Query("DELETE FROM privacy_settings WHERE profileId = :profileId")
    fun delete(profileId: String)
}

@Database(entities = [PrivacySettingsEntity::class], version = 2, exportSchema = false)
abstract class PrivacyDatabase : RoomDatabase() {
    abstract fun privacyDao(): PrivacyDao

    companion object {
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE privacy_settings ADD COLUMN cookiesAllowed INTEGER NOT NULL DEFAULT 1")
            }
        }
    }
}

class PrivacyManager(context: android.content.Context) {
    private val database = Room.databaseBuilder(
        context.applicationContext,
        PrivacyDatabase::class.java,
        "privacy_settings.db",
    ).addMigrations(PrivacyDatabase.MIGRATION_1_2).allowMainThreadQueries().build()
    private val dao = database.privacyDao()
    private val settingsCache = ConcurrentHashMap<String, PrivacySettings>()

    fun getSettings(profileId: String): PrivacySettings = settingsCache[profileId] ?: synchronized(this) {
        settingsCache[profileId] ?: (dao.get(profileId)?.toDomain() ?: PrivacySettings(profileId).also { dao.upsert(it.toEntity()) })
            .also { settingsCache[profileId] = it }
    }

    fun saveSettings(settings: PrivacySettings) {
        dao.upsert(settings.toEntity())
        settingsCache[settings.profileId] = settings
    }

    fun clearSettings(profileId: String) {
        dao.delete(profileId)
        settingsCache.remove(profileId)
    }

    fun applyTo(
        webView: WebView,
        profileId: String,
        settings: PrivacySettings = getSettings(profileId),
    ): PrivacyApplyResult {
        webView.settings.apply {
            javaScriptEnabled = settings.javascriptEnabled
            javaScriptCanOpenWindowsAutomatically = settings.popupsAllowed
            setSupportMultipleWindows(settings.popupsAllowed)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                safeBrowsingEnabled = settings.safeBrowsingEnabled
            }
        }
        CookieManager.getInstance().setAcceptCookie(settings.cookiesAllowed)
        CookieManager.getInstance().setAcceptThirdPartyCookies(
            webView,
            settings.cookiesAllowed && settings.thirdPartyCookiesAllowed,
        )
        val unsupported = buildList {
            if (settings.trackingProtection != TrackingProtectionLevel.OFF) {
                add("Full tracker blocking is not exposed by Android WebView.")
            }
            if (settings.doNotTrack) {
                add("Do Not Track is stored but not exposed by Android WebView.")
            }
        }
        return PrivacyApplyResult(settings, unsupported)
    }

    fun close() = database.close()

}
