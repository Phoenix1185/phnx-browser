package com.phoenix.phnx.adblock

data class AdBlockSettings(
    val profileId: String,
    val enabled: Boolean = true,
    val blockAds: Boolean = true,
    val blockTrackers: Boolean = true,
    val blockMaliciousAds: Boolean = true,
    val siteExceptions: Set<String> = emptySet(),
)

data class BlockedRequestStats(
    val evaluatedRequests: Long = 0,
    val blockedRequests: Long = 0,
    val blockedAds: Long = 0,
    val blockedTrackers: Long = 0,
    val blockedMaliciousAds: Long = 0,
)
