package com.ancient.wenyan

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.ancient.wenyan.data.CurriculumDataSource
import com.ancient.wenyan.data.WenYanRepository
import com.ancient.wenyan.domain.fsrs.CardFsrsState
import com.ancient.wenyan.domain.model.Article
import com.ancient.wenyan.domain.model.Flashcard
import com.ancient.wenyan.ui.screens.*
import com.ancient.wenyan.ui.sound.HapticManager
import com.ancient.wenyan.ui.sound.SoundEffectManager
import com.ancient.wenyan.ui.theme.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class MainTab(
    val title: String,
    val icon: ImageVector
) {
    TODAY("今日背诵", Icons.Default.Home),
    LIBRARY("篇目文库", Icons.AutoMirrored.Filled.MenuBook),
    PRACTICE("专项练习", Icons.Default.Shuffle),
    FOOTPRINT("研墨足迹", Icons.Default.CalendarMonth)
}

sealed class OverlayScreen {
    data class Flashcards(
        val title: String,
        val cards: List<Pair<Flashcard, CardFsrsState>>,
        val initialIndex: Int = 0,
        val initialCompletedCount: Int = 0,
        val sessionId: String = "session_default",
        val sessionType: String = "GENERAL"
    ) : OverlayScreen()
    data class Cloze(val article: Article) : OverlayScreen()
    data object Settings : OverlayScreen()
    data object GaoKaoScenario : OverlayScreen()
    data class SnowballRecitation(val articleId: String = "art_chibifu") : OverlayScreen()
    data class CertificateAndCopybook(val articleId: String = "art_chibifu") : OverlayScreen()
}

// State holder for OverlayScreen to preserve recitation and settings overlay state across configuration changes (Bug 4.2)
object OverlayScreenStateHolder {
    fun save(screen: OverlayScreen?, repository: WenYanRepository): Any? {
        return when (screen) {
            null -> null
            is OverlayScreen.Settings -> arrayListOf("SETTINGS")
            is OverlayScreen.GaoKaoScenario -> arrayListOf("GAOKAO_SCENARIO")
            is OverlayScreen.SnowballRecitation -> arrayListOf("SNOWBALL", screen.articleId)
            is OverlayScreen.CertificateAndCopybook -> arrayListOf("CERTIFICATE", screen.articleId)
            is OverlayScreen.Cloze -> arrayListOf("CLOZE", screen.article.id)
            is OverlayScreen.Flashcards -> {
                val active = repository.getActiveSession()
                val (idx, count, cardIds) = if (active != null && active.id == screen.sessionId) {
                    Triple(active.currentIndex, active.completedCount, active.cardIds)
                } else {
                    Triple(screen.initialIndex, screen.initialCompletedCount, screen.cards.map { it.first.id })
                }
                arrayListOf(
                    "FLASHCARDS",
                    screen.title,
                    screen.sessionId,
                    screen.sessionType,
                    idx,
                    count,
                    ArrayList(cardIds)
                )
            }
        }
    }

    fun restore(saved: Any?, repository: WenYanRepository): OverlayScreen? {
        return when (saved) {
            is List<*> -> {
                when (saved.getOrNull(0) as? String) {
                    "SETTINGS" -> OverlayScreen.Settings
                    "GAOKAO_SCENARIO" -> OverlayScreen.GaoKaoScenario
                    "SNOWBALL" -> {
                        val articleId = saved.getOrNull(1) as? String ?: "art_chibifu"
                        OverlayScreen.SnowballRecitation(articleId)
                    }
                    "CERTIFICATE" -> {
                        val articleId = saved.getOrNull(1) as? String ?: "art_chibifu"
                        OverlayScreen.CertificateAndCopybook(articleId)
                    }
                    "CLOZE" -> {
                        val articleId = saved.getOrNull(1) as? String
                        val article = articleId?.let { CurriculumDataSource.ARTICLE_MAP[it] }
                        if (article != null) OverlayScreen.Cloze(article) else null
                    }
                    "FLASHCARDS" -> {
                        val title = saved.getOrNull(1) as? String ?: ""
                        val sessionId = saved.getOrNull(2) as? String ?: "session_default"
                        val sessionType = saved.getOrNull(3) as? String ?: "GENERAL"
                        val index = (saved.getOrNull(4) as? Number)?.toInt() ?: 0
                        val count = (saved.getOrNull(5) as? Number)?.toInt() ?: 0
                        @Suppress("UNCHECKED_CAST")
                        val cardIds = (saved.getOrNull(6) as? List<String>) ?: emptyList()

                        val active = repository.getActiveSession()
                        val (actualIndex, actualCount, actualCards) = if (active != null && active.id == sessionId && !active.isComplete) {
                            val restored = repository.restoreCardsForSession(active)
                            Triple(active.currentIndex, active.completedCount, restored)
                        } else {
                            val restored = cardIds.mapNotNull { id ->
                                val fc = CurriculumDataSource.getFlashcard(id)
                                if (fc != null) Pair(fc, repository.getCardState(id)) else null
                            }
                            Triple(index, count, restored)
                        }

                        if (actualCards.isNotEmpty()) {
                            OverlayScreen.Flashcards(
                                title = title,
                                cards = actualCards,
                                initialIndex = actualIndex,
                                initialCompletedCount = actualCount,
                                sessionId = sessionId,
                                sessionType = sessionType
                            )
                        } else null
                    }
                    else -> null
                }
            }
            else -> null
        }
    }
}

internal fun overlayScreenSaver(repository: WenYanRepository): Saver<MutableState<OverlayScreen?>, Any> =
    Saver(
        save = { state -> OverlayScreenStateHolder.save(state.value, repository) },
        restore = { saved -> mutableStateOf(OverlayScreenStateHolder.restore(saved, repository)) }
    )

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var repository: WenYanRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge()
        } catch (_: Throwable) {
            // Safe fallback if OEM window manager rejects edge-to-edge
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            try {
                window.isNavigationBarContrastEnforced = false
            } catch (_: Throwable) {}
        }

        setContent {
            WenYanTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = LocalContext.current
                    val soundManager = remember { SoundEffectManager.getInstance(context) }
                    val hapticManager = remember { HapticManager.getInstance(context) }

                    var selectedTab by rememberSaveable { mutableStateOf(MainTab.TODAY) }
                    var overlayScreen by rememberSaveable(
                        saver = remember(repository) { overlayScreenSaver(repository) }
                    ) {
                        mutableStateOf<OverlayScreen?>(null)
                    }

                    // Navigation BackHandler
                    BackHandler(enabled = overlayScreen != null || selectedTab != MainTab.TODAY) {
                        if (overlayScreen != null) {
                            overlayScreen = null
                        } else if (selectedTab != MainTab.TODAY) {
                            selectedTab = MainTab.TODAY
                        }
                    }

                    // Active recitation session (Flashcards or Cloze) or Main Tab Flow
                    AnimatedContent(
                        targetState = overlayScreen,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "screen_transition"
                    ) { screen ->
                        when (screen) {
                            is OverlayScreen.Flashcards -> {
                                FlipCardScreen(
                                    title = screen.title,
                                    cards = screen.cards,
                                    repository = repository,
                                    onBack = { overlayScreen = null },
                                    initialIndex = screen.initialIndex,
                                    initialCompletedCount = screen.initialCompletedCount,
                                    sessionId = screen.sessionId,
                                    sessionType = screen.sessionType,
                                    onProgressUpdate = { idx, count ->
                                        overlayScreen = screen.copy(
                                            initialIndex = idx,
                                            initialCompletedCount = count
                                        )
                                    }
                                )
                            }

                            is OverlayScreen.Cloze -> {
                                ClozeRecitationScreen(
                                    article = screen.article,
                                    onBack = { overlayScreen = null }
                                )
                            }

                            is OverlayScreen.Settings -> {
                                SettingsScreen(
                                    repository = repository,
                                    onBack = { overlayScreen = null }
                                )
                            }

                            is OverlayScreen.GaoKaoScenario -> {
                                GaoKaoScenarioScreen(
                                    onNavigateBack = { overlayScreen = null }
                                )
                            }

                            is OverlayScreen.SnowballRecitation -> {
                                SnowballRecitationScreen(
                                    articleId = screen.articleId,
                                    onNavigateBack = { overlayScreen = null }
                                )
                            }

                            is OverlayScreen.CertificateAndCopybook -> {
                                CertificateAndCopybookScreen(
                                    articleId = screen.articleId,
                                    onNavigateBack = { overlayScreen = null }
                                )
                            }

                            null -> {
                                val configuration = LocalConfiguration.current
                                val isWideScreen = configuration.screenWidthDp >= 600

                                val mainTabContent: @Composable (Modifier) -> Unit = { contentModifier ->
                                    var totalDragX by remember { mutableFloatStateOf(0f) }
                                    Box(
                                        modifier = contentModifier
                                            .pointerInput(selectedTab) {
                                                detectHorizontalDragGestures(
                                                    onDragStart = { totalDragX = 0f },
                                                    onHorizontalDrag = { _, dragAmount ->
                                                        totalDragX += dragAmount
                                                    },
                                                    onDragEnd = {
                                                        val threshold = 72.dp.toPx()
                                                        if (totalDragX < -threshold) {
                                                            val nextOrdinal = (selectedTab.ordinal + 1).coerceAtMost(MainTab.entries.size - 1)
                                                            if (nextOrdinal != selectedTab.ordinal) {
                                                                hapticManager.tapLight()
                                                                soundManager.playClick()
                                                                selectedTab = MainTab.entries[nextOrdinal]
                                                            }
                                                        } else if (totalDragX > threshold) {
                                                            val prevOrdinal = (selectedTab.ordinal - 1).coerceAtLeast(0)
                                                            if (prevOrdinal != selectedTab.ordinal) {
                                                                hapticManager.tapLight()
                                                                soundManager.playClick()
                                                                selectedTab = MainTab.entries[prevOrdinal]
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                    ) {
                                        AnimatedContent(
                                            targetState = selectedTab,
                                            transitionSpec = {
                                                if (targetState.ordinal > initialState.ordinal) {
                                                    (slideInHorizontally(
                                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                                        initialOffsetX = { fullWidth -> fullWidth }
                                                    ) + fadeIn(tween(220))).togetherWith(
                                                        slideOutHorizontally(
                                                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                                            targetOffsetX = { fullWidth -> -fullWidth }
                                                        ) + fadeOut(tween(180))
                                                    )
                                                } else {
                                                    (slideInHorizontally(
                                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                                        initialOffsetX = { fullWidth -> -fullWidth }
                                                    ) + fadeIn(tween(220))).togetherWith(
                                                        slideOutHorizontally(
                                                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                                            targetOffsetX = { fullWidth -> fullWidth }
                                                        ) + fadeOut(tween(180))
                                                    )
                                                }
                                            },
                                            label = "main_tab_slide_transition"
                                        ) { tab ->
                                            when (tab) {
                                                MainTab.TODAY -> {
                                                    DashboardScreen(
                                                        repository = repository,
                                                        onStartTodayReview = {
                                                            val active = repository.getActiveSession()
                                                            if (active != null && active.sessionType == "TODAY_DUE" && !active.isComplete) {
                                                                val restored = repository.restoreCardsForSession(active)
                                                                if (restored.isNotEmpty()) {
                                                                    overlayScreen = OverlayScreen.Flashcards(
                                                                        title = active.title,
                                                                        cards = restored,
                                                                        initialIndex = active.currentIndex,
                                                                        initialCompletedCount = active.completedCount,
                                                                        sessionId = active.id,
                                                                        sessionType = active.sessionType
                                                                    )
                                                                    return@DashboardScreen
                                                                }
                                                            }
                                                            val dueCards = repository.getDueQueue()
                                                            overlayScreen = OverlayScreen.Flashcards(
                                                                title = "今日复习 · FSRS调度队列",
                                                                cards = dueCards,
                                                                initialIndex = 0,
                                                                initialCompletedCount = 0,
                                                                sessionId = "today_due_${System.currentTimeMillis()}",
                                                                sessionType = "TODAY_DUE"
                                                            )
                                                        },
                                                        onStartGaoKaoReview = {
                                                            val active = repository.getActiveSession()
                                                            if (active != null && active.sessionType == "GAOKAO_72" && !active.isComplete) {
                                                                val restored = repository.restoreCardsForSession(active)
                                                                if (restored.isNotEmpty()) {
                                                                    overlayScreen = OverlayScreen.Flashcards(
                                                                        title = active.title,
                                                                        cards = restored,
                                                                        initialIndex = active.currentIndex,
                                                                        initialCompletedCount = active.completedCount,
                                                                        sessionId = active.id,
                                                                        sessionType = active.sessionType
                                                                    )
                                                                    return@DashboardScreen
                                                                }
                                                            }
                                                            val gaoKaoCards = repository.getRandomQueue(
                                                                limit = 20,
                                                                moduleIds = null,
                                                                gaoKaoOnly = true
                                                            )
                                                            overlayScreen = OverlayScreen.Flashcards(
                                                                title = "高考必背 72 篇专项背诵",
                                                                cards = gaoKaoCards,
                                                                initialIndex = 0,
                                                                initialCompletedCount = 0,
                                                                sessionId = "gaokao_72_${System.currentTimeMillis()}",
                                                                sessionType = "GAOKAO_72"
                                                            )
                                                        },
                                                        onResumeActiveSession = { active ->
                                                            val restored = repository.restoreCardsForSession(active)
                                                            if (restored.isNotEmpty()) {
                                                                overlayScreen = OverlayScreen.Flashcards(
                                                                    title = active.title,
                                                                    cards = restored,
                                                                    initialIndex = active.currentIndex,
                                                                    initialCompletedCount = active.completedCount,
                                                                    sessionId = active.id,
                                                                    sessionType = active.sessionType
                                                                )
                                                            }
                                                        },
                                                        onNavigateToPractice = {
                                                            selectedTab = MainTab.PRACTICE
                                                        },
                                                        onOpenSettings = {
                                                            overlayScreen = OverlayScreen.Settings
                                                        }
                                                    )
                                                }

                                                MainTab.LIBRARY -> {
                                                    ChapterTreeScreen(
                                                        repository = repository,
                                                        onBack = {
                                                            selectedTab = MainTab.TODAY
                                                        },
                                                        onStartFlashcards = { article ->
                                                            val fcs = repository.getFlashcardsForArticle(article.id)
                                                            val cardsWithState = fcs.map { Pair(it, repository.getCardState(it.id)) }
                                                            overlayScreen = OverlayScreen.Flashcards(
                                                                title = "《${article.title}》· 闪卡背诵",
                                                                cards = cardsWithState,
                                                                initialIndex = 0,
                                                                initialCompletedCount = 0,
                                                                sessionId = "article_${article.id}",
                                                                sessionType = "ARTICLE"
                                                            )
                                                        },
                                                        onStartCloze = { article ->
                                                            overlayScreen = OverlayScreen.Cloze(article)
                                                        },
                                                        onOpenSnowball = { articleId ->
                                                            overlayScreen = OverlayScreen.SnowballRecitation(articleId)
                                                        },
                                                        onOpenCertificate = { articleId ->
                                                            overlayScreen = OverlayScreen.CertificateAndCopybook(articleId)
                                                        }
                                                    )
                                                }

                                                MainTab.PRACTICE -> {
                                                    PracticeScreen(
                                                        repository = repository,
                                                        onStartSession = { title, cards ->
                                                            overlayScreen = OverlayScreen.Flashcards(
                                                                title = title,
                                                                cards = cards,
                                                                initialIndex = 0,
                                                                initialCompletedCount = 0,
                                                                sessionId = "practice_${System.currentTimeMillis()}",
                                                                sessionType = "PRACTICE"
                                                            )
                                                        },
                                                        onStartCloze = { article ->
                                                            overlayScreen = OverlayScreen.Cloze(article)
                                                        },
                                                        onOpenGaoKaoScenario = {
                                                            overlayScreen = OverlayScreen.GaoKaoScenario
                                                        },
                                                        onOpenSnowball = { artId ->
                                                            overlayScreen = OverlayScreen.SnowballRecitation(artId)
                                                        },
                                                        onOpenCertificate = { artId ->
                                                            overlayScreen = OverlayScreen.CertificateAndCopybook(artId)
                                                        }
                                                    )
                                                }

                                                MainTab.FOOTPRINT -> {
                                                    FootprintScreen(
                                                        repository = repository,
                                                        onOpenSettings = {
                                                            overlayScreen = OverlayScreen.Settings
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                if (isWideScreen) {
                                    Row(modifier = Modifier.fillMaxSize()) {
                                        NavigationRail(
                                            containerColor = MaterialTheme.colorScheme.surface,
                                            header = {
                                                Spacer(modifier = Modifier.height(16.dp))
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                                Spacer(modifier = Modifier.height(16.dp))
                                            }
                                        ) {
                                            MainTab.entries.forEach { tab ->
                                                val selected = (selectedTab == tab)
                                                NavigationRailItem(
                                                    selected = selected,
                                                    onClick = {
                                                        if (selectedTab != tab) {
                                                            hapticManager.tapLight()
                                                            soundManager.playClick()
                                                            selectedTab = tab
                                                        }
                                                    },
                                                    icon = {
                                                        Icon(
                                                            imageVector = tab.icon,
                                                            contentDescription = tab.title
                                                        )
                                                    },
                                                    label = {
                                                        Text(
                                                            text = tab.title,
                                                            fontFamily = FontFamily.SansSerif,
                                                            fontSize = 12.sp,
                                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                                        )
                                                    },
                                                    colors = NavigationRailItemDefaults.colors(
                                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                    )
                                                )
                                            }
                                        }
                                        mainTabContent(
                                            Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                        )
                                    }
                                } else {
                                    Scaffold(
                                        bottomBar = {
                                            NavigationBar(
                                                containerColor = MaterialTheme.colorScheme.surface,
                                                tonalElevation = 3.dp
                                            ) {
                                                MainTab.entries.forEach { tab ->
                                                    val selected = (selectedTab == tab)
                                                    NavigationBarItem(
                                                        selected = selected,
                                                        onClick = {
                                                            if (selectedTab != tab) {
                                                                hapticManager.tapLight()
                                                                soundManager.playClick()
                                                                selectedTab = tab
                                                            }
                                                        },
                                                        icon = {
                                                            Icon(
                                                                imageVector = tab.icon,
                                                                contentDescription = tab.title
                                                            )
                                                        },
                                                        label = {
                                                            Text(
                                                                text = tab.title,
                                                                fontFamily = FontFamily.SansSerif,
                                                                fontSize = 12.sp,
                                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                                            )
                                                        },
                                                        colors = NavigationBarItemDefaults.colors(
                                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                        )
                                                    )
                                                }
                                            }
                                        },
                                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                                        containerColor = MaterialTheme.colorScheme.background
                                    ) { innerPadding ->
                                        mainTabContent(
                                            Modifier
                                                .fillMaxSize()
                                                .padding(bottom = innerPadding.calculateBottomPadding())
                                                .consumeWindowInsets(innerPadding)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
