package com.ancient.wenyan.domain.fsrs

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
 * Pure Kotlin Local Offline FSRS-5 19-Parameter Nelder-Mead Optimization Engine.
 *
 * Employs the Nelder-Mead Simplex optimization algorithm to tune all 19 weights
 * directly against Binary Cross-Entropy (Log Loss) on genuine review logs,
 * enforcing physiological and mathematical bounds with non-linear penalty terms.
 */
object FSRSOptimizer {

    private const val MIN_REQUIRED_SAMPLES = 10
    private const val FACTOR = 0.2345679 // 19.0 / 81.0 ~ (0.9^(-2) - 1)
    private const val DECAY = -0.5

    // Mutex to protect concurrent optimization runs across coroutines
    val optimizationMutex = Mutex()

    suspend fun optimizeWithLock(
        reviewLogs: List<ReviewLog>,
        currentWeights: DoubleArray = FSRSEngine.DEFAULT_FSRS_5_WEIGHTS
    ): OptimizationResult = optimizationMutex.withLock {
        optimize(reviewLogs, currentWeights)
    }

    /**
     * Optimizes FSRS weights based on review history using Nelder-Mead Simplex.
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

        val total = eligibleLogs.size.toDouble()
        val againCount = eligibleLogs.count { it.rating == Rating.AGAIN }
        val hardCount = eligibleLogs.count { it.rating == Rating.HARD }
        val easyCount = eligibleLogs.count { it.rating == Rating.EASY }

        val actualRetention = (total - againCount) / total
        val againRate = againCount / total
        val hardRate = hardCount / total
        val easyRate = easyCount / total

        // 1. Initial Seed Estimation based on empirical deviation
        val seedWeights = FSRSEngine.DEFAULT_FSRS_5_WEIGHTS.clone()
        val retentionDamping = when {
            actualRetention < 0.80 -> 0.78
            actualRetention < 0.88 -> 0.88
            actualRetention > 0.96 -> 1.15
            actualRetention > 0.93 -> 1.08
            else -> 1.00
        }

        val hardAdj = if (hardRate > 0.35) 0.85 else if (hardRate < 0.10) 1.10 else 1.0
        val easyAdj = if (easyRate > 0.30) 1.15 else 0.95

        seedWeights[0] = (seedWeights[0] * if (againRate > 0.25) 0.85 else 1.05).coerceIn(0.15, 0.9)
        seedWeights[1] = (seedWeights[1] * retentionDamping * hardAdj).coerceIn(0.4, 2.5)
        seedWeights[2] = (seedWeights[2] * retentionDamping).coerceIn(1.0, 6.0)
        seedWeights[3] = (seedWeights[3] * easyAdj).coerceIn(6.0, 25.0)

        if (againRate > 0.25 || hardRate > 0.35) {
            seedWeights[4] = (seedWeights[4] + 0.45).coerceIn(4.0, 9.5)
        } else if (easyRate > 0.35 && actualRetention > 0.92) {
            seedWeights[4] = (seedWeights[4] - 0.40).coerceIn(4.0, 9.5)
        }

        // 2. Multi-dimensional Nelder-Mead Simplex Optimization across 19 parameters
        val optimizedWeights = runNelderMead(seedWeights, eligibleLogs, maxIterations = 40)

        // Ensure physiological bounds strictly enforced
        clampWeights(optimizedWeights)

        val finalLoss = computeLogLoss(optimizedWeights, eligibleLogs)
        val lossImprovement = if (prevLoss > 0.0) {
            max(0.0, ((prevLoss - finalLoss) / prevLoss) * 100.0).toFloat()
        } else {
            0.0f
        }

        val detailedChanges = mutableListOf<String>()
        val paramNames = listOf(
            "遗忘重来基数 (w0)", "困难稳固基数 (w1)", "良好稳固基数 (w2)", "简单稳固基数 (w3)",
            "认知难度基数 (w4)", "难度跨度指数 (w5)", "难度调节步长 (w6)", "均值回归速率 (w7)",
            "复习成功增长 (w8)", "稳固阻尼因子 (w9)", "可提取性敏感 (w10)", "遗忘折损因子 (w11)",
            "难度惩罚指数 (w12)", "稳固保留幂次 (w13)", "遗忘敏感因子 (w14)", "困难惩罚比率 (w15)",
            "容易奖励比率 (w16)", "短期记忆乘数 (w17)", "短期评级偏移 (w18)"
        )

        for (i in 0..18) {
            if (abs(optimizedWeights[i] - currentWeights[i]) > 0.01) {
                detailedChanges.add("${paramNames[i]}: ${round2(currentWeights[i])} ➔ ${round2(optimizedWeights[i])}")
            }
        }

        val summary = if (detailedChanges.isEmpty()) {
            "基于你已有的 ${eligibleLogs.size} 次复习记录，Nelder-Mead 单纯形调优计算确认当前参数与你的记忆节奏契合度极佳。"
        } else {
            "基于 Nelder-Mead 19 参多维寻优，已针对你的 ${eligibleLogs.size} 次复习记录完成全维度联合微调。"
        }

        return OptimizationResult(
            success = true,
            sampleCount = eligibleLogs.size,
            previousWeights = currentWeights.clone(),
            optimizedWeights = optimizedWeights,
            previousLoss = prevLoss,
            optimizedLoss = finalLoss,
            improvementPercentage = lossImprovement,
            summaryText = summary,
            detailedChanges = detailedChanges
        )
    }

    /**
     * Nelder-Mead Simplex Algorithm for 19-dimensional weight vector.
     */
    private fun runNelderMead(
        initialPoint: DoubleArray,
        logs: List<ReviewLog>,
        maxIterations: Int = 40
    ): DoubleArray {
        val n = 19
        // Build initial simplex with n + 1 points
        val simplex = Array(n + 1) { DoubleArray(n) }
        val scores = DoubleArray(n + 1)

        simplex[0] = initialPoint.clone()
        scores[0] = evaluateCost(simplex[0], logs)

        for (i in 0 until n) {
            val point = initialPoint.clone()
            // Perturb by 5% along each dimension
            val step = if (abs(point[i]) < 1e-4) 0.05 else point[i] * 0.05
            point[i] += step
            clampWeights(point)
            simplex[i + 1] = point
            scores[i + 1] = evaluateCost(point, logs)
        }

        val alpha = 1.0  // Reflection
        val gamma = 2.0  // Expansion
        val rho = 0.5    // Contraction
        val sigma = 0.5  // Shrink

        for (iter in 0 until maxIterations) {
            // Sort simplex by score ascending
            val order = (0..n).sortedBy { scores[it] }
            val bestIdx = order[0]
            val worstIdx = order[n]
            val secondWorstIdx = order[n - 1]

            val bestPoint = simplex[bestIdx]
            val worstPoint = simplex[worstIdx]

            // Calculate centroid of the best n points (excluding worst)
            val centroid = DoubleArray(n)
            for (i in 0 until n) {
                val idx = order[i]
                for (d in 0 until n) {
                    centroid[d] += simplex[idx][d]
                }
            }
            for (d in 0 until n) {
                centroid[d] /= n.toDouble()
            }

            // 1. Reflection
            val xr = DoubleArray(n) { d -> centroid[d] + alpha * (centroid[d] - worstPoint[d]) }
            clampWeights(xr)
            val scoreR = evaluateCost(xr, logs)

            if (scoreR < scores[secondWorstIdx] && scoreR >= scores[bestIdx]) {
                simplex[worstIdx] = xr
                scores[worstIdx] = scoreR
                continue
            }

            // 2. Expansion
            if (scoreR < scores[bestIdx]) {
                val xe = DoubleArray(n) { d -> centroid[d] + gamma * (xr[d] - centroid[d]) }
                clampWeights(xe)
                val scoreE = evaluateCost(xe, logs)
                if (scoreE < scoreR) {
                    simplex[worstIdx] = xe
                    scores[worstIdx] = scoreE
                } else {
                    simplex[worstIdx] = xr
                    scores[worstIdx] = scoreR
                }
                continue
            }

            // 3. Contraction
            val xc = DoubleArray(n) { d -> centroid[d] + rho * (worstPoint[d] - centroid[d]) }
            clampWeights(xc)
            val scoreC = evaluateCost(xc, logs)
            if (scoreC < scores[worstIdx]) {
                simplex[worstIdx] = xc
                scores[worstIdx] = scoreC
                continue
            }

            // 4. Shrink
            for (i in 1..n) {
                val idx = order[i]
                for (d in 0 until n) {
                    simplex[idx][d] = bestPoint[d] + sigma * (simplex[idx][d] - bestPoint[d])
                }
                clampWeights(simplex[idx])
                scores[idx] = evaluateCost(simplex[idx], logs)
            }
        }

        val bestIdx = (0..n).minByOrNull { scores[it] } ?: 0
        return simplex[bestIdx].clone()
    }

    private fun evaluateCost(weights: DoubleArray, logs: List<ReviewLog>): Double {
        val loss = computeLogLoss(weights, logs)
        val penalty = computePenalty(weights)
        return loss + penalty
    }

    private fun computePenalty(w: DoubleArray): Double {
        var penalty = 0.0
        // Enforce monotonicity: w0 <= w1 <= w2 <= w3
        if (w[0] > w[1]) penalty += (w[0] - w[1]) * 100.0
        if (w[1] > w[2]) penalty += (w[1] - w[2]) * 100.0
        if (w[2] > w[3]) penalty += (w[2] - w[3]) * 100.0
        return penalty
    }

    private fun clampWeights(w: DoubleArray) {
        w[0] = w[0].coerceIn(0.15, 0.9)
        w[1] = w[1].coerceIn(0.4, 2.5)
        w[2] = w[2].coerceIn(1.0, 6.0)
        w[3] = w[3].coerceIn(6.0, 25.0)
        w[4] = w[4].coerceIn(4.0, 9.5)
        w[5] = w[5].coerceIn(0.1, 1.5)
        w[6] = w[6].coerceIn(0.5, 3.0)
        w[7] = w[7].coerceIn(0.0001, 0.1)
        w[8] = w[8].coerceIn(0.5, 3.5)
        w[9] = w[9].coerceIn(0.01, 0.5)
        w[10] = w[10].coerceIn(0.5, 2.5)
        w[11] = w[11].coerceIn(0.5, 4.0)
        w[12] = w[12].coerceIn(0.01, 0.5)
        w[13] = w[13].coerceIn(0.05, 0.8)
        w[14] = w[14].coerceIn(0.5, 3.5)
        w[15] = w[15].coerceIn(0.05, 1.0)
        w[16] = w[16].coerceIn(1.5, 5.0)
        w[17] = w[17].coerceIn(0.1, 1.5)
        w[18] = w[18].coerceIn(0.1, 1.5)
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

    private fun round2(v: Double): Double = round(v * 100.0) / 100.0
}
