package com.ancient.wenyan

import com.ancient.wenyan.data.CurriculumDataSource
import com.ancient.wenyan.data.WenYanRepository
import com.ancient.wenyan.domain.fsrs.CardState
import com.ancient.wenyan.domain.fsrs.Rating
import com.ancient.wenyan.domain.model.StudyGoalsConfig
import com.ancient.wenyan.domain.model.StudyOrderPreference
import com.ancient.wenyan.domain.model.TodayStudyProgress
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit Test Suite for Daily Study & Review Goals (Anki-style New Card & Review Limits)
 */
class DailyGoalsAndQueueTest {

    private lateinit var repository: WenYanRepository

    @Before
    fun setUp() {
        repository = WenYanRepository()
    }

    @Test
    fun test01_studyGoalsConfigDefaults() {
        val config = repository.getStudyGoalsConfig()
        assertEquals("Default daily new limit should be 20", 20, config.dailyNewLimit)
        assertEquals("Default daily review limit should be 100", 100, config.dailyReviewLimit)
        assertEquals("Default study order should be REVIEW_FIRST", StudyOrderPreference.REVIEW_FIRST, config.orderPreference)
    }

    @Test
    fun test02_customDailyNewCardsLimitTruncation() {
        val limitedQueue = repository.getDueQueue(dailyNewLimit = 7)
        val newCardsInQueue = limitedQueue.filter { it.second.state == CardState.NEW }
        assertEquals("Queue should contain at most 7 new cards", 7, newCardsInQueue.size)
    }

    @Test
    fun test03_zeroDailyNewCardsEnablesPureReviewMode() {
        // When dailyNewLimit is 0 (Pure Review Mode), no NEW cards should enter queue
        val pureReviewQueue = repository.getDueQueue(dailyNewLimit = 0)
        val newCardsInQueue = pureReviewQueue.filter { it.second.state == CardState.NEW }
        assertEquals("Pure review mode should have 0 new cards", 0, newCardsInQueue.size)
    }

    @Test
    fun test04_dailyReviewLimitTruncation() {
        val now = System.currentTimeMillis()
        // Convert 5 cards to review status via FSRS learning steps graduation
        val first5Cards = repository.getDueQueue(nowMillis = now, dailyNewLimit = 5).map { it.first.id }
        for (id in first5Cards) {
            repository.submitRating(id, Rating.GOOD, nowMillis = now)
        }
        // FSRS default learning steps are [1m, 10m]. The first GOOD rating puts cards into LEARNING (step 1, due in 10m).
        // Advance time past the 10m step to rate GOOD again and graduate cards to REVIEW state.
        val graduateTime = now + 600_000L + 1_000L
        for (id in first5Cards) {
            repository.submitRating(id, Rating.GOOD, nowMillis = graduateTime)
        }

        // Fast forward 10 days so all graduated review cards are due
        val future = graduateTime + 10 * 86400000L
        val queueWithReviewLimit = repository.getDueQueue(
            nowMillis = future,
            dailyNewLimit = 0,
            dailyReviewLimit = 2
        )
        val reviewCardsInQueue = queueWithReviewLimit.filter { it.second.state == CardState.REVIEW }
        assertEquals("Queue should limit review cards to 2", 2, reviewCardsInQueue.size)
        assertEquals("Total queue size should be 2 when dailyNewLimit is 0 and dailyReviewLimit is 2", 2, queueWithReviewLimit.size)
    }

    @Test
    fun test05_studyOrderPreferencesAffectQueueArrangement() {
        val now = System.currentTimeMillis()
        // Graduate 2 cards to review status via FSRS learning steps
        val cards = repository.getDueQueue(nowMillis = now, dailyNewLimit = 2).map { it.first.id }
        for (id in cards) {
            repository.submitRating(id, Rating.GOOD, nowMillis = now)
        }
        val graduateTime = now + 600_000L + 1_000L
        for (id in cards) {
            repository.submitRating(id, Rating.GOOD, nowMillis = graduateTime)
        }

        // Fast forward 10 days
        val future = graduateTime + 10 * 86400000L

        // Test SRS_PRIORITY with REVIEW_FIRST
        val reviewFirstQueue = repository.getDueQueue(
            nowMillis = future,
            dailyNewLimit = 5,
            dailyReviewLimit = 10,
            orderMode = com.ancient.wenyan.domain.model.RecitationOrderMode.SRS_PRIORITY,
            orderPref = StudyOrderPreference.REVIEW_FIRST
        )
        assertTrue("reviewFirstQueue should contain at least 2 cards", reviewFirstQueue.size >= 2)
        val reviewFirstCardState = reviewFirstQueue.first().second.state
        assertNotEquals("Under REVIEW_FIRST, first card should not be NEW", CardState.NEW, reviewFirstCardState)
        assertEquals("Under REVIEW_FIRST, first card should be REVIEW", CardState.REVIEW, reviewFirstCardState)

        // Test SRS_PRIORITY with NEW_FIRST
        val newFirstQueue = repository.getDueQueue(
            nowMillis = future,
            dailyNewLimit = 5,
            dailyReviewLimit = 10,
            orderMode = com.ancient.wenyan.domain.model.RecitationOrderMode.SRS_PRIORITY,
            orderPref = StudyOrderPreference.NEW_FIRST
        )
        assertTrue("newFirstQueue should contain at least 2 cards", newFirstQueue.size >= 2)
        val newFirstCardState = newFirstQueue.first().second.state
        assertEquals("Under NEW_FIRST, first card should be NEW", CardState.NEW, newFirstCardState)

        // Test SRS_PRIORITY with MIXED (Cards from review and new queues should both be present and interleaved)
        val mixedQueue = repository.getDueQueue(
            nowMillis = future,
            dailyNewLimit = 5,
            dailyReviewLimit = 10,
            orderMode = com.ancient.wenyan.domain.model.RecitationOrderMode.SRS_PRIORITY,
            orderPref = StudyOrderPreference.MIXED
        )
        assertTrue("mixedQueue should contain both review and new cards", mixedQueue.size >= 4)
        val statesInMixed = mixedQueue.map { it.second.state }
        assertTrue("mixedQueue must contain REVIEW cards", statesInMixed.contains(CardState.REVIEW))
        assertTrue("mixedQueue must contain NEW cards", statesInMixed.contains(CardState.NEW))
    }

    @Test
    fun test07_dailyReviewLimitDoesNotTruncateLearningAndRelearningCards() {
        val now = System.currentTimeMillis()
        // Card 1: Relearning
        // Card 2: Learning
        // Card 3 & 4: Review (Graduated)
        val cards = repository.getDueQueue(nowMillis = now, dailyNewLimit = 4).map { it.first.id }
        assertEquals(4, cards.size)

        val relearnId = cards[0]
        val learnId = cards[1]
        val reviewId1 = cards[2]
        val reviewId2 = cards[3]

        // Setup Relearning card: rate EASY to graduate 15 days ago, then AGAIN at now - 10m
        repository.submitRating(relearnId, Rating.EASY, nowMillis = now - 15 * 86400000L)
        repository.submitRating(relearnId, Rating.AGAIN, nowMillis = now - 600_000L) // due now

        // Setup Learning card: rate GOOD at now - 10m -> enters LEARNING step 1, due now
        repository.submitRating(learnId, Rating.GOOD, nowMillis = now - 600_000L) // due now

        // Setup Review cards: graduate them in the past so they are due now
        val past = now - 10 * 86400000L
        repository.submitRating(reviewId1, Rating.EASY, nowMillis = past)
        repository.submitRating(reviewId2, Rating.EASY, nowMillis = past)

        assertEquals(CardState.RELEARNING, repository.getCardState(relearnId).state)
        assertEquals(CardState.LEARNING, repository.getCardState(learnId).state)
        assertEquals(CardState.REVIEW, repository.getCardState(reviewId1).state)
        assertEquals(CardState.REVIEW, repository.getCardState(reviewId2).state)

        // Case A: dailyReviewLimit = 1 (truncates 2 review cards down to 1, while keeping both relearning and learning cards)
        val queueLimit1 = repository.getDueQueue(
            nowMillis = now,
            dailyNewLimit = 0,
            dailyReviewLimit = 1
        )
        val relearnInQueue = queueLimit1.filter { it.second.state == CardState.RELEARNING }
        val learnInQueue = queueLimit1.filter { it.second.state == CardState.LEARNING }
        val reviewInQueue = queueLimit1.filter { it.second.state == CardState.REVIEW }

        assertEquals("Relearning cards must NOT be truncated", 1, relearnInQueue.size)
        assertEquals("Learning cards must NOT be truncated", 1, learnInQueue.size)
        assertEquals("Review cards must be truncated to dailyReviewLimit = 1", 1, reviewInQueue.size)
        assertEquals("Total queue size should be 1 + 1 + 1 = 3", 3, queueLimit1.size)

        // Case B: dailyReviewLimit = 0 (Pure review disabled: 0 review cards, but learning & relearning remain)
        val queueLimit0 = repository.getDueQueue(
            nowMillis = now,
            dailyNewLimit = 0,
            dailyReviewLimit = 0
        )
        val reviewInQueue0 = queueLimit0.filter { it.second.state == CardState.REVIEW }
        val nonReviewInQueue0 = queueLimit0.filter { it.second.state != CardState.REVIEW && it.second.state != CardState.NEW }

        assertEquals("0 review cards when dailyReviewLimit = 0", 0, reviewInQueue0.size)
        assertEquals("Learning & relearning cards must still be retained when dailyReviewLimit = 0", 2, nonReviewInQueue0.size)
    }

    @Test
    fun test06_todayStudyProgressCalculations() {
        val progress = TodayStudyProgress(
            todayNewLearned = 10,
            targetNew = 20,
            todayReviewed = 30,
            targetReview = 50
        )

        assertEquals(0.5f, progress.newProgressPercentage, 0.001f)
        assertEquals(0.6f, progress.reviewProgressPercentage, 0.001f)
        assertFalse("New goal not yet reached", progress.isNewGoalReached)
        assertFalse("Review goal not yet reached", progress.isReviewGoalReached)

        val completedProgress = TodayStudyProgress(
            todayNewLearned = 20,
            targetNew = 20,
            todayReviewed = 55,
            targetReview = 50
        )

        assertTrue("New goal reached", completedProgress.isNewGoalReached)
        assertTrue("Review goal reached", completedProgress.isReviewGoalReached)
        assertEquals(1.0f, completedProgress.newProgressPercentage, 0.001f)
        assertEquals(1.0f, completedProgress.reviewProgressPercentage, 0.001f)

        // Pure Review Mode: targetNew = 0 (new goal is automatically satisfied)
        val pureReviewProgress = TodayStudyProgress(
            todayNewLearned = 0,
            targetNew = 0,
            todayReviewed = 10,
            targetReview = 10
        )
        assertTrue("In Pure Review Mode (targetNew=0), new goal is reached", pureReviewProgress.isNewGoalReached)
        assertTrue("Review goal reached", pureReviewProgress.isReviewGoalReached)
    }

    @Test
    fun test08_overlayScreenSaverPreservesStateAcrossConfigurationChanges() {
        // 1. Test OverlayScreen.Settings save & restore
        val savedSettings = OverlayScreenStateHolder.save(OverlayScreen.Settings, repository)
        assertNotNull(savedSettings)
        val restoredSettings = OverlayScreenStateHolder.restore(savedSettings, repository)
        assertNotNull(restoredSettings)
        assertTrue(restoredSettings is OverlayScreen.Settings)

        // 2. Test OverlayScreen.Cloze save & restore
        val article = CurriculumDataSource.ALL_ARTICLES.first()
        val savedCloze = OverlayScreenStateHolder.save(OverlayScreen.Cloze(article), repository)
        assertNotNull(savedCloze)
        val restoredCloze = OverlayScreenStateHolder.restore(savedCloze, repository)
        assertNotNull(restoredCloze)
        val restoredClozeScreen = restoredCloze as OverlayScreen.Cloze
        assertEquals(article.id, restoredClozeScreen.article.id)

        // 3. Test OverlayScreen.Flashcards save & restore
        val dueCards = repository.getDueQueue(dailyNewLimit = 5)
        val flashcardsScreen = OverlayScreen.Flashcards(
            title = "测试背诵进度",
            cards = dueCards,
            initialIndex = 2,
            initialCompletedCount = 2,
            sessionId = "test_session_saver_123",
            sessionType = "TODAY_DUE"
        )
        val savedFlashcards = OverlayScreenStateHolder.save(flashcardsScreen, repository)
        assertNotNull(savedFlashcards)

        val restoredFlashcards = OverlayScreenStateHolder.restore(savedFlashcards, repository)
        assertNotNull(restoredFlashcards)
        val restoredScreen = restoredFlashcards as OverlayScreen.Flashcards
        assertEquals("测试背诵进度", restoredScreen.title)
        assertEquals("test_session_saver_123", restoredScreen.sessionId)
        assertEquals(2, restoredScreen.initialIndex)
        assertEquals(2, restoredScreen.initialCompletedCount)
        assertEquals(dueCards.size, restoredScreen.cards.size)

        // 4. Test Completed Flashcard Session save & restore (Bug: rotation on celebration screen)
        val completedFlashcardScreen = OverlayScreen.Flashcards(
            title = "已通关背诵",
            cards = dueCards,
            initialIndex = dueCards.size,
            initialCompletedCount = dueCards.size,
            sessionId = "test_completed_session",
            sessionType = "TODAY_DUE"
        )
        val savedCompleted = OverlayScreenStateHolder.save(completedFlashcardScreen, repository)
        assertNotNull(savedCompleted)
        val restoredCompleted = OverlayScreenStateHolder.restore(savedCompleted, repository) as? OverlayScreen.Flashcards
        assertNotNull(restoredCompleted)
        assertEquals(dueCards.size, restoredCompleted!!.initialIndex)
        assertEquals(dueCards.size, restoredCompleted.initialCompletedCount)

        // 5. Test null state save & restore
        val savedNull = OverlayScreenStateHolder.save(null, repository)
        assertNull(savedNull)
        val restoredNull = OverlayScreenStateHolder.restore(savedNull, repository)
        assertNull(restoredNull)
    }

    @Test
    fun test09_sequentialRecitationOrderPreferences() {
        val now = System.currentTimeMillis()
        val art1 = CurriculumDataSource.ALL_ARTICLES[0]
        val art2 = CurriculumDataSource.ALL_ARTICLES[1]
        val art1Cards = repository.getFlashcardsForArticle(art1.id)
        val art2Cards = repository.getFlashcardsForArticle(art2.id)

        // Make art1 card 0 a due review card
        repository.submitRating(art1Cards[0].id, Rating.EASY, nowMillis = now - 15 * 86400000L)

        // Make art2 card 0 a due review card
        repository.submitRating(art2Cards[0].id, Rating.EASY, nowMillis = now - 15 * 86400000L)

        // In SEQUENTIAL mode with MIXED preference: articles strictly follow textbook order
        val mixedSeqQueue = repository.getDueQueue(
            nowMillis = now,
            dailyNewLimit = 5,
            dailyReviewLimit = 10,
            scope = setOf(art1.moduleId, art2.moduleId),
            orderMode = com.ancient.wenyan.domain.model.RecitationOrderMode.SEQUENTIAL,
            orderPref = StudyOrderPreference.MIXED
        )
        assertTrue(mixedSeqQueue.isNotEmpty())
        val firstArticleId = mixedSeqQueue.first().first.articleId
        assertEquals("Under MIXED SEQUENTIAL, first article should match textbook order", art1.id, firstArticleId)
    }
}
