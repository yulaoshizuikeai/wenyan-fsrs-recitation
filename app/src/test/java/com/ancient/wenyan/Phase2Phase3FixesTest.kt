package com.ancient.wenyan

import com.ancient.wenyan.data.CurriculumDataSource
import com.ancient.wenyan.data.WenYanRepository
import com.ancient.wenyan.domain.cloze.ClozeEngine
import com.ancient.wenyan.domain.fsrs.*
import com.ancient.wenyan.domain.model.ActiveSession
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * Targeted Unit Tests for Phase 2 (P1) and Phase 3 (P2) Bug Fixes:
 * - Natural day boundary in review queue and stats calculation
 * - Active session title URL encoding/decoding
 * - FSRSOptimizer idempotency across successive calibrations
 * - ClozeEngine short clauses (2-3 chars) and full Chinese punctuation set
 * - ClozeEngine Level 1 keyword length descending replacement
 */
class Phase2Phase3FixesTest {

    @Test
    fun test01_fsrsOptimizerIdempotency() {
        val logs = List(25) { i ->
            ReviewLog(
                cardId = "card_$i",
                rating = if (i % 3 == 0) Rating.AGAIN else Rating.GOOD,
                previousState = CardState.LEARNING,
                currentState = CardState.REVIEW,
                stability = 2.0,
                difficulty = 5.0,
                elapsedDays = 1,
                scheduledDays = 2,
                reviewTime = System.currentTimeMillis()
            )
        }

        val run1 = FSRSOptimizer.optimize(logs, FSRSEngine.DEFAULT_FSRS_5_WEIGHTS)
        assertTrue(run1.success)

        // Second run using the result of run 1 as currentWeights
        val run2 = FSRSOptimizer.optimize(logs, run1.optimizedWeights)
        assertTrue(run2.success)

        // Due to anchoring on base weights, repeated runs on the same dataset produce identical optimized weights
        assertArrayEquals(
            "Repeated optimization must be idempotent and not drift/diverge",
            run1.optimizedWeights,
            run2.optimizedWeights,
            0.0001
        )
    }

    @Test
    fun test02_clozeEngineShortClauseMasking() {
        // Classic 2-3 char clauses like "子曰：学而时习之，不亦说乎？"
        val text = "子曰：学而时习之，不亦说乎？"
        val tokens = ClozeEngine.tokenize(text, level = 2, emptyList())

        // "子曰" is 2 chars, previously ignored with clause.length >= 4. Now with >= 2, it should have masked tokens
        assertTrue("Short 2-char clause '子曰' should have masked part in Level 2", tokens.any { it.isMasked })

        val maskedL2 = ClozeEngine.generateLevel2(text)
        assertTrue("Level 2 text masking should mask short clauses", maskedL2.contains("⟦"))
    }

    @Test
    fun test03_clozeEngineFullPunctuationSupport() {
        // Test text containing quotes and book title marks: 《论语》曰：“温故而知新，可以为师矣。”
        val text = "《论语》曰：“温故而知新，可以为师矣。”"

        val l3 = ClozeEngine.generateLevel3(text)
        // Book title mark 《 》 and quotes “ ” should be preserved as punctuation
        assertTrue(l3.contains("《"))
        assertTrue(l3.contains("》"))
        assertTrue(l3.contains("“"))
        assertTrue(l3.contains("”"))

        val l4 = ClozeEngine.generateLevel4(text)
        assertTrue(l4.contains("《"))
        assertTrue(l4.contains("》"))
        assertTrue(l4.contains("“"))
        assertTrue(l4.contains("”"))
    }

    @Test
    fun test04_clozeEngineKeywordLengthDescendingMatch() {
        // "天下" vs "天下归心" - longer keyword must be matched first
        val text = "山不厌高，海不厌深。周公吐哺，天下归心。"
        val keywords = listOf("天下", "天下归心")

        val l1Tokens = ClozeEngine.tokenize(text, level = 1, keywords)
        val maskedToken = l1Tokens.find { it.isMasked }
        assertNotNull("Should mask keyword", maskedToken)
        assertEquals("Longer keyword '天下归心' must be preferred over '天下'", "天下归心", maskedToken!!.originalText)

        val l1Text = ClozeEngine.generateLevel1(text, keywords)
        // Should mask the 4-char phrase as ⟦ ____ ⟧, not ⟦ __ ⟧归心
        assertTrue("Should fully mask '天下归心'", l1Text.contains("⟦ ____ ⟧"))
    }

    @Test
    fun test05_activeSessionSpecialCharacterSerialization() {
        val repo = WenYanRepository(context = null)
        val session = ActiveSession(
            id = "test_session_1",
            title = "劝学 | 高考必背 (荀子 · 战国)",
            sessionType = "FLIP_CARD",
            currentIndex = 1,
            completedCount = 1,
            totalCards = 4,
            lastActiveMillis = System.currentTimeMillis(),
            cardIds = listOf("card_1", "card_2", "card_3", "card_4")
        )

        repo.saveActiveSession(session)
        val loaded = repo.activeSessionFlow.value
        assertNotNull(loaded)
        assertEquals(session.id, loaded!!.id)
        assertEquals("Title with pipe '|' should be preserved accurately", session.title, loaded.title)
        assertEquals(session.currentIndex, loaded.currentIndex)
    }

    @Test
    fun test06_reviewQueueNaturalDayBoundary() {
        val repo = WenYanRepository(context = null)
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()

        // Card due at 23:30 tonight
        val tonightMillis = today.atTime(23, 30).atZone(zone).toInstant().toEpochMilli()
        // Card due at 08:00 tomorrow morning
        val tomorrowMorningMillis = today.plusDays(1).atTime(8, 0).atZone(zone).toInstant().toEpochMilli()

        // Get two genuine flashcard IDs from repository
        val article = CurriculumDataSource.ALL_ARTICLES.first()
        val cards = CurriculumDataSource.generateFlashcardsForArticle(article)
        val card1 = cards[0]
        val card2 = cards[1]

        // Set card 1 due tonight
        repo.setCardStateForTesting(
            CardFsrsState(
                cardId = card1.id,
                state = CardState.REVIEW,
                dueTime = tonightMillis,
                stability = 5.0,
                difficulty = 5.0,
                elapsedDays = 2,
                scheduledDays = 2,
                reps = 1,
                lapses = 0,
                lastReviewTime = today.minusDays(2).atStartOfDay(zone).toInstant().toEpochMilli()
            )
        )

        // Set card 2 due tomorrow
        repo.setCardStateForTesting(
            CardFsrsState(
                cardId = card2.id,
                state = CardState.REVIEW,
                dueTime = tomorrowMorningMillis,
                stability = 5.0,
                difficulty = 5.0,
                elapsedDays = 2,
                scheduledDays = 3,
                reps = 1,
                lapses = 0,
                lastReviewTime = today.minusDays(2).atStartOfDay(zone).toInstant().toEpochMilli()
            )
        )

        // Query today's due queue (dailyNewLimit = 0 to only inspect review queue)
        val currentNowMillis = today.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val dueQueue = repo.getDueQueue(nowMillis = currentNowMillis, dailyNewLimit = 0)
        val dueIds = dueQueue.map { it.first.id }

        assertTrue("Card due tonight should be included in today's review queue", dueIds.contains(card1.id))
        assertFalse("Card due tomorrow morning must NOT be overdrawn into today's review queue", dueIds.contains(card2.id))
    }
}
