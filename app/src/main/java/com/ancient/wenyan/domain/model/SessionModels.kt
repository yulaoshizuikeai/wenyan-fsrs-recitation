package com.ancient.wenyan.domain.model

/**
 * Represents an active in-progress recitation session that can be saved and resumed mid-way,
 * providing Anki-like persistent study decks and breakpoint continuation.
 */
data class ActiveSession(
    val id: String,
    val title: String,
    val sessionType: String,
    val cardIds: List<String>,
    val currentIndex: Int,
    val completedCount: Int,
    val totalCards: Int,
    val lastActiveMillis: Long = System.currentTimeMillis()
) {
    val isComplete: Boolean
        get() = cardIds.isEmpty() || currentIndex >= cardIds.size

    val progressPercentage: Float
        get() = if (cardIds.isNotEmpty()) (currentIndex.toFloat() / cardIds.size.toFloat()).coerceIn(0f, 1f) else 1f
}
