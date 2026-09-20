package com.ancient.wenyan

import com.ancient.wenyan.data.CurriculumDataSource
import com.ancient.wenyan.domain.fsrs.FSRSEngine
import com.ancient.wenyan.domain.fsrs.Rating
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit Test Suite for Multi-Cloze Sentence Variants and Intensified FSRS Scheduling.
 */
class MultiClozeVariantAndIntensifiedFsrsTest {

    @Test
    fun test01_qinYuanChunChangShaNoMisalignment() {
        val article = CurriculumDataSource.ALL_ARTICLES.first { it.title == "沁园春·长沙" }
        val cards = CurriculumDataSource.generateFlashcardsForArticle(article)

        // Find cards corresponding to the opening 3-clause unit: 独立寒秋，湘江北去，橘子洲头。
        val openingCards = cards.filter {
            it.maskedSegment in listOf("独立寒秋", "湘江北去", "橘子洲头")
        }

        assertEquals("Must generate 3 variants for the 3-clause sentence", 3, openingCards.size)

        // Variant 1
        val c1 = openingCards.find { it.maskedSegment == "独立寒秋" }!!
        assertEquals(1, c1.clozeIndex)
        assertEquals(3, c1.totalClozes)
        assertTrue(c1.frontPrompt.contains("⟦ ________ ⟧"))
        assertTrue(c1.frontPrompt.contains("湘江北去"))
        assertTrue(c1.frontPrompt.contains("橘子洲头"))
        assertFalse(c1.frontPrompt.contains("看万山红遍")) // MUST NOT leak into next couplet!

        // Variant 2
        val c2 = openingCards.find { it.maskedSegment == "湘江北去" }!!
        assertEquals(2, c2.clozeIndex)
        assertEquals(3, c2.totalClozes)
        assertTrue(c2.frontPrompt.contains("独立寒秋"))
        assertTrue(c2.frontPrompt.contains("⟦ ________ ⟧"))
        assertTrue(c2.frontPrompt.contains("橘子洲头"))

        // Variant 3
        val c3 = openingCards.find { it.maskedSegment == "橘子洲头" }!!
        assertEquals(3, c3.clozeIndex)
        assertEquals(3, c3.totalClozes)
        assertTrue(c3.frontPrompt.contains("独立寒秋"))
        assertTrue(c3.frontPrompt.contains("湘江北去"))
        assertTrue(c3.frontPrompt.contains("⟦ ________ ⟧"))
    }

    @Test
    fun test02_dengGaoCoupletBiDirectionalTesting() {
        val article = CurriculumDataSource.ALL_ARTICLES.first { it.title == "登高" }
        val cards = CurriculumDataSource.generateFlashcardsForArticle(article)

        // Check for '无边落木萧萧下，不尽长江滚滚来。'
        val coupletCards = cards.filter {
            it.maskedSegment in listOf("无边落木萧萧下", "不尽长江滚滚来")
        }

        assertEquals("Must generate 2 variants (upper tests lower, lower tests upper)", 2, coupletCards.size)

        val upperTestLower = coupletCards.find { it.maskedSegment == "不尽长江滚滚来" }!!
        assertEquals("UPPER_PROMPT_LOWER", upperTestLower.cardType)
        assertTrue(upperTestLower.frontPrompt.startsWith("无边落木萧萧下"))
        assertTrue(upperTestLower.frontPrompt.contains("⟦ ________ ⟧"))

        val lowerTestUpper = coupletCards.find { it.maskedSegment == "无边落木萧萧下" }!!
        assertEquals("LOWER_PROMPT_UPPER", lowerTestUpper.cardType)
        assertTrue(lowerTestUpper.frontPrompt.startsWith("⟦ ________ ⟧"))
        assertTrue(lowerTestUpper.frontPrompt.contains("不尽长江滚滚来"))
    }

    @Test
    fun test03_fullVerseContextAndHighlight() {
        val article = CurriculumDataSource.ALL_ARTICLES.first { it.title == "短歌行" }
        val cards = CurriculumDataSource.generateFlashcardsForArticle(article)

        val card = cards.first { it.maskedSegment == "人生几何" }
        assertNotNull(card.fullVerseContext)
        assertTrue(card.fullVerseContext!!.contains("【人生几何】"))
        assertTrue(card.fullVerseContext!!.contains("对酒当歌"))
    }

    @Test
    fun test04_fsrsRetentionAndStabilityWeightAdjustment() {
        val defaultFsrs = FSRSEngine()

        // 1. Verify enhanced retention
        assertEquals("Request retention should be configured to 0.93 for tighter intervals", 0.93, defaultFsrs.requestRetention, 0.001)

        // 2. Verify recitation stability damping factor
        assertEquals(0.72, defaultFsrs.recitationStabilityFactor, 0.001)

        // 3. Compare with standard unadjusted stability
        val rawGoodWeight = FSRSEngine.DEFAULT_FSRS_5_WEIGHTS[Rating.GOOD.value - 1] // 3.173
        val adjustedGoodStability = defaultFsrs.initialStability(Rating.GOOD)

        assertEquals(rawGoodWeight * 0.72, adjustedGoodStability, 0.001)
        assertTrue("Adjusted stability must be lower than raw weight to intensify review", adjustedGoodStability < rawGoodWeight)

        // 4. Verify calculated interval is compressed for higher review frequency
        val compressedInterval = defaultFsrs.nextInterval(adjustedGoodStability)
        // Standard interval with 0.90 retention and 3.173 stability would be 3 days.
        // With 0.72 factor and 0.93 retention, interval compresses to 1-2 days.
        assertTrue("First good interval should be <= 2 days for tight early consolidation", compressedInterval <= 2)
    }
}
