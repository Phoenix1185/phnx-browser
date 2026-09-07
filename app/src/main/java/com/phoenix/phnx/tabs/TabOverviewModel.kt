package com.phoenix.phnx.tabs

import android.net.Uri

data class TabOverviewItem(
    val id: String,
    val profileId: String,
    val title: String,
    val url: String,
    val isPrivate: Boolean,
    val isActive: Boolean,
    val groupId: String?,
    val groupTitle: String?,
)

val TabOverviewItem.domain: String
    get() = Uri.parse(url).host?.removePrefix("www.").orEmpty()

fun TabOverviewItem.matches(query: String): Boolean {
    val needle = query.trim()
    if (needle.isEmpty()) return true
    return listOf(title, domain, url).any { it.contains(needle, ignoreCase = true) }
}

fun Tab.toOverviewItem(activeTabId: String): TabOverviewItem = TabOverviewItem(
    id = id,
    profileId = profileId,
    title = title.ifBlank { "New tab" },
    url = url,
    isPrivate = isPrivate,
    isActive = id == activeTabId,
    groupId = groupId,
    groupTitle = groupTitle,
)
