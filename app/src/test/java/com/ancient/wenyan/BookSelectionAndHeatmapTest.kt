package com.ancient.wenyan

import com.ancient.wenyan.data.CurriculumDataSource
import com.ancient.wenyan.data.WenYanRepository
import com.ancient.wenyan.domain.fsrs.Rating
import com.ancient.wenyan.domain.model.BookPresets
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class BookSelectionAndHeatmapTest {

    private lateinit var repository: WenYanRepository

    @Before
    fun setUp() {
        repository = WenYanRepository()
    }

    // ========================================================================
    // 1. Book Selection & Presets Tests
    // ========================================================================

    @Test
    fun test01_singleBooksModuleCoverage() {
        assertEquals("Must have 6 single books", 6, BookPresets.ALL_SINGLE_BOOKS.size)

        val totalArticles = BookPresets.ALL_SINGLE_BOOKS.sumOf { it.totalArticles }
        assertEquals("Total articles across single books must be 100", 100, totalArticles)

        val allModuleIds = BookPresets.ALL_SINGLE_BOOKS.flatMap { it.moduleIds }.toSet()
        assertEquals("Must cover all 11 curriculum modules", 11, allModuleIds.size)
    }

    @Test
    fun test02_bixiu1BookSelectionFiltering() {
        // Set scope to 必修上册
        repository.setSelectedBookScope(BookPresets.BOOK_BX_1.moduleIds, BookPresets.BOOK_BX_1.name)

        assertEquals("Selected book name must be updated", "必修上册", repository.selectedBookName.value)
        assertEquals("Selected book scope must match", BookPresets.BOOK_BX_1.moduleIds, repository.selectedBookScope.value)

        // Verify random queue only samples from 必修上册
        val randomQueue = repository.getRandomQueue(limit = 30)
        assertTrue("Random queue must not be empty", randomQueue.isNotEmpty())

        val articleMap = CurriculumDataSource.ARTICLE_MAP
        for ((card, _) in randomQueue) {
            val article = articleMap[card.articleId]
            assertNotNull("Article must exist", article)
            assertTrue("Article moduleId must be in 必修上册", article!!.moduleId in BookPresets.BOOK_BX_1.moduleIds)
        }
    }

    @Test
    fun test03_scopedDeckStatsComputation() {
        // Compute stats for all books
        val allStats = repository.computeStats(scope = null)
        assertTrue("All cards total must be > 300", allStats.totalCards > 300)

        // Compute stats for 必修上册 only
        val bx1Stats = repository.computeStats(scope = BookPresets.BOOK_BX_1.moduleIds)
        assertTrue("BX1 cards must be less than total cards", bx1Stats.totalCards < allStats.totalCards)
        assertTrue("BX1 cards must be > 0", bx1Stats.totalCards > 0)
    }

    @Test
    fun test04_scopedDueQueueFiltering() {
        // Due queue with 选修欣赏 scope
        val xxScope = BookPresets.BOOK_XX_APPRECIATION.moduleIds
        val dueQueue = repository.getDueQueue(scope = xxScope, dailyNewLimit = 15)

        val articleMap = CurriculumDataSource.ARTICLE_MAP
        for ((card, _) in dueQueue) {
            val article = articleMap[card.articleId]
            assertNotNull(article)
            assertEquals("ModuleId must be MODULE_XX_APPRECIATION", "MODULE_XX_APPRECIATION", article!!.moduleId)
        }
    }

    // ========================================================================
    // 2. Heatmap & Daily Recitation Activity Tests
    // ========================================================================

    @Test
    fun test05_initialHeatmapStatsClean() {
        val stats = repository.heatmapStatsFlow.value
        assertNotNull("Heatmap stats must not be null", stats)
        assertEquals("Initial active days must be 0 for fresh user", 0, stats.activeDays)
        assertEquals("Initial total reviews must be 0", 0, stats.totalReviews)
        assertEquals("Initial streak must be 0", 0, stats.currentStreak)
    }

    @Test
    fun test06_submitRatingIncrementsTodayCountAndHeatmap() {
        val beforeStats = repository.heatmapStatsFlow.value
        val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val beforeTodayCount = beforeStats.dailyReviewMap[todayStr] ?: 0

        // Submit a rating for a card
        val firstCard = repository.getDueQueue(dailyNewLimit = 1).first().first
        repository.submitRating(firstCard.id, Rating.GOOD)

        val afterStats = repository.heatmapStatsFlow.value
        val afterTodayCount = afterStats.dailyReviewMap[todayStr] ?: 0

        assertEquals("Today's review count must increment by 1", beforeTodayCount + 1, afterTodayCount)
        assertEquals("Total reviews must increment by 1", beforeStats.totalReviews + 1, afterStats.totalReviews)
    }

    // ========================================================================
    // 3. Onboarding Tutorial State Tests
    // ========================================================================

    @Test
    fun test07_onboardingStateManagement() {
        // In unit test without Context, defaults to false
        val initial = repository.isOnboardingCompleted()
        assertEquals("Default onboarding state should be false without prefs", false, initial)

        repository.setOnboardingCompleted(true)
        // Verify method executes safely without crash
    }

    @Test
    fun test08_cardStateTransitionsAndSafety() {
        val firstCard = repository.getDueQueue(dailyNewLimit = 1).first().first
        assertEquals(com.ancient.wenyan.domain.fsrs.CardState.NEW, repository.getCardState(firstCard.id).state)

        repository.submitRating(firstCard.id, Rating.GOOD)
        val stateAfterGood = repository.getCardState(firstCard.id)
        assertNotEquals(com.ancient.wenyan.domain.fsrs.CardState.NEW, stateAfterGood.state)
        assertTrue(stateAfterGood.reps > 0)
        assertTrue(stateAfterGood.lastReviewTime != null)

        repository.clearPersistedCardStates()
        val stateAfterClear = repository.getCardState(firstCard.id)
        assertEquals(com.ancient.wenyan.domain.fsrs.CardState.NEW, stateAfterClear.state)
    }

    // ========================================================================
    // 4. Chapter Tree Library & FSRS Robustness Regression Tests
    // ========================================================================

    @Test
    fun test09_chapterTreeModulesAndProgressSafety() {
        val modules = repository.getModules()
        assertEquals("Must have exactly 11 curriculum modules", 11, modules.size)

        var totalArticlesFound = 0
        for (module in modules) {
            val articles = repository.getArticlesByModule(module.id)
            assertTrue("Module ${module.name} must have articles", articles.isNotEmpty())
            totalArticlesFound += articles.size

            for (article in articles) {
                val progress = repository.getArticleProgress(article.id)
                assertEquals(article.id, progress.articleId)
                assertFalse("Mastery percentage must never be NaN for ${article.title}", progress.masteryPercentage.isNaN())
                assertFalse("Mastery percentage must never be Infinite for ${article.title}", progress.masteryPercentage.isInfinite())
                assertTrue("Mastery percentage must be within 0..100 for ${article.title}", progress.masteryPercentage in 0f..100f)
                assertTrue("Total cards must be >= 0", progress.totalCards >= 0)
            }
        }
        assertEquals("Total articles across all modules must be 100", 100, totalArticlesFound)
    }

    @Test
    fun test10_fsrsCalendarDayAndShortTermStability() {
        val engine = repository.fsrsEngine

        // 1. Verify midnight calendar-day calculation
        // Review at 23:50 today, review again at 08:00 tomorrow (8h10m gap, but DIFFERENT calendar day)
        val zone = java.time.ZoneId.systemDefault()
        val todayNight = java.time.LocalDate.now().atTime(23, 50).atZone(zone).toInstant().toEpochMilli()
        val tomorrowMorning = java.time.LocalDate.now().plusDays(1).atTime(8, 0).atZone(zone).toInstant().toEpochMilli()

        val card = com.ancient.wenyan.domain.fsrs.CardFsrsState(
            cardId = "test_card",
            state = com.ancient.wenyan.domain.fsrs.CardState.REVIEW,
            stability = 5.0,
            difficulty = 5.0,
            lastReviewTime = todayNight
        )

        val result = engine.evaluateReview(card, Rating.GOOD, tomorrowMorning)
        // Since it's a new calendar day, elapsedDays should be 1 (NOT same-day short term)
        assertTrue("Interval should reflect full recall stability update across calendar days", result.updatedCard.scheduledDays >= 1)

        // 2. Short term stability check for HARD vs GOOD
        val initialS = 4.0
        val hardS = engine.shortTermStability(initialS, Rating.HARD)
        val goodS = engine.shortTermStability(initialS, Rating.GOOD)

        assertTrue("GOOD short-term stability should not decrease", goodS >= initialS)
        assertTrue("HARD short-term stability should be strictly less than GOOD", hardS < goodS)
    }
}

