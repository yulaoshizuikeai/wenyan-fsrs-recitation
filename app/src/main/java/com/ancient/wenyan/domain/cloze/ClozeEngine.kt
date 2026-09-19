package com.ancient.wenyan.domain.cloze

/**
 * 5-Level Progressive Cloze Masking Engine for classical Chinese poetry and prose recitation.
 * - Level 0: Full text unchanged.
 * - Level 1: Key phrases and exam keywords masked with ⟦ __ ⟧.
 * - Level 2: Second half of each clause masked.
 * - Level 3: Skeleton prompt (first character of each clause preserved).
 * - Level 4: Full blind masking (all Chinese characters masked with underscores, punctuation preserved).
 */
object ClozeEngine {

    fun generateLevel0(text: String): String = text

    fun generateLevel1(text: String, keywords: List<String>): String {
        var masked = text
        for (kw in keywords) {
            if (kw.isNotBlank()) {
                masked = masked.replace(kw, "⟦ ${"_".repeat(kw.length)} ⟧")
            }
        }
        return masked
    }

    fun generateLevel2(text: String): String {
        val clauses = text.split("，", "。", "；", "！", "？", "：", "、").filter { it.isNotBlank() }
        var result = text
        for (clause in clauses) {
            if (clause.length >= 4) {
                val half = clause.length / 2
                val toMask = clause.substring(half)
                result = result.replace(toMask, "⟦ ${"_".repeat(toMask.length)} ⟧")
            }
        }
        return result
    }

    fun generateLevel3(text: String): String {
        val sb = StringBuilder()
        var atStartOfClause = true
        for (ch in text) {
            when {
                ch in listOf('，', '。', '；', '！', '？', '：', '“', '”', '、', '\n', '\r', ' ') -> {
                    sb.append(ch)
                    atStartOfClause = true
                }
                atStartOfClause -> {
                    sb.append(ch)
                    atStartOfClause = false
                }
                else -> {
                    sb.append('_')
                }
            }
        }
        return sb.toString()
    }

    fun generateLevel4(text: String): String {
        val sb = StringBuilder()
        for (ch in text) {
            if (ch in listOf('，', '。', '；', '！', '？', '：', '“', '”', '、', '\n', '\r', ' ')) {
                sb.append(ch)
            } else {
                sb.append('_')
            }
        }
        return sb.toString()
    }
}
