package com.ancient.wenyan.domain.fsrs

/**
 * FSRS card lifecycle states.
 * - [NEW]: Unstudied card (S and D uninitialized).
 * - [LEARNING]: Card in short-term acquisition steps.
 * - [REVIEW]: Card graduated into long-term spaced repetition.
 * - [RELEARNING]: Card lapsed from Review on Again rating, undergoing reacquisition.
 */
enum class CardState(val value: Int) {
    NEW(0),
    LEARNING(1),
    REVIEW(2),
    RELEARNING(3);

    companion object {
        fun fromValue(value: Int): CardState =
            entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Invalid CardState value: $value. Must be 0, 1, 2, or 3.")
    }
}
