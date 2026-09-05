package com.phoenix.phnx.bookmarks

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

data class BookmarkItem(
    val id: String,
    val profileId: String,
    val title: String,
    val url: String,
    val folderId: String?,
    val createdAt: Long,
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val title: String,
    val url: String,
    val folderId: String?,
    val createdAt: Long,
) {
    fun toDomain() = BookmarkItem(id, profileId, title, url, folderId, createdAt)
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE profileId = :profileId ORDER BY createdAt DESC")
    fun getForProfile(profileId: String): List<BookmarkEntity>

    @Query("SELECT * FROM bookmarks WHERE profileId = :profileId AND url = :url LIMIT 1")
    fun findByUrl(profileId: String, url: String): BookmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE profileId = :profileId AND id = :id")
    fun delete(profileId: String, id: String)

    @Query("DELETE FROM bookmarks WHERE profileId = :profileId")
    fun clearProfile(profileId: String)
}

@Database(entities = [BookmarkEntity::class], version = 1, exportSchema = false)
abstract class BookmarkDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
}

class BookmarkManager(context: Context) {
    private val database = Room.databaseBuilder(
        context.applicationContext,
        BookmarkDatabase::class.java,
        "bookmarks.db",
    ).allowMainThreadQueries().build()
    private val dao = database.bookmarkDao()

    fun add(profileId: String, title: String, url: String, folderId: String? = null): BookmarkItem {
        val existing = dao.findByUrl(profileId, url)
        val entity = BookmarkEntity(
            id = existing?.id ?: UUID.randomUUID().toString(),
            profileId = profileId,
            title = title.ifBlank { url },
            url = url,
            folderId = folderId,
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
        )
        dao.upsert(entity)
        return entity.toDomain()
    }

    fun getForProfile(profileId: String): List<BookmarkItem> =
        dao.getForProfile(profileId).map(BookmarkEntity::toDomain)

    fun delete(profileId: String, id: String) = dao.delete(profileId, id)

    fun clearProfile(profileId: String) = dao.clearProfile(profileId)

    fun close() = database.close()
}
