package com.phoenix.phnx.tabs

import java.util.UUID

const val DEFAULT_PROFILE_ID = "profile_default"

data class Tab(
    val id: String = UUID.randomUUID().toString(),
    val profileId: String = DEFAULT_PROFILE_ID,
    var title: String = "New tab",
    var url: String = "",
    val isPrivate: Boolean = false,
    var isLoading: Boolean = false,
    var canGoBack: Boolean = false,
    var canGoForward: Boolean = false,
)
