package com.phoenix.phnx.pages

data class FindInPageResult(
    val activeMatchOrdinal: Int,
    val numberOfMatches: Int,
    val isDoneCounting: Boolean,
) {
    fun summary(): String = when {
        !isDoneCounting -> "Counting..."
        numberOfMatches <= 0 -> "No matches"
        else -> {
            val current = activeMatchOrdinal.coerceIn(0, numberOfMatches - 1) + 1
            "Match $current of $numberOfMatches"
        }
    }
}
