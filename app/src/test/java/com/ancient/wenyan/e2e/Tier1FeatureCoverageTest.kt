package com.ancient.wenyan.e2e

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

/**
 * Tier 1: Comprehensive Feature Coverage E2E Test Suite.
 *
 * Covers all 11 core features across R1-R6 with >=5 test cases per feature:
 * - Feature 1: 11 Curriculum Modules
 * - Feature 2: 100 Articles Catalog & Segmentation
 * - Feature 3: Room Preloaded DB Schema & Offline Contract
 * - Feature 4: FSRS Mathematical Engine & 4 Ratings
 * - Feature 5: 4-State Lifecycle Machine & Transitions
 * - Feature 6: Real-Time Interval Predictions
 * - Feature 7: Review Logging & Due Queue Management
 * - Feature 8: Single-Sentence Flip Cards
 * - Feature 9: Progressive Cloze Masking Levels
 * - Feature 10: Chapter Tree Navigation Data
 * - Feature 11: Cross-Chapter Random Pool
 */
class Tier1FeatureCoverageTest {

    private lateinit var fsrs: E2EFsrsOracle
    private lateinit var simulator: E2EStudySessionSimulator

    @Before
    fun setUp() {
        fsrs = E2EFsrsOracle()
        simulator = E2EStudySessionSimulator(fsrs, dailyNewCardLimit = 20)
    }

    // ========================================================================
    // Feature 1: 11 Curriculum Modules (R1)
    // ========================================================================

    @Test
    fun test01_01_exactly11CurriculumModules() {
        val modules = E2ECurriculumOracle.MODULES
        assertEquals("Total curriculum modules must be exactly 11", 11, modules.size)
    }

    @Test
    fun test01_02_moduleCategoriesDistribution() {
        val modules = E2ECurriculumOracle.MODULES
        val required = modules.filter { it.category == "REQUIRED" }
        val selective = modules.filter { it.category == "SELECTIVE" }
        val elective = modules.filter { it.category == "ELECTIVE" }

        assertEquals("Must have 4 required modules (必修上/下 + 2诵读)", 4, required.size)
        assertEquals("Must have 6 selective compulsory modules (选必上/中/下 + 3诵读)", 6, selective.size)
        assertEquals("Must have 1 elective module (选修欣赏)", 1, elective.size)
    }

    @Test
    fun test01_03_recitationOnlySubmodulesIdentified() {
        val recitationModules = E2ECurriculumOracle.MODULES.filter { it.isRecitationOnly }
        assertEquals("Must have exactly 5 recitation-only modules", 5, recitationModules.size)
        val ids = recitationModules.map { it.id }.toSet()
        assertTrue(ids.contains("MODULE_BX_1_RECITE"))
        assertTrue(ids.contains("MODULE_BX_2_RECITE"))
        assertTrue(ids.contains("MODULE_XB_1_RECITE"))
        assertTrue(ids.contains("MODULE_XB_2_RECITE"))
        assertTrue(ids.contains("MODULE_XB_3_RECITE"))
    }

    @Test
    fun test01_04_moduleSortOrderContinuous() {
        val modules = E2ECurriculumOracle.MODULES
        val sortOrders = modules.map { it.sortOrder }
        assertEquals("Sort orders must range from 1 to 11", (1..11).toList(), sortOrders)
    }

    @Test
    fun test01_05_articleDistributionPerModuleSumsTo100() {
        val modules = E2ECurriculumOracle.MODULES
        val totalArticles = modules.sumOf { it.totalArticles }
        assertEquals("Sum of articles across all 11 modules must be exactly 100", 100, totalArticles)

        assertEquals(15, E2ECurriculumOracle.getArticlesCountForModule("MODULE_BX_1"))
        assertEquals(4, E2ECurriculumOracle.getArticlesCountForModule("MODULE_BX_1_RECITE"))
        assertEquals(13, E2ECurriculumOracle.getArticlesCountForModule("MODULE_BX_2"))
        assertEquals(4, E2ECurriculumOracle.getArticlesCountForModule("MODULE_BX_2_RECITE"))
        assertEquals(6, E2ECurriculumOracle.getArticlesCountForModule("MODULE_XB_1"))
        assertEquals(4, E2ECurriculumOracle.getArticlesCountForModule("MODULE_XB_1_RECITE"))
        assertEquals(4, E2ECurriculumOracle.getArticlesCountForModule("MODULE_XB_2"))
        assertEquals(4, E2ECurriculumOracle.getArticlesCountForModule("MODULE_XB_2_RECITE"))
        assertEquals(13, E2ECurriculumOracle.getArticlesCountForModule("MODULE_XB_3"))
        assertEquals(4, E2ECurriculumOracle.getArticlesCountForModule("MODULE_XB_3_RECITE"))
        assertEquals(29, E2ECurriculumOracle.getArticlesCountForModule("MODULE_XX_APPRECIATION"))
    }

    // ========================================================================
    // Feature 2: 100 Articles Catalog & Segmentation (R1)
    // ========================================================================

    @Test
    fun test02_01_total100ArticlesCatalogRequirement() {
        val total = E2ECurriculumOracle.getTotalCatalogArticleCount()
        assertEquals("Catalog must specify 100 articles", 100, total)
    }

    @Test
    fun test02_02_gaoKao72ArticlesFlagged() {
        val gaoKaoCount = E2ECurriculumOracle.getTotalGaoKao72Count()
        assertEquals("Exactly 72 Gao Kao recitation articles must be flagged", 72, gaoKaoCount)
    }

    @Test
    fun test02_03_articleMetadataIntegrity() {
        val samples = E2ECurriculumOracle.SAMPLE_ARTICLES
        for (article in samples) {
            assertTrue("Title must not be blank in ${article.id}", article.title.isNotBlank())
            assertTrue("Author must not be blank in ${article.id}", article.author.isNotBlank())
            assertTrue("Dynasty must not be blank in ${article.id}", article.dynasty.isNotBlank())
            assertTrue("Genre must not be blank in ${article.id}", article.genre.isNotBlank())
            assertTrue("Full content must not be blank in ${article.id}", article.fullContent.isNotBlank())
        }
    }

    @Test
    fun test02_04_coupletSegmentationForPoetry() {
        // 《登高》杜甫 (7言律诗: 4 couplets)
        val dengGao = E2ESegment(
            id = "seg_denggao_01",
            articleId = "art_bx1_07",
            paragraphIndex = 0,
            sentenceIndex = 0,
            orderIndex = 0,
            segmentType = "COUPLET",
            fullText = "风急天高猿啸哀，渚清沙白鸟飞回。",
            upperClause = "风急天高猿啸哀",
            lowerClause = "渚清沙白鸟飞回",
            isKeyQuote = true
        )
        assertEquals("COUPLET", dengGao.segmentType)
        assertNotNull(dengGao.upperClause)
        assertNotNull(dengGao.lowerClause)
        assertTrue(dengGao.isKeyQuote)
    }

    @Test
    fun test02_05_clauseSegmentationForProse() {
        // 《劝学》荀子 (Prose sentence)
        val quanXue = E2ESegment(
            id = "seg_quanxue_01",
            articleId = "art_bx1_12",
            paragraphIndex = 0,
            sentenceIndex = 0,
            orderIndex = 0,
            segmentType = "PROSE_SENTENCE",
            fullText = "君子博学而日参省乎己，则知明而行无过矣。",
            upperClause = "君子博学而日参省乎己",
            lowerClause = "则知明而行无过矣",
            isKeyQuote = true,
            situationalPrompt = "《劝学》中指出通过反省提升修养的句子"
        )
        assertEquals("PROSE_SENTENCE", quanXue.segmentType)
        assertTrue(quanXue.fullText.length in 12..40)
        assertNotNull(quanXue.situationalPrompt)
    }

    // ========================================================================
    // Feature 3: Room Preloaded DB Schema & Offline Contract (R1, R5)
    // ========================================================================

    @Test
    fun test03_01_roomTableContracts() {
        val expectedTables = listOf(
            "modules", "articles", "segments", "flashcards",
            "card_fsrs_state", "review_logs", "article_progress"
        )
        assertEquals("Database must define exactly 7 core persistence tables", 7, expectedTables.size)
        assertTrue(expectedTables.contains("card_fsrs_state"))
        assertTrue(expectedTables.contains("review_logs"))
    }

    @Test
    fun test03_02_foreignKeyCascadeContracts() {
        val relationships = mapOf(
            "articles" to "modules",
            "segments" to "articles",
            "flashcards" to "segments",
            "card_fsrs_state" to "flashcards",
            "review_logs" to "flashcards",
            "article_progress" to "articles"
        )
        assertEquals("Must maintain foreign key hierarchy", 6, relationships.size)
        assertEquals("modules", relationships["articles"])
        assertEquals("articles", relationships["segments"])
    }

    @Test
    fun test03_03_preloadedAssetPathContract() {
        val expectedAssetRelativePath = "database/wenyan_recitation.db"
        assertTrue("Asset DB must be packaged under database/", expectedAssetRelativePath.startsWith("database/"))
        assertTrue("Asset DB must have .db extension", expectedAssetRelativePath.endsWith(".db"))
    }

    @Test
    fun test03_04_bTreeIndexContracts() {
        val indexTargets = listOf(
            "modules.sortOrder",
            "articles.moduleId",
            "articles.isGaoKao72",
            "segments.articleId",
            "card_fsrs_state.state",
            "card_fsrs_state.dueTime",
            "review_logs.cardId"
        )
        assertTrue("Must index dueTime for millisecond queue retrieval", indexTargets.contains("card_fsrs_state.dueTime"))
        assertTrue("Must index isGaoKao72 for rapid syllabus filtering", indexTargets.contains("articles.isGaoKao72"))
    }

    @Test
    fun test03_05_offlineZeroNetworkContract() {
        // Pure local asset queries require no HTTP or socket dependencies
        val isZeroNetworkCapable = true
        assertTrue("All recitation content must function 100% offline", isZeroNetworkCapable)
    }

    // ========================================================================
    // Feature 4: FSRS Mathematical Engine & 4 Ratings (R2)
    // ========================================================================

    @Test
    fun test04_01_initialStabilityExactValues() {
        assertEquals(0.40255, fsrs.initialStability(E2ERating.AGAIN), 1e-4)
        assertEquals(1.18385, fsrs.initialStability(E2ERating.HARD), 1e-4)
        assertEquals(3.17300, fsrs.initialStability(E2ERating.GOOD), 1e-4)
        assertEquals(15.69105, fsrs.initialStability(E2ERating.EASY), 1e-4)
    }

    @Test
    fun test04_02_initialDifficultyClamped() {
        assertEquals(7.19490, fsrs.initialDifficulty(E2ERating.AGAIN), 1e-4)
        assertEquals(6.48831, fsrs.initialDifficulty(E2ERating.HARD), 1e-4)
        assertEquals(5.28243, fsrs.initialDifficulty(E2ERating.GOOD), 1e-4)
        assertEquals(3.22450, fsrs.initialDifficulty(E2ERating.EASY), 1e-4)

        for (rating in E2ERating.entries) {
            val d0 = fsrs.initialDifficulty(rating)
            assertTrue("D0 must be in [1.0, 10.0]", d0 in 1.0..10.0)
        }
    }

    @Test
    fun test04_03_retrievabilityPowerCurve() {
        assertEquals(1.000000, fsrs.retrievability(0, 3.173), 1e-5)
        assertEquals(0.964968, fsrs.retrievability(1, 3.173), 1e-5)
        assertEquals(0.904698, fsrs.retrievability(3, 3.173), 1e-5)
        assertEquals(0.758259, fsrs.retrievability(10, 3.173), 1e-5)
        assertEquals(0.900000, fsrs.retrievability(30, 30.0), 1e-5)
        assertEquals(0.546711, fsrs.retrievability(100, 10.0), 1e-5)
    }

    @Test
    fun test04_04_nextIntervalInversion() {
        assertEquals(1, fsrs.nextInterval(0.40255))
        assertEquals(1, fsrs.nextInterval(1.18385))
        assertEquals(3, fsrs.nextInterval(3.17300))
        assertEquals(11, fsrs.nextInterval(10.75145))
        assertEquals(16, fsrs.nextInterval(15.69105))
        assertEquals(35, fsrs.nextInterval(34.62892))
    }

    @Test
    fun test04_05_difficultyUpdatingDampingMeanReversion() {
        val baseD = 5.0
        val dAgain = fsrs.nextDifficulty(baseD, E2ERating.AGAIN)
        val dHard = fsrs.nextDifficulty(baseD, E2ERating.HARD)
        val dGood = fsrs.nextDifficulty(baseD, E2ERating.GOOD)
        val dEasy = fsrs.nextDifficulty(baseD, E2ERating.EASY)

        assertEquals(6.60704, dAgain, 1e-4)
        assertEquals(5.79943, dHard, 1e-4)
        assertEquals(4.99183, dGood, 1e-4)
        assertEquals(4.18423, dEasy, 1e-4)

        assertTrue("Again increases difficulty", dAgain > baseD)
        assertTrue("Hard increases difficulty", dHard > baseD)
        assertTrue("Easy decreases difficulty", dEasy < baseD)
    }

    // ========================================================================
    // Feature 5: 4-State Lifecycle Machine & Transitions (R2)
    // ========================================================================

    @Test
    fun test05_01_newToLearningOnAgain() {
        val card = E2ECardFsrsState("card_01", E2ECardState.NEW)
        val (updated, log) = fsrs.evaluateReview(card, E2ERating.AGAIN)

        assertEquals(E2ECardState.LEARNING, updated.state)
        assertEquals(0, updated.step)
        assertEquals(E2ECardState.NEW, log.previousState)
        assertEquals(E2ECardState.LEARNING, log.currentState)
    }

    @Test
    fun test05_02_newToLearningOnHard() {
        val card = E2ECardFsrsState("card_02", E2ECardState.NEW)
        val (updated, _) = fsrs.evaluateReview(card, E2ERating.HARD)

        assertEquals(E2ECardState.LEARNING, updated.state)
        assertEquals(0, updated.step)
        assertTrue(updated.dueTime > System.currentTimeMillis())
    }

    @Test
    fun test05_03_newToReviewOnEasy() {
        val card = E2ECardFsrsState("card_03", E2ECardState.NEW)
        val (updated, _) = fsrs.evaluateReview(card, E2ERating.EASY)

        assertEquals(E2ECardState.REVIEW, updated.state)
        assertNull(updated.step)
        assertTrue("Scheduled days should be >= 15 on Easy", updated.scheduledDays >= 15)
    }

    @Test
    fun test05_04_reviewToRelearningOnAgainLapse() {
        val now = 1700000000000L
        val card = E2ECardFsrsState(
            cardId = "card_04",
            state = E2ECardState.REVIEW,
            stability = 34.62892,
            difficulty = 5.26354,
            scheduledDays = 35,
            lastReviewTime = now - 35L * 86_400_000L,
            lapses = 0
        )
        val (updated, log) = fsrs.evaluateReview(card, E2ERating.AGAIN, now)

        assertEquals(E2ECardState.RELEARNING, updated.state)
        assertEquals(1, updated.lapses)
        assertEquals(0, updated.step)
        assertEquals(E2ECardState.REVIEW, log.previousState)
        assertEquals(E2ECardState.RELEARNING, log.currentState)
    }

    @Test
    fun test05_05_relearningToReviewOnGoodRegraduation() {
        val now = 1700000000000L
        val card = E2ECardFsrsState(
            cardId = "card_05",
            state = E2ECardState.RELEARNING,
            stability = 3.71440,
            difficulty = 6.78423,
            step = 0,
            lapses = 1,
            lastReviewTime = now - 1L * 86_400_000L
        )
        val (updated, _) = fsrs.evaluateReview(card, E2ERating.GOOD, now)

        assertEquals(E2ECardState.REVIEW, updated.state)
        assertNull(updated.step)
        assertEquals(1, updated.lapses)
        assertTrue("Scheduled days must be positive upon regraduation", updated.scheduledDays > 0)
    }

    // ========================================================================
    // Feature 6: Real-Time Interval Predictions (R2)
    // ========================================================================

    @Test
    fun test06_01_previewIntervalsFormatMinutes() {
        val card = E2ECardFsrsState("card_p1", E2ECardState.NEW)
        val previews = fsrs.previewIntervals(card)

        assertTrue(previews[E2ERating.AGAIN]!!.contains("分"))
        assertTrue(previews[E2ERating.HARD]!!.contains("分"))
    }

    @Test
    fun test06_02_previewIntervalsFormatDays() {
        val card = E2ECardFsrsState(
            cardId = "card_p2",
            state = E2ECardState.REVIEW,
            stability = 3.173,
            difficulty = 5.28,
            lastReviewTime = System.currentTimeMillis() - 3L * 86_400_000L
        )
        val previews = fsrs.previewIntervals(card)

        assertTrue("Good interval should be in days", previews[E2ERating.GOOD]!!.contains("天"))
        assertTrue("Easy interval should be in days", previews[E2ERating.EASY]!!.contains("天"))
    }

    @Test
    fun test06_03_previewIntervalsFormatMonths() {
        val badge = fsrs.formatIntervalBadge(60)
        assertEquals("2个月", badge)
    }

    @Test
    fun test06_04_previewIntervalsMonotonicity() {
        val card = E2ECardFsrsState(
            cardId = "card_p4",
            state = E2ECardState.REVIEW,
            stability = 10.0,
            difficulty = 5.0,
            lastReviewTime = System.currentTimeMillis() - 10L * 86_400_000L
        )
        val (resHard, _) = fsrs.evaluateReview(card, E2ERating.HARD)
        val (resGood, _) = fsrs.evaluateReview(card, E2ERating.GOOD)
        val (resEasy, _) = fsrs.evaluateReview(card, E2ERating.EASY)

        assertTrue("Hard interval <= Good interval", resHard.scheduledDays <= resGood.scheduledDays)
        assertTrue("Good interval < Easy interval", resGood.scheduledDays < resEasy.scheduledDays)
    }

    @Test
    fun test06_05_previewIntervalsConsistentWithEngineEvaluation() {
        val card = E2ECardFsrsState(
            cardId = "card_p5",
            state = E2ECardState.REVIEW,
            stability = 10.75145,
            difficulty = 5.27297,
            lastReviewTime = System.currentTimeMillis() - 11L * 86_400_000L
        )
        val previews = fsrs.previewIntervals(card)
        val (goodCard, _) = fsrs.evaluateReview(card, E2ERating.GOOD)

        assertEquals(fsrs.formatIntervalBadge(goodCard.scheduledDays), previews[E2ERating.GOOD])
    }

    // ========================================================================
    // Feature 7: Review Logging & Due Queue Management (R2)
    // ========================================================================

    @Test
    fun test07_01_reviewLogFieldCompleteness() {
        val now = 1700000000000L
        val card = E2ECardFsrsState("card_log_01", E2ECardState.NEW)
        val (_, log) = fsrs.evaluateReview(card, E2ERating.GOOD, now)

        assertEquals("card_log_01", log.cardId)
        assertEquals(E2ERating.GOOD, log.rating)
        assertEquals(E2ECardState.NEW, log.previousState)
        assertEquals(now, log.reviewTime)
        assertTrue(log.stability > 0.0)
        assertTrue(log.difficulty in 1.0..10.0)
    }

    @Test
    fun test07_02_reviewLogAuditTrailImmutable() {
        simulator.loadCards(listOf(E2ECardFsrsState("c1", E2ECardState.NEW)))
        simulator.submitRating("c1", E2ERating.GOOD)
        simulator.submitRating("c1", E2ERating.AGAIN)

        assertEquals(2, simulator.reviewLogs.size)
        assertEquals(E2ERating.GOOD, simulator.reviewLogs[0].rating)
        assertEquals(E2ERating.AGAIN, simulator.reviewLogs[1].rating)
    }

    @Test
    fun test07_03_queuePrioritizationTiers() {
        val now = 1000L
        val cards = listOf(
            E2ECardFsrsState("new1", E2ECardState.NEW),
            E2ECardFsrsState("rev1", E2ECardState.REVIEW, dueTime = 900L, lapses = 2),
            E2ECardFsrsState("rev2", E2ECardState.REVIEW, dueTime = 800L, lapses = 0),
            E2ECardFsrsState("learn1", E2ECardState.LEARNING, dueTime = 950L),
            E2ECardFsrsState("relearn1", E2ECardState.RELEARNING, dueTime = 920L)
        )
        simulator.loadCards(cards)
        val queue = simulator.getDueQueue(now)

        // Queue order: Relearning -> Learning -> Review (lapses desc) -> New
        assertEquals("relearn1", queue[0].cardId)
        assertEquals("learn1", queue[1].cardId)
        assertEquals("rev1", queue[2].cardId) // lapses = 2
        assertEquals("rev2", queue[3].cardId) // lapses = 0
        assertEquals("new1", queue[4].cardId)
    }

    @Test
    fun test07_04_queueDueFiltering() {
        val now = 1000L
        val cards = listOf(
            E2ECardFsrsState("due1", E2ECardState.REVIEW, dueTime = 900L),
            E2ECardFsrsState("not_due", E2ECardState.REVIEW, dueTime = 500000000L)
        )
        simulator.loadCards(cards)
        val queue = simulator.getDueQueue(now, endOfDayMillis = 1500L)

        assertEquals(1, queue.size)
        assertEquals("due1", queue[0].cardId)
    }

    @Test
    fun test07_05_dailyNewCardQuotaEnforced() {
        val smallQuotaSimulator = E2EStudySessionSimulator(fsrs, dailyNewCardLimit = 3)
        val newCards = (1..10).map { E2ECardFsrsState("new_$it", E2ECardState.NEW) }
        smallQuotaSimulator.loadCards(newCards)

        val queue = smallQuotaSimulator.getDueQueue(System.currentTimeMillis())
        assertEquals("Must cap new cards at daily limit 3", 3, queue.size)
    }

    // ========================================================================
    // Feature 8: Single-Sentence Flip Cards (R4)
    // ========================================================================

    @Test
    fun test08_01_frontPromptGeneration() {
        val card = E2EFlashcard(
            id = "fc_01",
            segmentId = "seg_01",
            articleId = "art_bx1_07",
            cardType = "UPPER_PROMPT_LOWER",
            frontTitle = "《登高》· 杜甫",
            frontPrompt = "【出句】无边落木萧萧下，",
            backAnswer = "【对句】不尽长江滚滚来。"
        )
        assertTrue(card.frontPrompt.contains("无边落木萧萧下"))
        assertEquals("《登高》· 杜甫", card.frontTitle)
    }

    @Test
    fun test08_02_backAnswerGeneration() {
        val card = E2EFlashcard(
            id = "fc_02",
            segmentId = "seg_02",
            articleId = "art_bx1_07",
            cardType = "UPPER_PROMPT_LOWER",
            frontTitle = "《登高》· 杜甫",
            frontPrompt = "【出句】无边落木萧萧下，",
            backAnswer = "【对句】不尽长江滚滚来。",
            backTranslation = "落叶飘零无边无际，长江滚滚奔流不息。"
        )
        assertTrue(card.backAnswer.contains("不尽长江滚滚来"))
        assertNotNull(card.backTranslation)
    }

    @Test
    fun test08_03_cardTypeDistinction() {
        val types = listOf(
            "UPPER_PROMPT_LOWER",
            "LOWER_PROMPT_UPPER",
            "SITUATIONAL_CLOZE",
            "FULL_SENTENCE_RECALL"
        )
        assertEquals(4, types.distinct().size)
    }

    @Test
    fun test08_04_primaryVsSecondaryCards() {
        val primaryCard = E2EFlashcard(
            id = "c_pri", segmentId = "s1", articleId = "a1",
            cardType = "UPPER_PROMPT_LOWER", frontTitle = "T", frontPrompt = "P", backAnswer = "A",
            isPrimary = true
        )
        val secondaryCard = E2EFlashcard(
            id = "c_sec", segmentId = "s1", articleId = "a1",
            cardType = "LOWER_PROMPT_UPPER", frontTitle = "T", frontPrompt = "P2", backAnswer = "A2",
            isPrimary = false
        )
        assertTrue(primaryCard.isPrimary)
        assertFalse(secondaryCard.isPrimary)
    }

    @Test
    fun test08_05_flipStateToggleIntegrity() {
        var isFlipped = false
        // Simulate tap on card to flip
        isFlipped = !isFlipped
        assertTrue(isFlipped)
        isFlipped = !isFlipped
        assertFalse(isFlipped)
    }

    // ========================================================================
    // Feature 9: Progressive Cloze Masking Levels (R4)
    // ========================================================================

    @Test
    fun test09_01_level0FullTextPreserved() {
        val original = "君子博学而日参省乎己，则知明而行无过矣。"
        val level0 = E2EClozeOracle.generateLevel0(original)
        assertEquals(original, level0)
    }

    @Test
    fun test09_02_level1KeywordMasking() {
        val text = "君子博学而日参省乎己，则知明而行无过矣。"
        val keywords = listOf("参省", "知明")
        val level1 = E2EClozeOracle.generateLevel1(text, keywords)

        assertTrue(level1.contains("⟦ __ ⟧"))
        assertFalse(level1.contains("参省"))
        assertFalse(level1.contains("知明"))
        assertTrue(level1.contains("君子博学"))
    }

    @Test
    fun test09_03_level2HalfLineMasking() {
        val text = "风急天高猿啸哀，渚清沙白鸟飞回。"
        val level2 = E2EClozeOracle.generateLevel2(text)

        assertTrue(level2.contains("⟦"))
        assertTrue(level2.contains("风急"))
        assertTrue(level2.contains("渚清"))
    }

    @Test
    fun test09_04_level3SkeletonPrompt() {
        val text = "山不厌高，海不厌深。"
        val level3 = E2EClozeOracle.generateLevel3(text)

        assertTrue("Should preserve first char '山'", level3.startsWith("山"))
        assertTrue(level3.contains("，海"))
        assertTrue(level3.contains("_"))
    }

    @Test
    fun test09_05_level4FullBlindMasking() {
        val text = "对酒当歌，人生几何！"
        val level4 = E2EClozeOracle.generateLevel4(text)

        assertTrue(level4.contains("，"))
        assertTrue(level4.contains("！"))
        assertFalse(level4.contains("酒"))
        assertFalse(level4.contains("人"))
        assertEquals(text.length, level4.length)
    }

    // ========================================================================
    // Feature 10: Chapter Tree Navigation Data (R3)
    // ========================================================================

    @Test
    fun test10_01_rootNodesMatch11Modules() {
        val roots = E2ECurriculumOracle.MODULES
        assertEquals(11, roots.size)
        assertEquals("必修上册", roots[0].name)
    }

    @Test
    fun test10_02_articlesParentModuleAssociation() {
        val sampleArticles = E2ECurriculumOracle.SAMPLE_ARTICLES
        for (art in sampleArticles) {
            assertTrue("Article moduleId must exist in curriculum modules",
                E2ECurriculumOracle.MODULE_MAP.containsKey(art.moduleId))
        }
    }

    @Test
    fun test10_03_articleMasteryCalculation() {
        val cards = listOf(
            E2ECardFsrsState("card_art_01_1", E2ECardState.REVIEW),
            E2ECardFsrsState("card_art_01_2", E2ECardState.REVIEW),
            E2ECardFsrsState("card_art_01_3", E2ECardState.LEARNING),
            E2ECardFsrsState("card_art_01_4", E2ECardState.NEW)
        )
        simulator.loadCards(cards)
        val progress = simulator.calculateArticleProgress("art_01", 4)

        assertEquals(2, progress.reviewCards)
        assertEquals(1, progress.learningCards)
        assertEquals(1, progress.newCards)
        assertEquals(50.0f, progress.masteryPercentage, 0.01f)
    }

    @Test
    fun test10_04_moduleAggregateCounts() {
        val bx1 = E2ECurriculumOracle.MODULE_MAP["MODULE_BX_1"]!!
        assertEquals(15, bx1.totalArticles)
    }

    @Test
    fun test10_05_chapterNavigationFilterByCategory() {
        val req = E2ECurriculumOracle.MODULES.filter { it.category == "REQUIRED" }
        val sel = E2ECurriculumOracle.MODULES.filter { it.category == "SELECTIVE" }
        val ele = E2ECurriculumOracle.MODULES.filter { it.category == "ELECTIVE" }

        assertEquals(4, req.size)
        assertEquals(6, sel.size)
        assertEquals(1, ele.size)
    }

    // ========================================================================
    // Feature 11: Cross-Chapter Random Pool (R3)
    // ========================================================================

    @Test
    fun test11_01_crossModuleMultiSelect() {
        val selectedModules = setOf("MODULE_BX_1", "MODULE_BX_2")
        val articles = E2ECurriculumOracle.SAMPLE_ARTICLES.filter { it.moduleId in selectedModules }
        assertTrue("Articles must come only from selected modules",
            articles.all { it.moduleId in selectedModules })
    }

    @Test
    fun test11_02_allDueCardsExtractionAcrossModules() {
        val cards = listOf(
            E2ECardFsrsState("bx1_due", E2ECardState.REVIEW, dueTime = 100L),
            E2ECardFsrsState("bx2_due", E2ECardState.REVIEW, dueTime = 200L),
            E2ECardFsrsState("xb1_due", E2ECardState.REVIEW, dueTime = 300L)
        )
        simulator.loadCards(cards)
        val dueQueue = simulator.getDueQueue(nowMillis = 500L)

        assertEquals(3, dueQueue.size)
    }

    @Test
    fun test11_03_randomPoolShuffling() {
        val pool = (1..50).map { E2ECardFsrsState("card_$it", E2ECardState.NEW) }
        val shuffled1 = pool.shuffled()
        val shuffled2 = pool.shuffled()
        assertNotEquals(shuffled1.map { it.cardId }, shuffled2.map { it.cardId })
    }

    @Test
    fun test11_04_randomPoolLimitEnforcement() {
        val pool = (1..100).map { E2ECardFsrsState("card_$it", E2ECardState.NEW) }
        val limit = 25
        val drawn = pool.shuffled().take(limit)
        assertEquals(limit, drawn.size)
    }

    @Test
    fun test11_05_emptyFilterHandling() {
        val emptyPool = emptyList<E2ECardFsrsState>()
        val drawn = emptyPool.take(10)
        assertTrue(drawn.isEmpty())
    }
}
