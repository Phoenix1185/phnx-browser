package com.phoenix.phnx.downloads

import android.app.DownloadManager
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

data class DownloadItem(
    val id: String,
    val profileId: String,
    val downloadId: Long,
    val url: String,
    val filename: String,
    val mimeType: String,
    val createdAt: Long,
)

data class DownloadProgress(
    val downloadedBytes: Long,
    val totalBytes: Long,
)

@Entity(tableName = "download_records")
data class DownloadRecordEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val downloadId: Long,
    val url: String,
    val filename: String,
    val mimeType: String,
    val createdAt: Long,
) {
    fun toDomain() = DownloadItem(id, profileId, downloadId, url, filename, mimeType, createdAt)
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM download_records WHERE profileId = :profileId ORDER BY createdAt DESC")
    fun getForProfile(profileId: String): List<DownloadRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(record: DownloadRecordEntity)

    @Query("DELETE FROM download_records WHERE profileId = :profileId AND id = :id")
    fun delete(profileId: String, id: String)

    @Query("DELETE FROM download_records WHERE profileId = :profileId")
    fun clearProfile(profileId: String)
}

@Database(entities = [DownloadRecordEntity::class], version = 1, exportSchema = false)
abstract class DownloadDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
}

class DownloadManager(context: Context) {
    private val appContext = context.applicationContext
    private val androidDownloadManager = appContext.getSystemService(DownloadManager::class.java)
    private val database = Room.databaseBuilder(appContext, DownloadDatabase::class.java, "downloads.db")
        .allowMainThreadQueries()
        .build()
    private val dao = database.downloadDao()

    fun record(profileId: String, downloadId: Long, url: String, filename: String, mimeType: String) {
        dao.insert(
            DownloadRecordEntity(
                id = "$profileId:$downloadId",
                profileId = profileId,
                downloadId = downloadId,
                url = url,
                filename = filename,
                mimeType = mimeType,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    fun getForProfile(profileId: String): List<DownloadItem> =
        dao.getForProfile(profileId).map(DownloadRecordEntity::toDomain)

    fun query(downloadId: Long): DownloadStatus {
        val cursor = androidDownloadManager.query(DownloadManager.Query().setFilterById(downloadId))
        cursor.use {
            if (!it.moveToFirst()) return DownloadStatus.MISSING
            val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val reason = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
            return when (status) {
                DownloadManager.STATUS_PENDING -> DownloadStatus.QUEUED
                DownloadManager.STATUS_RUNNING -> DownloadStatus.DOWNLOADING
                DownloadManager.STATUS_PAUSED -> DownloadStatus.PAUSED
                DownloadManager.STATUS_SUCCESSFUL -> DownloadStatus.COMPLETED
                DownloadManager.STATUS_FAILED -> DownloadStatus.FAILED(reason)
                else -> DownloadStatus.MISSING
            }
        }
    }

    fun progress(downloadId: Long): DownloadProgress? {
        val cursor = androidDownloadManager.query(DownloadManager.Query().setFilterById(downloadId))
        cursor.use {
            if (!it.moveToFirst()) return null
            return DownloadProgress(
                downloadedBytes = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)),
                totalBytes = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)),
            )
        }
    }

    fun uri(downloadId: Long) = androidDownloadManager.getUriForDownloadedFile(downloadId)

    fun remove(profileId: String, item: DownloadItem) {
        androidDownloadManager.remove(item.downloadId)
        dao.delete(profileId, item.id)
    }

    fun clearProfile(profileId: String) {
        getForProfile(profileId).forEach { androidDownloadManager.remove(it.downloadId) }
        dao.clearProfile(profileId)
    }

    fun close() = database.close()
}

sealed interface DownloadStatus {
    data object QUEUED : DownloadStatus
    data object DOWNLOADING : DownloadStatus
    data object PAUSED : DownloadStatus
    data object COMPLETED : DownloadStatus
    data object MISSING : DownloadStatus
    data class FAILED(val reason: Int) : DownloadStatus
}
