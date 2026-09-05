package com.phoenix.phnx.network

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profile_network_configs")
data class NetworkConfigEntity(
    @PrimaryKey val profileId: String,
    val id: String,
    val mode: String,
    val proxyType: String?,
    val proxyHost: String,
    val proxyPort: Int,
    val username: String,
    val credentialReference: String?,
    val enabled: Boolean,
)

fun NetworkConfigEntity.toDomain(): ProfileNetworkConfig = ProfileNetworkConfig(
    id = id,
    profileId = profileId,
    mode = NetworkMode.valueOf(mode),
    proxyType = proxyType?.let(ProxyType::valueOf),
    proxyHost = proxyHost,
    proxyPort = proxyPort,
    username = username,
    credentialReference = credentialReference,
    enabled = enabled,
)

fun ProfileNetworkConfig.toEntity() = NetworkConfigEntity(
    id = id,
    profileId = profileId,
    mode = mode.name,
    proxyType = proxyType?.name,
    proxyHost = proxyHost,
    proxyPort = proxyPort,
    username = username,
    credentialReference = credentialReference,
    enabled = enabled,
)
