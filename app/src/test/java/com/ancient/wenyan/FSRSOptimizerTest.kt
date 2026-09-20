package com.ancient.wenyan

import com.ancient.wenyan.data.WenYanRepository
import com.ancient.wenyan.domain.fsrs.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit Test Suite for FSRS-5 Auto-Tuning Optimizer & Repository Settings.
 */
class FSRSOptimizerTest {

    @Test
    fun test01_minimumSampleThreshold() {
        val fewLogs = listOf(
            ReviewLog(
                cardId = "card_1",
                rating = Rating.GOOD,
                previousState = CardState.NEW,
                currentState = CardState.REVIEW,
                stability = 2.5,
                difficulty = 5.0,
                elapsedDays = 0,
                scheduledDays = 2,
                reviewTime = System.currentTimeMillis()
            )
        )

        val result = FSRSOptimizer.optimize(fewLogs)
        assertFalse("Should fail optimization when samples < 10", result.success)
        assertEquals(1, result.sampleCount)
        assertTrue(result.summaryText.contains("建议积累至少 10 条"))
        assertArrayEquals(
            "Weights should remain unchanged",
            FSRSEngine.DEFAULT_FSRS_5_WEIGHTS,
            result.optimizedWeights,
            0.0001
        )
    }

    @Test
    fun test02_highForgettingRate_dampsInitialStability() {
        // Simulate a struggling learner: 15 logs with 60% AGAIN
        val strugglingLogs = mutableListOf<ReviewLog>()
        for (i in 1..15) {
            val rating = if (i % 2 == 0) Rating.AGAIN else Rating.HARD
            strugglingLogs.add(
                ReviewLog(
                    cardId = "card_$i",
                    rating = rating,
                    previousState = CardState.LEARNING,
                    currentState = if (rating == Rating.AGAIN) CardState.RELEARNING else CardState.REVIEW,
                    stability = 1.0,
                    difficulty = 6.0,
                    elapsedDays = 1,
                    scheduledDays = 1,
                    reviewTime = System.currentTimeMillis()
                )
            )
        }

        val defaultWeights = FSRSEngine.DEFAULT_FSRS_5_WEIGHTS
        val result = FSRSOptimizer.optimize(strugglingLogs, defaultWeights)

        assertTrue("Optimization should succeed with 15 logs", result.success)
        assertEquals(15, result.sampleCount)
        // High forgetting rate should reduce initial Good stability w2 to tighten reviews
        assertTrue(
            "w2 (Good S0) should decrease from default ${defaultWeights[2]} for struggling learner",
            result.optimizedWeights[2] < defaultWeights[2]
        )
        // High forgetting rate should reduce initial Hard stability w1
        assertTrue(
            "w1 (Hard S0) should decrease from default ${defaultWeights[1]}",
            result.optimizedWeights[1] < defaultWeights[1]
        )
    }

    @Test
    fun test03_highMasteryLearner_boostsStability() {
        // Simulate a strong learner: 20 logs with 80% EASY and 20% GOOD
        val strongLogs = mutableListOf<ReviewLog>()
        for (i in 1..20) {
            val rating = if (i <= 16) Rating.EASY else Rating.GOOD
            strongLogs.add(
                ReviewLog(
                    cardId = "card_$i",
                    rating = rating,
                    previousState = CardState.REVIEW,
                    currentState = CardState.REVIEW,
                    stability = 5.0,
                    difficulty = 4.0,
                    elapsedDays = 3,
                    scheduledDays = 5,
                    reviewTime = System.currentTimeMillis()
                )
            )
        }

        val defaultWeights = FSRSEngine.DEFAULT_FSRS_5_WEIGHTS
        val result = FSRSOptimizer.optimize(strongLogs, defaultWeights)

        assertTrue("Optimization should succeed", result.success)
        assertTrue(
            "w2 (Good S0) should increase or stay high for strong learner",
            result.optimizedWeights[2] >= defaultWeights[2]
        )
        assertTrue(
            "w3 (Easy S0) should increase for easy-heavy learner",
            result.optimizedWeights[3] > defaultWeights[3]
        )
    }

    @Test
    fun test04_physiologicalBoundsEnforced() {
        // Simulate an extreme outlier dataset
        val extremeLogs = List(50) { i ->
            ReviewLog(
                cardId = "extreme_$i",
                rating = Rating.AGAIN,
                previousState = CardState.REVIEW,
                currentState = CardState.RELEARNING,
                stability = 0.01,
                difficulty = 10.0,
                elapsedDays = 1,
                scheduledDays = 1,
                reviewTime = System.currentTimeMillis()
            )
        }

        val result = FSRSOptimizer.optimize(extremeLogs)
        assertTrue(result.success)

        // Verify bounds:
        assertTrue("w2 must be >= 1.0", result.optimizedWeights[2] >= 1.0)
        assertTrue("w2 must be <= 6.0", result.optimizedWeights[2] <= 6.0)
        assertTrue("w1 must be >= 0.4", result.optimizedWeights[1] >= 0.4)
        assertTrue("w1 must be <= 2.5", result.optimizedWeights[1] <= 2.5)
        assertTrue("w8 must be >= 0.5", result.optimizedWeights[8] >= 0.5)
        assertTrue("w8 must be <= 3.5", result.optimizedWeights[8] <= 3.5)
        assertTrue("w11 must be >= 0.5", result.optimizedWeights[11] >= 0.5)
        assertTrue("w11 must be <= 4.0", result.optimizedWeights[11] <= 4.0)
    }

    @Test
    fun test05_repositoryParameterUpdateAndReset() {
        val repo = WenYanRepository(context = null)

        // Check defaults
        assertEquals(0.93, repo.fsrsEngine.requestRetention, 0.001)
        assertEquals(0.72, repo.fsrsEngine.recitationStabilityFactor, 0.001)
        assertEquals(36500, repo.fsrsEngine.maximumInterval)

        // Custom update
        val customWeights = FSRSEngine.DEFAULT_FSRS_5_WEIGHTS.clone()
        customWeights[2] = 2.85
        repo.updateFSRSSettings(
            weights = customWeights,
            retention = 0.95,
            factor = 0.65,
            maxInterval = 730
        )

        assertEquals(0.95, repo.fsrsEngine.requestRetention, 0.001)
        assertEquals(0.65, repo.fsrsEngine.recitationStabilityFactor, 0.001)
        assertEquals(730, repo.fsrsEngine.maximumInterval)
        assertEquals(2.85, repo.fsrsEngine.weights[2], 0.001)

        // Reset to defaults
        repo.resetFSRSSettingsToDefault()
        assertEquals(0.93, repo.fsrsEngine.requestRetention, 0.001)
        assertEquals(0.72, repo.fsrsEngine.recitationStabilityFactor, 0.001)
        assertEquals(36500, repo.fsrsEngine.maximumInterval)
        assertEquals(FSRSEngine.DEFAULT_FSRS_5_WEIGHTS[2], repo.fsrsEngine.weights[2], 0.001)
    }

    @Test
    fun test06_autoTunePeriodicExecutionTrigger() {
        val repo = WenYanRepository(context = null)

        // Submit 20 reviews with struggling ratings
        for (i in 1..20) {
            repo.submitRating("card_$i", if (i % 2 == 0) Rating.AGAIN else Rating.HARD)
        }

        // Auto-tuning should have evaluated or recorded logs
        assertTrue("Should have recorded review logs", repo.getReviewLogs().size >= 20)
    }
}
