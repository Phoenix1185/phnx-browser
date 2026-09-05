package com.phoenix.phnx.profiles

import androidx.room.Entity

@Entity(tableName = "tab_sessions", primaryKeys = ["profileId", "tabId"])
data class TabSessionEntity(
    val profileId: String,
    val tabId: String,
    val title: String,
    val url: String,
    val isPrivate: Boolean,
    val position: Int,
    val isActive: Boolean,
)
