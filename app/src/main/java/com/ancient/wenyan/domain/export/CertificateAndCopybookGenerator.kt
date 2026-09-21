package com.ancient.wenyan.domain.export

import com.ancient.wenyan.data.CurriculumDataSource
import com.ancient.wenyan.domain.model.Article
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class GraduationCertificateData(
    val articleTitle: String,
    val author: String,
    val dynasty: String,
    val totalSentences: Int,
    val masteryPercentage: Float,
    val issueDate: String,
    val sealText: String = "研墨通达",
    val commendationText: String
)

data class CopybookCharacter(
    val char: Char,
    val isPunctuation: Boolean
)

data class CopybookLine(
    val characters: List<CopybookCharacter>
)

data class CopybookData(
    val articleTitle: String,
    val author: String,
    val lines: List<CopybookLine>
)

/**
 * Classical Chinese Graduation Certificate & Calligraphy Copybook Generator.
 */
object CertificateAndCopybookGenerator {

    fun generateCertificate(
        article: Article,
        masteryPercentage: Float
    ): GraduationCertificateData {
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"))
        val commendation = when {
            masteryPercentage >= 100f -> "学贯篇章，字字珠玑。胸罗万卷，已臻化境。"
            masteryPercentage >= 90f -> "研读精深，声韵纯熟。温故知新，更上层楼。"
            else -> "持之以恒，渐入佳境。笃学尚行，金石为开。"
        }

        val flashcards = CurriculumDataSource.generateFlashcardsForArticle(article)
        val sentenceCount = if (flashcards.isNotEmpty()) flashcards.distinctBy { it.unitIndex }.size else article.paragraphs.size

        return GraduationCertificateData(
            articleTitle = article.title,
            author = article.author,
            dynasty = article.dynasty,
            totalSentences = sentenceCount,
            masteryPercentage = masteryPercentage,
            issueDate = today,
            sealText = if (masteryPercentage >= 100f) "圆融贯通" else "笃学精进",
            commendationText = commendation
        )
    }

    fun generateCopybook(article: Article): CopybookData {
        val lines = mutableListOf<CopybookLine>()
        val flashcards = CurriculumDataSource.generateFlashcardsForArticle(article)
        val sentences = if (flashcards.isNotEmpty()) {
            flashcards.distinctBy { it.unitIndex }.map { it.fullVerseContext ?: it.backAnswer }
        } else {
            article.paragraphs
        }

        for (sent in sentences) {
            val chars = sent.map { c: Char ->
                CopybookCharacter(
                    char = c,
                    isPunctuation = !c.isLetterOrDigit()
                )
            }
            lines.add(CopybookLine(chars))
        }
        return CopybookData(
            articleTitle = article.title,
            author = article.author,
            lines = lines
        )
    }
}
