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
import com.example.ui.FocusLockScreen
import com.example.ui.FocusUiEvent
import com.example.ui.FocusViewModel
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
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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

                AnimatedContent(
                    targetState = uiState.isWelcomeScreenVisible,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "screen_transition"
                ) { showWelcome ->
                    if (showWelcome) {
                        WelcomeScreen(
                            nameDraft = uiState.nameDraft,
                            nameError = uiState.nameError,
                            isEditingExistingName = uiState.canNavigateBackFromWelcome,
                            onNameDraftChange = viewModel::onNameDraftChange,
                            onSubmitClick = viewModel::onSubmitNameClick,
                            onCancelEdit = viewModel::onCancelEditName
                        )
                    } else {
                        FocusLockScreen(
                            uiState = uiState,
                            snackbarHostState = snackbarHostState,
                            needsNotificationPermission = !hasNotificationPermission,
                            onRequestNotificationPermission = viewModel::requestNotificationPermission,
                            onSelectTab = viewModel::selectTab,
                            onEditNameClick = viewModel::onEditNameClick,
                            onEnableServiceClick = viewModel::onEnableServiceClick,
                            onHoursInputChange = viewModel::onHoursInputChange,
                            onMinutesInputChange = viewModel::onMinutesInputChange,
                            onAutoFullscreenChange = viewModel::onAutoFullscreenTimerChange,
                            onOpenFullScreenTimer = viewModel::openFullScreenTimer,
                            onCloseFullScreenTimer = viewModel::closeFullScreenTimer,
                            onSearchQueryChange = viewModel::onSearchQueryChange,
                            onToggleApp = viewModel::toggleApp,
                            onSelectAllFiltered = viewModel::selectAllFiltered,
                            onClearAllFiltered = viewModel::clearAllFiltered,
                            onStartSessionClick = viewModel::onStartSessionClick,
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
