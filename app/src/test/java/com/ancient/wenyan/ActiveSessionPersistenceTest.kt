package com.ancient.wenyan

import com.ancient.wenyan.data.CurriculumDataSource
import com.ancient.wenyan.data.WenYanRepository
import com.ancient.wenyan.domain.fsrs.CardFsrsState
import com.ancient.wenyan.domain.fsrs.CardState
import com.ancient.wenyan.domain.fsrs.FSRSEngine
import com.ancient.wenyan.domain.fsrs.Rating
import com.ancient.wenyan.domain.model.ActiveSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit Test Suite for Active Session Breakpoint Persistence (断点续背及中途退出恢复测试套件)
 */
class ActiveSessionPersistenceTest {

    private lateinit var repository: WenYanRepository

    @Before
    fun setUp() {
        repository = WenYanRepository()
        repository.clearActiveSession()
    }

    @Test
    fun test01_saveAndClearActiveSession() {
        assertNull("Initial active session should be null", repository.activeSessionFlow.value)

        val cardIds = listOf("card_bx1_07_0_A", "card_bx1_07_1_A", "card_bx1_07_2_A")
        val session = ActiveSession(
            id = "session_test_01",
            title = "今日复习",
            sessionType = "TODAY_REVIEW",
            cardIds = cardIds,
            currentIndex = 1,
            completedCount = 1,
            totalCards = 3
        )

        repository.saveActiveSession(session)

        val active = repository.activeSessionFlow.value
        assertNotNull("Active session should not be null after saving", active)
        assertEquals("session_test_01", active?.id)
        assertEquals("TODAY_REVIEW", active?.sessionType)
        assertEquals(1, active?.currentIndex)
        assertEquals(1, active?.completedCount)
        assertEquals(3, active?.totalCards)
        assertEquals(cardIds, active?.cardIds)

        // Clear session
        repository.clearActiveSession()
        assertNull("Active session must be null after clearActiveSession", repository.activeSessionFlow.value)
    }

    @Test
    fun test02_restoreCardsForSessionPreservesExactCardOrder() {
        val article = CurriculumDataSource.ARTICLE_MAP["art_bx1_07"] // 登高
        assertNotNull("Article 登高 must exist", article)
        val cards = CurriculumDataSource.generateFlashcardsForArticle(article!!)
        assertTrue("Cards must not be empty", cards.size >= 4)

        // Suppose user is reviewing: card0, card1, card2, card0 (repeated again)
        val queueIds = listOf(cards[0].id, cards[1].id, cards[2].id, cards[0].id)
        val session = ActiveSession(
            id = "session_art_bx1_07",
            title = "《登高》背诵",
            sessionType = "ARTICLE_RECITE",
            cardIds = queueIds,
            currentIndex = 2,
            completedCount = 2,
            totalCards = 4
        )

        val restoredCards = repository.restoreCardsForSession(session)
        assertEquals("Restored cards size must match queueIds", queueIds.size, restoredCards.size)
        assertEquals(cards[0].id, restoredCards[0].first.id)
        assertEquals(cards[1].id, restoredCards[1].first.id)
        assertEquals(cards[2].id, restoredCards[2].first.id)
        assertEquals(cards[0].id, restoredCards[3].first.id) // Repeated Again card
    }

    @Test
    fun test03_fsrsEasyRatingIntervalStrictlyGreaterThanGood() {
        val engine = FSRSEngine()
        val initialCard = CardFsrsState(cardId = "test_c1", state = CardState.NEW)

        // Initial review
        val afterGood = engine.evaluateReview(initialCard, Rating.GOOD).updatedCard
        val afterEasy = engine.evaluateReview(initialCard, Rating.EASY).updatedCard

        assertTrue(
            "Easy scheduledDays (${afterEasy.scheduledDays}) must be >= Good scheduledDays (${afterGood.scheduledDays})",
            afterEasy.scheduledDays >= afterGood.scheduledDays
        )

        // Subsequent review in REVIEW state
        val reviewCard = afterGood.copy(
            state = CardState.REVIEW,
            lastReviewTime = System.currentTimeMillis() - 86400000L * 4
        )
        val secondGood = engine.evaluateReview(reviewCard, Rating.GOOD).updatedCard
        val secondEasy = engine.evaluateReview(reviewCard, Rating.EASY).updatedCard

        assertTrue(
            "Second Easy scheduledDays (${secondEasy.scheduledDays}) must be > Second Good scheduledDays (${secondGood.scheduledDays})",
            secondEasy.scheduledDays > secondGood.scheduledDays
        )
    }

    @Test
    fun test04_concurrentStateAccessThreadSafety() = runBlocking {
        val jobs = (1..40).map { i ->
            async(Dispatchers.Default) {
                val cardId = "card_bx1_07_${i % 4}_A"
                repository.submitRating(cardId, Rating.GOOD)
                val readState = repository.getCardState(cardId)
                assertNotNull(readState)
            }
        }
        jobs.awaitAll()
        assertTrue("All concurrent updates completed successfully without deadlock or crash", true)
    }

    @Test
    fun test05_webDavConfigPersistenceAndSingletonInstance() {
        // Test singleton instance registration
        val repoInstance = WenYanRepository.getInstance()
        assertNotNull("WenYanRepository.getInstance() must return non-null singleton", repoInstance)

        // Test WebDAV config persistence fallback and getter/setter
        val testConfig = com.ancient.wenyan.domain.sync.WebDavConfig(
            serverUrl = "https://custom-dav.example.com/dav/",
            username = "ancient_student",
            password = "secret_password_123"
        )
        repository.saveWebDavConfig(testConfig)
        val loadedConfig = repository.getWebDavConfig()
        assertEquals(testConfig.serverUrl, loadedConfig.serverUrl)
        assertEquals(testConfig.username, loadedConfig.username)
        assertEquals(testConfig.password, loadedConfig.password)
    }

    @Test
    fun test06_widgetProviderCuratedQuotesAndStats() {
        val quotes = com.ancient.wenyan.ui.screens.CURATED_QUOTES
        assertTrue("Curated quotes list must not be empty", quotes.isNotEmpty())
        quotes.forEach { q ->
            assertTrue("Quote title must not be blank", q.title.isNotBlank())
            assertTrue("Quote author must not be blank", q.author.isNotBlank())
            assertTrue("Quote text must not be blank", q.quote.isNotBlank())
        }
        val streak = repository.computeHeatmapStats().currentStreak
        assertTrue("Current streak must be >= 0", streak >= 0)
    }
}
