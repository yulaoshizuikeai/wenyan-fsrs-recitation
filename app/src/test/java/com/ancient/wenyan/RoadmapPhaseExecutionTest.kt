package com.ancient.wenyan

import com.ancient.wenyan.data.CurriculumDataSource
import com.ancient.wenyan.data.GaoKaoScenarioDataSource
import com.ancient.wenyan.data.WenYanRepository
import com.ancient.wenyan.domain.cloze.SnowballChainingEngine
import com.ancient.wenyan.domain.export.CertificateAndCopybookGenerator
import com.ancient.wenyan.domain.fsrs.*
import com.ancient.wenyan.domain.speech.DiffType
import com.ancient.wenyan.domain.speech.RecitationDiffEngine
import com.ancient.wenyan.domain.sync.WebDavBackupManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/**
 * Verification test suite for ROADMAP Phase 1, Phase 2, Phase 3, and Phase 4 execution.
 */
class RoadmapPhaseExecutionTest {

    @Test
    fun test01_fsrsLeechDetectionAndThreshold() {
        val engine = FSRSEngine()
        var card = CardFsrsState(
            cardId = "test_leech_01",
            state = CardState.REVIEW,
            stability = 5.0,
            difficulty = 5.0,
            scheduledDays = 5,
            lapses = 3 // 已经遗忘 3 次
        )
        assertFalse("3 lapses should not trigger Leech", card.isLeech)

        // 第 4 次评分 AGAIN
        val result = engine.evaluateReview(card, Rating.AGAIN)
        assertTrue("4 lapses must trigger isLeech = true", result.updatedCard.isLeech)
        assertEquals(4, result.updatedCard.lapses)
    }

    @Test
    fun test02_fsrsDiscreteFuzzingFactor() {
        val engine = FSRSEngine()
        val baseInterval = 100 // 100 天间隔
        val fuzzedValues = mutableSetOf<Int>()

        for (i in 1..20) {
            val fuzzed = engine.applyFuzz(baseInterval, "card_fuzz_$i")
            fuzzedValues.add(fuzzed)
            // 验证扰动在 +-5% 范围 (95..105)
            assertTrue("Fuzzed interval $fuzzed must be within 95..105", fuzzed in 95..105)
        }
        // 多张不同卡片哈希应产生离散化效果，防止同天集中爆发雪崩
        assertTrue("Different cards should produce varying fuzzed intervals", fuzzedValues.size > 1)

        // 小间隔 (< 3天) 不扰动
        assertEquals(1, engine.applyFuzz(1, "card_small_1"))
        assertEquals(2, engine.applyFuzz(2, "card_small_2"))
    }

    @Test
    fun test03_recitationDiffEngineLCS() {
        // Case 1: Perfect recitation
        val expected = "不宜妄自菲薄，引喻失义"
        val spokenPerfect = "不宜妄自菲薄，引喻失义"
        val resPerfect = RecitationDiffEngine.evaluate(spokenPerfect, expected)
        assertTrue(resPerfect.isPerfect)
        assertEquals(100f, resPerfect.accuracy, 0.01f)
        assertEquals(0, resPerfect.missingCount)
        assertEquals(0, resPerfect.extraCount)

        // Case 2: Missing characters (漏字)
        val spokenMissing = "不宜妄自，引喻失义"
        val resMissing = RecitationDiffEngine.evaluate(spokenMissing, expected)
        assertFalse(resMissing.isPerfect)
        assertTrue("Missing count should be 2 for '菲薄'", resMissing.missingCount >= 2)

        // Case 3: Extra characters (多字)
        val spokenExtra = "不宜妄自菲薄呀，引喻失义"
        val resExtra = RecitationDiffEngine.evaluate(spokenExtra, expected)
        assertEquals(1, resExtra.extraCount)
    }

    @Test
    fun test04_snowballChainingProgression() {
        val article = CurriculumDataSource.ARTICLE_MAP["art_bx1_14"]
        assertNotNull("ChiBiFu should exist", article)

        val expectedUnitCount = CurriculumDataSource.generateFlashcardsForArticle(article!!).distinctBy { it.unitIndex }.size
        val stages = SnowballChainingEngine.buildStages(article)
        assertEquals("Total stages should equal units count", expectedUnitCount, stages.size)

        // Stage 0: 只有 Unit 0
        assertEquals(1, stages[0].chainUnits.size)
        assertEquals(stages[0].newlyAddedUnit, stages[0].chainUnits[0])

        // Stage 1: 累加到 Unit 0 + Unit 1
        assertEquals(2, stages[1].chainUnits.size)

        // Final Stage: 贯通全篇
        val lastStage = stages.last()
        assertEquals(expectedUnitCount, lastStage.chainUnits.size)
        assertEquals(1.0f, lastStage.progressRatio, 0.001f)
    }

    @Test
    fun test05_gaoKaoScenarioQuestionsValidation() {
        val questions = GaoKaoScenarioDataSource.QUESTIONS
        assertTrue("Should have curated GaoKao questions", questions.size >= 10)

        for (q in questions) {
            assertTrue("Prompt must not be blank", q.prompt.isNotBlank())
            assertTrue("Answer must not be blank", q.answer.isNotBlank())
            assertTrue("Key points must not be empty", q.keyPoints.isNotEmpty())
            assertNotNull("Referenced article must exist", CurriculumDataSource.ARTICLE_MAP[q.articleId])
        }
    }

    @Test
    fun test06_certificateAndCopybookGeneration() {
        val article = CurriculumDataSource.ARTICLE_MAP["art_bx1_14"]!!

        val cert = CertificateAndCopybookGenerator.generateCertificate(article, 100f)
        assertEquals("赤壁赋", cert.articleTitle)
        assertEquals("苏轼", cert.author)
        assertEquals(100f, cert.masteryPercentage, 0.01f)
        assertEquals("圆融贯通", cert.sealText)

        val copybook = CertificateAndCopybookGenerator.generateCopybook(article)
        assertEquals("赤壁赋", copybook.articleTitle)
        assertTrue(copybook.lines.isNotEmpty())
    }

    @Test
    fun test07_webDavBackupAndRestoreCycle() = runBlocking {
        val repo = WenYanRepository(context = null)
        val article = CurriculumDataSource.ARTICLE_MAP["art_bx1_14"]!!
        val cards = CurriculumDataSource.generateFlashcardsForArticle(article)
        // Submit ratings to create data
        repo.submitRating(cards[0].id, Rating.GOOD)
        repo.submitRating(cards[1].id, Rating.EASY)

        val backupJson = WebDavBackupManager.createBackupJson(repo)
        assertTrue("Backup json should contain version", backupJson.contains("\"version\": 1"))
        assertTrue("Backup json should contain cards", backupJson.contains("\"cards\":"))
        assertTrue("Backup json should contain reviewLogs", backupJson.contains("\"reviewLogs\":"))

        // Create new clean repository and restore
        val newRepo = WenYanRepository(context = null)
        val restoredCount = WebDavBackupManager.restoreFromJson(backupJson, newRepo)
        assertTrue("Should restore at least 2 cards", restoredCount >= 2)
        assertEquals(CardState.LEARNING, newRepo.getCardState(cards[0].id).state)
        assertEquals(CardState.REVIEW, newRepo.getCardState(cards[1].id).state)
    }

    @Test
    fun test08_nelderMeadOptimizationWithMutex() = runBlocking {
        val strugglingLogs = mutableListOf<ReviewLog>()
        for (i in 1..20) {
            strugglingLogs.add(
                ReviewLog(
                    cardId = "log_$i",
                    rating = if (i % 3 == 0) Rating.AGAIN else Rating.HARD,
                    previousState = CardState.LEARNING,
                    currentState = CardState.REVIEW,
                    stability = 1.2,
                    difficulty = 6.5,
                    elapsedDays = 1,
                    scheduledDays = 2,
                    reviewTime = System.currentTimeMillis()
                )
            )
        }

        val result = FSRSOptimizer.optimizeWithLock(strugglingLogs)
        assertTrue(result.success)
        assertEquals(20, result.sampleCount)
        assertEquals(19, result.optimizedWeights.size)
        // 验证调优后的参数依然保持生理单调性 w0 <= w1 <= w2 <= w3
        val w = result.optimizedWeights
        assertTrue("Monotonicity w0 <= w1", w[0] <= w[1] + 0.001)
        assertTrue("Monotonicity w1 <= w2", w[1] <= w[2] + 0.001)
        assertTrue("Monotonicity w2 <= w3", w[2] <= w[3] + 0.001)
    }
}
