package com.ancient.wenyan.data

import android.content.Context
import android.content.SharedPreferences
import com.ancient.wenyan.domain.fsrs.CardFsrsState
import com.ancient.wenyan.domain.fsrs.CardState
import com.ancient.wenyan.domain.fsrs.FSRSEngine
import com.ancient.wenyan.domain.fsrs.Rating
import com.ancient.wenyan.domain.fsrs.ReviewLog
import com.ancient.wenyan.domain.model.Article
import com.ancient.wenyan.domain.model.ArticleProgress
import com.ancient.wenyan.domain.model.Flashcard
import com.ancient.wenyan.domain.model.Module
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.max

data class DeckStats(
    val totalCards: Int,
    val newCards: Int,
    val learningCards: Int,
    val reviewCards: Int,
    val dueCards: Int,
    val todayReviewedCount: Int,
    val retentionPercentage: Float
)

class WenYanRepository(
    private val context: Context? = null,
    val fsrsEngine: FSRSEngine = FSRSEngine()
) {
    private val cardsStateMap = mutableMapOf<String, CardFsrsState>()
    private val reviewLogs = mutableListOf<ReviewLog>()

    private val _statsFlow = MutableStateFlow(computeStats())
    val statsFlow: StateFlow<DeckStats> = _statsFlow.asStateFlow()

    init {
        initializeCards()
    }

    private fun initializeCards() {
        for (article in CurriculumDataSource.ALL_ARTICLES) {
            val flashcards = CurriculumDataSource.generateFlashcardsForArticle(article)
            for (fc in flashcards) {
                cardsStateMap[fc.id] = CardFsrsState(
                    cardId = fc.id,
                    state = CardState.NEW,
                    stability = 0.0,
                    difficulty = 0.0
                )
            }
        }
        _statsFlow.value = computeStats()
    }

    fun getModules(): List<Module> = CurriculumDataSource.MODULES

    fun getArticlesByModule(moduleId: String): List<Article> =
        CurriculumDataSource.getArticlesByModule(moduleId)

    fun getArticle(articleId: String): Article? =
        CurriculumDataSource.ARTICLE_MAP[articleId]

    fun getFlashcardsForArticle(articleId: String): List<Flashcard> {
        val article = getArticle(articleId) ?: return emptyList()
        return CurriculumDataSource.generateFlashcardsForArticle(article)
    }

    fun getCardState(cardId: String): CardFsrsState {
        return cardsStateMap[cardId] ?: CardFsrsState(cardId = cardId)
    }

    fun submitRating(
        cardId: String,
        rating: Rating,
        nowMillis: Long = System.currentTimeMillis()
    ): CardFsrsState {
        val currentState = getCardState(cardId)
        val result = fsrsEngine.evaluateReview(currentState, rating, nowMillis)
        cardsStateMap[cardId] = result.updatedCard
        reviewLogs.add(result.reviewLog)
        _statsFlow.value = computeStats(nowMillis)
        return result.updatedCard
    }

    fun getDueQueue(
        nowMillis: Long = System.currentTimeMillis(),
        dailyNewLimit: Int = 20
    ): List<Pair<Flashcard, CardFsrsState>> {
        val allFlashcards = CurriculumDataSource.ALL_ARTICLES.flatMap {
            CurriculumDataSource.generateFlashcardsForArticle(it)
        }.associateBy { it.id }

        val relearningQueue = cardsStateMap.values
            .filter { it.state == CardState.RELEARNING && it.dueTime <= nowMillis }
            .sortedBy { it.dueTime }

        val learningQueue = cardsStateMap.values
            .filter { it.state == CardState.LEARNING && it.dueTime <= nowMillis }
            .sortedBy { it.dueTime }

        val reviewQueue = cardsStateMap.values
            .filter { it.state == CardState.REVIEW && it.dueTime <= (nowMillis + 86_400_000L) }
            .sortedWith(compareByDescending<CardFsrsState> { it.lapses }.thenBy { it.dueTime })

        val newQueue = cardsStateMap.values
            .filter { it.state == CardState.NEW }
            .take(dailyNewLimit)

        val queueCards = (relearningQueue + learningQueue + reviewQueue + newQueue)
        return queueCards.mapNotNull { cardState ->
            val fc = allFlashcards[cardState.cardId]
            if (fc != null) Pair(fc, cardState) else null
        }
    }

    fun getRandomQueue(
        limit: Int = 20,
        moduleIds: Set<String>? = null
    ): List<Pair<Flashcard, CardFsrsState>> {
        val targetArticles = if (moduleIds.isNullOrEmpty()) {
            CurriculumDataSource.ALL_ARTICLES
        } else {
            CurriculumDataSource.ALL_ARTICLES.filter { it.moduleId in moduleIds }
        }

        val cards = targetArticles.flatMap {
            CurriculumDataSource.generateFlashcardsForArticle(it)
        }.shuffled().take(limit)

        return cards.map { fc ->
            Pair(fc, getCardState(fc.id))
        }
    }

    fun getArticleProgress(articleId: String): ArticleProgress {
        val flashcards = getFlashcardsForArticle(articleId)
        val total = flashcards.size
        var newCount = 0
        var learningCount = 0
        var reviewCount = 0

        for (fc in flashcards) {
            val state = getCardState(fc.id)
            when (state.state) {
                CardState.NEW -> newCount++
                CardState.LEARNING, CardState.RELEARNING -> learningCount++
                CardState.REVIEW -> reviewCount++
            }
        }

        val mastery = if (total > 0) (reviewCount.toFloat() / total.toFloat()) * 100.0f else 0.0f
        return ArticleProgress(
            articleId = articleId,
            totalCards = total,
            newCards = newCount,
            learningCards = learningCount,
            reviewCards = reviewCount,
            masteryPercentage = mastery
        )
    }

    fun computeStats(nowMillis: Long = System.currentTimeMillis()): DeckStats {
        val total = cardsStateMap.size
        var newCount = 0
        var learningCount = 0
        var reviewCount = 0
        var dueCount = 0

        for (card in cardsStateMap.values) {
            when (card.state) {
                CardState.NEW -> newCount++
                CardState.LEARNING -> {
                    learningCount++
                    if (card.dueTime <= nowMillis) dueCount++
                }
                CardState.RELEARNING -> {
                    learningCount++
                    if (card.dueTime <= nowMillis) dueCount++
                }
                CardState.REVIEW -> {
                    reviewCount++
                    if (card.dueTime <= (nowMillis + 86_400_000L)) dueCount++
                }
            }
        }

        val todayReviews = reviewLogs.count { it.reviewTime >= (nowMillis - 86_400_000L) }
        val retention = if (reviewLogs.isNotEmpty()) {
            val remembered = reviewLogs.count { it.rating != Rating.AGAIN }
            (remembered.toFloat() / reviewLogs.size.toFloat()) * 100.0f
        } else {
            100.0f
        }

        return DeckStats(
            totalCards = total,
            newCards = newCount,
            learningCards = learningCount,
            reviewCards = reviewCount,
            dueCards = dueCount,
            todayReviewedCount = todayReviews,
            retentionPercentage = retention
        )
    }

    companion object {
        @Volatile
        private var instance: WenYanRepository? = null

        fun getInstance(context: Context? = null): WenYanRepository {
            return instance ?: synchronized(this) {
                instance ?: WenYanRepository(context).also { instance = it }
            }
        }
    }
}
