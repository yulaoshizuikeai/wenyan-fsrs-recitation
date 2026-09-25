package com.ancient.wenyan

import com.ancient.wenyan.data.CurriculumDataSource
import com.ancient.wenyan.data.WenYanRepository
import com.ancient.wenyan.domain.cloze.SnowballChainingEngine
import com.ancient.wenyan.domain.fsrs.CardState
import com.ancient.wenyan.domain.fsrs.Rating
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit Test Suite for Chained Recitation & FSRS Integration (串联背诵与 FSRS 联动测试套件)
 */
class ChainedRecitationFsrsTest {

    private lateinit var repository: WenYanRepository

    @Before
    fun setUp() {
        repository = WenYanRepository()
    }

    @Test
    fun test01_snowballEngineAssignsCardIdsToUnits() {
        val article = CurriculumDataSource.ARTICLE_MAP["art_bx1_07"] // 杜甫《登高》
        assertNotNull("Article 登高 must exist", article)

        val stages = SnowballChainingEngine.buildStages(article!!)
        assertEquals("登高 has 4 stages", 4, stages.size)

        for (stage in stages) {
            val unit = stage.newlyAddedUnit
            assertTrue("Each snowball unit must carry associated cardIds", unit.cardIds.isNotEmpty())
            assertTrue("Unit index must match stage index", unit.index == stage.stageIndex)
            for (cid in unit.cardIds) {
                assertTrue("Card ID must belong to 登高", cid.contains("art_bx1_07"))
            }
        }
    }

    @Test
    fun test02_submitChainedRecitationBatchUpdatesFsrsAndHeatmap() {
        val article = CurriculumDataSource.ARTICLE_MAP["art_bx1_07"]!!
        val stages = SnowballChainingEngine.buildStages(article)

        val ratingsMap = mutableMapOf<String, Rating>()
        // Mark first 3 stages as GOOD (smooth), last stage as HARD (bottleneck)
        for (stage in stages) {
            val rating = if (stage.stageIndex == 3) Rating.HARD else Rating.GOOD
            for (cid in stage.newlyAddedUnit.cardIds) {
                ratingsMap[cid] = rating
            }
        }

        assertTrue("Ratings map must not be empty", ratingsMap.isNotEmpty())

        val summary = repository.submitChainedRecitationBatch(ratingsMap)

        assertEquals("Total units in summary should match ratings map size", ratingsMap.size, summary.totalUnits)
        assertTrue("Smooth count should be positive", summary.smoothUnitsCount > 0)
        assertTrue("Bottleneck count should be positive", summary.bottleneckUnitsCount > 0)
        assertTrue("Average interval days should be positive", summary.averageNextIntervalDays > 0.0)

        // Verify card states were updated
        for ((cardId, _) in ratingsMap) {
            val state = repository.getCardState(cardId)
            assertNotEquals("Card state should no longer be NEW after chained review", CardState.NEW, state.state)
            assertTrue("Card stability should increase from 0", state.stability > 0.0)
            assertTrue("Card reps should be 1", state.reps >= 1)
        }

        // Verify today's progress reflects the batch
        val todayProgress = repository.computeTodayProgress()
        assertTrue("Today progress should count learned cards", todayProgress.todayNewLearned > 0 || todayProgress.todayReviewed > 0)
    }

    @Test
    fun test03_transitionBottlenecksCanBeRecordedAndRetrieved() {
        val articleId = "art_bx1_08" // 琵琶行
        repository.recordTransitionBottleneck(articleId, fromUnitIndex = 1, toUnitIndex = 2)
        repository.recordTransitionBottleneck(articleId, fromUnitIndex = 4, toUnitIndex = 5)

        val bottlenecks = repository.getTransitionBottlenecks(articleId)
        assertTrue("Should contain transition 1->2", bottlenecks.contains(Pair(1, 2)))
        assertTrue("Should contain transition 4->5", bottlenecks.contains(Pair(4, 5)))
        assertFalse("Should not contain unrecorded transition 2->3", bottlenecks.contains(Pair(2, 3)))
    }

    @Test
    fun test04_chainedBatchGracefullyHandlesEmptyInput() {
        val summary = repository.submitChainedRecitationBatch(emptyMap())
        assertEquals("Total units for empty batch should be 0", 0, summary.totalUnits)
        assertEquals("Smooth units count should be 0", 0, summary.smoothUnitsCount)
        assertEquals("Bottleneck units count should be 0", 0, summary.bottleneckUnitsCount)
        assertTrue("Updated cards should be empty", summary.updatedCards.isEmpty())
    }
}
