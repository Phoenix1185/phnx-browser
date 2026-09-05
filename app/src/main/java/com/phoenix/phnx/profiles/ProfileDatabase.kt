package com.phoenix.phnx.profiles

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class ProfileConverters {
    @TypeConverter
    fun fromStatus(status: ProfileStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): ProfileStatus = ProfileStatus.valueOf(value)
}

@Database(entities = [ProfileEntity::class, TabSessionEntity::class], version = 2, exportSchema = false)
@TypeConverters(ProfileConverters::class)
abstract class ProfileDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun tabSessionDao(): TabSessionDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS tab_sessions (
                        profileId TEXT NOT NULL,
                        tabId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        url TEXT NOT NULL,
                        isPrivate INTEGER NOT NULL,
                        position INTEGER NOT NULL,
                        isActive INTEGER NOT NULL,
                        PRIMARY KEY(profileId, tabId)
                    )
                    """.trimIndent(),
                )
            }
        }
    }
}
