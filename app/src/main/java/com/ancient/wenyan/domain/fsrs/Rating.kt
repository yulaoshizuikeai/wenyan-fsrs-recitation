package com.ancient.wenyan.domain.fsrs

/**
 * FSRS review rating grades.
 */
enum class Rating(val value: Int, val label: String) {
    AGAIN(1, "重来"),
    HARD(2, "困难"),
    GOOD(3, "良好"),
    EASY(4, "简单");

    companion object {
        fun fromValue(value: Int): Rating =
            entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Invalid rating value: $value. Must be 1..4.")
    }
}
