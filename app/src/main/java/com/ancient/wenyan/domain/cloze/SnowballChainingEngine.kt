package com.ancient.wenyan.domain.cloze

import com.ancient.wenyan.data.CurriculumDataSource
import com.ancient.wenyan.domain.model.Article

data class SnowballUnit(
    val index: Int,
    val text: String,
    val cardIds: List<String> = emptyList()
)

data class SnowballStage(
    val stageIndex: Int,                // 当前滚雪球阶段 (从 0 到 totalStages - 1)
    val totalStages: Int,
    val newlyAddedUnit: SnowballUnit,   // 本阶段最新加入的句联
    val chainUnits: List<SnowballUnit>, // 累计需要连背的所有句联 (从 Unit 0 到 Unit stageIndex)
    val progressRatio: Float            // 滚雪球累计完成比例
)

/**
 * Snowball Paragraph Chaining Recitation Engine (长文滚雪球串联背诵引擎).
 * Breaks down long classical prose into progressive cumulative chaining steps:
 * 1st -> 1st + 2nd -> 1st + 2nd + 3rd -> ... -> full article.
 */
object SnowballChainingEngine {

    fun buildStages(article: Article): List<SnowballStage> {
        val flashcards = CurriculumDataSource.generateFlashcardsForArticle(article)
        val units = if (flashcards.isNotEmpty()) {
            flashcards.groupBy { it.unitIndex }
                .entries
                .sortedBy { it.key }
                .mapIndexed { idx, entry ->
                    val cardsForUnit = entry.value
                    val firstCard = cardsForUnit.first()
                    SnowballUnit(
                        index = idx,
                        text = firstCard.fullVerseContext ?: firstCard.backAnswer,
                        cardIds = cardsForUnit.map { it.id }
                    )
                }
        } else {
            article.paragraphs.mapIndexed { idx, p -> SnowballUnit(idx, p) }
        }

        if (units.isEmpty()) return emptyList()

        val stages = mutableListOf<SnowballStage>()
        val total = units.size

        for (i in 0 until total) {
            val chain = units.subList(0, i + 1)
            stages.add(
                SnowballStage(
                    stageIndex = i,
                    totalStages = total,
                    newlyAddedUnit = units[i],
                    chainUnits = chain,
                    progressRatio = (i + 1).toFloat() / total.toFloat()
                )
            )
        }
        return stages
    }
}
