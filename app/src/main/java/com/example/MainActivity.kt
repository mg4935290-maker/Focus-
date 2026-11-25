package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quran.ui.QuranUiEvent
import com.example.quran.ui.QuranViewModel
import com.example.ui.FocusLockScreen
import com.example.ui.FocusUiEvent
import com.example.ui.FocusViewModel
import com.example.ui.LaunchIntroScreen
import com.example.ui.WelcomeScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: FocusViewModel = viewModel()
                val quranViewModel: QuranViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val quranUiState by quranViewModel.uiState.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }
                val lifecycleOwner = LocalLifecycleOwner.current

                var hasNotificationPermission by remember {
                    mutableStateOf(checkNotificationPermission())
                }

                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { granted ->
                    hasNotificationPermission = granted || checkNotificationPermission()
                }

                val audioPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { uri ->
                    if (uri != null) {
                        runCatching {
                            contentResolver.takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        }
                        val name = resolveFileDisplayName(uri)
                        viewModel.onAudioFilePicked(uri.toString(), name)
                    }
                }

                val multiQuranFileLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenMultipleDocuments()
                ) { uris ->
                    if (uris.isNotEmpty()) {
                        quranViewModel.onMultipleFilesSelected(uris)
                    }
                }

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            hasNotificationPermission = checkNotificationPermission()
                            viewModel.refreshServiceAndTimer()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                LaunchedEffect(Unit) {
                    viewModel.events.collect { event ->
                        when (event) {
                            is FocusUiEvent.ShowMessage -> {
                                val text = getString(event.messageResId)
                                Toast.makeText(this@MainActivity, text, Toast.LENGTH_LONG).show()
                                launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    snackbarHostState.showSnackbar(text)
                                }
                            }
                            FocusUiEvent.OpenAccessibilitySettings -> {
                                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            }
                            FocusUiEvent.OpenBatterySettings -> {
                                runCatching {
                                    startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                                }.onFailure {
                                    runCatching {
                                        startActivity(
                                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = Uri.fromParts("package", packageName, null)
                                            }
                                        )
                                    }
                                }
                            }
                            FocusUiEvent.NavigateHome -> {
                                startActivity(
                                    Intent(Intent.ACTION_MAIN)
                                        .addCategory(Intent.CATEGORY_HOME)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                            FocusUiEvent.LaunchAudioFilePicker -> {
                                audioPickerLauncher.launch(arrayOf("audio/*"))
                            }
                            FocusUiEvent.RequestNotificationPermission -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    quranViewModel.events.collect { event ->
                        when (event) {
                            is QuranUiEvent.ShowMessage -> {
                                Toast.makeText(this@MainActivity, event.message, Toast.LENGTH_LONG).show()
                                launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    snackbarHostState.showSnackbar(event.message)
                                }
                            }
                            QuranUiEvent.LaunchMultiFilePicker -> {
                                multiQuranFileLauncher.launch(arrayOf("audio/*"))
                            }
                            is QuranUiEvent.OpenPdfViewer -> {
                                quranViewModel.openPdfViewer(event.file)
                            }
                            is QuranUiEvent.NotifyFocusLockRefresh -> {
                                viewModel.refreshServiceAndTimer()
                            }
                        }
                    }
                }

                val screenDestination = when {
                    uiState.showLaunchIntroScreen -> "launch_intro"
                    uiState.isWelcomeScreenVisible -> "welcome"
                    else -> "main"
                }

                AnimatedContent(
                    targetState = screenDestination,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "screen_transition"
                ) { destination ->
                    when (destination) {
                        "launch_intro" -> {
                            LaunchIntroScreen(
                                userName = uiState.userName,
                                onStartClick = viewModel::onLaunchIntroStartClick
                            )
                        }
                        "welcome" -> {
                            WelcomeScreen(
                                nameDraft = uiState.nameDraft,
                                nameError = uiState.nameError,
                                isEditingExistingName = uiState.canNavigateBackFromWelcome,
                                onNameDraftChange = viewModel::onNameDraftChange,
                                onSubmitClick = viewModel::onSubmitNameClick,
                                onCancelEdit = viewModel::onCancelEditName
                            )
                        }
                        else -> {
                            FocusLockScreen(
                                uiState = uiState,
                                quranUiState = quranUiState,
                                quranViewModel = quranViewModel,
                                snackbarHostState = snackbarHostState,
                                needsNotificationPermission = !hasNotificationPermission,
                                onRequestNotificationPermission = viewModel::requestNotificationPermission,
                                onSelectTab = viewModel::selectTab,
                                onSelectStudySubSection = viewModel::selectStudySubSection,
                                onOpenFocusAudio = viewModel::openFocusAudio,
                                onSelectTasksSubSection = viewModel::selectTasksSubSection,
                                onDismissSessionFinished = viewModel::dismissSessionFinishedScreen,
                                onEditNameClick = viewModel::onEditNameClick,
                                onEnableServiceClick = viewModel::onEnableServiceClick,
                                onStudyAppLockChange = viewModel::onStudyAppLockEnabledChange,
                                onOpenBatterySettingsClick = viewModel::onOpenBatterySettingsClick,
                                onDismissAccessibilityOptional = viewModel::dismissAccessibilityOptionalDialog,
                                onStartWithoutAppLock = viewModel::startSessionWithoutAppLock,
                                onOpenAccessibilityFromDialog = viewModel::openAccessibilitySettingsFromDialog,
                                onHoursInputChange = viewModel::onHoursInputChange,
                                onMinutesInputChange = viewModel::onMinutesInputChange,
                                onSelectTimerMode = viewModel::selectTimerMode,
                                onOpenWithTimerMode = viewModel::openFocusWithTimerMode,
                                onAutoFullscreenChange = viewModel::onAutoFullscreenTimerChange,
                                onOpenFullScreenTimer = viewModel::openFullScreenTimer,
                                onCloseFullScreenTimer = viewModel::closeFullScreenTimer,
                                onSearchQueryChange = viewModel::onSearchQueryChange,
                                onToggleApp = viewModel::toggleApp,
                                onSelectAllFiltered = viewModel::selectAllFiltered,
                                onClearAllFiltered = viewModel::clearAllFiltered,
                                onConfirmAllowedApps = viewModel::confirmAllowedAppsSelection,
                                onStartSessionClick = viewModel::onStartSessionClick,
                                onDismissStartConfirm = viewModel::dismissStartConfirmDialog,
                                onConfirmStartSession = viewModel::confirmStartSession,
                                onRequestCancelSession = viewModel::requestCancelSession,
                                onDismissCancelSession = viewModel::dismissCancelSession,
                                onConfirmCancelSession = viewModel::confirmCancelSession,
                                onNewTaskInputChange = viewModel::onNewTaskInputChange,
                                onAddTaskClick = viewModel::onAddTaskClick,
                                onToggleTask = viewModel::onToggleTask,
                                onDeleteTask = viewModel::onDeleteTask,
                                onDismissAchievement = viewModel::dismissAchievementDialog,
                                onSelectBuiltInSound = viewModel::selectBuiltInSound,
                                onSelectUploadedTrack = viewModel::selectUploadedTrack,
                                onUploadAudioClick = viewModel::onRequestUploadAudio,
                                onDeleteUploadedTrack = viewModel::deleteUploadedTrack,
                                onAudioVolumeChange = viewModel::onAudioVolumeChange,
                                onAudioLoopToggle = viewModel::onAudioLoopToggle,
                                onToggleAudioPlayback = viewModel::onToggleAudioPlayback
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun resolveFileDisplayName(uri: Uri): String {
        val queried = runCatching {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) cursor.getString(idx) else null
                    } else null
                }
        }.getOrNull()
        return queried?.takeIf { it.isNotBlank() }
            ?: uri.lastPathSegment
            ?: getString(R.string.audio_uploaded_title)
    }
}
