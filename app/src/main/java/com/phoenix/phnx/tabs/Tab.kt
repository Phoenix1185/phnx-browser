package com.phoenix.phnx.tabs

import java.util.UUID

data class Tab(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "New tab",
    var url: String = "",
    val isPrivate: Boolean = false,
    var isLoading: Boolean = false,
    var canGoBack: Boolean = false,
    var canGoForward: Boolean = false,
)
