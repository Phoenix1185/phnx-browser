package com.phoenix.phnx.profiles

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class ProfileConverters {
    @TypeConverter
    fun fromStatus(status: ProfileStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): ProfileStatus = ProfileStatus.valueOf(value)
}

@Database(entities = [ProfileEntity::class], version = 1, exportSchema = false)
@TypeConverters(ProfileConverters::class)
abstract class ProfileDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
}
