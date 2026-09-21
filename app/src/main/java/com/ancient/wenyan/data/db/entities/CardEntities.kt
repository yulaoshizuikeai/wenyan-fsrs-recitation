package com.ancient.wenyan.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ancient.wenyan.domain.fsrs.CardFsrsState
import com.ancient.wenyan.domain.fsrs.CardState
import com.ancient.wenyan.domain.fsrs.Rating
import com.ancient.wenyan.domain.fsrs.ReviewLog

/**
 * Room Entity representing the persistent FSRS state of a recitation card.
 */
@Entity(
    tableName = "card_states",
    indices = [
        Index(value = ["state"]),
        Index(value = ["dueTime"]),
        Index(value = ["isLeech"])
    ]
)
data class CardStateEntity(
    @PrimaryKey val cardId: String,
    val state: String, // NEW, LEARNING, REVIEW, RELEARNING
    val step: Int?,
    val stability: Double,
    val difficulty: Double,
    val elapsedDays: Int,
    val scheduledDays: Int,
    val reps: Int,
    val lapses: Int,
    val lastReviewTime: Long?,
    val dueTime: Long,
    val isLeech: Boolean = false
) {
    fun toDomain(): CardFsrsState = CardFsrsState(
        cardId = cardId,
        state = try { CardState.valueOf(state) } catch (_: Exception) { CardState.NEW },
        step = step,
        stability = stability,
        difficulty = difficulty,
        elapsedDays = elapsedDays,
        scheduledDays = scheduledDays,
        reps = reps,
        lapses = lapses,
        lastReviewTime = lastReviewTime,
        dueTime = dueTime,
        isLeech = isLeech
    )

    companion object {
        fun fromDomain(domain: CardFsrsState): CardStateEntity = CardStateEntity(
            cardId = domain.cardId,
            state = domain.state.name,
            step = domain.step,
            stability = domain.stability,
            difficulty = domain.difficulty,
            elapsedDays = domain.elapsedDays,
            scheduledDays = domain.scheduledDays,
            reps = domain.reps,
            lapses = domain.lapses,
            lastReviewTime = domain.lastReviewTime,
            dueTime = domain.dueTime,
            isLeech = domain.isLeech
        )
    }
}

/**
 * Room Entity representing genuine recitation logs without 500-entry truncation limit.
 */
@Entity(
    tableName = "review_logs",
    indices = [
        Index(value = ["cardId"]),
        Index(value = ["reviewTime"])
    ]
)
data class ReviewLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: String,
    val rating: String, // AGAIN, HARD, GOOD, EASY
    val previousState: String,
    val currentState: String,
    val stability: Double,
    val difficulty: Double,
    val elapsedDays: Int,
    val scheduledDays: Int,
    val reviewTime: Long,
    val durationMs: Long = 0L
) {
    fun toDomain(): ReviewLog = ReviewLog(
        id = id,
        cardId = cardId,
        rating = try { Rating.valueOf(rating) } catch (_: Exception) { Rating.GOOD },
        previousState = try { CardState.valueOf(previousState) } catch (_: Exception) { CardState.NEW },
        currentState = try { CardState.valueOf(currentState) } catch (_: Exception) { CardState.REVIEW },
        stability = stability,
        difficulty = difficulty,
        elapsedDays = elapsedDays,
        scheduledDays = scheduledDays,
        reviewTime = reviewTime,
        durationMs = durationMs
    )

    companion object {
        fun fromDomain(domain: ReviewLog): ReviewLogEntity = ReviewLogEntity(
            id = domain.id,
            cardId = domain.cardId,
            rating = domain.rating.name,
            previousState = domain.previousState.name,
            currentState = domain.currentState.name,
            stability = domain.stability,
            difficulty = domain.difficulty,
            elapsedDays = domain.elapsedDays,
            scheduledDays = domain.scheduledDays,
            reviewTime = domain.reviewTime,
            durationMs = domain.durationMs
        )
    }
}

/**
 * Daily study statistics entity for heatmaps and streak calculations.
 */
@Entity(tableName = "daily_study_records")
data class DailyRecordEntity(
    @PrimaryKey val date: String, // yyyy-MM-dd
    val reviewCount: Int = 0,
    val newLearnedCount: Int = 0
)
