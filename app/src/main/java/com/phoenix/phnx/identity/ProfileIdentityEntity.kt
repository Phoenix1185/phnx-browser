package com.phoenix.phnx.identity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profile_identities")
data class ProfileIdentityEntity(
    @PrimaryKey val profileId: String,
    val presetId: String,
    val userAgent: String,
    val platform: String,
    val operatingSystem: String,
    val viewportWidth: Int,
    val viewportHeight: Int,
    val screenWidth: Int,
    val screenHeight: Int,
    val colorDepth: Int,
    val deviceScaleFactor: Double,
    val locale: String,
    val language: String,
    val languages: String,
    val timezone: String,
    val touchSupport: Boolean,
    val mobileMode: Boolean,
    val clientHintsPlatform: String,
    val clientHintsMobile: Boolean,
    val clientHintsBrands: String,
)

fun ProfileIdentityEntity.toDomain() = BrowserIdentityConfig(
    profileId = profileId,
    presetId = presetId,
    name = if (presetId == DevicePresets.SYSTEM_DEFAULT) "System Default"
    else DevicePresets.get(presetId)?.name ?: presetId,
    userAgent = userAgent,
    platform = platform,
    operatingSystem = operatingSystem,
    viewportWidth = viewportWidth,
    viewportHeight = viewportHeight,
    screenWidth = screenWidth,
    screenHeight = screenHeight,
    colorDepth = colorDepth,
    deviceScaleFactor = deviceScaleFactor,
    locale = locale,
    language = language,
    languages = languages.split(LIST_SEPARATOR).filter(String::isNotBlank),
    timezone = timezone,
    touchSupport = touchSupport,
    mobileMode = mobileMode,
    clientHints = ClientHintsConfig(
        platform = clientHintsPlatform,
        mobile = clientHintsMobile,
        brands = clientHintsBrands.split(LIST_SEPARATOR).filter(String::isNotBlank),
    ),
)

fun BrowserIdentityConfig.toEntity() = ProfileIdentityEntity(
    profileId = profileId,
    presetId = presetId,
    userAgent = userAgent,
    platform = platform,
    operatingSystem = operatingSystem,
    viewportWidth = viewportWidth,
    viewportHeight = viewportHeight,
    screenWidth = screenWidth,
    screenHeight = screenHeight,
    colorDepth = colorDepth,
    deviceScaleFactor = deviceScaleFactor,
    locale = locale,
    language = language,
    languages = languages.joinToString(LIST_SEPARATOR),
    timezone = timezone,
    touchSupport = touchSupport,
    mobileMode = mobileMode,
    clientHintsPlatform = clientHints.platform,
    clientHintsMobile = clientHints.mobile,
    clientHintsBrands = clientHints.brands.joinToString(LIST_SEPARATOR),
)

private const val LIST_SEPARATOR = "|"
