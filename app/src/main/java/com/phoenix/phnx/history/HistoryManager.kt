package com.phoenix.phnx.history

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
import java.util.UUID

data class HistoryEntry(
    val id: String,
    val profileId: String,
    val url: String,
    val title: String,
    val visitedAt: Long,
    val visitCount: Int,
) {
    val host: String
        get() = url.substringAfter("://", "")
            .substringBeforeAny('/', '?', '#')
            .substringAfterLast('@')
}

private fun String.substringBeforeAny(vararg delimiters: Char): String =
    substringBeforeFirstOrNull(delimiters) ?: this

private fun String.substringBeforeFirstOrNull(delimiters: CharArray): String? {
    val index = indexOfFirst { it in delimiters }
    return if (index >= 0) substring(0, index) else null
}

@Entity(tableName = "history_entries")
data class HistoryEntryEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val url: String,
    val title: String,
    val visitedAt: Long,
    val visitCount: Int,
) {
    fun toDomain() = HistoryEntry(id, profileId, url, title, visitedAt, visitCount)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history_entries WHERE profileId = :profileId ORDER BY visitedAt DESC")
    fun getForProfile(profileId: String): List<HistoryEntryEntity>

    @Query("SELECT * FROM history_entries WHERE profileId = :profileId AND (title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%') ORDER BY visitedAt DESC")
    fun search(profileId: String, query: String): List<HistoryEntryEntity>

    @Query("SELECT * FROM history_entries WHERE profileId = :profileId AND url = :url LIMIT 1")
    fun findByUrl(profileId: String, url: String): HistoryEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entry: HistoryEntryEntity)

    @Query("DELETE FROM history_entries WHERE profileId = :profileId AND id = :id")
    fun delete(profileId: String, id: String)

    @Query("DELETE FROM history_entries WHERE profileId = :profileId")
    fun clearProfile(profileId: String)

    @Query("DELETE FROM history_entries WHERE profileId = :profileId AND visitedAt < :cutoff")
    fun deleteBefore(profileId: String, cutoff: Long)
}

@Database(entities = [HistoryEntryEntity::class], version = 1, exportSchema = false)
abstract class HistoryDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
}

class HistoryManager(context: Context) {
    private val database = Room.databaseBuilder(
        context.applicationContext,
        HistoryDatabase::class.java,
        "history.db",
    ).allowMainThreadQueries().build()
    private val dao = database.historyDao()

    fun recordVisit(profileId: String, url: String, title: String, isPrivate: Boolean = false) {
        if (isPrivate || !url.isRecordable()) return
        val existing = dao.findByUrl(profileId, url)
        dao.upsert(
            HistoryEntryEntity(
                id = existing?.id ?: UUID.randomUUID().toString(),
                profileId = profileId,
                url = url,
                title = title.ifBlank { url },
                visitedAt = System.currentTimeMillis(),
                visitCount = (existing?.visitCount ?: 0) + 1,
            ),
        )
    }

    fun getForProfile(profileId: String): List<HistoryEntry> =
        dao.getForProfile(profileId).map(HistoryEntryEntity::toDomain)

    fun search(profileId: String, query: String): List<HistoryEntry> {
        val cleanQuery = query.trim()
        return if (cleanQuery.isEmpty()) getForProfile(profileId)
        else dao.search(profileId, cleanQuery).map(HistoryEntryEntity::toDomain)
    }

    fun delete(profileId: String, id: String) = dao.delete(profileId, id)

    fun clearProfile(profileId: String) = dao.clearProfile(profileId)

    fun deleteBefore(profileId: String, cutoff: Long) = dao.deleteBefore(profileId, cutoff)

    fun close() = database.close()

    private fun String.isRecordable(): Boolean =
        (startsWith("https://") || startsWith("http://")) && this != "https://phnx.local/"
}
