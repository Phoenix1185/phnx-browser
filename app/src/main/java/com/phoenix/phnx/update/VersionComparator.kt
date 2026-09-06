package com.phoenix.phnx.update

object VersionComparator {
    fun compare(left: String, right: String): Int {
        val leftParts = numericParts(left)
        val rightParts = numericParts(right)
        val count = maxOf(leftParts.size, rightParts.size)
        for (index in 0 until count) {
            val difference = (leftParts.getOrNull(index) ?: 0) - (rightParts.getOrNull(index) ?: 0)
            if (difference != 0) return difference.coerceIn(-1, 1)
        }
        return 0
    }

    fun isNewer(candidate: String, current: String): Boolean = compare(candidate, current) > 0

    private fun numericParts(version: String): List<Int> = version
        .removePrefix("v")
        .substringBefore('-')
        .split('.')
        .mapNotNull { it.toIntOrNull() }
}
