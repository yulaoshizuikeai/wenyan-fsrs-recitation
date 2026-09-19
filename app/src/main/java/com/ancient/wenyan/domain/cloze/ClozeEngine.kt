package com.ancient.wenyan.domain.cloze

/**
 * 5-Level Progressive Cloze Masking Engine for classical Chinese poetry and prose recitation.
 * - Level 0: Full text unchanged.
 * - Level 1: Key phrases and exam keywords masked with ⟦ __ ⟧.
 * - Level 2: Second half of each clause masked.
 * - Level 3: Skeleton prompt (first character of each clause preserved).
 * - Level 4: Full blind masking (all Chinese characters masked with underscores, punctuation preserved).
 */
data class ClozeToken(
    val id: Int,
    val originalText: String,
    val isMasked: Boolean
)

object ClozeEngine {

    fun tokenize(text: String, level: Int, keywords: List<String>): List<ClozeToken> {
        var nextId = 1
        when (level) {
            0 -> return listOf(ClozeToken(nextId++, text, false))
            1 -> {
                val validKeywords = keywords.filter { it.isNotBlank() }
                if (validKeywords.isEmpty()) return listOf(ClozeToken(nextId++, text, false))
                val tokens = mutableListOf<ClozeToken>()
                var cursor = 0
                val sortedKws = validKeywords.sortedByDescending { it.length }
                while (cursor < text.length) {
                    var matchKw: String? = null
                    for (kw in sortedKws) {
                        if (text.startsWith(kw, cursor)) {
                            matchKw = kw
                            break
                        }
                    }
                    if (matchKw != null) {
                        tokens.add(ClozeToken(nextId++, matchKw, isMasked = true))
                        cursor += matchKw.length
                    } else {
                        val nextMatchIndex = sortedKws.mapNotNull { kw ->
                            val idx = text.indexOf(kw, cursor)
                            if (idx != -1) idx else null
                        }.minOrNull() ?: text.length

                        val plain = text.substring(cursor, nextMatchIndex)
                        if (plain.isNotEmpty()) {
                            tokens.add(ClozeToken(nextId++, plain, isMasked = false))
                        }
                        cursor = nextMatchIndex
                    }
                }
                return tokens
            }
            2 -> {
                val punctuation = setOf('，', '。', '；', '！', '？', '：', '、', '\n', '\r', ' ')
                val tokens = mutableListOf<ClozeToken>()
                var start = 0
                for (i in text.indices) {
                    if (text[i] in punctuation) {
                        val clause = text.substring(start, i)
                        if (clause.length >= 4) {
                            val half = clause.length / 2
                            tokens.add(ClozeToken(nextId++, clause.substring(0, half), false))
                            tokens.add(ClozeToken(nextId++, clause.substring(half), true))
                        } else if (clause.isNotEmpty()) {
                            tokens.add(ClozeToken(nextId++, clause, false))
                        }
                        tokens.add(ClozeToken(nextId++, text[i].toString(), false))
                        start = i + 1
                    }
                }
                if (start < text.length) {
                    val remaining = text.substring(start)
                    if (remaining.length >= 4) {
                        val half = remaining.length / 2
                        tokens.add(ClozeToken(nextId++, remaining.substring(0, half), false))
                        tokens.add(ClozeToken(nextId++, remaining.substring(half), true))
                    } else if (remaining.isNotEmpty()) {
                        tokens.add(ClozeToken(nextId++, remaining, false))
                    }
                }
                return tokens
            }
            3 -> {
                val punctuation = setOf('，', '。', '；', '！', '？', '：', '“', '”', '、', '\n', '\r', ' ')
                val tokens = mutableListOf<ClozeToken>()
                var start = 0
                for (i in text.indices) {
                    if (text[i] in punctuation) {
                        val clause = text.substring(start, i)
                        if (clause.isNotEmpty()) {
                            tokens.add(ClozeToken(nextId++, clause.take(1), false))
                            if (clause.length > 1) {
                                tokens.add(ClozeToken(nextId++, clause.substring(1), true))
                            }
                        }
                        tokens.add(ClozeToken(nextId++, text[i].toString(), false))
                        start = i + 1
                    }
                }
                if (start < text.length) {
                    val remaining = text.substring(start)
                    if (remaining.isNotEmpty()) {
                        tokens.add(ClozeToken(nextId++, remaining.take(1), false))
                        if (remaining.length > 1) {
                            tokens.add(ClozeToken(nextId++, remaining.substring(1), true))
                        }
                    }
                }
                return tokens
            }
            4 -> {
                val punctuation = setOf('，', '。', '；', '！', '？', '：', '“', '”', '、', '\n', '\r', ' ')
                val tokens = mutableListOf<ClozeToken>()
                var start = 0
                for (i in text.indices) {
                    if (text[i] in punctuation) {
                        val clause = text.substring(start, i)
                        if (clause.isNotEmpty()) {
                            tokens.add(ClozeToken(nextId++, clause, true))
                        }
                        tokens.add(ClozeToken(nextId++, text[i].toString(), false))
                        start = i + 1
                    }
                }
                if (start < text.length) {
                    val remaining = text.substring(start)
                    if (remaining.isNotEmpty()) {
                        tokens.add(ClozeToken(nextId++, remaining, true))
                    }
                }
                return tokens
            }
            else -> return listOf(ClozeToken(nextId++, text, false))
        }
    }

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
