package com.phoenix.phnx.search

import android.content.Context

data class SearchEngine(
    val id: String,
    val name: String,
    val searchUrl: String,
)

class SearchEngineManager(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("search", Context.MODE_PRIVATE)

    val available: List<SearchEngine> = listOf(
        SearchEngine("google", "Google", "https://www.google.com/search?q="),
        SearchEngine("bing", "Bing", "https://www.bing.com/search?q="),
        SearchEngine("duckduckgo", "DuckDuckGo", "https://duckduckgo.com/?q="),
    )

    fun current(): SearchEngine = available.firstOrNull { it.id == preferences.getString(KEY_ENGINE, null) } ?: available.first()

    fun setCurrent(id: String): SearchEngine {
        val engine = available.firstOrNull { it.id == id } ?: available.first()
        preferences.edit().putString(KEY_ENGINE, engine.id).apply()
        return engine
    }

    private companion object {
        const val KEY_ENGINE = "selected_engine"
    }
}
