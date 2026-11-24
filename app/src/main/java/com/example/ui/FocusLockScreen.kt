package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SettingsAccessibility
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.FilterChip
import com.example.quran.ui.QuranContainerScreen
import com.example.quran.ui.QuranFullPlayerModal
import com.example.quran.ui.QuranMiniPlayerBar
import com.example.quran.ui.QuranStatsAndGamificationScreen
import com.example.quran.ui.QuranSubTab
import com.example.quran.ui.QuranUiState
import com.example.quran.ui.QuranViewModel
import com.example.quran.ui.QuramerHubScreen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.BuiltInSound
import com.example.data.InstalledApp
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StudySuccessGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FocusLockScreen(
    uiState: FocusUiState,
    quranUiState: QuranUiState,
    quranViewModel: QuranViewModel,
    snackbarHostState: SnackbarHostState,
    needsNotificationPermission: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onSelectTab: (MainTab) -> Unit,
    onSelectStudySubSection: (StudySubSection) -> Unit,
    onSelectTasksSubSection: (TasksSubSection) -> Unit,
    onEditNameClick: () -> Unit,
    onEnableServiceClick: () -> Unit,
    onStudyAppLockChange: (Boolean) -> Unit,
    onOpenBatterySettingsClick: () -> Unit,
    onDismissAccessibilityOptional: () -> Unit,
    onStartWithoutAppLock: () -> Unit,
    onOpenAccessibilityFromDialog: () -> Unit,
    onHoursInputChange: (String) -> Unit,
    onMinutesInputChange: (String) -> Unit,
    onSelectTimerMode: (SessionTimerMode) -> Unit,
    onOpenWithTimerMode: (SessionTimerMode) -> Unit,
    onAutoFullscreenChange: (Boolean) -> Unit,
    onOpenFullScreenTimer: () -> Unit,
    onCloseFullScreenTimer: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleApp: (String) -> Unit,
    onSelectAllFiltered: () -> Unit,
    onClearAllFiltered: () -> Unit,
    onConfirmAllowedApps: () -> Unit,
    onStartSessionClick: () -> Unit,
    onDismissStartConfirm: () -> Unit,
    onConfirmStartSession: () -> Unit,
    onRequestCancelSession: () -> Unit,
    onDismissCancelSession: () -> Unit,
    onConfirmCancelSession: () -> Unit,
    onNewTaskInputChange: (String) -> Unit,
    onAddTaskClick: () -> Unit,
    onToggleTask: (String) -> Unit,
    onDeleteTask: (String) -> Unit,
    onDismissAchievement: () -> Unit,
    onSelectBuiltInSound: (BuiltInSound) -> Unit,
    onSelectUploadedTrack: (String) -> Unit,
    onUploadAudioClick: () -> Unit,
    onDeleteUploadedTrack: (String) -> Unit,
    onAudioVolumeChange: (Float) -> Unit,
    onAudioLoopToggle: (Boolean) -> Unit,
    onToggleAudioPlayback: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.showAccessibilityOptionalDialog) {
        AccessibilityOptionalDialog(
            onStartWithoutLock = onStartWithoutAppLock,
            onOpenSettings = onOpenAccessibilityFromDialog,
            onDismiss = onDismissAccessibilityOptional
        )
    }

    quranUiState.pendingPlaySuggestion?.let { suggestion ->
        QuranPlaySuggestionDialog(
            title = suggestion.title,
            message = suggestion.message,
            onAccept = quranViewModel::acceptPlaySuggestion,
            onDecline = quranViewModel::declinePlaySuggestion
        )
    }

    if (uiState.showStartConfirmDialog) {
        ConfirmAllowedAppsDialog(
            hours = uiState.hoursInput.toLongOrNull() ?: 0L,
            minutes = uiState.minutesInput.toLongOrNull() ?: 0L,
            isStopwatch = uiState.selectedTimerMode == SessionTimerMode.STOPWATCH,
            selectedApps = uiState.selectedAllowedApps,
            allowedCount = uiState.allowedPackages.size,
            onConfirm = onConfirmStartSession,
            onDismiss = onDismissStartConfirm
        )
    }

    if (uiState.showCancelSessionDialog) {
        AlertDialog(
            onDismissRequest = onDismissCancelSession,
            title = { Text(stringResource(R.string.cancel_session_dialog_title)) },
            text = { Text(stringResource(R.string.cancel_session_dialog_body)) },
            confirmButton = {
                Button(
                    onClick = onConfirmCancelSession,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource(R.string.cancel_session_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissCancelSession) {
                    Text(stringResource(R.string.cancel_session_dismiss))
                }
            }
        )
    }

    uiState.achievementReasonResId?.let { reasonRes ->
        AchievementCelebrationDialog(
            reasonResId = reasonRes,
            onDismiss = onDismissAchievement
        )
    }

    if (uiState.isFullScreenTimerVisible && uiState.isSessionActive) {
        FullScreenTimerScreen(
            uiState = uiState,
            onToggleAudioPlayback = onToggleAudioPlayback,
            onExitFullScreen = onCloseFullScreenTimer
        )
        return
    }

    if (quranUiState.isFullPlayerVisible) {
        QuranFullPlayerModal(
            state = quranUiState,
            onClose = quranViewModel::closeFullPlayer,
            onPlayTracks = quranViewModel::playTracks,
            onTogglePlayPause = quranViewModel::togglePlayPause,
            onSeekTo = quranViewModel::seekTo,
            onSeekBack10s = quranViewModel::seekBack10s,
            onSeekForward10s = quranViewModel::seekForward10s,
            onPrevSurah = quranViewModel::previousSurahAudio,
            onNextSurah = quranViewModel::nextSurahAudio,
            onSpeedSelected = quranViewModel::setPlaybackSpeed,
            onCycleRepeat = quranViewModel::cycleRepeatMode,
            onToggleShuffle = quranViewModel::toggleShuffle
        )
        return
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 840.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavRail) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (quranUiState.playbackState.hasActiveTrack) {
                            QuranMiniPlayerBar(
                                playback = quranUiState.playbackState,
                                surahs = quranUiState.surahs,
                                onTogglePlayPause = quranViewModel::togglePlayPause,
                                onOpenFullPlayer = quranViewModel::openFullPlayer
                            )
                        }
                        if (uiState.selectedTab == MainTab.FOCUS &&
                            uiState.studySubSection == StudySubSection.FOCUS_LOCK
                        ) {
                            BottomSessionActionPanel(
                                isSessionActive = uiState.isSessionActive,
                                onStartSessionClick = onStartSessionClick,
                                onCancelSessionClick = onRequestCancelSession
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CreatedByGalileoFooter(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                        FocusBottomNavigationBar(
                            selectedTab = uiState.selectedTab,
                            onSelectTab = onSelectTab
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    FocusNavigationRail(
                        selectedTab = uiState.selectedTab,
                        onSelectTab = onSelectTab
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        when (uiState.selectedTab) {
                            MainTab.HOME -> {
                                QuramerHubScreen(
                                    focusState = uiState,
                                    quranState = quranUiState,
                                    onEditNameClick = onEditNameClick,
                                    onOpenTimerMode = { onOpenWithTimerMode(SessionTimerMode.TIMER) },
                                    onOpenStopwatchMode = { onOpenWithTimerMode(SessionTimerMode.STOPWATCH) },
                                    onOpenSmartTimer = {
                                        quranViewModel.selectSubTab(QuranSubTab.SMART_TIMER)
                                        onSelectTab(MainTab.QURAN)
                                    },
                                    onResumeLastReading = {
                                        quranViewModel.openSurahInReader(quranUiState.profile.lastViewedSurah)
                                        quranViewModel.selectSubTab(QuranSubTab.READER)
                                        onSelectTab(MainTab.QURAN)
                                    },
                                    onOpenLibrary = {
                                        quranViewModel.selectSubTab(QuranSubTab.LIBRARY)
                                        onSelectTab(MainTab.QURAN)
                                    },
                                    onOpenTreeFarm = { onSelectTasksSubSection(TasksSubSection.TREE_FARM) },
                                    onOpenStats = { onSelectTab(MainTab.STATS) },
                                    onAcceptHubQuranSuggestion = {
                                        if (quranUiState.audioTracks.isNotEmpty()) {
                                            quranViewModel.proposeQuranListeningForStudyOrHub()
                                        } else {
                                            quranViewModel.selectSubTab(QuranSubTab.SMART_TIMER)
                                            onSelectTab(MainTab.QURAN)
                                        }
                                    },
                                    onDismissHubQuranSuggestion = quranViewModel::dismissHubSuggestion
                                )
                            }
                            MainTab.FOCUS -> {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface,
                                        tonalElevation = 1.dp,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            FilterChip(
                                                selected = uiState.studySubSection == StudySubSection.FOCUS_LOCK,
                                                onClick = { onSelectStudySubSection(StudySubSection.FOCUS_LOCK) },
                                                label = { Text(stringResource(R.string.tab_focus)) },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("study_subtab_lock")
                                            )
                                            FilterChip(
                                                selected = uiState.studySubSection == StudySubSection.STUDY_AUDIO,
                                                onClick = { onSelectStudySubSection(StudySubSection.STUDY_AUDIO) },
                                                label = { Text(stringResource(R.string.tab_audio)) },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("study_subtab_audio")
                                            )
                                        }
                                    }

                                    when (uiState.studySubSection) {
                                        StudySubSection.FOCUS_LOCK -> {
                                            FocusSessionTabContent(
                                                uiState = uiState,
                                                needsNotificationPermission = needsNotificationPermission,
                                                showInlineActionButton = useNavRail,
                                                onRequestNotificationPermission = onRequestNotificationPermission,
                                                onEditNameClick = onEditNameClick,
                                                onEnableServiceClick = onEnableServiceClick,
                                                onStudyAppLockChange = onStudyAppLockChange,
                                                onOpenBatterySettingsClick = onOpenBatterySettingsClick,
                                                onHoursInputChange = onHoursInputChange,
                                                onMinutesInputChange = onMinutesInputChange,
                                                onSelectTimerMode = onSelectTimerMode,
                                                onAutoFullscreenChange = onAutoFullscreenChange,
                                                onOpenFullScreenTimer = onOpenFullScreenTimer,
                                                onSearchQueryChange = onSearchQueryChange,
                                                onToggleApp = onToggleApp,
                                                onSelectAllFiltered = onSelectAllFiltered,
                                                onClearAllFiltered = onClearAllFiltered,
                                                onConfirmAllowedApps = onConfirmAllowedApps,
                                                onStartSessionClick = onStartSessionClick,
                                                onCancelSessionClick = onRequestCancelSession
                                            )
                                        }
                                        StudySubSection.STUDY_AUDIO -> {
                                            AudioTabContent(
                                                uiState = uiState,
                                                needsNotificationPermission = needsNotificationPermission,
                                                onRequestNotificationPermission = onRequestNotificationPermission,
                                                onSelectBuiltInSound = onSelectBuiltInSound,
                                                onSelectUploadedTrack = onSelectUploadedTrack,
                                                onUploadAudioClick = onUploadAudioClick,
                                                onDeleteUploadedTrack = onDeleteUploadedTrack,
                                                onVolumeChange = onAudioVolumeChange,
                                                onLoopToggle = onAudioLoopToggle,
                                                onTogglePlayback = onToggleAudioPlayback
                                            )
                                        }
                                    }
                                }
                            }
                            MainTab.QURAN -> {
                                QuranContainerScreen(
                                    quranState = quranUiState,
                                    quranViewModel = quranViewModel
                                )
                            }
                            MainTab.TASKS -> {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface,
                                        tonalElevation = 1.dp,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            FilterChip(
                                                selected = uiState.tasksSubSection == TasksSubSection.DAILY_TASKS,
                                                onClick = { onSelectTasksSubSection(TasksSubSection.DAILY_TASKS) },
                                                label = { Text(stringResource(R.string.tab_tasks)) },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("tasks_subtab_tasks")
                                            )
                                            FilterChip(
                                                selected = uiState.tasksSubSection == TasksSubSection.TREE_FARM,
                                                onClick = { onSelectTasksSubSection(TasksSubSection.TREE_FARM) },
                                                label = { Text(stringResource(R.string.tab_farm)) },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("tasks_subtab_farm")
                                            )
                                        }
                                    }

                                    when (uiState.tasksSubSection) {
                                        TasksSubSection.DAILY_TASKS -> {
                                            DailyTasksTabContent(
                                                uiState = uiState,
                                                onNewTaskInputChange = onNewTaskInputChange,
                                                onAddTaskClick = onAddTaskClick,
                                                onToggleTask = onToggleTask,
                                                onDeleteTask = onDeleteTask
                                            )
                                        }
                                        TasksSubSection.TREE_FARM -> {
                                            TreeFarmTabContent(uiState = uiState)
                                        }
                                    }
                                }
                            }
                            MainTab.STATS -> {
                                QuranStatsAndGamificationScreen(
                                    focusState = uiState,
                                    quranState = quranUiState,
                                    onUpdateGoals = quranViewModel::updateGoals
                                )
                            }
                        }
                    }

                    if (useNavRail) {
                        if (quranUiState.playbackState.hasActiveTrack) {
                            QuranMiniPlayerBar(
                                playback = quranUiState.playbackState,
                                surahs = quranUiState.surahs,
                                onTogglePlayPause = quranViewModel::togglePlayPause,
                                onOpenFullPlayer = quranViewModel::openFullPlayer
                            )
                        }
                        CreatedByGalileoFooter(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfirmAllowedAppsDialog(
    hours: Long,
    minutes: Long,
    isStopwatch: Boolean,
    selectedApps: List<InstalledApp>,
    allowedCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val totalMs = (hours * 60L + minutes) * 60_000L
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = stringResource(R.string.confirm_apps_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = if (isStopwatch) {
                            "الوضع: ستوب وتش (عدّاد مفتوح المدة)"
                        } else {
                            stringResource(
                                R.string.confirm_apps_duration,
                                formatDurationArabic(totalMs)
                            )
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                Text(
                    text = stringResource(R.string.confirm_apps_list_header, allowedCount),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (selectedApps.isEmpty()) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = stringResource(R.string.confirm_apps_none),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(items = selectedApps, key = { it.packageName }) { app ->
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (app.icon != null) {
                                        Image(
                                            bitmap = app.icon,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Text(
                                        text = app.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.confirm_apps_lock_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.testTag("confirm_start_session_button")
            ) {
                Text(stringResource(R.string.confirm_apps_ok))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("review_apps_button")
            ) {
                Text(stringResource(R.string.confirm_apps_review))
            }
        }
    )
}

@Composable
private fun AchievementCelebrationDialog(
    reasonResId: Int,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = stringResource(R.string.achievement_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.achievement_message),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("achievement_message_text")
                )
                Text(
                    text = stringResource(reasonResId),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("achievement_dismiss_button")
            ) {
                Text(stringResource(R.string.achievement_ok_button))
            }
        }
    )
}

@Composable
private fun FocusBottomNavigationBar(
    selectedTab: MainTab,
    onSelectTab: (MainTab) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = selectedTab == MainTab.HOME,
            onClick = { onSelectTab(MainTab.HOME) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == MainTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                    contentDescription = null
                )
            },
            label = { Text(stringResource(R.string.tab_home), maxLines = 1) },
            modifier = Modifier.testTag("nav_tab_home")
        )
        NavigationBarItem(
            selected = selectedTab == MainTab.FOCUS,
            onClick = { onSelectTab(MainTab.FOCUS) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == MainTab.FOCUS) Icons.Filled.Lock else Icons.Outlined.Lock,
                    contentDescription = null
                )
            },
            label = { Text(stringResource(R.string.tab_focus), maxLines = 1) },
            modifier = Modifier.testTag("nav_tab_focus")
        )
        NavigationBarItem(
            selected = selectedTab == MainTab.QURAN,
            onClick = { onSelectTab(MainTab.QURAN) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == MainTab.QURAN) {
                        Icons.AutoMirrored.Filled.MenuBook
                    } else {
                        Icons.AutoMirrored.Outlined.MenuBook
                    },
                    contentDescription = null
                )
            },
            label = { Text(stringResource(R.string.tab_quran), maxLines = 1) },
            modifier = Modifier.testTag("nav_tab_quran")
        )
        NavigationBarItem(
            selected = selectedTab == MainTab.TASKS,
            onClick = { onSelectTab(MainTab.TASKS) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == MainTab.TASKS) Icons.Filled.TaskAlt else Icons.Outlined.TaskAlt,
                    contentDescription = null
                )
            },
            label = { Text(stringResource(R.string.tab_tasks), maxLines = 1) },
            modifier = Modifier.testTag("nav_tab_tasks")
        )
        NavigationBarItem(
            selected = selectedTab == MainTab.STATS,
            onClick = { onSelectTab(MainTab.STATS) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == MainTab.STATS) Icons.Filled.BarChart else Icons.Outlined.BarChart,
                    contentDescription = null
                )
            },
            label = { Text(stringResource(R.string.tab_stats), maxLines = 1) },
            modifier = Modifier.testTag("nav_tab_stats")
        )
    }
}

@Composable
private fun FocusNavigationRail(
    selectedTab: MainTab,
    onSelectTab: (MainTab) -> Unit
) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Spacer(Modifier.height(16.dp))
        NavigationRailItem(
            selected = selectedTab == MainTab.HOME,
            onClick = { onSelectTab(MainTab.HOME) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == MainTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                    contentDescription = null
                )
            },
            label = { Text(stringResource(R.string.tab_home)) },
            modifier = Modifier.testTag("rail_tab_home")
        )
        NavigationRailItem(
            selected = selectedTab == MainTab.FOCUS,
            onClick = { onSelectTab(MainTab.FOCUS) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == MainTab.FOCUS) Icons.Filled.Lock else Icons.Outlined.Lock,
                    contentDescription = null
                )
            },
            label = { Text(stringResource(R.string.tab_focus)) },
            modifier = Modifier.testTag("rail_tab_focus")
        )
        NavigationRailItem(
            selected = selectedTab == MainTab.QURAN,
            onClick = { onSelectTab(MainTab.QURAN) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == MainTab.QURAN) {
                        Icons.AutoMirrored.Filled.MenuBook
                    } else {
                        Icons.AutoMirrored.Outlined.MenuBook
                    },
                    contentDescription = null
                )
            },
            label = { Text(stringResource(R.string.tab_quran)) },
            modifier = Modifier.testTag("rail_tab_quran")
        )
        NavigationRailItem(
            selected = selectedTab == MainTab.TASKS,
            onClick = { onSelectTab(MainTab.TASKS) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == MainTab.TASKS) Icons.Filled.TaskAlt else Icons.Outlined.TaskAlt,
                    contentDescription = null
                )
            },
            label = { Text(stringResource(R.string.tab_tasks)) },
            modifier = Modifier.testTag("rail_tab_tasks")
        )
        NavigationRailItem(
            selected = selectedTab == MainTab.STATS,
            onClick = { onSelectTab(MainTab.STATS) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == MainTab.STATS) Icons.Filled.BarChart else Icons.Outlined.BarChart,
                    contentDescription = null
                )
            },
            label = { Text(stringResource(R.string.tab_stats)) },
            modifier = Modifier.testTag("rail_tab_stats")
        )
    }
}

@Composable
private fun FocusSessionTabContent(
    uiState: FocusUiState,
    needsNotificationPermission: Boolean,
    showInlineActionButton: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onEditNameClick: () -> Unit,
    onEnableServiceClick: () -> Unit,
    onStudyAppLockChange: (Boolean) -> Unit,
    onOpenBatterySettingsClick: () -> Unit,
    onHoursInputChange: (String) -> Unit,
    onMinutesInputChange: (String) -> Unit,
    onSelectTimerMode: (SessionTimerMode) -> Unit,
    onAutoFullscreenChange: (Boolean) -> Unit,
    onOpenFullScreenTimer: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleApp: (String) -> Unit,
    onSelectAllFiltered: () -> Unit,
    onClearAllFiltered: () -> Unit,
    onConfirmAllowedApps: () -> Unit,
    onStartSessionClick: () -> Unit,
    onCancelSessionClick: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 700.dp

        if (isWideScreen) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.46f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FocusHeroCard(
                        uiState = uiState,
                        onEditNameClick = onEditNameClick,
                        onOpenFullScreenTimer = onOpenFullScreenTimer
                    )
                    HomeStatsCard(uiState = uiState)
                    if (needsNotificationPermission) {
                        NotificationPermissionBanner(
                            onRequestNotificationPermission = onRequestNotificationPermission
                        )
                    }
                    AccessibilityStatusCard(
                        isEnabled = uiState.isServiceEnabled,
                        onEnableClick = onEnableServiceClick,
                        onBatterySettingsClick = onOpenBatterySettingsClick
                    )
                    DurationSelectorCard(
                        selectedTimerMode = uiState.selectedTimerMode,
                        hoursInput = uiState.hoursInput,
                        minutesInput = uiState.minutesInput,
                        isSessionActive = uiState.isSessionActive,
                        autoOpenFullscreenTimer = uiState.autoOpenFullscreenTimer,
                        studyAppLockEnabled = uiState.studyAppLockEnabled,
                        onSelectTimerMode = onSelectTimerMode,
                        onHoursChange = onHoursInputChange,
                        onMinutesChange = onMinutesInputChange,
                        onAutoFullscreenChange = onAutoFullscreenChange,
                        onStudyAppLockChange = onStudyAppLockChange
                    )
                    if (showInlineActionButton) {
                        SessionActionButton(
                            isSessionActive = uiState.isSessionActive,
                            onStartSessionClick = onStartSessionClick,
                            onCancelSessionClick = onCancelSessionClick
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(0.54f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AllowedAppsHeader(
                        allowedCount = uiState.allowedPackages.size,
                        isLocked = uiState.isSessionActive,
                        isSelectionConfirmed = uiState.isAppsSelectionConfirmed,
                        searchQuery = uiState.searchQuery,
                        onSearchQueryChange = onSearchQueryChange,
                        onSelectAll = onSelectAllFiltered,
                        onClearAll = onClearAllFiltered,
                        onConfirmApps = onConfirmAllowedApps
                    )
                    AllowedAppsListContainer(
                        isLoading = uiState.isLoadingApps,
                        apps = uiState.filteredApps,
                        allowedPackages = uiState.allowedPackages,
                        isLocked = uiState.isSessionActive,
                        onToggleApp = onToggleApp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(key = "hero") {
                    FocusHeroCard(
                        uiState = uiState,
                        onEditNameClick = onEditNameClick,
                        onOpenFullScreenTimer = onOpenFullScreenTimer
                    )
                }
                item(key = "home_stats") {
                    HomeStatsCard(uiState = uiState)
                }
                if (needsNotificationPermission) {
                    item(key = "notif_permission") {
                        NotificationPermissionBanner(
                            onRequestNotificationPermission = onRequestNotificationPermission
                        )
                    }
                }
                item(key = "service_status") {
                    AccessibilityStatusCard(
                        isEnabled = uiState.isServiceEnabled,
                        onEnableClick = onEnableServiceClick,
                        onBatterySettingsClick = onOpenBatterySettingsClick
                    )
                }
                item(key = "duration") {
                    DurationSelectorCard(
                        selectedTimerMode = uiState.selectedTimerMode,
                        hoursInput = uiState.hoursInput,
                        minutesInput = uiState.minutesInput,
                        isSessionActive = uiState.isSessionActive,
                        autoOpenFullscreenTimer = uiState.autoOpenFullscreenTimer,
                        studyAppLockEnabled = uiState.studyAppLockEnabled,
                        onSelectTimerMode = onSelectTimerMode,
                        onHoursChange = onHoursInputChange,
                        onMinutesChange = onMinutesInputChange,
                        onAutoFullscreenChange = onAutoFullscreenChange,
                        onStudyAppLockChange = onStudyAppLockChange
                    )
                }
                item(key = "apps_header") {
                    AllowedAppsHeader(
                        allowedCount = uiState.allowedPackages.size,
                        isLocked = uiState.isSessionActive,
                        isSelectionConfirmed = uiState.isAppsSelectionConfirmed,
                        searchQuery = uiState.searchQuery,
                        onSearchQueryChange = onSearchQueryChange,
                        onSelectAll = onSelectAllFiltered,
                        onClearAll = onClearAllFiltered,
                        onConfirmApps = onConfirmAllowedApps
                    )
                }

                if (uiState.isLoadingApps) {
                    item(key = "loading") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (uiState.filteredApps.isEmpty()) {
                    item(key = "empty") {
                        EmptyAppsState()
                    }
                } else {
                    items(
                        items = uiState.filteredApps,
                        key = { it.packageName }
                    ) { app ->
                        AppAllowanceRow(
                            app = app,
                            isAllowed = app.packageName in uiState.allowedPackages,
                            isLocked = uiState.isSessionActive,
                            onToggle = { onToggleApp(app.packageName) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FocusHeroCard(
    uiState: FocusUiState,
    onEditNameClick: () -> Unit,
    onOpenFullScreenTimer: () -> Unit
) {
    val isStopwatch = uiState.isStopwatchActive
    val displayMs = if (isStopwatch) uiState.stopwatchElapsedMs else uiState.remainingMs
    val progress = remember(isStopwatch, displayMs, uiState.sessionDurationMs) {
        if (isStopwatch) {
            ((displayMs % 3_600_000L).toFloat() / 3_600_000f).coerceIn(0f, 1f)
        } else if (uiState.sessionDurationMs > 0L && displayMs > 0L) {
            (displayMs.toFloat() / uiState.sessionDurationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    }

    val ringTrackColor = Color.White.copy(alpha = 0.20f)
    val ringActiveColor = Color(0xFF34D399)
    val ringAccentColor = Color(0xFFFBBF24)

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF06281E)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("focus_hero_card")
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_quramer_hero_1795552750213),
                contentDescription = stringResource(R.string.cd_hero_banner),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .clip(MaterialTheme.shapes.large)
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xB3062018),
                                Color(0xEA062018)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Salawat banner inside the app hero
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF34D399).copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, Color(0xFF6EE7B7).copy(alpha = 0.55f)),
                    modifier = Modifier.testTag("hero_salawat_banner")
                ) {
                    Text(
                        text = stringResource(R.string.salawat_banner_text),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA7F3D0),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (uiState.userName.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.White.copy(alpha = 0.14f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable(onClick = onEditNameClick)
                                    .testTag("edit_name_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.greeting_user_study, uiState.userName),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color(0xFFFDE68A),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = stringResource(R.string.cd_edit_name),
                                        tint = Color(0xFFFDE68A),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White
                        )
                        Text(
                            text = stringResource(R.string.app_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFCBD5E1)
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = if (uiState.isSessionActive) {
                            ringActiveColor.copy(alpha = 0.22f)
                        } else {
                            Color.White.copy(alpha = 0.12f)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (uiState.isSessionActive) ringActiveColor else Color.White.copy(alpha = 0.25f)
                        )
                    ) {
                        Box(
                            modifier = Modifier.padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = if (uiState.isSessionActive) ringActiveColor else ringAccentColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = uiState.isSessionActive,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(84.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val strokeWidth = 7.dp.toPx()
                                    drawArc(
                                        color = ringTrackColor,
                                        startAngle = -90f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                    )
                                    drawArc(
                                        color = ringActiveColor,
                                        startAngle = -90f,
                                        sweepAngle = 360f * progress,
                                        useCenter = false,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Filled.Timer,
                                    contentDescription = null,
                                    tint = ringAccentColor,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = stringResource(
                                        if (isStopwatch) {
                                            R.string.stopwatch_active_title
                                        } else {
                                            R.string.session_active_title
                                        }
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = ringActiveColor
                                )
                                Text(
                                    text = formatCountdown(displayMs),
                                    style = MaterialTheme.typography.displaySmall,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    color = Color.White,
                                    modifier = Modifier.testTag("countdown_timer_text")
                                )
                                if (isStopwatch) {
                                    Text(
                                        text = "يتم احتساب الوقت للإحصائيات وزراعة شجرة كل ساعة • التطبيقات مقفولة",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFCBD5E1)
                                    )
                                } else {
                                    val endFormatted = remember(uiState.sessionEndMs) {
                                        SimpleDateFormat("hh:mm a", Locale.getDefault())
                                            .format(Date(uiState.sessionEndMs))
                                    }
                                    Text(
                                        text = stringResource(R.string.session_end_time_label, endFormatted),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFCBD5E1)
                                    )
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = onOpenFullScreenTimer,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("open_fullscreen_timer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.OpenInFull,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.fullscreen_timer_open_button))
                        }
                    }
                }

                if (!uiState.isSessionActive) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.session_idle_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = stringResource(R.string.session_idle_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(
                            R.string.today_study_badge,
                            formatDurationArabic(uiState.dailyStudyMs)
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFFDE68A)
                    )
                    Text(
                        text = stringResource(
                            R.string.session_allowed_summary,
                            uiState.allowedPackages.size
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFA7F3D0),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                CreatedByGalileoFooter(
                    modifier = Modifier.fillMaxWidth(),
                    textColor = Color(0xFFCBD5E1)
                )
            }
        }
    }
}

@Composable
private fun HomeStatsCard(uiState: FocusUiState) {
    val unplantedMinutes = (uiState.unplantedStudyMs / 60_000L).toInt().coerceIn(0, 59)

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_stats_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.BarChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.home_stats_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HomeStatTile(
                    icon = Icons.Filled.Schedule,
                    label = stringResource(R.string.stat_today_study_label),
                    value = formatDurationArabic(uiState.dailyStudyMs),
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("stat_today_study")
                )
                HomeStatTile(
                    icon = Icons.Filled.History,
                    label = stringResource(R.string.stat_total_study_label),
                    value = formatDurationArabic(uiState.totalStudyMs),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("stat_total_study")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HomeStatTile(
                    icon = Icons.Filled.Park,
                    label = stringResource(R.string.stat_trees_label),
                    value = stringResource(R.string.stat_trees_value, uiState.plantedTrees.size),
                    subtitle = stringResource(R.string.stat_next_tree_hint, unplantedMinutes),
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("stat_trees_count")
                )
                HomeStatTile(
                    icon = Icons.Filled.TaskAlt,
                    label = stringResource(R.string.stat_tasks_label),
                    value = stringResource(
                        R.string.stat_tasks_value,
                        uiState.completedTasksCount,
                        uiState.dailyTasks.size
                    ),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("stat_tasks_count")
                )
            }
        }
    }
}

@Composable
private fun HomeStatTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = containerColor,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun NotificationPermissionBanner(
    onRequestNotificationPermission: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("notification_permission_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.NotificationsActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = stringResource(R.string.salawat_permission_needed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = onRequestNotificationPermission,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text(stringResource(R.string.audio_permission_grant))
            }
        }
    }
}

@Composable
private fun AccessibilityOptionalDialog(
    onStartWithoutLock: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.SettingsAccessibility,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        },
        title = {
            Text("خدمة قفل التطبيقات اختيارية")
        },
        text = {
            Text(
                "خدمة حظر التطبيقات غير مفعلة حالياً من إعدادات الجهاز. " +
                    "تقدر تبدأ جلسة المذاكرة والعداد دلوقتي مباشرة من غير ما تروح الإعدادات، أو تفعّل الحظر لو حابب.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onStartWithoutLock,
                modifier = Modifier.testTag("start_without_lock_button")
            ) {
                Text("ابدأ الجلسة الآن (بدون الإعدادات)")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onOpenSettings,
                modifier = Modifier.testTag("open_accessibility_optional_button")
            ) {
                Text("تفعيل القفل من الإعدادات")
            }
        }
    )
}

@Composable
private fun QuranPlaySuggestionDialog(
    title: String,
    message: String,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDecline,
        icon = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text(title) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Button(
                onClick = onAccept,
                modifier = Modifier.testTag("suggestion_accept_button")
            ) {
                Text("أه، شغّل الآن")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDecline,
                modifier = Modifier.testTag("suggestion_decline_button")
            ) {
                Text("لا، بدون تشغيل")
            }
        }
    )
}

@Composable
private fun AccessibilityStatusCard(
    isEnabled: Boolean,
    onEnableClick: () -> Unit,
    onBatterySettingsClick: () -> Unit
) {
    val accentColor = if (isEnabled) StudySuccessGreen else MaterialTheme.colorScheme.secondary
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("service_status_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isEnabled) {
                                Icons.Filled.CheckCircle
                            } else {
                                Icons.Filled.WarningAmber
                            },
                            contentDescription = stringResource(R.string.cd_service_status_icon),
                            tint = accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            if (isEnabled) R.string.service_status_active else R.string.service_status_inactive
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(
                            if (isEnabled) R.string.service_hint_active else R.string.service_hint_inactive
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isEnabled) {
                OutlinedButton(
                    onClick = onEnableClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("enable_service_button"),
                    shape = MaterialTheme.shapes.small
                ) {
                    Icon(
                        imageVector = Icons.Filled.SettingsAccessibility,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = stringResource(R.string.btn_service_settings))
                }
            } else {
                Button(
                    onClick = onEnableClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("enable_service_button"),
                    shape = MaterialTheme.shapes.small
                ) {
                    Icon(
                        imageVector = Icons.Filled.SettingsAccessibility,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = stringResource(R.string.btn_enable_service))
                }
            }

            TextButton(
                onClick = onBatterySettingsClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("battery_settings_button")
            ) {
                Text(
                    text = "إذا كان الهاتف يقفل الخدمة تلقائياً: استثناء من توفير البطارية (اختياري)",
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun DurationSelectorCard(
    selectedTimerMode: SessionTimerMode,
    hoursInput: String,
    minutesInput: String,
    isSessionActive: Boolean,
    autoOpenFullscreenTimer: Boolean,
    studyAppLockEnabled: Boolean,
    onSelectTimerMode: (SessionTimerMode) -> Unit,
    onHoursChange: (String) -> Unit,
    onMinutesChange: (String) -> Unit,
    onAutoFullscreenChange: (Boolean) -> Unit,
    onStudyAppLockChange: (Boolean) -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.section_duration_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Timer vs Stopwatch selector (only two modes: تايمر or ستوب وتش)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = selectedTimerMode == SessionTimerMode.TIMER,
                    onClick = { onSelectTimerMode(SessionTimerMode.TIMER) },
                    enabled = !isSessionActive,
                    label = { Text(stringResource(R.string.timer_mode_title)) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mode_timer_chip")
                )
                FilterChip(
                    selected = selectedTimerMode == SessionTimerMode.STOPWATCH,
                    onClick = { onSelectTimerMode(SessionTimerMode.STOPWATCH) },
                    enabled = !isSessionActive,
                    label = { Text(stringResource(R.string.stopwatch_mode_title)) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mode_stopwatch_chip")
                )
            }

            if (selectedTimerMode == SessionTimerMode.TIMER) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = hoursInput,
                        onValueChange = onHoursChange,
                        enabled = !isSessionActive,
                        label = { Text(stringResource(R.string.label_hours)) },
                        suffix = { Text(stringResource(R.string.unit_hours)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("hours_input")
                    )

                    OutlinedTextField(
                        value = minutesInput,
                        onValueChange = onMinutesChange,
                        enabled = !isSessionActive,
                        label = { Text(stringResource(R.string.label_minutes)) },
                        suffix = { Text(stringResource(R.string.unit_minutes)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("minutes_input")
                    )
                }
            } else {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.stopwatch_mode_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (isSessionActive) {
                Text(
                    text = stringResource(R.string.duration_locked_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "تفعيل قفل التطبيقات أثناء الجلسة (يمكنك إيقافه لبدء العداد فقط بدون إعدادات)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = studyAppLockEnabled,
                    onCheckedChange = onStudyAppLockChange,
                    enabled = !isSessionActive,
                    modifier = Modifier.testTag("study_app_lock_switch")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.fullscreen_timer_auto_label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = autoOpenFullscreenTimer,
                    onCheckedChange = onAutoFullscreenChange,
                    modifier = Modifier.testTag("auto_fullscreen_switch")
                )
            }
        }
    }
}

@Composable
private fun AllowedAppsHeader(
    allowedCount: Int,
    isLocked: Boolean,
    isSelectionConfirmed: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearAll: () -> Unit,
    onConfirmApps: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isLocked) Icons.Filled.Lock else Icons.Filled.Apps,
                    contentDescription = null,
                    tint = if (isLocked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.section_allowed_apps_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (!isLocked && isSelectionConfirmed) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = StudySuccessGreen.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, StudySuccessGreen.copy(alpha = 0.5f)),
                        modifier = Modifier.testTag("apps_confirmed_badge")
                    ) {
                        Text(
                            text = stringResource(R.string.allowed_apps_confirmed_badge),
                            style = MaterialTheme.typography.labelMedium,
                            color = StudySuccessGreen,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        text = stringResource(R.string.allowed_count_badge, allowedCount),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        if (isLocked) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("apps_locked_hint")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.allowed_apps_locked_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text(stringResource(R.string.search_apps_hint)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.cd_clear_search)
                        )
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("app_search_input")
        )

        if (!isLocked) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onConfirmApps,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelectionConfirmed) {
                            StudySuccessGreen
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        contentColor = Color.White
                    ),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("confirm_apps_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.action_confirm_apps),
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = onSelectAll,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("select_all_apps_button")
                    ) {
                        Text(stringResource(R.string.action_select_all))
                    }
                    Spacer(Modifier.width(4.dp))
                    TextButton(
                        onClick = onClearAll,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("clear_all_apps_button")
                    ) {
                        Text(stringResource(R.string.action_clear_all))
                    }
                }
            }

            if (!isSelectionConfirmed) {
                Text(
                    text = stringResource(R.string.allowed_apps_confirm_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AllowedAppsListContainer(
    isLoading: Boolean,
    apps: List<InstalledApp>,
    allowedPackages: Set<String>,
    isLocked: Boolean,
    onToggleApp: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (apps.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            EmptyAppsState()
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(items = apps, key = { it.packageName }) { app ->
                AppAllowanceRow(
                    app = app,
                    isAllowed = app.packageName in allowedPackages,
                    isLocked = isLocked,
                    onToggle = { onToggleApp(app.packageName) }
                )
            }
        }
    }
}

@Composable
private fun AppAllowanceRow(
    app: InstalledApp,
    isAllowed: Boolean,
    isLocked: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (isAllowed) {
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (isAllowed) {
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isLocked) 0.78f else 1f)
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = !isLocked, onClick = onToggle)
            .testTag("app_row_${app.packageName}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (app.icon != null) {
                Image(
                    bitmap = app.icon,
                    contentDescription = stringResource(R.string.cd_app_icon, app.label),
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Apps,
                            contentDescription = stringResource(R.string.cd_app_icon, app.label),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Checkbox(
                checked = isAllowed,
                onCheckedChange = if (isLocked) null else { _ -> onToggle() },
                enabled = !isLocked,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("app_checkbox_${app.packageName}")
            )
        }
    }
}

@Composable
private fun EmptyAppsState() {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Apps,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = stringResource(R.string.empty_apps_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.empty_apps_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BottomSessionActionPanel(
    isSessionActive: Boolean,
    onStartSessionClick: () -> Unit,
    onCancelSessionClick: () -> Unit
) {
    Surface(
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            SessionActionButton(
                isSessionActive = isSessionActive,
                onStartSessionClick = onStartSessionClick,
                onCancelSessionClick = onCancelSessionClick
            )
        }
    }
}

@Composable
private fun SessionActionButton(
    isSessionActive: Boolean,
    onStartSessionClick: () -> Unit,
    onCancelSessionClick: () -> Unit
) {
    if (isSessionActive) {
        OutlinedButton(
            onClick = onCancelSessionClick,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("cancel_session_button")
        ) {
            Icon(
                imageVector = Icons.Filled.Stop,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.btn_cancel_session),
                style = MaterialTheme.typography.titleMedium
            )
        }
    } else {
        Button(
            onClick = onStartSessionClick,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("start_session_button")
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.btn_start_session),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

private fun formatCountdown(remainingMs: Long): String {
    val totalSeconds = (remainingMs / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
