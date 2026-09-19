package com.ancient.wenyan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.ancient.wenyan.data.CurriculumDataSource
import com.ancient.wenyan.data.WenYanRepository
import com.ancient.wenyan.domain.fsrs.CardFsrsState
import com.ancient.wenyan.domain.model.Article
import com.ancient.wenyan.domain.model.Flashcard
import com.ancient.wenyan.ui.screens.*
import com.ancient.wenyan.ui.theme.AncientColorScheme
import com.ancient.wenyan.ui.theme.WenYanTheme

sealed class Screen {
    data object Dashboard : Screen()
    data object Chapters : Screen()
    data object RandomReview : Screen()
    data class Flashcards(val title: String, val cards: List<Pair<Flashcard, CardFsrsState>>) : Screen()
    data class Cloze(val article: Article) : Screen()
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
                    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }

                    // Handle hardware / system back button
                    BackHandler(enabled = currentScreen !is Screen.Dashboard) {
                        currentScreen = Screen.Dashboard
                    }

                    when (val screen = currentScreen) {
                        is Screen.Dashboard -> {
                            DashboardScreen(
                                repository = repository,
                                onNavigateToChapters = { currentScreen = Screen.Chapters },
                                onNavigateToRandom = { currentScreen = Screen.RandomReview },
                                onStartTodayReview = {
                                    val dueCards = repository.getDueQueue()
                                    currentScreen = Screen.Flashcards(
                                        title = "今日复习 · FSRS调度队列",
                                        cards = dueCards
                                    )
                                },
                                onStartGaoKaoReview = {
                                    val gaoKaoCards = repository.getRandomQueue(
                                        limit = 20,
                                        moduleIds = null
                                    )
                                    currentScreen = Screen.Flashcards(
                                        title = "高考必背 72 篇专项背诵",
                                        cards = gaoKaoCards
                                    )
                                }
                            )
                        }

                        is Screen.Chapters -> {
                            ChapterTreeScreen(
                                repository = repository,
                                onBack = { currentScreen = Screen.Dashboard },
                                onStartFlashcards = { article ->
                                    val fcs = repository.getFlashcardsForArticle(article.id)
                                    val cardsWithState = fcs.map { Pair(it, repository.getCardState(it.id)) }
                                    currentScreen = Screen.Flashcards(
                                        title = "《${article.title}》· 闪卡背诵",
                                        cards = cardsWithState
                                    )
                                },
                                onStartCloze = { article ->
                                    currentScreen = Screen.Cloze(article)
                                }
                            )
                        }

                        is Screen.RandomReview -> {
                            RandomReviewScreen(
                                repository = repository,
                                onBack = { currentScreen = Screen.Dashboard },
                                onStartSession = { title, cards ->
                                    currentScreen = Screen.Flashcards(title = title, cards = cards)
                                }
                            )
                        }

                        is Screen.Flashcards -> {
                            FlipCardScreen(
                                title = screen.title,
                                cards = screen.cards,
                                repository = repository,
                                onBack = { currentScreen = Screen.Dashboard }
                            )
                        }

                        is Screen.Cloze -> {
                            ClozeRecitationScreen(
                                article = screen.article,
                                onBack = { currentScreen = Screen.Chapters }
                            )
                        }
                    }
                }
            }
        }
    }
}
