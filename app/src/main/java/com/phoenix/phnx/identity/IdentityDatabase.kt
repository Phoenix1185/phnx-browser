package com.phoenix.phnx.identity

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase

@Dao
interface IdentityDao {
    @Query("SELECT * FROM profile_identities WHERE profileId = :profileId LIMIT 1")
    fun get(profileId: String): ProfileIdentityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(config: ProfileIdentityEntity)

    @Delete
    fun delete(config: ProfileIdentityEntity)
}

@Database(entities = [ProfileIdentityEntity::class], version = 1, exportSchema = false)
abstract class IdentityDatabase : RoomDatabase() {
    abstract fun identityDao(): IdentityDao
}
