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

enum class TrackingProtectionLevel {
    OFF,
    STANDARD,
    STRICT,
}

data class PrivacySettings(
    val profileId: String,
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
    val thirdPartyCookiesAllowed: Boolean,
    val trackingProtection: String,
    val doNotTrack: Boolean,
    val javascriptEnabled: Boolean,
    val popupsAllowed: Boolean,
    val safeBrowsingEnabled: Boolean,
) {
    fun toDomain(): PrivacySettings = PrivacySettings(
        profileId = profileId,
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

@Database(entities = [PrivacySettingsEntity::class], version = 1, exportSchema = false)
abstract class PrivacyDatabase : RoomDatabase() {
    abstract fun privacyDao(): PrivacyDao
}

class PrivacyManager(context: android.content.Context) {
    private val database = Room.databaseBuilder(
        context.applicationContext,
        PrivacyDatabase::class.java,
        "privacy_settings.db",
    ).allowMainThreadQueries().build()
    private val dao = database.privacyDao()

    fun getSettings(profileId: String): PrivacySettings =
        dao.get(profileId)?.toDomain() ?: PrivacySettings(profileId).also { saveSettings(it) }

    fun saveSettings(settings: PrivacySettings) = dao.upsert(settings.toEntity())

    fun clearSettings(profileId: String) = dao.delete(profileId)

    fun applyTo(webView: WebView, profileId: String): PrivacyApplyResult {
        val settings = getSettings(profileId)
        webView.settings.apply {
            javaScriptEnabled = settings.javascriptEnabled
            javaScriptCanOpenWindowsAutomatically = settings.popupsAllowed
            setSupportMultipleWindows(settings.popupsAllowed)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                safeBrowsingEnabled = settings.safeBrowsingEnabled
            }
        }
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, settings.thirdPartyCookiesAllowed)
        webView.evaluateJavascript(if (settings.doNotTrack) DNT_ENABLED_SCRIPT else DNT_DISABLED_SCRIPT, null)

        val unsupported = buildList {
            if (settings.trackingProtection != TrackingProtectionLevel.OFF) {
                add("Full tracker blocking is not exposed by Android WebView.")
            }
        }
        return PrivacyApplyResult(settings, unsupported)
    }

    fun close() = database.close()

    private companion object {
        const val DNT_ENABLED_SCRIPT = """
            (function() {
              try {
                Object.defineProperty(navigator, 'doNotTrack', { configurable: true, get: function() { return '1'; } });
              } catch (_) {}
            })();
        """
        const val DNT_DISABLED_SCRIPT = """
            (function() {
              try { delete navigator.doNotTrack; } catch (_) {}
            })();
        """
    }
}
