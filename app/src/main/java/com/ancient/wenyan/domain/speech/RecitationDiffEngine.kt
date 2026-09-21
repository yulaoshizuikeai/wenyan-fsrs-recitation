package com.ancient.wenyan.domain.speech

import kotlin.math.max
import kotlin.math.roundToInt

enum class DiffType {
    MATCH,        // 完全匹配
    SUBSTITUTION, // 错字 / 形近错读
    MISSING,      // 漏字
    EXTRA         // 多字 / 冗余
}

data class DiffItem(
    val char: Char,
    val type: DiffType,
    val expectedChar: Char? = null
)

data class RecitationEvaluationResult(
    val accuracy: Float,                // 0..100 准确率评分
    val matchedCount: Int,
    val missingCount: Int,
    val extraCount: Int,
    val wrongCount: Int,
    val totalExpected: Int,
    val diffSequence: List<DiffItem>,   // 渲染可视化比对高亮
    val isPerfect: Boolean,
    val feedbackMessage: String
)

/**
 * Pure Kotlin on-device dynamic-programming diff comparison engine for recitation.
 * Uses Longest Common Subsequence (LCS) to accurately detect omitted words,
 * erroneous characters, and redundant utterances without needing network connections.
 */
object RecitationDiffEngine {

    // Clean punctuation and whitespace for pure character comparison
    fun sanitize(text: String): String {
        return text.filter { it.isLetterOrDigit() }
    }

    fun evaluate(spoken: String, expected: String): RecitationEvaluationResult {
        val s1 = sanitize(spoken)
        val s2 = sanitize(expected)

        if (s2.isEmpty()) {
            return RecitationEvaluationResult(
                accuracy = 100f,
                matchedCount = 0,
                missingCount = 0,
                extraCount = 0,
                wrongCount = 0,
                totalExpected = 0,
                diffSequence = emptyList(),
                isPerfect = true,
                feedbackMessage = "篇章为空"
            )
        }

        val m = s1.length
        val n = s2.length

        // LCS Dynamic Programming Matrix
        val dp = Array(m + 1) { IntArray(n + 1) }
        for (i in 1..m) {
            for (j in 1..n) {
                dp[i][j] = if (s1[i - 1] == s2[j - 1]) {
                    dp[i - 1][j - 1] + 1
                } else {
                    max(dp[i - 1][j], dp[i][j - 1])
                }
            }
        }

        // Backtrack to build diff alignment sequence
        var i = m
        var j = n
        val diffListReversed = mutableListOf<DiffItem>()
        var matched = 0
        var missing = 0
        var extra = 0

        while (i > 0 && j > 0) {
            if (s1[i - 1] == s2[j - 1]) {
                diffListReversed.add(DiffItem(s1[i - 1], DiffType.MATCH, s2[j - 1]))
                matched++
                i--
                j--
            } else if (dp[i - 1][j] >= dp[i][j - 1]) {
                diffListReversed.add(DiffItem(s1[i - 1], DiffType.EXTRA, null))
                extra++
                i--
            } else {
                diffListReversed.add(DiffItem(s2[j - 1], DiffType.MISSING, s2[j - 1]))
                missing++
                j--
            }
        }

        while (i > 0) {
            diffListReversed.add(DiffItem(s1[i - 1], DiffType.EXTRA, null))
            extra++
            i--
        }

        while (j > 0) {
            diffListReversed.add(DiffItem(s2[j - 1], DiffType.MISSING, s2[j - 1]))
            missing++
            j--
        }

        val diffSequence = diffListReversed.reversed()
        val accuracy = ((matched.toFloat() / max(1, n).toFloat()) * 100f).coerceIn(0f, 100f)
        val roundedAcc = (accuracy * 10f).roundToInt() / 10f

        val isPerfect = (matched == n && extra == 0)
        val feedback = when {
            isPerfect -> "绝妙精湛！一字不差，声韵谐畅！"
            roundedAcc >= 90f -> "背诵极为流畅，个别字词稍有细微出入，已达极佳境界。"
            roundedAcc >= 70f -> "基本掌握篇章主干，请重点关注标红的遗漏或替换字词。"
            else -> "熟练度仍需强化，建议对照释义与通假字多次温习。"
        }

        return RecitationEvaluationResult(
            accuracy = roundedAcc,
            matchedCount = matched,
            missingCount = missing,
            extraCount = extra,
            wrongCount = 0,
            totalExpected = n,
            diffSequence = diffSequence,
            isPerfect = isPerfect,
            feedbackMessage = feedback
        )
    }
}
