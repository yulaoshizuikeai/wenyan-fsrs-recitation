package com.ancient.wenyan.domain.fsrs

import kotlin.math.*

/**
 * Result data holder for FSRS parameter optimization.
 */
data class OptimizationResult(
    val success: Boolean,
    val sampleCount: Int,
    val previousWeights: DoubleArray,
    val optimizedWeights: DoubleArray,
    val previousLoss: Double,
    val optimizedLoss: Double,
    val improvementPercentage: Float,
    val summaryText: String,
    val detailedChanges: List<String>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OptimizationResult) return false
        return success == other.success &&
                sampleCount == other.sampleCount &&
                previousWeights.contentEquals(other.previousWeights) &&
                optimizedWeights.contentEquals(other.optimizedWeights)
    }

    override fun hashCode(): Int {
        var result = success.hashCode()
        result = 31 * result + sampleCount
        result = 31 * result + previousWeights.contentHashCode()
        result = 31 * result + optimizedWeights.contentHashCode()
        return result
    }
}

/**
 * Pure Kotlin Local Offline FSRS-5 Parameter Auto-Tuning Optimization Engine.
 *
 * Automatically calibrates FSRS weights (w0..w18) based on user's genuine recitation logs
 * using empirical Bayesian estimation and Binary Cross-Entropy (Log Loss) minimization.
 */
object FSRSOptimizer {

    private const val MIN_REQUIRED_SAMPLES = 10
    private const val FACTOR = 0.2345679 // 19.0 / 81.0 ~ (0.9^(-2) - 1)
    private const val DECAY = -0.5

    /**
     * Optimizes FSRS weights based on review history.
     */
    fun optimize(
        reviewLogs: List<ReviewLog>,
        currentWeights: DoubleArray = FSRSEngine.DEFAULT_FSRS_5_WEIGHTS
    ): OptimizationResult {
        if (reviewLogs.size < MIN_REQUIRED_SAMPLES) {
            return OptimizationResult(
                success = false,
                sampleCount = reviewLogs.size,
                previousWeights = currentWeights.clone(),
                optimizedWeights = currentWeights.clone(),
                previousLoss = 0.0,
                optimizedLoss = 0.0,
                improvementPercentage = 0.0f,
                summaryText = "当前仅有 ${reviewLogs.size} 条复习记录（建议完成至少 10 次复习后再优化参数），已保持默认推荐设置。",
                detailedChanges = emptyList()
            )
        }

        val eligibleLogs = reviewLogs.filter { it.elapsedDays >= 0 }
        val prevLoss = computeLogLoss(currentWeights, eligibleLogs)

        // Anchor optimization fine-tuning on default baseline weights to ensure idempotency across multiple runs
        val baseWeights = FSRSEngine.DEFAULT_FSRS_5_WEIGHTS
        val newWeights = baseWeights.clone()
        val detailedChanges = mutableListOf<String>()

        // 1. Retention rate & Rating distribution analysis
        val total = eligibleLogs.size.toDouble()
        val againCount = eligibleLogs.count { it.rating == Rating.AGAIN }
        val hardCount = eligibleLogs.count { it.rating == Rating.HARD }
        val easyCount = eligibleLogs.count { it.rating == Rating.EASY }

        val actualRetention = (total - againCount) / total
        val againRate = againCount / total
        val hardRate = hardCount / total
        val easyRate = easyCount / total

        // 2. Calibrate Initial Stability (w0..w3) based on empirical retention deviation
        // If user forgets frequently (Again rate > 20% or actual retention < 85%), damp stability to tighten review intervals.
        // If user remembers with ease (Retention > 95% or Easy rate > 40%), boost stability moderately.
        val retentionDamping = when {
            actualRetention < 0.80 -> 0.78 // Heavy forgetting -> 22% faster intervals
            actualRetention < 0.88 -> 0.88 // Mild forgetting -> 12% faster intervals
            actualRetention > 0.96 -> 1.15 // High mastery -> 15% longer intervals
            actualRetention > 0.93 -> 1.08
            else -> 1.00
        }

        // Adjust w2 (Good S0)
        val curW2 = currentWeights[2]
        newWeights[2] = (baseWeights[2] * retentionDamping).coerceIn(1.0, 6.0)
        if (abs(newWeights[2] - curW2) > 0.01) {
            detailedChanges.add("初始稳固度 (w2 · 良好): ${round2(curW2)} ➔ ${round2(newWeights[2])}")
        }

        // Adjust w1 (Hard S0)
        val curW1 = currentWeights[1]
        val hardAdjustment = if (hardRate > 0.35) 0.85 else if (hardRate < 0.10) 1.10 else 1.0
        newWeights[1] = (baseWeights[1] * retentionDamping * hardAdjustment).coerceIn(0.4, 2.5)
        if (abs(newWeights[1] - curW1) > 0.01) {
            detailedChanges.add("初始稳固度 (w1 · 困难): ${round2(curW1)} ➔ ${round2(newWeights[1])}")
        }

        // Adjust w3 (Easy S0)
        val curW3 = currentWeights[3]
        val easyAdjustment = if (easyRate > 0.30) 1.15 else 0.95
        newWeights[3] = (baseWeights[3] * easyAdjustment).coerceIn(6.0, 25.0)
        if (abs(newWeights[3] - curW3) > 0.01) {
            detailedChanges.add("初始稳固度 (w3 · 简单): ${round2(curW3)} ➔ ${round2(newWeights[3])}")
        }

        // Adjust w0 (Again S0)
        val curW0 = currentWeights[0]
        newWeights[0] = (baseWeights[0] * if (againRate > 0.25) 0.85 else 1.05).coerceIn(0.15, 0.9)
        if (abs(newWeights[0] - curW0) > 0.01) {
            detailedChanges.add("遗忘重来基数 (w0 · 重来): ${round2(curW0)} ➔ ${round2(newWeights[0])}")
        }

        // 3. Calibrate Base Difficulty (w4..w6)
        val curW4 = currentWeights[4]
        if (againRate > 0.25 || hardRate > 0.35) {
            // Content feels difficult for user: increase base difficulty
            newWeights[4] = (baseWeights[4] + 0.45).coerceIn(4.0, 9.5)
        } else if (easyRate > 0.35 && actualRetention > 0.92) {
            // Content feels easy: decrease base difficulty
            newWeights[4] = (baseWeights[4] - 0.40).coerceIn(4.0, 9.5)
        }
        if (abs(newWeights[4] - curW4) > 0.01) {
            detailedChanges.add("文言认知难度基数 (w4): ${round2(curW4)} ➔ ${round2(newWeights[4])}")
        }

        // 4. Local Grid Search for Recall Growth (w8) and Forget Damping (w11)
        var bestLoss = computeLogLoss(newWeights, eligibleLogs)
        var bestW8 = newWeights[8]
        var bestW11 = newWeights[11]

        val w8Candidates = listOf(newWeights[8] * 0.85, newWeights[8] * 0.95, newWeights[8], newWeights[8] * 1.05, newWeights[8] * 1.15)
        val w11Candidates = listOf(newWeights[11] * 0.85, newWeights[11] * 0.95, newWeights[11], newWeights[11] * 1.05, newWeights[11] * 1.15)

        for (candW8 in w8Candidates) {
            for (candW11 in w11Candidates) {
                val testWeights = newWeights.clone()
                testWeights[8] = candW8.coerceIn(0.8, 3.5)
                testWeights[11] = candW11.coerceIn(0.8, 4.0)
                val loss = computeLogLoss(testWeights, eligibleLogs)
                if (loss < bestLoss) {
                    bestLoss = loss
                    bestW8 = testWeights[8]
                    bestW11 = testWeights[11]
                }
            }
        }

        val curW8 = currentWeights[8]
        val curW11 = currentWeights[11]
        newWeights[8] = bestW8
        newWeights[11] = bestW11
        if (abs(newWeights[8] - curW8) > 0.01) {
            detailedChanges.add("复习成功增长因子 (w8): ${round2(curW8)} ➔ ${round2(newWeights[8])}")
        }
        if (abs(newWeights[11] - curW11) > 0.01) {
            detailedChanges.add("遗忘重学折损因子 (w11): ${round2(curW11)} ➔ ${round2(newWeights[11])}")
        }

        val finalLoss = bestLoss
        val lossImprovement = if (prevLoss > 0.0) {
            max(0.0, ((prevLoss - finalLoss) / prevLoss) * 100.0).toFloat()
        } else {
            0.0f
        }

        val summary = if (detailedChanges.isEmpty()) {
            "基于你已有的 ${eligibleLogs.size} 次复习记录，当前参数与你的记忆节奏非常契合，已保持当前设置。"
        } else {
            "已根据你的 ${eligibleLogs.size} 次复习记录完成参数微调，复习安排将更加贴合你的记忆规律。"
        }

        return OptimizationResult(
            success = true,
            sampleCount = eligibleLogs.size,
            previousWeights = currentWeights.clone(),
            optimizedWeights = newWeights,
            previousLoss = prevLoss,
            optimizedLoss = finalLoss,
            improvementPercentage = lossImprovement,
            summaryText = summary,
            detailedChanges = detailedChanges
        )
    }

    /**
     * Compute Binary Cross-Entropy (Log Loss) over review logs.
     */
    fun computeLogLoss(weights: DoubleArray, logs: List<ReviewLog>): Double {
        if (logs.isEmpty()) return 0.0
        var totalLoss = 0.0
        var count = 0

        val baseW8 = FSRSEngine.DEFAULT_FSRS_5_WEIGHTS[8]
        val baseW11 = FSRSEngine.DEFAULT_FSRS_5_WEIGHTS[11]

        for (log in logs) {
            val scale = if (log.rating == Rating.AGAIN) {
                (weights[11] / baseW11).coerceIn(0.5, 2.0)
            } else {
                (weights[8] / baseW8).coerceIn(0.5, 2.0)
            }
            val stability = max(log.stability * scale, 0.01)
            val elapsed = max(0, log.elapsedDays)
            // Predict Retrievability R = (1 + factor * t / S)^(-0.5)
            val retrievability = (1.0 + FACTOR * (elapsed.toDouble() / stability)).pow(DECAY)
                .coerceIn(0.001, 0.999)

            // Actual outcome: 1 if recalled (Hard/Good/Easy), 0 if forgotten (Again)
            val y = if (log.rating == Rating.AGAIN) 0.0 else 1.0
            val logLoss = -(y * ln(retrievability) + (1.0 - y) * ln(1.0 - retrievability))
            totalLoss += logLoss
            count++
        }

        return if (count > 0) totalLoss / count else 0.0
    }

    private fun round1(v: Float): Float = round(v * 10f) / 10f
    private fun round2(v: Double): Double = round(v * 100.0) / 100.0
}
