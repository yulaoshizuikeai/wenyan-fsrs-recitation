package com.ancient.wenyan.domain.fsrs

data class CardFsrsState(
    val cardId: String,
    val state: CardState = CardState.NEW,
    val step: Int? = null,
    val stability: Double = 0.0,
    val difficulty: Double = 0.0,
    val elapsedDays: Int = 0,
    val scheduledDays: Int = 0,
    val reps: Int = 0,
    val lapses: Int = 0,
    val lastReviewTime: Long? = null,
    val dueTime: Long = 0L,
    val isLeech: Boolean = false
)

data class ReviewLog(
    val id: Long = 0,
    val cardId: String,
    val rating: Rating,
    val previousState: CardState,
    val currentState: CardState,
    val stability: Double,
    val difficulty: Double,
    val elapsedDays: Int,
    val scheduledDays: Int,
    val reviewTime: Long,
    val durationMs: Long = 0L
)

data class NextStateResult(
    val updatedCard: CardFsrsState,
    val reviewLog: ReviewLog,
    val nextIntervalDays: Double
)
