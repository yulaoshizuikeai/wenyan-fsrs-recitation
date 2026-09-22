package com.ancient.wenyan.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ancient.wenyan.data.DeckStats
import com.ancient.wenyan.data.WenYanRepository
import com.ancient.wenyan.domain.model.Article
import com.ancient.wenyan.domain.model.ArticleProgress
import com.ancient.wenyan.domain.model.Flashcard
import com.ancient.wenyan.domain.model.Module
import com.ancient.wenyan.domain.fsrs.CardFsrsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ChapterTreeUiState(
    val modules: List<Module> = emptyList(),
    val stats: DeckStats = DeckStats(0, 0, 0, 0, 0, 0, 0f)
)

@HiltViewModel
class ChapterTreeViewModel @Inject constructor(
    private val repository: WenYanRepository
) : ViewModel() {

    val uiState: StateFlow<ChapterTreeUiState> = repository.statsFlow
        .map { stats ->
            ChapterTreeUiState(
                modules = repository.getModules(),
                stats = stats
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ChapterTreeUiState(modules = repository.getModules())
        )

    fun getArticleProgress(articleId: String): ArticleProgress {
        return repository.getArticleProgress(articleId)
    }

    fun getFlashcardsWithState(articleId: String): List<Pair<Flashcard, CardFsrsState>> {
        val fcs = repository.getFlashcardsForArticle(articleId)
        return fcs.map { Pair(it, repository.getCardState(it.id)) }
    }

    fun getRepository(): WenYanRepository = repository
}
