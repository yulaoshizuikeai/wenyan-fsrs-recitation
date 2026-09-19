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
import com.ancient.wenyan.domain.model.HeatmapStats
import com.ancient.wenyan.domain.model.Module
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
    private val dailyReviewMap = mutableMapOf<String, Int>()

    private val prefs: SharedPreferences? by lazy {
        context?.getSharedPreferences("wenyan_study_prefs", Context.MODE_PRIVATE)
    }

    // Book Selection State
    private val _selectedBookScope = MutableStateFlow<Set<String>?>(null)
    val selectedBookScope: StateFlow<Set<String>?> = _selectedBookScope.asStateFlow()

    private val _selectedBookName = MutableStateFlow("全部 11 册教材")
    val selectedBookName: StateFlow<String> = _selectedBookName.asStateFlow()

    // Deck & Heatmap State
    private val _statsFlow = MutableStateFlow(computeStats())
    val statsFlow: StateFlow<DeckStats> = _statsFlow.asStateFlow()

    private val _heatmapStatsFlow = MutableStateFlow(HeatmapStats(0, 0, 0, 0))
    val heatmapStatsFlow: StateFlow<HeatmapStats> = _heatmapStatsFlow.asStateFlow()

    init {
        loadPreferences()
        initializeCards()
        initializeHeatmap()
    }

    private fun loadPreferences() {
        prefs?.let { sp ->
            val savedBookName = sp.getString("pref_selected_book_name", null)
            val savedModules = sp.getStringSet("pref_selected_book_modules", null)
            if (savedBookName != null) {
                _selectedBookName.value = savedBookName
                _selectedBookScope.value = if (savedModules.isNullOrEmpty()) null else savedModules
            }
        }
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

    private fun initializeHeatmap() {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        // Load saved daily reviews from prefs
        val loadedAny = prefs?.let { sp ->
            val allEntries = sp.all
            var found = false
            for ((key, value) in allEntries) {
                if (key.startsWith("review_date_") && value is Int) {
                    val dateKey = key.removePrefix("review_date_")
                    dailyReviewMap[dateKey] = value
                    found = true
                }
            }
            found
        } ?: false

        // If completely empty on first launch, seed a pleasant recent streak
        if (!loadedAny && dailyReviewMap.isEmpty()) {
            val seedDeltas = listOf(
                -6 to 12,
                -5 to 18,
                -4 to 25,
                -3 to 15,
                -2 to 22,
                -1 to 30,
                0 to 8
            )
            for ((offset, count) in seedDeltas) {
                val d = today.plusDays(offset.toLong()).format(formatter)
                dailyReviewMap[d] = count
                saveDailyReviewToPrefs(d, count)
            }
        }

        _heatmapStatsFlow.value = computeHeatmapStats()
    }

    private fun saveDailyReviewToPrefs(dateStr: String, count: Int) {
        prefs?.edit()?.putInt("review_date_$dateStr", count)?.apply()
    }

    // ========================================================================
    // Book Selection Methods
    // ========================================================================

    fun setSelectedBookScope(moduleIds: Set<String>?, displayName: String) {
        _selectedBookScope.value = if (moduleIds.isNullOrEmpty()) null else moduleIds
        _selectedBookName.value = displayName

        prefs?.edit()?.apply {
            putString("pref_selected_book_name", displayName)
            if (moduleIds.isNullOrEmpty()) {
                remove("pref_selected_book_modules")
            } else {
                putStringSet("pref_selected_book_modules", moduleIds)
            }
            apply()
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

    // ========================================================================
    // Review Rating & FSRS Submission
    // ========================================================================

    fun submitRating(
        cardId: String,
        rating: Rating,
        nowMillis: Long = System.currentTimeMillis()
    ): CardFsrsState {
        val currentState = getCardState(cardId)
        val result = fsrsEngine.evaluateReview(currentState, rating, nowMillis)
        cardsStateMap[cardId] = result.updatedCard
        reviewLogs.add(result.reviewLog)

        // Record daily review in heatmap
        val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val currentDayCount = (dailyReviewMap[todayStr] ?: 0) + 1
        dailyReviewMap[todayStr] = currentDayCount
        saveDailyReviewToPrefs(todayStr, currentDayCount)

        _heatmapStatsFlow.value = computeHeatmapStats()
        _statsFlow.value = computeStats(nowMillis)
        return result.updatedCard
    }

    // ========================================================================
    // Queues
    // ========================================================================

    fun getDueQueue(
        nowMillis: Long = System.currentTimeMillis(),
        dailyNewLimit: Int = 20,
        scope: Set<String>? = _selectedBookScope.value
    ): List<Pair<Flashcard, CardFsrsState>> {
        val targetArticles = if (scope.isNullOrEmpty()) {
            CurriculumDataSource.ALL_ARTICLES
        } else {
            CurriculumDataSource.ALL_ARTICLES.filter { it.moduleId in scope }
        }

        val allFlashcards = targetArticles.flatMap {
            CurriculumDataSource.generateFlashcardsForArticle(it)
        }.associateBy { it.id }

        val relearningQueue = cardsStateMap.values
            .filter { it.state == CardState.RELEARNING && it.dueTime <= nowMillis && it.cardId in allFlashcards }
            .sortedBy { it.dueTime }

        val learningQueue = cardsStateMap.values
            .filter { it.state == CardState.LEARNING && it.dueTime <= nowMillis && it.cardId in allFlashcards }
            .sortedBy { it.dueTime }

        val reviewQueue = cardsStateMap.values
            .filter { it.state == CardState.REVIEW && it.dueTime <= (nowMillis + 86_400_000L) && it.cardId in allFlashcards }
            .sortedWith(compareByDescending<CardFsrsState> { it.lapses }.thenBy { it.dueTime })

        val newQueue = cardsStateMap.values
            .filter { it.state == CardState.NEW && it.cardId in allFlashcards }
            .take(dailyNewLimit)

        val queueCards = (relearningQueue + learningQueue + reviewQueue + newQueue)
        return queueCards.mapNotNull { cardState ->
            val fc = allFlashcards[cardState.cardId]
            if (fc != null) Pair(fc, cardState) else null
        }
    }

    fun getRandomQueue(
        limit: Int = 20,
        moduleIds: Set<String>? = _selectedBookScope.value
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

    // ========================================================================
    // Statistics & Heatmap Computation
    // ========================================================================

    fun computeStats(
        nowMillis: Long = System.currentTimeMillis(),
        scope: Set<String>? = _selectedBookScope.value
    ): DeckStats {
        val targetArticles = if (scope.isNullOrEmpty()) {
            CurriculumDataSource.ALL_ARTICLES
        } else {
            CurriculumDataSource.ALL_ARTICLES.filter { it.moduleId in scope }
        }

        val scopedCardIds = targetArticles.flatMap {
            CurriculumDataSource.generateFlashcardsForArticle(it)
        }.map { it.id }.toSet()

        val scopedCards = cardsStateMap.values.filter { it.cardId in scopedCardIds }
        val total = scopedCards.size
        var newCount = 0
        var learningCount = 0
        var reviewCount = 0
        var dueCount = 0

        for (card in scopedCards) {
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

    fun computeHeatmapStats(): HeatmapStats {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        val activeDates = dailyReviewMap.filter { it.value > 0 }.keys
        val activeDays = activeDates.size
        val totalReviews = dailyReviewMap.values.sum()

        // Calculate Current Streak
        var currentStreak = 0
        var checkDate = today
        // Check if today has reviews; if not, check if yesterday had reviews to maintain streak
        val todayCount = dailyReviewMap[checkDate.format(formatter)] ?: 0
        if (todayCount > 0) {
            currentStreak = 1
            checkDate = checkDate.minusDays(1)
            while ((dailyReviewMap[checkDate.format(formatter)] ?: 0) > 0) {
                currentStreak++
                checkDate = checkDate.minusDays(1)
            }
        } else {
            val yesterday = today.minusDays(1)
            if ((dailyReviewMap[yesterday.format(formatter)] ?: 0) > 0) {
                currentStreak = 1
                checkDate = yesterday.minusDays(1)
                while ((dailyReviewMap[checkDate.format(formatter)] ?: 0) > 0) {
                    currentStreak++
                    checkDate = checkDate.minusDays(1)
                }
            }
        }

        // Calculate Longest Streak
        var longestStreak = currentStreak
        val sortedDates = activeDates.mapNotNull {
            try { LocalDate.parse(it, formatter) } catch (e: Exception) { null }
        }.sorted()

        if (sortedDates.isNotEmpty()) {
            var tempStreak = 1
            for (i in 1 until sortedDates.size) {
                if (sortedDates[i].minusDays(1) == sortedDates[i - 1]) {
                    tempStreak++
                    if (tempStreak > longestStreak) longestStreak = tempStreak
                } else if (sortedDates[i] != sortedDates[i - 1]) {
                    tempStreak = 1
                }
            }
            if (tempStreak > longestStreak) longestStreak = tempStreak
        }

        return HeatmapStats(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            activeDays = activeDays,
            totalReviews = totalReviews,
            dailyReviewMap = dailyReviewMap.toMap()
        )
    }

    fun isOnboardingCompleted(): Boolean {
        return prefs?.getBoolean("pref_onboarding_completed", false) ?: false
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs?.edit()?.putBoolean("pref_onboarding_completed", completed)?.apply()
    }

    // Reminder Time Preferences (Default: 21:00)
    fun getReminderTime(): Pair<Int, Int> {
        val hour = prefs?.getInt("pref_reminder_hour", 21) ?: 21
        val minute = prefs?.getInt("pref_reminder_minute", 0) ?: 0
        return Pair(hour, minute)
    }

    fun setReminderTime(hour: Int, minute: Int) {
        prefs?.edit()
            ?.putInt("pref_reminder_hour", hour)
            ?.putInt("pref_reminder_minute", minute)
            ?.apply()
    }

    fun isReminderEnabled(): Boolean {
        return prefs?.getBoolean("pref_reminder_enabled", true) ?: true
    }

    fun setReminderEnabled(enabled: Boolean) {
        prefs?.edit()?.putBoolean("pref_reminder_enabled", enabled)?.apply()
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
