package com.ancient.wenyan.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ancient.wenyan.data.WenYanRepository
import com.ancient.wenyan.domain.model.RecitationOrderMode
import com.ancient.wenyan.domain.model.StudyGoalsConfig
import com.ancient.wenyan.domain.sync.WebDavBackupManager
import com.ancient.wenyan.domain.sync.WebDavConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val studyGoals: StudyGoalsConfig = StudyGoalsConfig(),
    val recitationOrderMode: RecitationOrderMode = RecitationOrderMode.SEQUENTIAL,
    val selectedBookName: String = "全部 11 册教材",
    val selectedBookScope: Set<String>? = null,
    val reminderTime: Pair<Int, Int> = Pair(21, 0),
    val isReminderEnabled: Boolean = true,
    val isSyncing: Boolean = false,
    val syncMessage: String? = null,
    val webDavConfig: WebDavConfig = WebDavConfig()
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: WenYanRepository
) : ViewModel() {

    private val _reminderTime = MutableStateFlow(repository.getReminderTime())
    private val _isReminderEnabled = MutableStateFlow(repository.isReminderEnabled())
    private val _webDavConfig = MutableStateFlow(repository.getWebDavConfig())
    private val _isSyncing = MutableStateFlow(false)
    private val _syncMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        combine(repository.studyGoalsConfig, repository.recitationOrderMode) { goals, order ->
            Pair(goals, order)
        },
        combine(repository.selectedBookName, repository.selectedBookScope) { bookName, bookScope ->
            Pair(bookName, bookScope)
        },
        combine(_reminderTime, _isReminderEnabled, _isSyncing, _syncMessage, _webDavConfig) { time, remEnabled, syncing, msg, webDav ->
            arrayOf(time, remEnabled, syncing, msg, webDav)
        }
    ) { (goals, order), (bookName, bookScope), syncArray ->
        @Suppress("UNCHECKED_CAST")
        SettingsUiState(
            studyGoals = goals,
            recitationOrderMode = order,
            selectedBookName = bookName,
            selectedBookScope = bookScope,
            reminderTime = syncArray[0] as Pair<Int, Int>,
            isReminderEnabled = syncArray[1] as Boolean,
            isSyncing = syncArray[2] as Boolean,
            syncMessage = syncArray[3] as String?,
            webDavConfig = syncArray[4] as WebDavConfig
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState(
            studyGoals = repository.getStudyGoalsConfig(),
            recitationOrderMode = repository.recitationOrderMode.value,
            selectedBookName = repository.selectedBookName.value,
            selectedBookScope = repository.selectedBookScope.value,
            reminderTime = repository.getReminderTime(),
            isReminderEnabled = repository.isReminderEnabled(),
            webDavConfig = repository.getWebDavConfig()
        )
    )

    fun saveWebDavConfig(config: WebDavConfig) {
        repository.saveWebDavConfig(config)
        _webDavConfig.value = config
    }

    fun setStudyGoalsConfig(config: StudyGoalsConfig) {
        repository.setStudyGoalsConfig(config)
    }

    fun setRecitationOrderMode(mode: RecitationOrderMode) {
        repository.setRecitationOrderMode(mode)
    }

    fun setSelectedBookScope(moduleIds: Set<String>?, displayName: String) {
        repository.setSelectedBookScope(moduleIds, displayName)
    }

    fun setReminderTime(hour: Int, minute: Int) {
        repository.setReminderTime(hour, minute)
        _reminderTime.value = Pair(hour, minute)
    }

    fun setReminderEnabled(enabled: Boolean) {
        repository.setReminderEnabled(enabled)
        _isReminderEnabled.value = enabled
    }

    fun setOnboardingCompleted(completed: Boolean) {
        repository.setOnboardingCompleted(completed)
    }

    fun clearPersistedCardStates(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.clearAllUserData()
            onComplete?.invoke()
        }
    }

    fun createBackupJson(): String {
        return WebDavBackupManager.createBackupJson(repository)
    }

    fun restoreFromJson(jsonStr: String, onComplete: ((Int) -> Unit)? = null) {
        viewModelScope.launch {
            val count = WebDavBackupManager.restoreFromJson(jsonStr, repository)
            _syncMessage.value = if (count > 0) "成功恢复 $count 张卡片记忆进度" else "备份解析失败或无有效数据"
            onComplete?.invoke(count)
        }
    }

    fun syncToWebDav(config: WebDavConfig, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isSyncing.value = true
            val json = WebDavBackupManager.createBackupJson(repository)
            val result = WebDavBackupManager.uploadToWebDav(config, json)
            _isSyncing.value = false
            _syncMessage.value = result.message
            onComplete(result.success, result.message)
        }
    }

    fun downloadFromWebDav(config: WebDavConfig, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isSyncing.value = true
            val (result, content) = WebDavBackupManager.downloadFromWebDav(config)
            if (result.success && content != null) {
                val restored = WebDavBackupManager.restoreFromJson(content, repository)
                _isSyncing.value = false
                val msg = "云端同步成功，已还原 $restored 张卡片进度"
                _syncMessage.value = msg
                onComplete(true, msg)
            } else {
                _isSyncing.value = false
                _syncMessage.value = result.message
                onComplete(false, result.message)
            }
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    fun getRepository(): WenYanRepository = repository
}
