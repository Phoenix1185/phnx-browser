package com.phoenix.phnx.profiles

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TabSessionDao {
    @Query("SELECT * FROM tab_sessions WHERE profileId = :profileId ORDER BY position ASC")
    fun getForProfile(profileId: String): List<TabSessionEntity>

    @Query("DELETE FROM tab_sessions WHERE profileId = :profileId")
    fun deleteForProfile(profileId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(sessions: List<TabSessionEntity>)
}
