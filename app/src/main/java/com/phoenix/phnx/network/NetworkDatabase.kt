package com.phoenix.phnx.network

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [NetworkConfigEntity::class], version = 1, exportSchema = false)
abstract class NetworkDatabase : RoomDatabase() {
    abstract fun configDao(): NetworkConfigDao
}
