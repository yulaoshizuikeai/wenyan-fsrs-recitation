package com.ancient.wenyan.domain.fsrs

import kotlin.math.*

/**
 * Production FSRS-5 (Free Spaced Repetition Scheduler) Algorithm Engine.
 *
 * Fully implements the DSR (Difficulty, Stability, Retrievability) spaced repetition
 * mathematical model with 19 parameters and 4-tier lifecycle transitions.
 */
class FSRSEngine(
    var weights: DoubleArray = DEFAULT_FSRS_5_WEIGHTS,
    var requestRetention: Double = 0.93,
    var maximumInterval: Int = 36500,
    val learningSteps: List<Long> = listOf(60_000L, 600_000L), // 1m, 10m
    val relearningSteps: List<Long> = listOf(600_000L),          // 10m
    var recitationStabilityFactor: Double = 0.72
) {
    fun updateParameters(
        newWeights: DoubleArray = this.weights,
        newRetention: Double = this.requestRetention,
        newFactor: Double = this.recitationStabilityFactor,
        newMaxInterval: Int = this.maximumInterval
    ) {
        this.weights = newWeights.clone()
        this.requestRetention = newRetention.coerceIn(0.70, 0.99)
        this.recitationStabilityFactor = newFactor.coerceIn(0.30, 1.50)
        this.maximumInterval = newMaxInterval.coerceAtLeast(1)
    }

    fun resetToDefaults() {
        this.weights = DEFAULT_FSRS_5_WEIGHTS.clone()
        this.requestRetention = 0.93
        this.recitationStabilityFactor = 0.72
        this.maximumInterval = 36500
    }

    companion object {
        val DEFAULT_FSRS_5_WEIGHTS = doubleArrayOf(
            0.40255,  // w0:  S0(Again)
            1.18385,  // w1:  S0(Hard)
            3.17300,  // w2:  S0(Good)
            15.69105, // w3:  S0(Easy)
            7.19490,  // w4:  D0(Again) base difficulty
            0.53450,  // w5:  D0 step exponent
            1.46040,  // w6:  Difficulty delta factor
            0.00460,  // w7:  Mean reversion rate
            1.54575,  // w8:  Recall S base factor
            0.11920,  // w9:  Recall S stability power damping
            1.01925,  // w10: Recall S retrievability exp factor
            1.93950,  // w11: Forget S long-term base factor
            0.11000,  // w12: Forget S difficulty power damping
            0.29605,  // w13: Forget S stability power factor
            2.26980,  // w14: Forget S retrievability exp factor
            0.23150,  // w15: Hard penalty factor
            2.98980,  // w16: Easy bonus factor
            0.51655,  // w17: Short-term S multiplier factor
            0.66210   // w18: Short-term S grade offset
        )
    }

    private val decay: Double = -0.5
    private val factor: Double = 0.9.pow(1.0 / decay) - 1.0 // 19.0 / 81.0 ~ 0.2345679

    fun retrievability(elapsedDays: Int, stability: Double): Double {
        if (stability <= 0.0) return 0.0
        val t = max(0, elapsedDays).toDouble()
        return (1.0 + factor * (t / stability)).pow(decay)
    }

    fun nextInterval(stability: Double, retention: Double = requestRetention): Int {
        if (stability <= 0.0) return 1
        val interval = (stability / factor) * (retention.pow(1.0 / decay) - 1.0)
        return min(max(round(interval).toInt(), 1), maximumInterval)
    }

    /**
     * Apply standard FSRS discrete fuzzing factor (+-5%) to avoid review clustering/avalanche
     * for cards with scheduled intervals >= 3 days.
     */
    fun applyFuzz(interval: Int, cardId: String, reps: Int = 0): Int {
        if (interval < 3) return interval
        val hash = kotlin.math.abs((cardId.hashCode() * 31) xor reps)
        val maxFuzz = max(1, (interval * 0.05).roundToInt())
        val delta = (hash % (2 * maxFuzz + 1)) - maxFuzz
        return min(max(interval + delta, 1), maximumInterval)
    }

    fun initialStability(rating: Rating): Double {
        return max(weights[rating.value - 1] * recitationStabilityFactor, 0.001)
    }

    fun initialDifficulty(rating: Rating): Double {
        val d = weights[4] - exp(weights[5] * (rating.value - 1.0)) + 1.0
        return d.coerceIn(1.0, 10.0)
    }

    fun nextDifficulty(currentD: Double, rating: Rating): Double {
        val deltaD = -weights[6] * (rating.value - 3.0)
        val dPrime = currentD + deltaD * (10.0 - currentD) / 9.0
        val dInitEasy = weights[4] - exp(weights[5] * 3.0) + 1.0
        val dDoublePrime = weights[7] * dInitEasy + (1.0 - weights[7]) * dPrime
        return dDoublePrime.coerceIn(1.0, 10.0)
    }

    fun nextRecallStability(d: Double, s: Double, r: Double, rating: Rating): Double {
        val hardPenalty = if (rating == Rating.HARD) weights[15] else 1.0
        val easyBonus = if (rating == Rating.EASY) weights[16] else 1.0
        val sInc = 1.0 + exp(weights[8]) * (11.0 - d) * s.pow(-weights[9]) *
                (exp((1.0 - r) * weights[10]) - 1.0) * hardPenalty * easyBonus
        return max(s * sInc, 0.001)
    }

    fun nextForgetStability(d: Double, s: Double, r: Double): Double {
        val sLong = weights[11] * d.pow(-weights[12]) * ((s + 1.0).pow(weights[13]) - 1.0) * exp((1.0 - r) * weights[14])
        val sShort = s / exp(weights[17] * weights[18])
        return max(min(sLong, sShort), 0.001)
    }

    fun shortTermStability(s: Double, rating: Rating): Double {
        val safeS = if (s.isNaN() || s <= 0.0) 0.001 else s
        var sInc = exp(weights[17] * (rating.value - 3.0 + weights[18]))
        if (rating.value >= 3) {
            sInc = max(sInc, 1.0)
        }
        return max(safeS * sInc, 0.001)
    }

    fun formatDurationBadge(millis: Long): String {
        val minutes = millis / 60_000L
        return if (minutes < 60) "${max(1, minutes)}分" else "${minutes / 60}小时"
    }

    fun formatIntervalBadge(days: Int): String {
        return when {
            days < 1 -> "1天"
            days < 30 -> "${days}天"
            days < 365 -> "${round(days / 30.0).toInt()}个月"
            else -> "${round((days / 365.0) * 10.0) / 10.0}年"
        }
    }

    fun previewIntervals(card: CardFsrsState, nowMillis: Long = System.currentTimeMillis()): Map<Rating, String> {
        return Rating.entries.associateWith { rating ->
            val result = evaluateReview(card, rating, nowMillis)
            val updatedCard = result.updatedCard
            when (updatedCard.state) {
                CardState.LEARNING, CardState.RELEARNING -> {
                    val stepDelay = when (rating) {
                        Rating.AGAIN -> learningSteps.firstOrNull() ?: 60_000L
                        Rating.HARD -> ((learningSteps.firstOrNull() ?: 60_000L) * 1.5).toLong()
                        else -> learningSteps.getOrNull(1) ?: 600_000L
                    }
                    formatDurationBadge(stepDelay)
                }
                CardState.REVIEW -> {
                    formatIntervalBadge(updatedCard.scheduledDays)
                }
                CardState.NEW -> "1天"
            }
        }
    }

    fun evaluateReview(
        card: CardFsrsState,
        rating: Rating,
        nowMillis: Long = System.currentTimeMillis()
    ): NextStateResult {
        val elapsedDays = if (card.lastReviewTime == null) 0 else {
            try {
                val zone = java.time.ZoneId.systemDefault()
                val lastDate = java.time.Instant.ofEpochMilli(card.lastReviewTime).atZone(zone).toLocalDate()
                val currentDate = java.time.Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
                java.time.temporal.ChronoUnit.DAYS.between(lastDate, currentDate).toInt().coerceAtLeast(0)
            } catch (_: Exception) {
                max(0, ((nowMillis - card.lastReviewTime) / 86_400_000L).toInt())
            }
        }
        val isSameDay = card.lastReviewTime != null && elapsedDays == 0

        val safeStability = if (card.stability.isNaN() || card.stability <= 0.0) 0.001 else card.stability
        val safeDifficulty = if (card.difficulty.isNaN()) 5.0 else card.difficulty.coerceIn(1.0, 10.0)

        val newStability: Double
        val newDifficulty: Double

        when (card.state) {
            CardState.NEW -> {
                newStability = initialStability(rating)
                newDifficulty = initialDifficulty(rating)
            }
            CardState.LEARNING, CardState.RELEARNING -> {
                if (isSameDay) {
                    newStability = shortTermStability(safeStability, rating)
                    newDifficulty = nextDifficulty(safeDifficulty, rating)
                } else {
                    val r = retrievability(elapsedDays, safeStability)
                    newStability = if (rating == Rating.AGAIN) {
                        nextForgetStability(safeDifficulty, safeStability, r)
                    } else {
                        nextRecallStability(safeDifficulty, safeStability, r, rating)
                    }
                    newDifficulty = nextDifficulty(safeDifficulty, rating)
                }
            }
            CardState.REVIEW -> {
                if (isSameDay) {
                    newStability = shortTermStability(safeStability, rating)
                } else {
                    val r = retrievability(elapsedDays, safeStability)
                    newStability = if (rating == Rating.AGAIN) {
                        nextForgetStability(safeDifficulty, safeStability, r)
                    } else {
                        nextRecallStability(safeDifficulty, safeStability, r, rating)
                    }
                }
                newDifficulty = nextDifficulty(safeDifficulty, rating)
            }
        }

        var nextState = card.state
        var nextStep = card.step
        var scheduledDays = 0
        var dueMillis = nowMillis
        var lapses = card.lapses

        when (card.state) {
            CardState.NEW -> {
                when (rating) {
                    Rating.AGAIN -> {
                        nextState = CardState.LEARNING
                        nextStep = 0
                        dueMillis = nowMillis + (learningSteps.getOrNull(0) ?: 60_000L)
                    }
                    Rating.HARD -> {
                        nextState = CardState.LEARNING
                        nextStep = 0
                        dueMillis = nowMillis + ((learningSteps.getOrNull(0) ?: 60_000L) * 1.5).toLong()
                    }
                    Rating.GOOD -> {
                        if (learningSteps.size <= 1) {
                            nextState = CardState.REVIEW
                            nextStep = null
                            scheduledDays = nextInterval(newStability)
                            dueMillis = nowMillis + scheduledDays * 86_400_000L
                        } else {
                            nextState = CardState.LEARNING
                            nextStep = 1
                            dueMillis = nowMillis + (learningSteps.getOrNull(1) ?: 600_000L)
                        }
                    }
                    Rating.EASY -> {
                        nextState = CardState.REVIEW
                        nextStep = null
                        scheduledDays = nextInterval(newStability)
                        dueMillis = nowMillis + scheduledDays * 86_400_000L
                    }
                }
            }
            CardState.LEARNING -> {
                val currentStep = card.step ?: 0
                when (rating) {
                    Rating.AGAIN -> {
                        nextStep = 0
                        dueMillis = nowMillis + (learningSteps.getOrNull(0) ?: 60_000L)
                    }
                    Rating.HARD -> {
                        dueMillis = nowMillis + (learningSteps.getOrNull(currentStep) ?: 600_000L)
                    }
                    Rating.GOOD -> {
                        if (currentStep + 1 >= learningSteps.size) {
                            nextState = CardState.REVIEW
                            nextStep = null
                            scheduledDays = nextInterval(newStability)
                            dueMillis = nowMillis + scheduledDays * 86_400_000L
                        } else {
                            nextStep = currentStep + 1
                            dueMillis = nowMillis + (learningSteps.getOrNull(nextStep) ?: 600_000L)
                        }
                    }
                    Rating.EASY -> {
                        nextState = CardState.REVIEW
                        nextStep = null
                        scheduledDays = nextInterval(newStability)
                        dueMillis = nowMillis + scheduledDays * 86_400_000L
                    }
                }
            }
            CardState.REVIEW -> {
                when (rating) {
                    Rating.AGAIN -> {
                        nextState = CardState.RELEARNING
                        nextStep = 0
                        lapses += 1
                        dueMillis = nowMillis + (relearningSteps.getOrNull(0) ?: 600_000L)
                    }
                    Rating.HARD -> {
                        scheduledDays = nextInterval(newStability)
                        dueMillis = nowMillis + scheduledDays * 86_400_000L
                    }
                    Rating.GOOD -> {
                        scheduledDays = nextInterval(newStability)
                        dueMillis = nowMillis + scheduledDays * 86_400_000L
                    }
                    Rating.EASY -> {
                        val r = if (card.lastReviewTime == null) 1.0 else retrievability(elapsedDays, card.stability)
                        val goodStability = if (isSameDay) shortTermStability(card.stability, Rating.GOOD) else nextRecallStability(card.difficulty, card.stability, r, Rating.GOOD)
                        val goodIvl = nextInterval(goodStability)
                        val easyIvl = nextInterval(newStability)
                        scheduledDays = min(max(easyIvl, goodIvl + 1), maximumInterval)
                        dueMillis = nowMillis + scheduledDays * 86_400_000L
                    }
                }
            }
            CardState.RELEARNING -> {
                val currentStep = card.step ?: 0
                when (rating) {
                    Rating.AGAIN -> {
                        nextStep = 0
                        dueMillis = nowMillis + (relearningSteps.getOrNull(0) ?: 600_000L)
                    }
                    Rating.HARD -> {
                        dueMillis = nowMillis + (relearningSteps.getOrNull(currentStep) ?: 600_000L)
                    }
                    Rating.GOOD, Rating.EASY -> {
                        nextState = CardState.REVIEW
                        nextStep = null
                        scheduledDays = nextInterval(newStability)
                        dueMillis = nowMillis + scheduledDays * 86_400_000L
                    }
                }
            }
        }

        if (nextState == CardState.REVIEW && scheduledDays >= 3) {
            scheduledDays = applyFuzz(scheduledDays, card.cardId, card.reps + 1)
            dueMillis = nowMillis + scheduledDays * 86_400_000L
        }

        val updatedCard = card.copy(
            state = nextState,
            step = nextStep,
            stability = newStability,
            difficulty = newDifficulty,
            elapsedDays = elapsedDays,
            scheduledDays = scheduledDays,
            reps = card.reps + 1,
            lapses = lapses,
            lastReviewTime = nowMillis,
            dueTime = dueMillis,
            isLeech = lapses >= 4
        )

        val log = ReviewLog(
            cardId = card.cardId,
            rating = rating,
            previousState = card.state,
            currentState = nextState,
            stability = newStability,
            difficulty = newDifficulty,
            elapsedDays = elapsedDays,
            scheduledDays = scheduledDays,
            reviewTime = nowMillis
        )

        return NextStateResult(
            updatedCard = updatedCard,
            reviewLog = log,
            nextIntervalDays = scheduledDays.toDouble()
        )
    }
}
