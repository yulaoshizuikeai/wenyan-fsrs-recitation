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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.ancient.wenyan.data.WenYanRepository
import com.ancient.wenyan.domain.fsrs.CardFsrsState
import com.ancient.wenyan.domain.model.Article
import com.ancient.wenyan.domain.model.Flashcard
import com.ancient.wenyan.ui.screens.*
import com.ancient.wenyan.ui.sound.HapticManager
import com.ancient.wenyan.ui.sound.SoundEffectManager
import com.ancient.wenyan.ui.theme.*
import kotlinx.coroutines.launch

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
}

class MainActivity : ComponentActivity() {
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

        val repository = WenYanRepository.getInstance(applicationContext)

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
                    var overlayScreen by remember { mutableStateOf<OverlayScreen?>(null) }

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
                                    sessionType = screen.sessionType
                                )
                            }

                            is OverlayScreen.Cloze -> {
                                ClozeRecitationScreen(
                                    article = screen.article,
                                    onBack = { overlayScreen = null }
                                )
                            }

                            null -> {
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
                                    containerColor = MaterialTheme.colorScheme.background
                                ) { innerPadding ->
                                    var totalDragX by remember { mutableFloatStateOf(0f) }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(innerPadding)
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
                                                        }
                                                    )
                                                }

                                                MainTab.FOOTPRINT -> {
                                                    FootprintScreen(repository = repository)
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
        }
    }
}
