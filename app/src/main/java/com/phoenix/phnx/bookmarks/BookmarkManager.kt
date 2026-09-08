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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.UUID

data class BookmarkItem(
    val id: String,
    val profileId: String,
    val title: String,
    val url: String,
    val folderId: String?,
    val createdAt: Long,
)

data class BookmarkFolder(
    val id: String,
    val profileId: String,
    val name: String,
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

@Entity(tableName = "bookmark_folders")
data class BookmarkFolderEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val name: String,
    val createdAt: Long,
) {
    fun toDomain() = BookmarkFolder(id, profileId, name, createdAt)
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE profileId = :profileId ORDER BY createdAt DESC")
    fun getForProfile(profileId: String): List<BookmarkEntity>

    @Query("SELECT * FROM bookmarks WHERE profileId = :profileId ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentForProfile(profileId: String, limit: Int): List<BookmarkEntity>

    @Query("SELECT * FROM bookmarks WHERE profileId = :profileId AND url = :url LIMIT 1")
    fun findByUrl(profileId: String, url: String): BookmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE profileId = :profileId AND id = :id")
    fun delete(profileId: String, id: String)

    @Query("DELETE FROM bookmarks WHERE profileId = :profileId")
    fun clearProfile(profileId: String)

    @Query("SELECT * FROM bookmark_folders WHERE profileId = :profileId ORDER BY name COLLATE NOCASE ASC")
    fun getFoldersForProfile(profileId: String): List<BookmarkFolderEntity>

    @Query("SELECT * FROM bookmark_folders WHERE profileId = :profileId AND id = :folderId LIMIT 1")
    fun getFolder(profileId: String, folderId: String): BookmarkFolderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertFolder(folder: BookmarkFolderEntity)

    @Query("UPDATE bookmark_folders SET name = :name WHERE profileId = :profileId AND id = :folderId")
    fun renameFolder(profileId: String, folderId: String, name: String): Int

    @Query("DELETE FROM bookmark_folders WHERE profileId = :profileId AND id = :folderId")
    fun deleteFolder(profileId: String, folderId: String): Int

    @Query("UPDATE bookmarks SET folderId = NULL WHERE profileId = :profileId AND folderId = :folderId")
    fun clearFolderAssignments(profileId: String, folderId: String)

    @Query("UPDATE bookmarks SET folderId = :folderId WHERE profileId = :profileId AND id = :bookmarkId")
    fun moveToFolder(profileId: String, bookmarkId: String, folderId: String?): Int
}

@Database(entities = [BookmarkEntity::class, BookmarkFolderEntity::class], version = 2, exportSchema = false)
abstract class BookmarkDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS bookmark_folders (
                        id TEXT NOT NULL,
                        profileId TEXT NOT NULL,
                        name TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent(),
                )
            }
        }
    }
}

class BookmarkManager(context: Context) {
    private val database = Room.databaseBuilder(
        context.applicationContext,
        BookmarkDatabase::class.java,
        "bookmarks.db",
    ).addMigrations(BookmarkDatabase.MIGRATION_1_2).allowMainThreadQueries().build()
    private val dao = database.bookmarkDao()

    fun add(profileId: String, title: String, url: String, folderId: String? = null): BookmarkItem {
        val existing = dao.findByUrl(profileId, url)
        val safeFolderId = folderId?.takeIf { dao.getFolder(profileId, it) != null } ?: existing?.folderId
        val entity = BookmarkEntity(
            id = existing?.id ?: UUID.randomUUID().toString(),
            profileId = profileId,
            title = title.ifBlank { url },
            url = url,
            folderId = safeFolderId,
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
        )
        dao.upsert(entity)
        return entity.toDomain()
    }

    fun getForProfile(profileId: String): List<BookmarkItem> =
        dao.getForProfile(profileId).map(BookmarkEntity::toDomain)

    fun getRecentForProfile(profileId: String, limit: Int): List<BookmarkItem> =
        dao.getRecentForProfile(profileId, limit.coerceAtLeast(0)).map(BookmarkEntity::toDomain)

    fun isBookmarked(profileId: String, url: String): Boolean = dao.findByUrl(profileId, url) != null

    fun getFolders(profileId: String): List<BookmarkFolder> =
        dao.getFoldersForProfile(profileId).map(BookmarkFolderEntity::toDomain)

    fun createFolder(profileId: String, name: String): BookmarkFolder? {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return null
        return BookmarkFolderEntity(
            id = UUID.randomUUID().toString(),
            profileId = profileId,
            name = cleanName,
            createdAt = System.currentTimeMillis(),
        ).also(dao::upsertFolder).toDomain()
    }

    fun renameFolder(profileId: String, folderId: String, name: String): Boolean {
        val cleanName = name.trim()
        return cleanName.isNotEmpty() && dao.renameFolder(profileId, folderId, cleanName) > 0
    }

    fun deleteFolder(profileId: String, folderId: String): Boolean {
        if (dao.getFolder(profileId, folderId) == null) return false
        dao.clearFolderAssignments(profileId, folderId)
        return dao.deleteFolder(profileId, folderId) > 0
    }

    fun moveToFolder(profileId: String, bookmarkId: String, folderId: String?): Boolean {
        if (folderId != null && dao.getFolder(profileId, folderId) == null) return false
        return dao.moveToFolder(profileId, bookmarkId, folderId) > 0
    }

    fun delete(profileId: String, id: String) = dao.delete(profileId, id)

    fun clearProfile(profileId: String) = dao.clearProfile(profileId)

    fun close() = database.close()
}
