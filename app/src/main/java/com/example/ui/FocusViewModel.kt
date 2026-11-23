package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.DailyTask
import com.example.data.FocusRepository
import com.example.data.InstalledApp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

enum class MainTab {
    FOCUS,
    TASKS_REWARD
}

data class FocusUiState(
    val userName: String = "",
    val nameDraft: String = "",
    val nameError: Boolean = false,
    val isWelcomeScreenVisible: Boolean = true,
    val canNavigateBackFromWelcome: Boolean = false,
    val selectedTab: MainTab = MainTab.FOCUS,
    val isLoadingApps: Boolean = true,
    val installedApps: List<InstalledApp> = emptyList(),
    val allowedPackages: Set<String> = emptySet(),
    val searchQuery: String = "",
    val durationInput: String = "25",
    val isServiceEnabled: Boolean = false,
    val sessionEndMs: Long = 0L,
    val sessionDurationMs: Long = 0L,
    val remainingMs: Long = 0L,
    val dailyTasks: List<DailyTask> = emptyList(),
    val newTaskInput: String = "",
    val rewardPackageName: String? = null,
    val isRewardClaimedToday: Boolean = false,
    val rewardEndMs: Long = 0L,
    val rewardRemainingMs: Long = 0L,
    val isRewardAppPickerOpen: Boolean = false,
    val rewardAppSearchQuery: String = ""
) {
    val isSessionActive: Boolean
        get() = remainingMs > 0L

    val completedTasksCount: Int
        get() = dailyTasks.count { it.isCompleted }

    val allTasksCompleted: Boolean
        get() = dailyTasks.isNotEmpty() && dailyTasks.all { it.isCompleted }

    val isRewardActive: Boolean
        get() = rewardRemainingMs > 0L

    val canClaimReward: Boolean
        get() = allTasksCompleted && !isRewardClaimedToday && !rewardPackageName.isNullOrBlank()

    val selectedRewardApp: InstalledApp?
        get() = installedApps.firstOrNull { it.packageName == rewardPackageName }

    val filteredApps: List<InstalledApp>
        get() {
            val q = searchQuery.trim()
            if (q.isEmpty()) return installedApps
            return installedApps.filter {
                it.label.contains(q, ignoreCase = true) ||
                    it.packageName.contains(q, ignoreCase = true)
            }
        }

    val filteredRewardPickerApps: List<InstalledApp>
        get() {
            val q = rewardAppSearchQuery.trim()
            if (q.isEmpty()) return installedApps
            return installedApps.filter {
                it.label.contains(q, ignoreCase = true) ||
                    it.packageName.contains(q, ignoreCase = true)
            }
        }
}

sealed interface FocusUiEvent {
    data class ShowMessage(val messageResId: Int, val arg: Long? = null) : FocusUiEvent
    data class ShowStringMessage(val messageResId: Int, val textArg: String) : FocusUiEvent
    data object OpenAccessibilitySettings : FocusUiEvent
    data object NavigateHome : FocusUiEvent
    data class LaunchApp(val packageName: String) : FocusUiEvent
}

class FocusViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: FocusRepository = FocusRepository(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(FocusUiState())
    val uiState: StateFlow<FocusUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<FocusUiEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<FocusUiEvent> = _events.asSharedFlow()

    private var tickerJob: Job? = null

    init {
        refreshAll()
    }

    fun refreshAll() {
        val savedName = repository.getUserName()
        val savedAllowed = repository.getAllowedPackages()
        val endMs = repository.getSessionEndMs()
        val durationMs = repository.getSessionDurationMs()
        val serviceEnabled = repository.isBlockerServiceEnabled()
        val tasks = repository.loadDailyTasks()
        val rewardPkg = repository.getRewardPackage()
        val rewardClaimedToday = repository.isRewardClaimedToday()
        val rewardEndMs = repository.getRewardEndMs()
        val now = System.currentTimeMillis()
        val remaining = (endMs - now).coerceAtLeast(0L)
        val rewardRemaining = (rewardEndMs - now).coerceAtLeast(0L)

        _uiState.update { state ->
            state.copy(
                userName = savedName,
                nameDraft = if (state.nameDraft.isEmpty()) savedName else state.nameDraft,
                isWelcomeScreenVisible = savedName.isBlank(),
                canNavigateBackFromWelcome = false,
                allowedPackages = savedAllowed,
                isServiceEnabled = serviceEnabled,
                sessionEndMs = endMs,
                sessionDurationMs = durationMs,
                remainingMs = remaining,
                dailyTasks = tasks,
                rewardPackageName = rewardPkg,
                isRewardClaimedToday = rewardClaimedToday,
                rewardEndMs = rewardEndMs,
                rewardRemainingMs = rewardRemaining
            )
        }

        ensureTickerState(endMs, rewardEndMs)

        viewModelScope.launch {
            val apps = repository.loadInstalledApps()
            _uiState.update { state ->
                state.copy(
                    isLoadingApps = false,
                    installedApps = apps
                )
            }
        }
    }

    fun refreshServiceAndTimer() {
        val endMs = repository.getSessionEndMs()
        val durationMs = repository.getSessionDurationMs()
        val serviceEnabled = repository.isBlockerServiceEnabled()
        val tasks = repository.loadDailyTasks()
        val rewardClaimedToday = repository.isRewardClaimedToday()
        val rewardEndMs = repository.getRewardEndMs()
        val now = System.currentTimeMillis()
        val remaining = (endMs - now).coerceAtLeast(0L)
        val rewardRemaining = (rewardEndMs - now).coerceAtLeast(0L)

        _uiState.update { state ->
            state.copy(
                isServiceEnabled = serviceEnabled,
                sessionEndMs = endMs,
                sessionDurationMs = durationMs,
                remainingMs = remaining,
                dailyTasks = tasks,
                isRewardClaimedToday = rewardClaimedToday,
                rewardEndMs = rewardEndMs,
                rewardRemainingMs = rewardRemaining
            )
        }
        ensureTickerState(endMs, rewardEndMs)
    }

    fun selectTab(tab: MainTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun onNameDraftChange(name: String) {
        _uiState.update { it.copy(nameDraft = name, nameError = false) }
    }

    fun onSubmitNameClick() {
        val trimmed = _uiState.value.nameDraft.trim()
        if (trimmed.isEmpty()) {
            _uiState.update { it.copy(nameError = true) }
            return
        }
        repository.saveUserName(trimmed)
        _uiState.update {
            it.copy(
                userName = trimmed,
                nameDraft = trimmed,
                nameError = false,
                isWelcomeScreenVisible = false,
                canNavigateBackFromWelcome = false
            )
        }
    }

    fun onEditNameClick() {
        _uiState.update { state ->
            state.copy(
                nameDraft = state.userName,
                nameError = false,
                isWelcomeScreenVisible = true,
                canNavigateBackFromWelcome = state.userName.isNotBlank()
            )
        }
    }

    fun onCancelEditName() {
        if (_uiState.value.canNavigateBackFromWelcome) {
            _uiState.update {
                it.copy(
                    isWelcomeScreenVisible = false,
                    canNavigateBackFromWelcome = false,
                    nameError = false
                )
            }
        }
    }

    fun toggleApp(packageName: String) {
        _uiState.update { state ->
            val updated = state.allowedPackages.toMutableSet()
            if (packageName in updated) updated.remove(packageName) else updated.add(packageName)
            state.copy(allowedPackages = updated)
        }
    }

    fun selectAllFiltered() {
        _uiState.update { state ->
            val updated = state.allowedPackages.toMutableSet()
            state.filteredApps.forEach { updated.add(it.packageName) }
            state.copy(allowedPackages = updated)
        }
    }

    fun clearAllFiltered() {
        _uiState.update { state ->
            val updated = state.allowedPackages.toMutableSet()
            state.filteredApps.forEach { updated.remove(it.packageName) }
            state.copy(allowedPackages = updated)
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onDurationInputChange(input: String) {
        val digitsOnly = input.filter { it.isDigit() }.take(4)
        _uiState.update { it.copy(durationInput = digitsOnly) }
    }

    fun selectPresetMinutes(minutes: Int) {
        _uiState.update { it.copy(durationInput = minutes.toString()) }
    }

    fun onEnableServiceClick() {
        _events.tryEmit(FocusUiEvent.OpenAccessibilitySettings)
    }

    fun onStartSessionClick() {
        val minutes = _uiState.value.durationInput.toLongOrNull() ?: 0L
        val serviceEnabled = repository.isBlockerServiceEnabled()
        _uiState.update { it.copy(isServiceEnabled = serviceEnabled) }

        when {
            minutes <= 0L -> {
                _events.tryEmit(FocusUiEvent.ShowMessage(R.string.msg_invalid_duration))
            }
            !serviceEnabled -> {
                _events.tryEmit(FocusUiEvent.ShowMessage(R.string.msg_enable_service_first))
                _events.tryEmit(FocusUiEvent.OpenAccessibilitySettings)
            }
            else -> {
                val (endMs, durationMs) = repository.startSession(
                    allowedPackages = _uiState.value.allowedPackages,
                    durationMinutes = minutes
                )
                _uiState.update { state ->
                    state.copy(
                        sessionEndMs = endMs,
                        sessionDurationMs = durationMs,
                        remainingMs = (endMs - System.currentTimeMillis()).coerceAtLeast(0L)
                    )
                }
                ensureTickerState(endMs, _uiState.value.rewardEndMs)
                _events.tryEmit(FocusUiEvent.ShowMessage(R.string.msg_session_started, minutes))
                _events.tryEmit(FocusUiEvent.NavigateHome)
            }
        }
    }

    // Daily Task Tracker & 1-Hour Reward
    fun onNewTaskInputChange(input: String) {
        _uiState.update { it.copy(newTaskInput = input) }
    }

    fun onAddTaskClick() {
        val title = _uiState.value.newTaskInput.trim()
        if (title.isEmpty()) return
        val newTask = DailyTask(
            id = UUID.randomUUID().toString(),
            title = title,
            isCompleted = false
        )
        val updated = _uiState.value.dailyTasks + newTask
        repository.saveDailyTasks(updated)
        _uiState.update {
            it.copy(
                dailyTasks = updated,
                newTaskInput = ""
            )
        }
    }

    fun onToggleTask(taskId: String) {
        val updated = _uiState.value.dailyTasks.map { task ->
            if (task.id == taskId) task.copy(isCompleted = !task.isCompleted) else task
        }
        repository.saveDailyTasks(updated)
        _uiState.update { it.copy(dailyTasks = updated) }
    }

    fun onDeleteTask(taskId: String) {
        val updated = _uiState.value.dailyTasks.filterNot { it.id == taskId }
        repository.saveDailyTasks(updated)
        _uiState.update { it.copy(dailyTasks = updated) }
    }

    fun openRewardAppPicker() {
        _uiState.update { it.copy(isRewardAppPickerOpen = true, rewardAppSearchQuery = "") }
    }

    fun dismissRewardAppPicker() {
        _uiState.update { it.copy(isRewardAppPickerOpen = false, rewardAppSearchQuery = "") }
    }

    fun onRewardAppSearchChange(query: String) {
        _uiState.update { it.copy(rewardAppSearchQuery = query) }
    }

    fun selectRewardApp(packageName: String) {
        repository.setRewardPackage(packageName)
        _uiState.update {
            it.copy(
                rewardPackageName = packageName,
                isRewardAppPickerOpen = false,
                rewardAppSearchQuery = ""
            )
        }
    }

    fun onClaimRewardClick() {
        val state = _uiState.value
        val rewardPkg = state.rewardPackageName
        if (rewardPkg.isNullOrBlank()) {
            _events.tryEmit(FocusUiEvent.ShowMessage(R.string.msg_reward_pick_app_first))
            return
        }
        if (!state.allTasksCompleted || state.isRewardClaimedToday) return

        val rewardEndMs = repository.claimDailyHourReward(rewardPkg)
        val now = System.currentTimeMillis()
        _uiState.update {
            it.copy(
                isRewardClaimedToday = true,
                rewardEndMs = rewardEndMs,
                rewardRemainingMs = (rewardEndMs - now).coerceAtLeast(0L)
            )
        }
        ensureTickerState(_uiState.value.sessionEndMs, rewardEndMs)

        val appLabel = state.selectedRewardApp?.label ?: rewardPkg
        _events.tryEmit(FocusUiEvent.ShowStringMessage(R.string.msg_reward_claimed, appLabel))
    }

    fun onOpenRewardAppClick() {
        val pkg = _uiState.value.rewardPackageName ?: return
        _events.tryEmit(FocusUiEvent.LaunchApp(pkg))
    }

    private fun ensureTickerState(sessionEndMs: Long, rewardEndMs: Long) {
        tickerJob?.cancel()
        val now = System.currentTimeMillis()
        if (sessionEndMs <= now && rewardEndMs <= now) return

        tickerJob = viewModelScope.launch {
            while (isActive) {
                val currentNow = System.currentTimeMillis()
                val sessionRem = (sessionEndMs - currentNow).coerceAtLeast(0L)
                val rewardRem = (rewardEndMs - currentNow).coerceAtLeast(0L)
                _uiState.update {
                    it.copy(
                        remainingMs = sessionRem,
                        rewardRemainingMs = rewardRem
                    )
                }
                if (sessionRem <= 0L && rewardRem <= 0L) break
                delay(1000L)
            }
        }
    }
}
