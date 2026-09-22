package com.ancient.wenyan.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ancient.wenyan.data.DeckStats
import com.ancient.wenyan.data.WenYanRepository
import com.ancient.wenyan.domain.model.ActiveSession
import com.ancient.wenyan.domain.model.HeatmapStats
import com.ancient.wenyan.domain.model.StudyGoalsConfig
import com.ancient.wenyan.domain.model.TodayStudyProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val stats: DeckStats = DeckStats(0, 0, 0, 0, 0, 0, 0f),
    val selectedBookScope: Set<String>? = null,
    val selectedBookName: String = "全部 11 册教材",
    val heatmapStats: HeatmapStats = HeatmapStats(0, 0, 0, 0),
    val activeSession: ActiveSession? = null,
    val studyGoals: StudyGoalsConfig = StudyGoalsConfig(),
    val todayProgress: TodayStudyProgress = TodayStudyProgress()
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: WenYanRepository
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        combine(repository.statsFlow, repository.selectedBookScope, repository.selectedBookName) { stats, scope, name ->
            Triple(stats, scope, name)
        },
        combine(repository.heatmapStatsFlow, repository.activeSessionFlow) { heatmap, active ->
            Pair(heatmap, active)
        },
        combine(repository.studyGoalsConfig, repository.todayStudyProgressFlow) { goals, progress ->
            Pair(goals, progress)
        }
    ) { (stats, scope, name), (heatmap, active), (goals, progress) ->
        DashboardUiState(
            stats = stats,
            selectedBookScope = scope,
            selectedBookName = name,
            heatmapStats = heatmap,
            activeSession = active,
            studyGoals = goals,
            todayProgress = progress
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun setSelectedBookScope(moduleIds: Set<String>?, displayName: String) {
        repository.setSelectedBookScope(moduleIds, displayName)
    }

    fun setStudyGoalsConfig(config: StudyGoalsConfig) {
        repository.setStudyGoalsConfig(config)
    }

    fun getRepository(): WenYanRepository = repository
}
