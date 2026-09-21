package com.ancient.wenyan.data

import android.content.Context
import android.content.SharedPreferences
import com.ancient.wenyan.domain.fsrs.CardFsrsState
import com.ancient.wenyan.domain.fsrs.CardState
import com.ancient.wenyan.domain.fsrs.FSRSEngine
import com.ancient.wenyan.domain.fsrs.FSRSOptimizer
import com.ancient.wenyan.domain.fsrs.OptimizationResult
import com.ancient.wenyan.domain.fsrs.Rating
import com.ancient.wenyan.domain.fsrs.ReviewLog
import com.ancient.wenyan.domain.model.Article
import com.ancient.wenyan.domain.model.ArticleProgress
import com.ancient.wenyan.domain.model.Flashcard
import com.ancient.wenyan.domain.model.ActiveSession
import com.ancient.wenyan.domain.model.HeatmapStats
import com.ancient.wenyan.domain.model.Module
import com.ancient.wenyan.domain.model.RecitationOrderMode
import com.ancient.wenyan.domain.model.StudyGoalsConfig
import com.ancient.wenyan.domain.model.StudyOrderPreference
import com.ancient.wenyan.domain.model.TodayStudyProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val cardsStateMap = java.util.concurrent.ConcurrentHashMap<String, CardFsrsState>()
    private val reviewLogs = java.util.concurrent.CopyOnWriteArrayList<ReviewLog>()
    private val dailyReviewMap = java.util.concurrent.ConcurrentHashMap<String, Int>()

    private val prefs: SharedPreferences? by lazy {
        context?.getSharedPreferences("wenyan_study_prefs", Context.MODE_PRIVATE)
    }

    // Active In-Progress Recitation Session (Anki-style breakpoint continuation)
    private val _activeSessionFlow = MutableStateFlow<ActiveSession?>(null)
    val activeSessionFlow: StateFlow<ActiveSession?> = _activeSessionFlow.asStateFlow()

    // Book Selection State
    private val _selectedBookScope = MutableStateFlow<Set<String>?>(null)
    val selectedBookScope: StateFlow<Set<String>?> = _selectedBookScope.asStateFlow()

    private val _selectedBookName = MutableStateFlow("全部 11 册教材")
    val selectedBookName: StateFlow<String> = _selectedBookName.asStateFlow()

    // Recitation Ordering State (Sequential by default to protect poem context)
    private val _recitationOrderMode = MutableStateFlow(RecitationOrderMode.SEQUENTIAL)
    val recitationOrderMode: StateFlow<RecitationOrderMode> = _recitationOrderMode.asStateFlow()

    // Daily Study Goals (Anki-style new cards & review limits)
    private val _studyGoalsConfig = MutableStateFlow(StudyGoalsConfig())
    val studyGoalsConfig: StateFlow<StudyGoalsConfig> = _studyGoalsConfig.asStateFlow()

    private val _todayStudyProgressFlow = MutableStateFlow(TodayStudyProgress())
    val todayStudyProgressFlow: StateFlow<TodayStudyProgress> = _todayStudyProgressFlow.asStateFlow()

    fun setRecitationOrderMode(mode: RecitationOrderMode) {
        _recitationOrderMode.value = mode
        prefs?.edit()?.putString("pref_recitation_order_mode", mode.name)?.apply()
    }

    fun getStudyGoalsConfig(): StudyGoalsConfig = _studyGoalsConfig.value

    fun setStudyGoalsConfig(config: StudyGoalsConfig) {
        _studyGoalsConfig.value = config
        prefs?.edit()?.apply {
            putInt("pref_daily_new_cards_limit", config.dailyNewLimit)
            putInt("pref_daily_review_cards_limit", config.dailyReviewLimit)
            putString("pref_study_order_pref", config.orderPreference.name)
            apply()
        }
        _todayStudyProgressFlow.value = computeTodayProgress()
    }

    fun setDailyNewCardsLimit(limit: Int) {
        val updated = _studyGoalsConfig.value.copy(dailyNewLimit = limit)
        setStudyGoalsConfig(updated)
    }

    fun setDailyReviewCardsLimit(limit: Int) {
        val updated = _studyGoalsConfig.value.copy(dailyReviewLimit = limit)
        setStudyGoalsConfig(updated)
    }

    fun setStudyOrderPreference(pref: StudyOrderPreference) {
        val updated = _studyGoalsConfig.value.copy(orderPreference = pref)
        setStudyGoalsConfig(updated)
    }

    // Deck & Heatmap State
    private val _statsFlow = MutableStateFlow(computeStats())
    val statsFlow: StateFlow<DeckStats> = _statsFlow.asStateFlow()

    private val _heatmapStatsFlow = MutableStateFlow(HeatmapStats(0, 0, 0, 0))
    val heatmapStatsFlow: StateFlow<HeatmapStats> = _heatmapStatsFlow.asStateFlow()

    init {
        loadPreferences()
        initializeCards()
        loadPersistedCardStates()
        initializeHeatmap()
        loadPersistedActiveSession()
        _todayStudyProgressFlow.value = computeTodayProgress()
    }

    private fun loadPreferences() {
        prefs?.let { sp ->
            val savedBookName = sp.getString("pref_selected_book_name", null)
            val savedModules = sp.getStringSet("pref_selected_book_modules", null)
            if (savedBookName != null) {
                _selectedBookName.value = savedBookName
                _selectedBookScope.value = if (savedModules.isNullOrEmpty()) null else savedModules
            }

            val savedOrderMode = sp.getString("pref_recitation_order_mode", RecitationOrderMode.SEQUENTIAL.name)
            _recitationOrderMode.value = try {
                RecitationOrderMode.valueOf(savedOrderMode ?: RecitationOrderMode.SEQUENTIAL.name)
            } catch (e: Exception) {
                RecitationOrderMode.SEQUENTIAL
            }

            // Load daily study goals
            val savedNewLimit = sp.getInt("pref_daily_new_cards_limit", 20)
            val savedReviewLimit = sp.getInt("pref_daily_review_cards_limit", 100)
            val savedOrderPrefStr = sp.getString("pref_study_order_pref", StudyOrderPreference.REVIEW_FIRST.name)
            val savedOrderPref = try {
                StudyOrderPreference.valueOf(savedOrderPrefStr ?: StudyOrderPreference.REVIEW_FIRST.name)
            } catch (_: Exception) {
                StudyOrderPreference.REVIEW_FIRST
            }
            _studyGoalsConfig.value = StudyGoalsConfig(
                dailyNewLimit = savedNewLimit,
                dailyReviewLimit = savedReviewLimit,
                orderPreference = savedOrderPref
            )

            // Load custom/optimized FSRS parameters
            val savedRetention = sp.getFloat("pref_fsrs_retention", 0.93f).toDouble()
            val savedFactor = sp.getFloat("pref_fsrs_recitation_factor", 0.72f).toDouble()
            val savedMaxInterval = sp.getInt("pref_fsrs_max_interval", 36500)
            val savedWeightsStr = sp.getString("pref_fsrs_weights", null)

            val weights = if (!savedWeightsStr.isNullOrBlank()) {
                try {
                    val arr = savedWeightsStr.split(",").map { it.trim().toDouble() }.toDoubleArray()
                    if (arr.size == 19) arr else FSRSEngine.DEFAULT_FSRS_5_WEIGHTS
                } catch (e: Exception) {
                    FSRSEngine.DEFAULT_FSRS_5_WEIGHTS
                }
            } else {
                FSRSEngine.DEFAULT_FSRS_5_WEIGHTS
            }

            fsrsEngine.updateParameters(weights, savedRetention, savedFactor, savedMaxInterval)

            // Load stored review logs
            val savedLogsStr = sp.getString("pref_persisted_review_logs_v1", null)
            if (!savedLogsStr.isNullOrBlank()) {
                try {
                    val lines = savedLogsStr.split("\n")
                    for (line in lines) {
                        val p = line.split("|")
                        if (p.size >= 8) {
                            reviewLogs.add(
                                ReviewLog(
                                    cardId = p[0],
                                    rating = Rating.valueOf(p[1]),
                                    previousState = CardState.valueOf(p[2]),
                                    currentState = CardState.valueOf(p[3]),
                                    stability = p[4].toDoubleOrNull() ?: 1.0,
                                    difficulty = p[5].toDoubleOrNull() ?: 5.0,
                                    elapsedDays = p[6].toIntOrNull() ?: 0,
                                    scheduledDays = p[7].toIntOrNull() ?: 1,
                                    reviewTime = p.getOrNull(8)?.toLongOrNull() ?: System.currentTimeMillis()
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    // Ignore parse errors
                }
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
        // Clean up legacy mock seed data if previously written
        prefs?.let { sp ->
            val hasCleaned = sp.getBoolean("has_cleaned_legacy_fake_seed_v1", false)
            if (!hasCleaned) {
                val editor = sp.edit()
                // Clear any pre-seeded fake entries
                for (key in sp.all.keys) {
                    if (key.startsWith("review_date_")) {
                        editor.remove(key)
                    }
                }
                editor.putBoolean("has_cleaned_legacy_fake_seed_v1", true)
                editor.apply()
            }
        }

        // Load genuine saved daily reviews from prefs
        prefs?.let { sp ->
            val allEntries = sp.all
            for ((key, value) in allEntries) {
                if (key.startsWith("review_date_") && value is Int) {
                    val dateKey = key.removePrefix("review_date_")
                    dailyReviewMap[dateKey] = value
                }
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
        val wasNew = currentState.state == CardState.NEW
        val result = fsrsEngine.evaluateReview(currentState, rating, nowMillis)
        synchronized(cardsStateMap) {
            cardsStateMap[cardId] = result.updatedCard
        }
        synchronized(reviewLogs) {
            reviewLogs.add(result.reviewLog)
        }
        persistReviewLogs()
        persistCardStates()

        // Auto-tune every 20 reviews asynchronously in background if enabled and sufficient logs
        val logCount = synchronized(reviewLogs) { reviewLogs.size }
        if (isAutoTuneEnabled() && logCount >= 10 && logCount % 20 == 0) {
            repositoryScope.launch {
                optimizeFSRSParameters()
            }
        }

        // Record daily review in heatmap
        val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val currentDayCount = synchronized(dailyReviewMap) {
            val count = (dailyReviewMap[todayStr] ?: 0) + 1
            dailyReviewMap[todayStr] = count
            count
        }
        saveDailyReviewToPrefs(todayStr, currentDayCount)

        // Record daily new/review count for Anki-style progress tracking
        prefs?.let { sp ->
            if (wasNew) {
                val newCount = sp.getInt("pref_daily_new_learned_$todayStr", 0) + 1
                sp.edit().putInt("pref_daily_new_learned_$todayStr", newCount).apply()
            } else {
                val revCount = sp.getInt("pref_daily_reviewed_$todayStr", 0) + 1
                sp.edit().putInt("pref_daily_reviewed_$todayStr", revCount).apply()
            }
        }
        _todayStudyProgressFlow.value = computeTodayProgress()

        _heatmapStatsFlow.value = computeHeatmapStats()
        _statsFlow.value = computeStats(nowMillis)
        return result.updatedCard
    }

    fun computeTodayProgress(): TodayStudyProgress {
        val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val newCount = prefs?.getInt("pref_daily_new_learned_$todayStr", 0) ?: 0
        val revCount = prefs?.getInt("pref_daily_reviewed_$todayStr", 0) ?: 0
        val config = _studyGoalsConfig.value
        return TodayStudyProgress(
            todayNewLearned = newCount,
            targetNew = config.dailyNewLimit,
            todayReviewed = revCount,
            targetReview = config.dailyReviewLimit
        )
    }

    // ========================================================================
    // Queues
    // ========================================================================

    fun getDueQueue(
        nowMillis: Long = System.currentTimeMillis(),
        dailyNewLimit: Int = _studyGoalsConfig.value.dailyNewLimit,
        dailyReviewLimit: Int = _studyGoalsConfig.value.dailyReviewLimit,
        scope: Set<String>? = _selectedBookScope.value,
        orderMode: RecitationOrderMode = _recitationOrderMode.value,
        orderPref: StudyOrderPreference = _studyGoalsConfig.value.orderPreference
    ): List<Pair<Flashcard, CardFsrsState>> {
        val targetArticles = if (scope.isNullOrEmpty()) {
            CurriculumDataSource.ALL_ARTICLES
        } else {
            CurriculumDataSource.ALL_ARTICLES.filter { it.moduleId in scope }
        }

        val allFlashcardsList = targetArticles.flatMap {
            CurriculumDataSource.generateFlashcardsForArticle(it)
        }
        val allFlashcards = allFlashcardsList.associateBy { it.id }

        val zone = java.time.ZoneId.systemDefault()
        val nowDate = java.time.Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val endOfTodayMillis = nowDate.atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()

        val relearningQueue = cardsStateMap.values
            .filter { it.state == CardState.RELEARNING && it.dueTime <= nowMillis && it.cardId in allFlashcards }
            .sortedBy { it.dueTime }

        val learningQueue = cardsStateMap.values
            .filter { it.state == CardState.LEARNING && it.dueTime <= nowMillis && it.cardId in allFlashcards }
            .sortedBy { it.dueTime }

        val reviewQueue = cardsStateMap.values
            .filter { it.state == CardState.REVIEW && it.dueTime <= endOfTodayMillis && it.cardId in allFlashcards }
            .sortedWith(compareByDescending<CardFsrsState> { it.lapses }.thenBy { it.dueTime })

        // Apply dailyReviewLimit ONLY to graduated review cards (CardState.REVIEW)
        // According to FSRS/Anki specifications, intraday learning (LEARNING) and relearning (RELEARNING)
        // are step-based queues and must not be truncated by dailyReviewLimit.
        val limitedReviewQueue = when {
            dailyReviewLimit <= 0 -> emptyList()
            dailyReviewLimit >= 999 -> reviewQueue
            else -> reviewQueue.take(dailyReviewLimit)
        }
        val allReviewCards = relearningQueue + learningQueue + limitedReviewQueue

        // Apply dailyNewLimit to new cards
        val allNewCards = cardsStateMap.values
            .filter { it.state == CardState.NEW && it.cardId in allFlashcards }
        val newQueue = when {
            dailyNewLimit <= 0 -> emptyList()
            dailyNewLimit >= 999 -> allNewCards
            else -> allNewCards.take(dailyNewLimit)
        }

        val candidateCards = when (orderPref) {
            StudyOrderPreference.REVIEW_FIRST -> (allReviewCards + newQueue)
            StudyOrderPreference.NEW_FIRST -> (newQueue + allReviewCards)
            StudyOrderPreference.MIXED -> interleaveLists(allReviewCards, newQueue)
        }.mapNotNull { cardState ->
            val fc = allFlashcards[cardState.cardId]
            if (fc != null) Pair(fc, cardState) else null
        }

        return when (orderMode) {
            RecitationOrderMode.SRS_PRIORITY -> {
                candidateCards
            }
            RecitationOrderMode.RANDOM_SHUFFLE -> {
                candidateCards.shuffled()
            }
            RecitationOrderMode.SEQUENTIAL -> {
                // "篇章聚类 + 篇内原序": 严格按原文篇章正序排布，维护诗文语脉与韵律
                val articleOrderMap = targetArticles.mapIndexed { idx, it -> it.id to idx }.toMap()
                val groupedByArticle = candidateCards.groupBy { it.first.articleId }

                // 篇目间按策略与紧迫度排布
                val sortedArticles = groupedByArticle.keys.sortedWith(
                    when (orderPref) {
                        StudyOrderPreference.MIXED -> {
                            // 自然混合顺承排布：直接遵循课本篇目编排原序
                            compareBy { articleId ->
                                articleOrderMap[articleId] ?: 0
                            }
                        }
                        StudyOrderPreference.REVIEW_FIRST -> {
                            compareBy<String> { articleId ->
                                val cards = groupedByArticle[articleId] ?: emptyList()
                                val hasReview = cards.any { it.second.state != CardState.NEW }
                                when {
                                    cards.any { it.second.state == CardState.RELEARNING } -> 0
                                    cards.any { it.second.state == CardState.LEARNING } -> 1
                                    cards.any { it.second.state == CardState.REVIEW && it.second.dueTime <= nowMillis } -> 2
                                    hasReview -> 3
                                    else -> 4 // 纯新课篇目靠后
                                }
                            }.thenBy { articleId ->
                                val cards = groupedByArticle[articleId] ?: emptyList()
                                cards.filter { it.second.state != CardState.NEW }
                                    .minOfOrNull { it.second.dueTime } ?: Long.MAX_VALUE
                            }.thenBy { articleId ->
                                articleOrderMap[articleId] ?: 0
                            }
                        }
                        StudyOrderPreference.NEW_FIRST -> {
                            compareBy<String> { articleId ->
                                val cards = groupedByArticle[articleId] ?: emptyList()
                                val hasReview = cards.any { it.second.state != CardState.NEW }
                                val hasNew = cards.any { it.second.state == CardState.NEW }
                                when {
                                    hasNew && !hasReview -> 0 // 纯新课优先
                                    hasNew -> 1
                                    else -> 2
                                }
                            }.thenBy { articleId ->
                                articleOrderMap[articleId] ?: 0
                            }
                        }
                    }
                )

                // 篇目内部严格按 unitIndex 与 clozeIndex 从首句到尾句正序推进
                sortedArticles.flatMap { articleId ->
                    val cardsInArticle = groupedByArticle[articleId] ?: emptyList()
                    cardsInArticle.sortedWith(
                        compareBy<Pair<Flashcard, CardFsrsState>> { it.first.unitIndex }
                            .thenBy { it.first.clozeIndex }
                    )
                }
            }
        }
    }

    private fun <T> interleaveLists(listA: List<T>, listB: List<T>): List<T> {
        if (listA.isEmpty()) return listB
        if (listB.isEmpty()) return listA
        val result = ArrayList<T>(listA.size + listB.size)
        var idxA = 0
        var idxB = 0
        val total = listA.size + listB.size
        for (i in 0 until total) {
            val takeFromA = if (idxA < listA.size && idxB < listB.size) {
                (idxA.toDouble() / listA.size) <= (idxB.toDouble() / listB.size)
            } else {
                idxA < listA.size
            }
            if (takeFromA) {
                result.add(listA[idxA++])
            } else {
                result.add(listB[idxB++])
            }
        }
        return result
    }

    fun getRandomQueue(
        limit: Int = 20,
        moduleIds: Set<String>? = _selectedBookScope.value,
        gaoKaoOnly: Boolean = false,
        preservePoemOrder: Boolean = true
    ): List<Pair<Flashcard, CardFsrsState>> {
        val baseArticles = if (moduleIds.isNullOrEmpty()) {
            CurriculumDataSource.ALL_ARTICLES
        } else {
            CurriculumDataSource.ALL_ARTICLES.filter { it.moduleId in moduleIds }
        }
        val targetArticles = if (gaoKaoOnly) {
            baseArticles.filter { it.isGaoKao72 }
        } else {
            baseArticles
        }

        val allCards = targetArticles.flatMap {
            CurriculumDataSource.generateFlashcardsForArticle(it)
        }

        if (allCards.isEmpty()) return emptyList()

        val sampledCards = allCards.shuffled().take(limit)

        if (!preservePoemOrder) {
            return sampledCards.map { Pair(it, getCardState(it.id)) }
        }

        // 顺承原序：抽取的卡片按篇目归拢，且篇内严格按原文先后次序排列
        val articleOrderMap = targetArticles.mapIndexed { idx, it -> it.id to idx }.toMap()
        val sortedCards = sampledCards.sortedWith(
            compareBy<Flashcard> { articleOrderMap[it.articleId] ?: 0 }
                .thenBy { it.unitIndex }
                .thenBy { it.clozeIndex }
        )

        return sortedCards.map { fc ->
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
                    val zone = java.time.ZoneId.systemDefault()
                    val nowDate = java.time.Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
                    val endOfTodayMillis = nowDate.atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()
                    if (card.dueTime <= endOfTodayMillis) dueCount++
                }
            }
        }

        val zone = java.time.ZoneId.systemDefault()
        val nowDate = java.time.Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val startOfTodayMillis = nowDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val todayReviews = reviewLogs.count { it.reviewTime >= startOfTodayMillis }
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

    private fun persistReviewLogs() {
        prefs?.let { sp ->
            val recentLogs = reviewLogs.takeLast(500)
            val sb = StringBuilder()
            for (log in recentLogs) {
                sb.append("${log.cardId}|${log.rating.name}|${log.previousState.name}|${log.currentState.name}|${log.stability}|${log.difficulty}|${log.elapsedDays}|${log.scheduledDays}|${log.reviewTime}\n")
            }
            sp.edit().putString("pref_persisted_review_logs_v1", sb.toString()).apply()
        }
    }

    private fun persistCardStates() {
        prefs?.let { sp ->
            val nonNewCards = synchronized(cardsStateMap) {
                cardsStateMap.values.filter {
                    it.state != CardState.NEW || it.reps > 0 || it.lapses > 0 || it.lastReviewTime != null
                }
            }
            val sb = StringBuilder()
            for (c in nonNewCards) {
                sb.append("${c.cardId}|${c.state.name}|${c.step ?: ""}|${c.stability}|${c.difficulty}|${c.elapsedDays}|${c.scheduledDays}|${c.reps}|${c.lapses}|${c.lastReviewTime ?: ""}|${c.dueTime}\n")
            }
            sp.edit().putString(PREF_CARD_STATES_KEY, sb.toString()).apply()
        }
    }

    private fun loadPersistedCardStates() {
        prefs?.let { sp ->
            val savedStr = sp.getString(PREF_CARD_STATES_KEY, null) ?: return
            if (savedStr.isBlank()) return
            val lines = savedStr.split("\n")
            var restoredCount = 0
            for (line in lines) {
                if (line.isBlank()) continue
                val parts = line.split("|")
                if (parts.size >= 11) {
                    try {
                        val cardId = parts[0]
                        val state = CardState.valueOf(parts[1])
                        val step = parts[2].toIntOrNull()
                        val stability = parts[3].toDoubleOrNull() ?: 0.0
                        val difficulty = parts[4].toDoubleOrNull() ?: 0.0
                        val elapsedDays = parts[5].toIntOrNull() ?: 0
                        val scheduledDays = parts[6].toIntOrNull() ?: 0
                        val reps = parts[7].toIntOrNull() ?: 0
                        val lapses = parts[8].toIntOrNull() ?: 0
                        val lastReviewTime = parts[9].toLongOrNull()
                        val dueTime = parts[10].toLongOrNull() ?: 0L

                        cardsStateMap[cardId] = CardFsrsState(
                            cardId = cardId,
                            state = state,
                            step = step,
                            stability = stability,
                            difficulty = difficulty,
                            elapsedDays = elapsedDays,
                            scheduledDays = scheduledDays,
                            reps = reps,
                            lapses = lapses,
                            lastReviewTime = lastReviewTime,
                            dueTime = dueTime
                        )
                        restoredCount++
                    } catch (e: Exception) {
                        // Ignore parse error on single corrupted card line
                    }
                }
            }
            if (restoredCount > 0) {
                _statsFlow.value = computeStats()
            }
        }
    }

    fun clearPersistedCardStates() {
        prefs?.edit()?.remove(PREF_CARD_STATES_KEY)?.apply()
        initializeCards()
    }

    // ========================================================================
    // Active In-Progress Recitation Session (Anki-style breakpoint continuation)
    // ========================================================================

    fun saveActiveSession(session: ActiveSession) {
        if (session.isComplete) {
            clearActiveSession()
            return
        }
        _activeSessionFlow.value = session
        prefs?.let { sp ->
            val cardIdsJoined = session.cardIds.joinToString(",")
            val encodedTitle = try { java.net.URLEncoder.encode(session.title, "UTF-8") } catch (_: Exception) { session.title }
            val encoded = "${session.id}|$encodedTitle|${session.sessionType}|${session.currentIndex}|${session.completedCount}|${session.totalCards}|${session.lastActiveMillis}|$cardIdsJoined"
            sp.edit().putString(PREF_ACTIVE_SESSION_KEY, encoded).apply()
        }
    }

    fun clearActiveSession() {
        _activeSessionFlow.value = null
        prefs?.edit()?.remove(PREF_ACTIVE_SESSION_KEY)?.apply()
    }

    fun getActiveSession(): ActiveSession? = _activeSessionFlow.value

    fun restoreCardsForSession(session: ActiveSession): List<Pair<Flashcard, CardFsrsState>> {
        return session.cardIds.mapNotNull { cardId ->
            val fc = CurriculumDataSource.getFlashcard(cardId)
            if (fc != null) Pair(fc, getCardState(cardId)) else null
        }
    }

    private fun loadPersistedActiveSession() {
        prefs?.let { sp ->
            val encoded = sp.getString(PREF_ACTIVE_SESSION_KEY, null) ?: return
            if (encoded.isBlank()) return
            try {
                val parts = encoded.split("|")
                if (parts.size >= 8) {
                    val id = parts[0]
                    val rawTitle = parts[1]
                    val title = try { java.net.URLDecoder.decode(rawTitle, "UTF-8") } catch (_: Exception) { rawTitle }
                    val sessionType = parts[2]
                    val currentIndex = parts[3].toIntOrNull() ?: 0
                    val completedCount = parts[4].toIntOrNull() ?: 0
                    val totalCards = parts[5].toIntOrNull() ?: 0
                    val lastActive = parts[6].toLongOrNull() ?: System.currentTimeMillis()
                    val cardIds = if (parts[7].isBlank()) emptyList() else parts[7].split(",").filter { it.isNotBlank() }
                    val session = ActiveSession(
                        id = id,
                        title = title,
                        sessionType = sessionType,
                        cardIds = cardIds,
                        currentIndex = currentIndex,
                        completedCount = completedCount,
                        totalCards = totalCards,
                        lastActiveMillis = lastActive
                    )
                    if (!session.isComplete) {
                        _activeSessionFlow.value = session
                    }
                }
            } catch (e: Exception) {
                // Ignore parse error on corrupted session
            }
        }
    }

    // ========================================================================
    // FSRS Parameter Optimization & Settings
    // ========================================================================

    suspend fun optimizeFSRSParameters(): OptimizationResult = withContext(Dispatchers.Default) {
        val logs = synchronized(reviewLogs) { reviewLogs.toList() }
        val result = FSRSOptimizer.optimize(logs, fsrsEngine.weights)
        if (result.success) {
            fsrsEngine.updateParameters(newWeights = result.optimizedWeights)
            saveFSRSSettingsToPrefs(
                weights = result.optimizedWeights,
                retention = fsrsEngine.requestRetention,
                factor = fsrsEngine.recitationStabilityFactor,
                maxInterval = fsrsEngine.maximumInterval,
                lastOptimized = System.currentTimeMillis()
            )
            _statsFlow.value = computeStats()
        }
        result
    }

    fun updateFSRSSettings(
        weights: DoubleArray? = null,
        retention: Double,
        factor: Double,
        maxInterval: Int
    ) {
        val targetWeights = weights ?: fsrsEngine.weights
        fsrsEngine.updateParameters(targetWeights, retention, factor, maxInterval)
        saveFSRSSettingsToPrefs(
            weights = targetWeights,
            retention = retention,
            factor = factor,
            maxInterval = maxInterval
        )
        _statsFlow.value = computeStats()
    }

    fun resetFSRSSettingsToDefault() {
        fsrsEngine.resetToDefaults()
        saveFSRSSettingsToPrefs(
            weights = FSRSEngine.DEFAULT_FSRS_5_WEIGHTS,
            retention = 0.93,
            factor = 0.72,
            maxInterval = 36500
        )
        _statsFlow.value = computeStats()
    }

    private fun saveFSRSSettingsToPrefs(
        weights: DoubleArray,
        retention: Double,
        factor: Double,
        maxInterval: Int,
        lastOptimized: Long? = null
    ) {
        prefs?.edit()?.apply {
            putString("pref_fsrs_weights", weights.joinToString(","))
            putFloat("pref_fsrs_retention", retention.toFloat())
            putFloat("pref_fsrs_recitation_factor", factor.toFloat())
            putInt("pref_fsrs_max_interval", maxInterval)
            if (lastOptimized != null) {
                putLong("pref_fsrs_last_optimized_time", lastOptimized)
            }
            apply()
        }
    }

    fun getReviewLogs(): List<ReviewLog> = reviewLogs.toList()

    fun isAutoTuneEnabled(): Boolean {
        return prefs?.getBoolean("pref_fsrs_auto_tune", true) ?: true
    }

    fun setAutoTuneEnabled(enabled: Boolean) {
        prefs?.edit()?.putBoolean("pref_fsrs_auto_tune", enabled)?.apply()
    }

    fun getLastOptimizedTime(): Long? {
        val t = prefs?.getLong("pref_fsrs_last_optimized_time", 0L) ?: 0L
        return if (t > 0L) t else null
    }

    internal fun setCardStateForTesting(state: CardFsrsState) {
        cardsStateMap[state.cardId] = state
    }

    companion object {
        private const val PREF_CARD_STATES_KEY = "pref_persisted_card_states_v2"
        private const val PREF_ACTIVE_SESSION_KEY = "pref_active_recitation_session_v1"

        @Volatile
        private var instance: WenYanRepository? = null

        fun getInstance(context: Context? = null): WenYanRepository {
            return instance ?: synchronized(this) {
                instance ?: WenYanRepository(context?.applicationContext).also { instance = it }
            }
        }
    }
}
