package com.phoenix.phnx.profiles

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
    val lastUsedAt: Long,
    val status: ProfileStatus,
    val storagePath: String,
)
