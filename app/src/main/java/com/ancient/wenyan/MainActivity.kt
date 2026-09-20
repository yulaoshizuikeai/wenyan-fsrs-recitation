package com.ancient.wenyan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ancient.wenyan.data.WenYanRepository
import com.ancient.wenyan.domain.fsrs.CardFsrsState
import com.ancient.wenyan.domain.model.Article
import com.ancient.wenyan.domain.model.Flashcard
import com.ancient.wenyan.ui.screens.*
import com.ancient.wenyan.ui.sound.HapticManager
import com.ancient.wenyan.ui.sound.SoundEffectManager
import com.ancient.wenyan.ui.theme.*

enum class MainTab(
    val title: String,
    val icon: ImageVector
) {
    TODAY("今日研读", Icons.Default.Home),
    LIBRARY("篇目文库", Icons.AutoMirrored.Filled.MenuBook),
    PRACTICE("专项练习", Icons.Default.Shuffle),
    FOOTPRINT("研墨足迹", Icons.Default.CalendarMonth)
}

sealed class OverlayScreen {
    data class Flashcards(val title: String, val cards: List<Pair<Flashcard, CardFsrsState>>) : OverlayScreen()
    data class Cloze(val article: Article) : OverlayScreen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

                    var selectedTab by remember { mutableStateOf(MainTab.TODAY) }
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
                                    onBack = { overlayScreen = null }
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
                                            containerColor = BgSurface,
                                            tonalElevation = 3.dp
                                        ) {
                                            MainTab.entries.forEach { tab ->
                                                val selected = (selectedTab == tab)
                                                NavigationBarItem(
                                                    selected = selected,
                                                    onClick = {
                                                        if (selectedTab != tab) {
                                                            selectedTab = tab
                                                            hapticManager.tapLight()
                                                            soundManager.playClick()
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
                                                        selectedIconColor = StudyNavy,
                                                        selectedTextColor = StudyNavy,
                                                        indicatorColor = StudyBlueLight,
                                                        unselectedIconColor = TextTertiary,
                                                        unselectedTextColor = TextTertiary
                                                    )
                                                )
                                            }
                                        }
                                    },
                                    containerColor = BgCanvas
                                ) { innerPadding ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(innerPadding)
                                    ) {
                                        when (selectedTab) {
                                            MainTab.TODAY -> {
                                                DashboardScreen(
                                                    repository = repository,
                                                    onStartTodayReview = {
                                                        val dueCards = repository.getDueQueue()
                                                        overlayScreen = OverlayScreen.Flashcards(
                                                            title = "今日复习 · FSRS调度队列",
                                                            cards = dueCards
                                                        )
                                                    },
                                                    onStartGaoKaoReview = {
                                                        val gaoKaoCards = repository.getRandomQueue(
                                                            limit = 20,
                                                            moduleIds = null,
                                                            gaoKaoOnly = true
                                                        )
                                                        overlayScreen = OverlayScreen.Flashcards(
                                                            title = "高考必背 72 篇专项背诵",
                                                            cards = gaoKaoCards
                                                        )
                                                    },
                                                    onNavigateToLibrary = {
                                                        selectedTab = MainTab.LIBRARY
                                                    },
                                                    onNavigateToPractice = {
                                                        selectedTab = MainTab.PRACTICE
                                                    }
                                                )
                                            }

                                            MainTab.LIBRARY -> {
                                                ChapterTreeScreen(
                                                    repository = repository,
                                                    onBack = { selectedTab = MainTab.TODAY },
                                                    onStartFlashcards = { article ->
                                                        val fcs = repository.getFlashcardsForArticle(article.id)
                                                        val cardsWithState = fcs.map { Pair(it, repository.getCardState(it.id)) }
                                                        overlayScreen = OverlayScreen.Flashcards(
                                                            title = "《${article.title}》· 闪卡背诵",
                                                            cards = cardsWithState
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
                                                            cards = cards
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
