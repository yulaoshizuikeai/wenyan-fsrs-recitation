package com.ancient.wenyan

import com.ancient.wenyan.data.CurriculumDataSource
import com.ancient.wenyan.data.WenYanRepository
import com.ancient.wenyan.domain.fsrs.CardState
import com.ancient.wenyan.domain.fsrs.Rating
import com.ancient.wenyan.domain.model.RecitationOrderMode
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit Test Suite for Sequential Poem & Article Recitation Guarantee (顺承篇章原序测试套件)
 *
 * Verifies that:
 * 1. Flashcards retain exact verse positions (unitIndex, totalUnits) and preceding context cues.
 * 2. Due review queue in SEQUENTIAL mode preserves the original poem/prose sequence even with mixed FSRS states.
 * 3. Cards across different articles are grouped cleanly by article without sentence interleaving.
 * 4. Random practice queue preserves verse order for multi-card articles when preservePoemOrder is enabled.
 * 5. Recitation order mode defaults to SEQUENTIAL and persists across setting changes.
 */
class SequentialRecitationOrderTest {

    private lateinit var repository: WenYanRepository

    @Before
    fun setUp() {
        repository = WenYanRepository()
    }

    @Test
    fun test01_flashcardsCarryAccurateUnitIndexAndContextBridge() {
        val article = CurriculumDataSource.ARTICLE_MAP["art_bx1_07"] // 杜甫《登高》
        assertNotNull("Article 登高 must exist", article)

        val cards = CurriculumDataSource.generateFlashcardsForArticle(article!!)
        assertTrue("登高 must have cards", cards.isNotEmpty())

        val totalUnits = cards.first().totalUnits
        assertEquals("登高 has 4 couplets", 4, totalUnits)

        // Verify each card has valid unitIndex and totalUnits
        for (card in cards) {
            assertEquals(totalUnits, card.totalUnits)
            assertTrue(card.unitIndex in 0 until totalUnits)
        }

        // Unit 0 should have no preceding hint (首联)
        val unit0Cards = cards.filter { it.unitIndex == 0 }
        for (c in unit0Cards) {
            assertNull("Unit 0 has no preceding verse", c.precedingClauseHint)
        }

        // Unit 1 should carry Unit 0 text as preceding cue (颔联上承首联)
        val unit1Cards = cards.filter { it.unitIndex == 1 }
        for (c in unit1Cards) {
            assertNotNull("Unit 1 must carry preceding verse context", c.precedingClauseHint)
            assertTrue("Preceding hint should contain text of unit 0", c.precedingClauseHint!!.contains("风急天高"))
        }
    }

    @Test
    fun test02_dueQueueInSequentialMode_preservesChronologicalPoemOrderWithMixedStates() {
        val article = CurriculumDataSource.ARTICLE_MAP["art_bx1_07"]!! // 杜甫《登高》
        val cards = repository.getFlashcardsForArticle(article.id)
        assertTrue(cards.size >= 4)

        // Scope to 必修上册 to isolate
        repository.setSelectedBookScope(setOf(article.moduleId), "测试模块")

        val now = System.currentTimeMillis()

        // Create mixed states across the units of 登高:
        // Card unit 0 -> review state (due now)
        val card0 = cards.first { it.unitIndex == 0 }
        repository.submitRating(card0.id, Rating.EASY, now - 86400000L * 10)

        // Card unit 1 -> learning state (due now)
        val card1 = cards.first { it.unitIndex == 1 }
        repository.submitRating(card1.id, Rating.HARD, now - 600000L)

        // Card unit 2 -> relearning state (due now)
        val card2 = cards.first { it.unitIndex == 2 }
        repository.submitRating(card2.id, Rating.EASY, now - 86400000L * 15)
        repository.submitRating(card2.id, Rating.AGAIN, now - 600000L) // lapsed from REVIEW -> RELEARNING

        // Card unit 3 -> remains NEW

        // Verify SRS Priority order would have jumbled them:
        val srsQueue = repository.getDueQueue(nowMillis = now, dailyNewLimit = 20, orderMode = RecitationOrderMode.SRS_PRIORITY)
        val dengGaoSrsCards = srsQueue.filter { it.first.articleId == article.id }
        assertTrue("Must include due cards in SRS queue", dengGaoSrsCards.size >= 3)
        // In traditional SRS: Relearning (unit 2) -> Learning (unit 1) -> Review (unit 0)
        assertEquals("SRS Priority puts relearning first", 2, dengGaoSrsCards[0].first.unitIndex)

        // NOW verify SEQUENTIAL mode:
        val seqQueue = repository.getDueQueue(nowMillis = now, dailyNewLimit = 20, orderMode = RecitationOrderMode.SEQUENTIAL)
        val dengGaoSeqCards = seqQueue.filter { it.first.articleId == article.id }
        assertTrue("Must include due cards in Sequential queue", dengGaoSeqCards.size >= 3)

        // Assert that the order of unitIndex is strictly monotonically non-decreasing!
        var prevUnitIndex = -1
        for ((card, _) in dengGaoSeqCards) {
            assertTrue(
                "Sequential mode MUST preserve text order: unitIndex ${card.unitIndex} >= $prevUnitIndex",
                card.unitIndex >= prevUnitIndex
            )
            prevUnitIndex = card.unitIndex
        }

        // First card MUST be unit 0, not unit 2!
        assertEquals("First card in sequential mode must be unit 0", 0, dengGaoSeqCards.first().first.unitIndex)
    }

    @Test
    fun test03_dueQueueInSequentialMode_groupsCardsByArticleWithoutInterleaving() {
        val now = System.currentTimeMillis()

        // Rate cards from art_bx1_07 (登高) and art_bx1_01 (沁园春·长沙)
        val dengGao = CurriculumDataSource.ARTICLE_MAP["art_bx1_07"]!!
        val changSha = CurriculumDataSource.ARTICLE_MAP["art_bx1_01"]!!

        val dengGaoCards = repository.getFlashcardsForArticle(dengGao.id)
        val changShaCards = repository.getFlashcardsForArticle(changSha.id)

        // Interleave ratings
        repository.submitRating(dengGaoCards[0].id, Rating.AGAIN, now - 600000L)
        repository.submitRating(changShaCards[0].id, Rating.AGAIN, now - 600000L)
        repository.submitRating(dengGaoCards[1].id, Rating.HARD, now - 600000L)
        repository.submitRating(changShaCards[1].id, Rating.HARD, now - 600000L)

        val queue = repository.getDueQueue(
            nowMillis = now,
            dailyNewLimit = 10,
            scope = setOf("MODULE_BX_1"),
            orderMode = RecitationOrderMode.SEQUENTIAL
        )

        // Verify that all cards of an article appear contiguously (never interleaved A -> B -> A -> B)
        val seenArticles = mutableListOf<String>()
        var currentArticle: String? = null

        for ((card, _) in queue) {
            if (card.articleId != currentArticle) {
                assertFalse(
                    "Article ${card.articleId} must not reappear after switching away!",
                    seenArticles.contains(card.articleId)
                )
                seenArticles.add(card.articleId)
                currentArticle = card.articleId
            }
        }
    }

    @Test
    fun test04_randomQueue_preservesPoemOrderWhenEnabled() {
        val randomCards = repository.getRandomQueue(
            limit = 30,
            moduleIds = setOf("MODULE_BX_1"),
            gaoKaoOnly = false,
            preservePoemOrder = true
        )

        assertTrue(randomCards.isNotEmpty())

        // Check each article cluster in the returned queue:
        val groupedByArticle = randomCards.groupBy { it.first.articleId }
        for ((articleId, articleCards) in groupedByArticle) {
            if (articleCards.size > 1) {
                var prevUnit = -1
                var prevCloze = -1
                for ((card, _) in articleCards) {
                    if (card.unitIndex == prevUnit) {
                        assertTrue("Cloze index should be non-decreasing", card.clozeIndex >= prevCloze)
                    } else {
                        assertTrue("Unit index must be non-decreasing for $articleId", card.unitIndex >= prevUnit)
                    }
                    prevUnit = card.unitIndex
                    prevCloze = card.clozeIndex
                }
            }
        }
    }

    @Test
    fun test05_recitationOrderModeDefaultsToSequentialAndConfigurable() {
        assertEquals(
            "Default recitation order mode must be SEQUENTIAL",
            RecitationOrderMode.SEQUENTIAL,
            repository.recitationOrderMode.value
        )

        repository.setRecitationOrderMode(RecitationOrderMode.SRS_PRIORITY)
        assertEquals(RecitationOrderMode.SRS_PRIORITY, repository.recitationOrderMode.value)

        repository.setRecitationOrderMode(RecitationOrderMode.RANDOM_SHUFFLE)
        assertEquals(RecitationOrderMode.RANDOM_SHUFFLE, repository.recitationOrderMode.value)

        repository.setRecitationOrderMode(RecitationOrderMode.SEQUENTIAL)
        assertEquals(RecitationOrderMode.SEQUENTIAL, repository.recitationOrderMode.value)
    }
}
