package com.phoenix.phnx.network

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface NetworkConfigDao {
    @Query("SELECT * FROM profile_network_configs WHERE profileId = :profileId LIMIT 1")
    fun getForProfile(profileId: String): NetworkConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(config: NetworkConfigEntity)

    @Delete
    fun delete(config: NetworkConfigEntity)
}
