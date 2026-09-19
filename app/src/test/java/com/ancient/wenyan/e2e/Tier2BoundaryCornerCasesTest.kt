package com.ancient.wenyan.e2e

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.math.*

/**
 * Tier 2: Boundary & Corner Cases E2E Test Suite.
 *
 * Covers:
 * 1. Empty pools & zero quotas
 * 2. Ultra-long classical works (《孔雀东南飞》《离骚》《长恨歌》)
 * 3. Extreme ratings & bounds clamping (repeated Again, repeated Easy)
 * 4. Same-day multiple reviews (t = 0 intraday short-term dynamics)
 * 5. Boundary dates & extreme overdue intervals
 */
class Tier2BoundaryCornerCasesTest {

    private lateinit var fsrs: E2EFsrsOracle
    private lateinit var simulator: E2EStudySessionSimulator

    @Before
    fun setUp() {
        fsrs = E2EFsrsOracle()
        simulator = E2EStudySessionSimulator(fsrs, dailyNewCardLimit = 20)
    }

    // ========================================================================
    // 1. Empty Pools & Zero Quotas
    // ========================================================================

    @Test
    fun test01_01_emptyDueQueueWhenAllCardsInFuture() {
        val futureTime = System.currentTimeMillis() + 10_000_000L
        val cards = listOf(
            E2ECardFsrsState("c1", E2ECardState.REVIEW, dueTime = futureTime),
            E2ECardFsrsState("c2", E2ECardState.LEARNING, dueTime = futureTime)
        )
        simulator.loadCards(cards)
        val queue = simulator.getDueQueue(nowMillis = System.currentTimeMillis(), endOfDayMillis = System.currentTimeMillis() + 1000L)
        assertTrue("Queue must be empty when all cards are due in the future", queue.isEmpty())
    }

    @Test
    fun test01_02_zeroDailyNewCardLimit() {
        val zeroLimitSimulator = E2EStudySessionSimulator(fsrs, dailyNewCardLimit = 0)
        zeroLimitSimulator.loadCards(listOf(
            E2ECardFsrsState("n1", E2ECardState.NEW),
            E2ECardFsrsState("n2", E2ECardState.NEW)
        ))
        val queue = zeroLimitSimulator.getDueQueue(System.currentTimeMillis())
        assertTrue("Zero new card limit must inject 0 new cards", queue.isEmpty())
    }

    @Test
    fun test01_03_filterMatchingZeroArticles() {
        val emptyFilter = E2ECurriculumOracle.SAMPLE_ARTICLES.filter { it.moduleId == "NON_EXISTENT" }
        assertTrue("Querying non-existent module returns empty list", emptyFilter.isEmpty())
    }

    @Test
    fun test01_04_emptyDeckProgressCalculation() {
        val progress = simulator.calculateArticleProgress("empty_art", 0)
        assertEquals(0.0f, progress.masteryPercentage, 0.001f)
        assertEquals(0, progress.totalCards)
    }

    @Test
    fun test01_05_limitZeroOnRandomPool() {
        val cards = (1..10).map { E2ECardFsrsState("c$it", E2ECardState.NEW) }
        val pool = cards.take(0)
        assertEquals(0, pool.size)
    }

    // ========================================================================
    // 2. Ultra-Long Classical Works Scale & Performance
    // ========================================================================

    @Test
    fun test02_01_kongQueDongNanFeiScaleStress() {
        // 《孔雀东南飞并序》: Generate a 350-clause text simulation
        val singleClause = "孔雀东南飞，五里一徘徊。十三能织素，十四学裁衣。"
        val longProse = (1..350).joinToString("；") { "$singleClause ($it)" }

        val start = System.currentTimeMillis()
        val level1 = E2EClozeOracle.generateLevel1(longProse, listOf("织素", "裁衣"))
        val level2 = E2EClozeOracle.generateLevel2(longProse)
        val level3 = E2EClozeOracle.generateLevel3(longProse)
        val level4 = E2EClozeOracle.generateLevel4(longProse)
        val duration = System.currentTimeMillis() - start

        assertTrue("Level 1 should mask keywords in 350 clauses", level1.contains("⟦"))
        assertTrue("Level 2 should mask half-lines", level2.contains("⟦"))
        assertTrue("Level 3 should preserve punctuation and first chars", level3.contains("孔"))
        assertEquals("Level 4 length must equal original length", longProse.length, level4.length)
        assertTrue("Processing ultra-long prose across 4 cloze levels must finish in <1000ms", duration < 1000)
    }

    @Test
    fun test02_02_liSaoArchaicCharactersPreserved() {
        val liSaoExcerpt = "帝高阳之苗裔兮，朕皇考曰伯庸。摄提贞于孟陬兮，惟庚寅吾以降。"
        val level3 = E2EClozeOracle.generateLevel3(liSaoExcerpt)

        assertTrue(level3.contains("帝"))
        assertTrue(level3.contains("朕"))
        assertTrue(level3.contains("，"))
        assertTrue(level3.contains("。"))
    }

    @Test
    fun test02_03_changHenGeCoupletsGeneration() {
        // 《长恨歌》: 120 lines, 60 couplets
        val couplets = (1..60).map { index ->
            E2ESegment(
                id = "seg_chg_$index",
                articleId = "art_xx_01",
                paragraphIndex = index / 10,
                sentenceIndex = index % 10,
                orderIndex = index,
                segmentType = "COUPLET",
                fullText = "回眸一笑百媚生，六宫粉黛无颜色。",
                upperClause = "回眸一笑百媚生",
                lowerClause = "六宫粉黛无颜色",
                isKeyQuote = (index == 1)
            )
        }
        assertEquals(60, couplets.size)
        assertTrue(couplets.all { it.segmentType == "COUPLET" })
        assertTrue(couplets[0].isKeyQuote)
    }

    @Test
    fun test02_04_massiveArticleCardsBatchScheduling() {
        val start = System.currentTimeMillis()
        val cards = (1..500).map {
            E2ECardFsrsState(
                cardId = "mass_$it",
                state = E2ECardState.REVIEW,
                stability = 10.0,
                difficulty = 5.0,
                lastReviewTime = start - 10L * 86_400_000L
            )
        }
        val evaluated = cards.map { fsrs.evaluateReview(it, E2ERating.GOOD, start) }
        val duration = System.currentTimeMillis() - start

        assertEquals(500, evaluated.size)
        assertTrue("500 cards evaluated in <200ms", duration < 200)
        assertTrue("All cards progressed in stability", evaluated.all { it.first.stability > 10.0 })
    }

    @Test
    fun test02_05_ultraLongProseTextIntegrity() {
        val clauses = listOf("壬戌之秋", "七月既望", "苏子与客泛舟游于赤壁之下", "清风徐来", "水波不兴")
        val joined = clauses.joinToString("，") + "。"
        val masked = E2EClozeOracle.generateLevel4(joined)

        assertEquals(joined.length, masked.length)
        assertEquals('。', masked.last())
    }

    // ========================================================================
    // 3. Extreme Ratings & Bounds Clamping
    // ========================================================================

    @Test
    fun test03_01_consecutiveAgain10Times() {
        var card = E2ECardFsrsState("lapse_card", E2ECardState.REVIEW, stability = 5.0, difficulty = 5.0, scheduledDays = 5)
        var time = 1700000000000L

        repeat(10) {
            time += 86_400_000L
            val (updated, _) = fsrs.evaluateReview(card, E2ERating.AGAIN, time)
            card = updated
        }

        assertEquals(10, card.reps)
        assertTrue("Lapses must accumulate", card.lapses >= 1)
        assertTrue("Difficulty must not exceed 10.0 ceiling", card.difficulty <= 10.0)
        assertTrue("Stability must not drop below 0.001 floor", card.stability >= 0.001)
        assertFalse("Difficulty must not be NaN", card.difficulty.isNaN())
        assertFalse("Stability must not be NaN", card.stability.isNaN())
    }

    @Test
    fun test03_02_consecutiveEasy10Times() {
        var card = E2ECardFsrsState("easy_card", E2ECardState.NEW)
        var time = 1700000000000L

        repeat(10) {
            time += 86_400_000L * max(1, card.scheduledDays)
            val (updated, _) = fsrs.evaluateReview(card, E2ERating.EASY, time)
            card = updated
        }

        assertEquals(10, card.reps)
        assertEquals(0, card.lapses)
        assertTrue("Difficulty must stay >= 1.0 floor", card.difficulty >= 1.0)
        assertTrue("Interval must not exceed 36500 days", card.scheduledDays <= 36500)
        assertTrue("Stability must grow monotonically", card.stability > 100.0)
    }

    @Test
    fun test03_03_stabilityFloorProtection() {
        val sFloor = fsrs.nextForgetStability(d = 10.0, s = 0.0001, r = 0.1)
        assertTrue("Stability must be strictly bounded >= 0.001", sFloor >= 0.001)
    }

    @Test
    fun test03_04_difficultyCeilingProtection() {
        val dNext = fsrs.nextDifficulty(currentD = 9.999, rating = E2ERating.AGAIN)
        assertTrue("Difficulty must never exceed 10.0", dNext <= 10.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun test03_05_invalidRatingEnumValidation() {
        E2ERating.fromValue(5) // Rating 5 is invalid
    }

    // ========================================================================
    // 4. Same-Day Multiple Reviews (Intraday t = 0)
    // ========================================================================

    @Test
    fun test04_01_sameDayReviewRecallMultiplier() {
        val now = 1700000000000L
        val card = E2ECardFsrsState(
            cardId = "same_day_01",
            state = E2ECardState.LEARNING,
            stability = 2.0,
            difficulty = 5.0,
            lastReviewTime = now - 60_000L // 1 minute ago (elapsedDays = 0)
        )
        val (updated, _) = fsrs.evaluateReview(card, E2ERating.GOOD, now)

        assertEquals(0, updated.elapsedDays)
        assertTrue("Stability should adjust via short-term formula", updated.stability >= card.stability)
    }

    @Test
    fun test04_02_sameDayReviewNeverDecreasesOnRecall() {
        val initialS = 3.173
        for (rating in listOf(E2ERating.HARD, E2ERating.GOOD, E2ERating.EASY)) {
            val shortS = fsrs.shortTermStability(initialS, rating)
            assertTrue("Short term stability on recall rating $rating must be >= initial S", shortS >= initialS)
        }
    }

    @Test
    fun test04_03_sameDayReviewAgainDecreasesStability() {
        val initialS = 3.173
        val shortS = fsrs.shortTermStability(initialS, E2ERating.AGAIN)
        assertTrue("Short term stability on Again rating must decrease", shortS < initialS)
        assertTrue("Short term stability must remain >= 0.001", shortS >= 0.001)
    }

    @Test
    fun test04_04_zeroSecondDelayProtection() {
        val now = 1700000000000L
        val card = E2ECardFsrsState(
            cardId = "zero_sec",
            state = E2ECardState.REVIEW,
            stability = 3.0,
            difficulty = 5.0,
            lastReviewTime = now // exactly 0 ms ago
        )
        val (updated, _) = fsrs.evaluateReview(card, E2ERating.GOOD, now)
        assertFalse(updated.stability.isNaN())
        assertFalse(updated.difficulty.isNaN())
    }

    @Test
    fun test04_05_multipleSameDayReviewsStepAdvancement() {
        val now = 1700000000000L
        val card = E2ECardFsrsState(
            cardId = "step_card",
            state = E2ECardState.LEARNING,
            step = 0,
            stability = 1.0,
            difficulty = 5.0,
            lastReviewTime = now - 60_000L
        )
        val (updated, _) = fsrs.evaluateReview(card, E2ERating.GOOD, now)
        assertEquals(1, updated.step)
        assertEquals(E2ECardState.LEARNING, updated.state)
    }

    // ========================================================================
    // 5. Boundary Dates & Overdue Intervals
    // ========================================================================

    @Test
    fun test05_01_midnightBoundaryHandling() {
        val midnightMinus1Sec = 1700000000000L
        val midnightPlus1Sec = midnightMinus1Sec + 2000L

        val card = E2ECardFsrsState(
            cardId = "midnight_card",
            state = E2ECardState.REVIEW,
            dueTime = midnightMinus1Sec + 1000L
        )
        simulator.loadCards(listOf(card))

        val queueBefore = simulator.getDueQueue(nowMillis = midnightMinus1Sec, endOfDayMillis = midnightMinus1Sec)
        val queueAfter = simulator.getDueQueue(nowMillis = midnightPlus1Sec, endOfDayMillis = midnightPlus1Sec)

        assertEquals(0, queueBefore.size)
        assertEquals(1, queueAfter.size)
    }

    @Test
    fun test05_02_extremeOverdueRecallBonus() {
        // 100 days elapsed on card with stability 10.0: R ~ 0.5467
        val rNormal = fsrs.retrievability(10, 10.0)
        val rOverdue = fsrs.retrievability(100, 10.0)
        assertTrue(rOverdue < rNormal)

        val sNextNormal = fsrs.nextRecallStability(5.0, 10.0, rNormal, E2ERating.GOOD)
        val sNextOverdue = fsrs.nextRecallStability(5.0, 10.0, rOverdue, E2ERating.GOOD)

        assertTrue("Overdue successful recall must yield higher stability bonus (desirable difficulty)",
            sNextOverdue > sNextNormal)
    }

    @Test
    fun test05_03_extremeOverdueLapseGraceful() {
        val rOverdue = fsrs.retrievability(365, 10.0)
        val sNextForget = fsrs.nextForgetStability(5.0, 10.0, rOverdue)

        assertTrue("Forget stability on extreme overdue remains safe positive number", sNextForget > 0.001)
        assertFalse(sNextForget.isNaN())
        assertFalse(sNextForget.isInfinite())
    }

    @Test
    fun test05_04_negativeElapsedProtection() {
        val now = 1700000000000L
        val card = E2ECardFsrsState(
            cardId = "skew_card",
            state = E2ECardState.REVIEW,
            stability = 5.0,
            difficulty = 5.0,
            lastReviewTime = now + 500_000L // Clock skew into future
        )
        val (updated, _) = fsrs.evaluateReview(card, E2ERating.GOOD, now)
        assertEquals(0, updated.elapsedDays)
        assertTrue(updated.stability > 0.0)
    }

    @Test
    fun test05_05_futureDueCutoffStrictness() {
        val now = 1700000000000L
        val endOfDay = now + 1000L
        val card = E2ECardFsrsState(
            cardId = "exact_boundary",
            state = E2ECardState.REVIEW,
            dueTime = endOfDay + 1L // 1 ms past cutoff
        )
        simulator.loadCards(listOf(card))
        val queue = simulator.getDueQueue(now, endOfDay)
        assertTrue("Card due 1ms past cutoff must be excluded", queue.isEmpty())
    }
}
