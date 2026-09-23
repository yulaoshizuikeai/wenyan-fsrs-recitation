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
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

data class ChapterTreeUiState(
    val modules: List<Module> = emptyList(),
    val stats: DeckStats = DeckStats(0, 0, 0, 0, 0, 0, 0f),
    val selectedBookName: String = "全部 11 册教材",
    val selectedBookScope: Set<String>? = null
)

@HiltViewModel
class ChapterTreeViewModel @Inject constructor(
    private val repository: WenYanRepository
) : ViewModel() {

    val uiState: StateFlow<ChapterTreeUiState> = combine(
        repository.statsFlow,
        repository.selectedBookScope,
        repository.selectedBookName
    ) { stats, bookScope, bookName ->
        ChapterTreeUiState(
            modules = repository.getModules(bookScope),
            stats = stats,
            selectedBookName = bookName,
            selectedBookScope = bookScope
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChapterTreeUiState(
            modules = repository.getModules(repository.selectedBookScope.value),
            selectedBookName = repository.selectedBookName.value,
            selectedBookScope = repository.selectedBookScope.value
        )
    )

    fun selectBook(name: String, scope: Set<String>?) {
        repository.setSelectedBookScope(scope, name)
    }

    fun getArticleProgress(articleId: String): ArticleProgress {
        return repository.getArticleProgress(articleId)
    }

    fun getFlashcardsWithState(articleId: String): List<Pair<Flashcard, CardFsrsState>> {
        val fcs = repository.getFlashcardsForArticle(articleId)
        return fcs.map { Pair(it, repository.getCardState(it.id)) }
    }

    fun getRepository(): WenYanRepository = repository
}
