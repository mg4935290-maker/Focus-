package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.audio.FocusAudioService
import com.example.data.BuiltInSound
import com.example.data.DailyTask
import com.example.data.FocusRepository
import com.example.data.InstalledApp
import com.example.data.PlantedTree
import com.example.data.SelectedAudioSource
import com.example.data.UploadedAudioTrack
import com.example.notifications.SalawatReminderScheduler
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
    HOME,
    FOCUS,
    QURAN,
    TASKS,
    STATS
}

enum class StudySubSection {
    FOCUS_LOCK,
    STUDY_AUDIO
}

enum class TasksSubSection {
    DAILY_TASKS,
    TREE_FARM
}

enum class SessionTimerMode {
    TIMER,
    STOPWATCH
}

data class FocusUiState(
    val userName: String = "",
    val nameDraft: String = "",
    val nameError: Boolean = false,
    val showLaunchIntroScreen: Boolean = true,
    val isWelcomeScreenVisible: Boolean = true,
    val canNavigateBackFromWelcome: Boolean = false,
    val selectedTab: MainTab = MainTab.HOME,
    val studySubSection: StudySubSection = StudySubSection.FOCUS_LOCK,
    val tasksSubSection: TasksSubSection = TasksSubSection.DAILY_TASKS,
    val selectedTimerMode: SessionTimerMode = SessionTimerMode.TIMER,
    val isLoadingApps: Boolean = true,
    val installedApps: List<InstalledApp> = emptyList(),
    val allowedPackages: Set<String> = emptySet(),
    val isAppsSelectionConfirmed: Boolean = false,
    val searchQuery: String = "",
    val hoursInput: String = "1",
    val minutesInput: String = "0",
    val isServiceEnabled: Boolean = false,
    val studyAppLockEnabled: Boolean = true,
    val showAccessibilityOptionalDialog: Boolean = false,
    val sessionStartMs: Long = 0L,
    val sessionEndMs: Long = 0L,
    val sessionDurationMs: Long = 0L,
    val remainingMs: Long = 0L,
    val isStopwatchActive: Boolean = false,
    val stopwatchElapsedMs: Long = 0L,
    val showStartConfirmDialog: Boolean = false,
    val showCancelSessionDialog: Boolean = false,
    val isFullScreenTimerVisible: Boolean = false,
    val autoOpenFullscreenTimer: Boolean = false,
    val dailyStudyMs: Long = 0L,
    val totalStudyMs: Long = 0L,
    val unplantedStudyMs: Long = 0L,
    val plantedTrees: List<PlantedTree> = emptyList(),
    val dailyTasks: List<DailyTask> = emptyList(),
    val newTaskInput: String = "",
    val achievementReasonResId: Int? = null,
    val uploadedTracks: List<UploadedAudioTrack> = emptyList(),
    val selectedBuiltInSound: BuiltInSound = BuiltInSound.RAIN,
    val selectedUploadedTrackId: String? = null,
    val audioVolume: Float = 0.65f,
    val audioLoopEnabled: Boolean = true,
    val isAudioPlaying: Boolean = false,
    val playingAudioTitle: String = ""
) {
    val isSessionActive: Boolean
        get() = remainingMs > 0L || isStopwatchActive

    val completedTasksCount: Int
        get() = dailyTasks.count { it.isCompleted }

    val allTasksCompleted: Boolean
        get() = dailyTasks.isNotEmpty() && dailyTasks.all { it.isCompleted }

    val selectedAllowedApps: List<InstalledApp>
        get() = installedApps.filter { it.packageName in allowedPackages }

    val selectedAudioSource: SelectedAudioSource
        get() {
            val uploaded = uploadedTracks.firstOrNull { it.id == selectedUploadedTrackId }
            return if (uploaded != null) {
                SelectedAudioSource.Uploaded(uploaded)
            } else {
                SelectedAudioSource.BuiltIn(selectedBuiltInSound)
            }
        }

    val filteredApps: List<InstalledApp>
        get() {
            val q = searchQuery.trim()
            if (q.isEmpty()) return installedApps
            return installedApps.filter {
                it.label.contains(q, ignoreCase = true) ||
                    it.packageName.contains(q, ignoreCase = true)
            }
        }
}

sealed interface FocusUiEvent {
    data class ShowMessage(val messageResId: Int) : FocusUiEvent
    data object OpenAccessibilitySettings : FocusUiEvent
    data object OpenBatterySettings : FocusUiEvent
    data object NavigateHome : FocusUiEvent
    data object LaunchAudioFilePicker : FocusUiEvent
    data object RequestNotificationPermission : FocusUiEvent
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
        observeAudioPlayback()
        refreshAll()
    }

    private fun observeAudioPlayback() {
        viewModelScope.launch {
            FocusAudioService.playbackState.collect { audio ->
                _uiState.update {
                    it.copy(
                        isAudioPlaying = audio.isPlaying,
                        playingAudioTitle = audio.activeTitle
                    )
                }
            }
        }
    }

    fun refreshAll() {
        val now = System.currentTimeMillis()
        val snapshot = repository.syncStudyProgress(now)
        val savedName = repository.getUserName()
        val savedAllowed = repository.getAllowedPackages()
        val serviceEnabled = repository.isBlockerServiceEnabled()
        val tasks = repository.loadDailyTasks()
        val uploadedTracks = repository.loadUploadedTracks()
        val builtInSound = BuiltInSound.fromId(repository.getSelectedBuiltInSoundId()) ?: BuiltInSound.RAIN
        val uploadedId = repository.getSelectedUploadedTrackId()
        val volume = repository.getAudioVolume()
        val loop = repository.isAudioLoopEnabled()
        val autoFullscreen = repository.isAutoFullscreenTimerEnabled()
        val appLockEnabled = repository.isStudyAppLockEnabled()
        val stopwatchActive = snapshot.isStopwatch && snapshot.sessionStartMs > 0L
        val remaining = if (stopwatchActive) 0L else (snapshot.sessionEndMs - now).coerceAtLeast(0L)
        val elapsed = if (stopwatchActive) (now - snapshot.sessionStartMs).coerceAtLeast(0L) else 0L

        _uiState.update { state ->
            state.copy(
                userName = savedName,
                nameDraft = if (state.nameDraft.isEmpty()) savedName else state.nameDraft,
                isWelcomeScreenVisible = savedName.isBlank(),
                canNavigateBackFromWelcome = false,
                allowedPackages = savedAllowed,
                isServiceEnabled = serviceEnabled,
                studyAppLockEnabled = appLockEnabled,
                selectedTimerMode = if (stopwatchActive) SessionTimerMode.STOPWATCH else state.selectedTimerMode,
                sessionStartMs = snapshot.sessionStartMs,
                sessionEndMs = snapshot.sessionEndMs,
                sessionDurationMs = snapshot.sessionDurationMs,
                remainingMs = remaining,
                isStopwatchActive = stopwatchActive,
                stopwatchElapsedMs = elapsed,
                dailyStudyMs = snapshot.dailyStudyMs,
                totalStudyMs = snapshot.totalStudyMs,
                unplantedStudyMs = snapshot.unplantedStudyMs,
                plantedTrees = snapshot.trees,
                dailyTasks = tasks,
                uploadedTracks = uploadedTracks,
                selectedBuiltInSound = builtInSound,
                selectedUploadedTrackId = uploadedId,
                audioVolume = volume,
                audioLoopEnabled = loop,
                autoOpenFullscreenTimer = autoFullscreen
            )
        }

        SalawatReminderScheduler.scheduleIfNeeded(getApplication())

        checkHoursAchievement(snapshot.dailyStudyMs, now)

        if (remaining > 0L || stopwatchActive) {
            startTicker()
        } else {
            tickerJob?.cancel()
        }

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
        val now = System.currentTimeMillis()
        val snapshot = repository.syncStudyProgress(now)
        val serviceEnabled = repository.isBlockerServiceEnabled()
        val tasks = repository.loadDailyTasks()
        val stopwatchActive = snapshot.isStopwatch && snapshot.sessionStartMs > 0L
        val remaining = if (stopwatchActive) 0L else (snapshot.sessionEndMs - now).coerceAtLeast(0L)
        val elapsed = if (stopwatchActive) (now - snapshot.sessionStartMs).coerceAtLeast(0L) else 0L
        val activeNow = remaining > 0L || stopwatchActive

        _uiState.update { state ->
            state.copy(
                isServiceEnabled = serviceEnabled,
                sessionStartMs = snapshot.sessionStartMs,
                sessionEndMs = snapshot.sessionEndMs,
                sessionDurationMs = snapshot.sessionDurationMs,
                remainingMs = remaining,
                isStopwatchActive = stopwatchActive,
                stopwatchElapsedMs = elapsed,
                dailyStudyMs = snapshot.dailyStudyMs,
                totalStudyMs = snapshot.totalStudyMs,
                unplantedStudyMs = snapshot.unplantedStudyMs,
                plantedTrees = snapshot.trees,
                dailyTasks = tasks,
                isFullScreenTimerVisible = if (!activeNow) false else state.isFullScreenTimerVisible
            )
        }

        checkHoursAchievement(snapshot.dailyStudyMs, now)

        if (activeNow) {
            startTicker()
        } else {
            tickerJob?.cancel()
            FocusAudioService.stop(getApplication())
        }
    }

    fun onLaunchIntroStartClick() {
        _uiState.update { it.copy(showLaunchIntroScreen = false) }
    }

    fun selectTimerMode(mode: SessionTimerMode) {
        if (_uiState.value.isSessionActive) return
        _uiState.update { it.copy(selectedTimerMode = mode) }
    }

    fun openFocusWithTimerMode(mode: SessionTimerMode) {
        _uiState.update { state ->
            state.copy(
                selectedTab = MainTab.FOCUS,
                studySubSection = StudySubSection.FOCUS_LOCK,
                selectedTimerMode = if (state.isSessionActive) state.selectedTimerMode else mode
            )
        }
    }

    fun selectTab(tab: MainTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun selectStudySubSection(section: StudySubSection) {
        _uiState.update { it.copy(selectedTab = MainTab.FOCUS, studySubSection = section) }
    }

    fun selectTasksSubSection(section: TasksSubSection) {
        _uiState.update { it.copy(selectedTab = MainTab.TASKS, tasksSubSection = section) }
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
        if (_uiState.value.isSessionActive) return
        _uiState.update { state ->
            val updated = state.allowedPackages.toMutableSet()
            if (packageName in updated) updated.remove(packageName) else updated.add(packageName)
            state.copy(allowedPackages = updated, isAppsSelectionConfirmed = false)
        }
    }

    fun selectAllFiltered() {
        if (_uiState.value.isSessionActive) return
        _uiState.update { state ->
            val updated = state.allowedPackages.toMutableSet()
            state.filteredApps.forEach { updated.add(it.packageName) }
            state.copy(allowedPackages = updated, isAppsSelectionConfirmed = false)
        }
    }

    fun clearAllFiltered() {
        if (_uiState.value.isSessionActive) return
        _uiState.update { state ->
            val updated = state.allowedPackages.toMutableSet()
            state.filteredApps.forEach { updated.remove(it.packageName) }
            state.copy(allowedPackages = updated, isAppsSelectionConfirmed = false)
        }
    }

    fun confirmAllowedAppsSelection() {
        if (_uiState.value.isSessionActive) return
        repository.saveAllowedPackages(_uiState.value.allowedPackages)
        _uiState.update { it.copy(isAppsSelectionConfirmed = true) }
        _events.tryEmit(FocusUiEvent.ShowMessage(R.string.msg_allowed_apps_confirmed))
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onHoursInputChange(input: String) {
        if (_uiState.value.isSessionActive) return
        val digitsOnly = input.filter { it.isDigit() }.take(2)
        _uiState.update { it.copy(hoursInput = digitsOnly) }
    }

    fun onMinutesInputChange(input: String) {
        if (_uiState.value.isSessionActive) return
        val digitsOnly = input.filter { it.isDigit() }.take(3)
        _uiState.update { it.copy(minutesInput = digitsOnly) }
    }

    fun onAutoFullscreenTimerChange(enabled: Boolean) {
        repository.saveAutoFullscreenTimerEnabled(enabled)
        _uiState.update { it.copy(autoOpenFullscreenTimer = enabled) }
    }

    fun openFullScreenTimer() {
        if (_uiState.value.isSessionActive) {
            _uiState.update { it.copy(isFullScreenTimerVisible = true) }
        }
    }

    fun closeFullScreenTimer() {
        _uiState.update { it.copy(isFullScreenTimerVisible = false) }
    }

    fun onStudyAppLockEnabledChange(enabled: Boolean) {
        repository.saveStudyAppLockEnabled(enabled)
        _uiState.update { it.copy(studyAppLockEnabled = enabled) }
    }

    fun onOpenBatterySettingsClick() {
        _events.tryEmit(FocusUiEvent.OpenBatterySettings)
    }

    fun onEnableServiceClick() {
        _events.tryEmit(FocusUiEvent.OpenAccessibilitySettings)
    }

    fun onStartSessionClick() {
        if (_uiState.value.isSessionActive) return
        val isStopwatch = _uiState.value.selectedTimerMode == SessionTimerMode.STOPWATCH
        val hours = _uiState.value.hoursInput.toLongOrNull() ?: 0L
        val minutes = _uiState.value.minutesInput.toLongOrNull() ?: 0L
        val totalMinutes = hours * 60L + minutes

        val serviceEnabled = repository.isBlockerServiceEnabled()
        _uiState.update { it.copy(isServiceEnabled = serviceEnabled) }

        when {
            !isStopwatch && totalMinutes <= 0L -> {
                _events.tryEmit(FocusUiEvent.ShowMessage(R.string.msg_invalid_duration))
            }
            _uiState.value.studyAppLockEnabled && !serviceEnabled -> {
                // Ask gracefully instead of forcing Android Settings every time
                _uiState.update { it.copy(showAccessibilityOptionalDialog = true) }
            }
            _uiState.value.studyAppLockEnabled && !_uiState.value.isAppsSelectionConfirmed -> {
                _uiState.update { it.copy(showStartConfirmDialog = true) }
            }
            else -> {
                confirmStartSession()
            }
        }
    }

    fun dismissAccessibilityOptionalDialog() {
        _uiState.update { it.copy(showAccessibilityOptionalDialog = false) }
    }

    fun startSessionWithoutAppLock() {
        _uiState.update { it.copy(showAccessibilityOptionalDialog = false) }
        confirmStartSession(navigateHome = false)
    }

    fun openAccessibilitySettingsFromDialog() {
        _uiState.update { it.copy(showAccessibilityOptionalDialog = false) }
        _events.tryEmit(FocusUiEvent.OpenAccessibilitySettings)
    }

    fun dismissStartConfirmDialog() {
        _uiState.update { it.copy(showStartConfirmDialog = false) }
    }

    fun confirmStartSession(navigateHome: Boolean = true) {
        val state = _uiState.value
        if (state.isSessionActive) return
        val isStopwatch = state.selectedTimerMode == SessionTimerMode.STOPWATCH
        val now = System.currentTimeMillis()
        val openFullScreen = state.autoOpenFullscreenTimer

        if (isStopwatch) {
            val startedAt = repository.startStopwatchSession(
                allowedPackages = state.allowedPackages,
                nowMs = now
            )
            _uiState.update {
                it.copy(
                    showStartConfirmDialog = false,
                    isAppsSelectionConfirmed = true,
                    sessionStartMs = startedAt,
                    sessionEndMs = Long.MAX_VALUE,
                    sessionDurationMs = 0L,
                    remainingMs = 0L,
                    isStopwatchActive = true,
                    stopwatchElapsedMs = 0L,
                    isFullScreenTimerVisible = openFullScreen
                )
            }
        } else {
            val hours = state.hoursInput.toLongOrNull() ?: 0L
            val minutes = state.minutesInput.toLongOrNull() ?: 0L
            val totalMinutes = hours * 60L + minutes
            if (totalMinutes <= 0L) {
                _uiState.update { it.copy(showStartConfirmDialog = false) }
                _events.tryEmit(FocusUiEvent.ShowMessage(R.string.msg_invalid_duration))
                return
            }

            val (endMs, durationMs) = repository.startSession(
                allowedPackages = state.allowedPackages,
                durationMinutes = totalMinutes,
                nowMs = now
            )
            _uiState.update {
                it.copy(
                    showStartConfirmDialog = false,
                    isAppsSelectionConfirmed = true,
                    sessionStartMs = now,
                    sessionEndMs = endMs,
                    sessionDurationMs = durationMs,
                    remainingMs = (endMs - now).coerceAtLeast(0L),
                    isStopwatchActive = false,
                    stopwatchElapsedMs = 0L,
                    isFullScreenTimerVisible = openFullScreen
                )
            }
        }

        startTicker()
        SalawatReminderScheduler.scheduleIfNeeded(getApplication())
        _events.tryEmit(FocusUiEvent.ShowMessage(R.string.msg_session_started))
        if (!openFullScreen && navigateHome && state.studyAppLockEnabled && state.isServiceEnabled) {
            _events.tryEmit(FocusUiEvent.NavigateHome)
        }
    }

    fun requestCancelSession() {
        if (_uiState.value.isSessionActive) {
            _uiState.update { it.copy(showCancelSessionDialog = true) }
        }
    }

    fun dismissCancelSession() {
        _uiState.update { it.copy(showCancelSessionDialog = false) }
    }

    fun confirmCancelSession() {
        val now = System.currentTimeMillis()
        val snapshot = repository.cancelSession(now)
        tickerJob?.cancel()
        FocusAudioService.stop(getApplication())

        _uiState.update {
            it.copy(
                showCancelSessionDialog = false,
                isFullScreenTimerVisible = false,
                sessionStartMs = 0L,
                sessionEndMs = 0L,
                sessionDurationMs = 0L,
                remainingMs = 0L,
                isStopwatchActive = false,
                stopwatchElapsedMs = 0L,
                dailyStudyMs = snapshot.dailyStudyMs,
                totalStudyMs = snapshot.totalStudyMs,
                unplantedStudyMs = snapshot.unplantedStudyMs,
                plantedTrees = snapshot.trees
            )
        }
        checkHoursAchievement(snapshot.dailyStudyMs, now)
        _events.tryEmit(FocusUiEvent.ShowMessage(R.string.msg_session_cancelled))
    }

    // Daily Task Tracker
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

        val allDone = updated.isNotEmpty() && updated.all { it.isCompleted }
        if (repository.tryConsumeTasksAchievement(allDone)) {
            _uiState.update { it.copy(achievementReasonResId = R.string.achievement_reason_tasks) }
        }
    }

    fun onDeleteTask(taskId: String) {
        val updated = _uiState.value.dailyTasks.filterNot { it.id == taskId }
        repository.saveDailyTasks(updated)
        _uiState.update { it.copy(dailyTasks = updated) }
    }

    fun dismissAchievementDialog() {
        _uiState.update { it.copy(achievementReasonResId = null) }
    }

    // Audio Controls
    fun selectBuiltInSound(sound: BuiltInSound) {
        repository.saveSelectedBuiltInSound(sound)
        val wasPlaying = _uiState.value.isAudioPlaying
        _uiState.update {
            it.copy(
                selectedBuiltInSound = sound,
                selectedUploadedTrackId = null
            )
        }
        if (wasPlaying) {
            startSelectedAudio()
        }
    }

    fun selectUploadedTrack(trackId: String) {
        repository.saveSelectedUploadedTrack(trackId)
        val wasPlaying = _uiState.value.isAudioPlaying
        _uiState.update { it.copy(selectedUploadedTrackId = trackId) }
        if (wasPlaying) {
            startSelectedAudio()
        }
    }

    fun onRequestUploadAudio() {
        _events.tryEmit(FocusUiEvent.LaunchAudioFilePicker)
    }

    fun onAudioFilePicked(uriString: String, displayName: String) {
        val track = UploadedAudioTrack(
            id = UUID.randomUUID().toString(),
            uriString = uriString,
            displayName = displayName
        )
        val updated = _uiState.value.uploadedTracks + track
        repository.saveUploadedTracks(updated)
        repository.saveSelectedUploadedTrack(track.id)
        _uiState.update {
            it.copy(
                uploadedTracks = updated,
                selectedUploadedTrackId = track.id
            )
        }
    }

    fun deleteUploadedTrack(trackId: String) {
        val wasSelectedAndPlaying =
            _uiState.value.selectedUploadedTrackId == trackId && _uiState.value.isAudioPlaying
        val updated = _uiState.value.uploadedTracks.filterNot { it.id == trackId }
        repository.saveUploadedTracks(updated)
        val newSelectedId = if (_uiState.value.selectedUploadedTrackId == trackId) {
            null
        } else {
            _uiState.value.selectedUploadedTrackId
        }
        if (newSelectedId == null) {
            repository.saveSelectedBuiltInSound(_uiState.value.selectedBuiltInSound)
        }
        _uiState.update {
            it.copy(
                uploadedTracks = updated,
                selectedUploadedTrackId = newSelectedId
            )
        }
        if (wasSelectedAndPlaying) {
            FocusAudioService.stop(getApplication())
        }
    }

    fun onAudioVolumeChange(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        repository.saveAudioVolume(clamped)
        _uiState.update { it.copy(audioVolume = clamped) }
        FocusAudioService.updateControls(getApplication(), clamped, _uiState.value.audioLoopEnabled)
    }

    fun onAudioLoopToggle(loop: Boolean) {
        repository.saveAudioLoopEnabled(loop)
        _uiState.update { it.copy(audioLoopEnabled = loop) }
        FocusAudioService.updateControls(getApplication(), _uiState.value.audioVolume, loop)
    }

    fun onToggleAudioPlayback() {
        if (_uiState.value.isAudioPlaying) {
            FocusAudioService.stop(getApplication())
        } else {
            startSelectedAudio()
        }
    }

    fun requestNotificationPermission() {
        _events.tryEmit(FocusUiEvent.RequestNotificationPermission)
    }

    private fun startSelectedAudio() {
        val state = _uiState.value
        val app = getApplication<Application>()
        when (val source = state.selectedAudioSource) {
            is SelectedAudioSource.BuiltIn -> {
                FocusAudioService.startBuiltIn(
                    context = app,
                    sound = source.sound,
                    volume = state.audioVolume,
                    loop = state.audioLoopEnabled
                )
            }
            is SelectedAudioSource.Uploaded -> {
                FocusAudioService.startUploadedUri(
                    context = app,
                    uriString = source.track.uriString,
                    title = source.track.displayName,
                    volume = state.audioVolume,
                    loop = state.audioLoopEnabled
                )
            }
        }
    }

    private fun checkHoursAchievement(dailyStudyMs: Long, nowMs: Long) {
        if (repository.tryConsumeHoursAchievement(dailyStudyMs, nowMs)) {
            _uiState.update { it.copy(achievementReasonResId = R.string.achievement_reason_hours) }
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                val snapshot = repository.syncStudyProgress(now)
                val stopwatchActive = snapshot.isStopwatch && snapshot.sessionStartMs > 0L
                val remaining = if (stopwatchActive) 0L else (snapshot.sessionEndMs - now).coerceAtLeast(0L)
                val elapsed = if (stopwatchActive) (now - snapshot.sessionStartMs).coerceAtLeast(0L) else 0L
                val activeNow = remaining > 0L || stopwatchActive

                _uiState.update { state ->
                    state.copy(
                        sessionStartMs = snapshot.sessionStartMs,
                        remainingMs = remaining,
                        isStopwatchActive = stopwatchActive,
                        stopwatchElapsedMs = elapsed,
                        dailyStudyMs = snapshot.dailyStudyMs,
                        totalStudyMs = snapshot.totalStudyMs,
                        unplantedStudyMs = snapshot.unplantedStudyMs,
                        plantedTrees = snapshot.trees,
                        sessionEndMs = snapshot.sessionEndMs,
                        sessionDurationMs = snapshot.sessionDurationMs,
                        isFullScreenTimerVisible = if (!activeNow) false else state.isFullScreenTimerVisible
                    )
                }

                checkHoursAchievement(snapshot.dailyStudyMs, now)

                if (!activeNow) {
                    FocusAudioService.stop(getApplication())
                    break
                }
                delay(1000L)
            }
        }
    }
}
