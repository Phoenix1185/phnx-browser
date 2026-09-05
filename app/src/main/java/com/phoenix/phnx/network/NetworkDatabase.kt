package com.phoenix.phnx.network

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [NetworkConfigEntity::class], version = 2, exportSchema = false)
abstract class NetworkDatabase : RoomDatabase() {
    abstract fun configDao(): NetworkConfigDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE profile_network_configs ADD COLUMN fallbackToFreeProxy INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE profile_network_configs ADD COLUMN fallbackToDirect INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE profile_network_configs ADD COLUMN freeProxyFallbacks TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
