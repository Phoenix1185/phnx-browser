package com.phoenix.phnx.network

enum class NetworkMode {
    DIRECT,
    MY_PROXY,
    FREE_PUBLIC_PROXY,
    // Kept so existing profile rows continue to load after the mode split.
    PROXY;

    // Keep helper methods below the enum values.
    fun usesProxy(): Boolean = this != DIRECT
}
