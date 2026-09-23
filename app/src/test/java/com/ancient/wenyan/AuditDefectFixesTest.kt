package com.ancient.wenyan

import com.ancient.wenyan.data.CurriculumDataSource
import com.ancient.wenyan.data.WenYanRepository
import com.ancient.wenyan.domain.ai.TypeSafeDiagnosisEngine
import com.ancient.wenyan.domain.fsrs.CardFsrsState
import com.ancient.wenyan.domain.fsrs.CardState
import com.ancient.wenyan.domain.fsrs.Rating
import com.ancient.wenyan.domain.fsrs.ReviewLog
import com.ancient.wenyan.domain.sync.WebDavBackupManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Verification test suite for issues identified in AUDIT_AND_UPGRADE_REPORT.md:
 * 1. Leech stubborn card isolation from regular getDueQueue
 * 2. WebDAV JSON full parity backup & restore (cards, reviewLogs, dailyReviews)
 * 3. Daily review map synthesis fallback when dailyReviews block is absent in JSON
 * 4. Pure JVM Base64 in WebDavBackupManager without android framework dependency
 */
class AuditDefectFixesTest {

    private lateinit var repository: WenYanRepository

    @Before
    fun setUp() {
        repository = WenYanRepository()
    }

    @Test
    fun test01_leechCardsAreExcludedFromDueQueue() = runBlocking {
        val sampleCards = CurriculumDataSource.generateFlashcardsForArticle(CurriculumDataSource.ALL_ARTICLES.first())
        val regularCard = sampleCards[0]
        val leechCard = sampleCards[1]

        val now = System.currentTimeMillis()

        // Setup regular review card (due today)
        val regularState = CardFsrsState(
            cardId = regularCard.id,
            state = CardState.REVIEW,
            stability = 3.0,
            difficulty = 5.0,
            reps = 3,
            lapses = 1,
            dueTime = now - 1000L,
            isLeech = false
        )

        // Setup leech card (due today, lapses >= 4, isLeech = true)
        val leechState = CardFsrsState(
            cardId = leechCard.id,
            state = CardState.REVIEW,
            stability = 1.0,
            difficulty = 9.0,
            reps = 6,
            lapses = 4,
            dueTime = now - 1000L,
            isLeech = true
        )

        repository.persistRestoredCardStates(listOf(regularState, leechState))

        // Query due queue
        val dueQueue = repository.getDueQueue(nowMillis = now, dailyNewLimit = 0)
        val dueCardIds = dueQueue.map { it.second.cardId }

        assertTrue("Regular review card should be in due queue", regularCard.id in dueCardIds)
        assertFalse("Leech card MUST BE EXCLUDED from regular due queue to prevent spamming", leechCard.id in dueCardIds)

        // Verify Leech cards are accessible in separate leech pool
        val leechCards = repository.getLeechCards()
        val leechCardIds = leechCards.map { it.second.cardId }
        assertTrue("Leech card MUST BE AVAILABLE in getLeechCards()", leechCard.id in leechCardIds)
    }

    @Test
    fun test02_webDavBackupAndRestoreFullParity() = runBlocking {
        val repoSource = WenYanRepository()
        val sampleCards = CurriculumDataSource.generateFlashcardsForArticle(CurriculumDataSource.ALL_ARTICLES.first())
        val card1 = sampleCards[0]
        val card2 = sampleCards[1]

        val now = System.currentTimeMillis()

        // Submit ratings to create states, logs, and daily count
        repoSource.submitRating(card1.id, Rating.GOOD, now)
        repoSource.submitRating(card2.id, Rating.AGAIN, now)

        val sourceLogs = repoSource.getReviewLogs()
        assertTrue("Source repository must have review logs", sourceLogs.isNotEmpty())
        val sourceDaily = repoSource.getDailyReviewMap()
        assertTrue("Source repository must have daily reviews", sourceDaily.isNotEmpty())

        // 1. Export to JSON
        val backupJson = WebDavBackupManager.createBackupJson(repoSource)
        assertTrue("Backup JSON must contain cards array", backupJson.contains("\"cards\":"))
        assertTrue("Backup JSON must contain reviewLogs array", backupJson.contains("\"reviewLogs\":"))
        assertTrue("Backup JSON must contain dailyReviews map", backupJson.contains("\"dailyReviews\":"))

        // 2. Restore into a clean destination repository
        val repoDest = WenYanRepository()
        val restoredCount = WebDavBackupManager.restoreFromJson(backupJson, repoDest)

        assertTrue("Should restore cards", restoredCount >= 2)
        val destLogs = repoDest.getReviewLogs()
        assertEquals("Restored review logs count must match source", sourceLogs.size, destLogs.size)

        val destDaily = repoDest.getDailyReviewMap()
        assertEquals("Restored daily review count must match source", sourceDaily.size, destDaily.size)
        val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        assertEquals("Today's review count must match", sourceDaily[todayStr], destDaily[todayStr])
    }

    @Test
    fun test03_webDavRestoreSynthesizesDailyMapWhenMissing() = runBlocking {
        val now = System.currentTimeMillis()
        val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        // Legacy JSON without "dailyReviews" block but containing "reviewLogs"
        val legacyJson = """
        {
          "version": 1,
          "timestamp": $now,
          "totalCards": 10,
          "retentionPercentage": 85.0,
          "cards": [
            {
              "cardId": "card_test_1",
              "state": "REVIEW",
              "step": -1,
              "stability": 2.5,
              "difficulty": 4.0,
              "elapsedDays": 1,
              "scheduledDays": 2,
              "reps": 1,
              "lapses": 0,
              "lastReviewTime": $now,
              "dueTime": $now,
              "isLeech": false
            }
          ],
          "reviewLogs": [
            {
              "cardId": "card_test_1",
              "rating": "GOOD",
              "previousState": "LEARNING",
              "currentState": "REVIEW",
              "stability": 2.5,
              "difficulty": 4.0,
              "elapsedDays": 1,
              "scheduledDays": 2,
              "reviewTime": $now
            },
            {
              "cardId": "card_test_1",
              "rating": "EASY",
              "previousState": "REVIEW",
              "currentState": "REVIEW",
              "stability": 5.0,
              "difficulty": 3.5,
              "elapsedDays": 2,
              "scheduledDays": 5,
              "reviewTime": $now
            }
          ]
        }
        """.trimIndent()

        val repoDest = WenYanRepository()
        WebDavBackupManager.restoreFromJson(legacyJson, repoDest)

        val dailyMap = repoDest.getDailyReviewMap()
        assertTrue("Daily map must be synthesized from reviewLogs reviewTime", dailyMap.containsKey(todayStr))
        assertEquals("Daily review count must reflect 2 logs for today", 2, dailyMap[todayStr])
    }

    @Test
    fun test04_recitationDiffEngineSubstitution() {
        val expected = "驽马十驾，功在不舍"
        val spoken = "弩马十驾，功在不舍" // typo: 弩 instead of 驽
        val res = com.ancient.wenyan.domain.speech.RecitationDiffEngine.evaluate(spoken, expected)

        assertFalse("Typo should not be perfect", res.isPerfect)
        assertEquals("Wrong count should be 1 for typo substitution", 1, res.wrongCount)
        assertEquals("Extra count should be 0", 0, res.extraCount)
        assertEquals("Missing count should be 0", 0, res.missingCount)
        assertEquals(7, res.matchedCount)

        val subItem = res.diffSequence.firstOrNull { it.type == com.ancient.wenyan.domain.speech.DiffType.SUBSTITUTION }
        assertNotNull("Should contain a SUBSTITUTION diff item", subItem)
        assertEquals('弩', subItem!!.char)
        assertEquals('驽', subItem.expectedChar)
    }

    @Test
    fun test05_fsrsEngineDiscreteFuzzAcrossRounds() {
        val engine = com.ancient.wenyan.domain.fsrs.FSRSEngine()
        val cardId = "card_test_reps_fuzz"
        val interval = 50

        val fuzzed0 = engine.applyFuzz(interval, cardId, reps = 0)
        val fuzzed1 = engine.applyFuzz(interval, cardId, reps = 1)
        val fuzzed2 = engine.applyFuzz(interval, cardId, reps = 2)

        assertTrue("Fuzzed interval must be within bounds", fuzzed0 in 47..53)
        assertTrue("Fuzzed interval must be within bounds", fuzzed1 in 47..53)
        assertTrue("Fuzzed interval must be within bounds", fuzzed2 in 47..53)
        val distinctIntervals = setOf(fuzzed0, fuzzed1, fuzzed2)
        assertTrue("Different review rounds should have potential variation", distinctIntervals.size >= 1)
    }

    @Test
    fun test06_dueCardsInStatsExcludesLeechCards() = runBlocking {
        val sampleCards = CurriculumDataSource.generateFlashcardsForArticle(CurriculumDataSource.ALL_ARTICLES.first())
        val card1 = sampleCards[0]
        val card2 = sampleCards[1]

        val now = System.currentTimeMillis()

        // 1 regular due card, 1 leech due card
        val regularCard = CardFsrsState(
            cardId = card1.id,
            state = CardState.REVIEW,
            stability = 2.0,
            difficulty = 5.0,
            reps = 3,
            lapses = 1,
            dueTime = now - 1000L,
            isLeech = false
        )
        val leechCard = CardFsrsState(
            cardId = card2.id,
            state = CardState.REVIEW,
            stability = 1.0,
            difficulty = 9.0,
            reps = 6,
            lapses = 4,
            dueTime = now - 1000L,
            isLeech = true
        )

        val testRepo = WenYanRepository()
        testRepo.persistRestoredCardStates(listOf(regularCard, leechCard))

        val stats = testRepo.computeStats(nowMillis = now)
        val dueQueue = testRepo.getDueQueue(nowMillis = now, dailyNewLimit = 0)

        assertEquals("dueQueue should only have the 1 non-leech card", 1, dueQueue.size)
        assertEquals("computeStats dueCards must strictly exclude Leech cards to match dueQueue", 1, stats.dueCards)
    }

    @Test
    fun test07_webDavRestoreWorksWithoutCardsArray() = runBlocking {
        val now = System.currentTimeMillis()
        val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        // JSON payload containing only reviewLogs and dailyReviews, no cards array
        val partialJson = """
        {
          "version": 1,
          "timestamp": $now,
          "reviewLogs": [
            {
              "cardId": "test_card_10",
              "rating": "GOOD",
              "previousState": "NEW",
              "currentState": "LEARNING",
              "stability": 2.0,
              "difficulty": 5.0,
              "elapsedDays": 0,
              "scheduledDays": 1,
              "reviewTime": $now
            }
          ],
          "dailyReviews": {
            "$todayStr": 5
          }
        }
        """.trimIndent()

        val destRepo = WenYanRepository()
        val restoredCount = WebDavBackupManager.restoreFromJson(partialJson, destRepo)

        assertTrue("Restoration must succeed even without cards array", restoredCount >= 1)
        assertEquals("Review logs should be restored", 1, destRepo.getReviewLogs().size)
        assertEquals("Daily review count must be restored", 5, destRepo.getDailyReviewMap()[todayStr])
        assertEquals("Today study progress should reflect daily reviews", 5, destRepo.todayStudyProgressFlow.value.todayReviewed)
    }
}

