package com.example

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
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
                                val text = if (event.arg != null) {
                                    getString(event.messageResId, event.arg)
                                } else {
                                    getString(event.messageResId)
                                }
                                Toast.makeText(this@MainActivity, text, Toast.LENGTH_LONG).show()
                                launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    snackbarHostState.showSnackbar(text)
                                }
                            }
                            is FocusUiEvent.ShowStringMessage -> {
                                val text = getString(event.messageResId, event.textArg)
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
                            is FocusUiEvent.LaunchApp -> {
                                val launchIntent = packageManager
                                    .getLaunchIntentForPackage(event.packageName)
                                    ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                if (launchIntent != null) {
                                    startActivity(launchIntent)
                                } else {
                                    Toast.makeText(
                                        this@MainActivity,
                                        getString(R.string.msg_reward_app_launch_error),
                                        Toast.LENGTH_SHORT
                                    ).show()
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
                            onSelectTab = viewModel::selectTab,
                            onEditNameClick = viewModel::onEditNameClick,
                            onEnableServiceClick = viewModel::onEnableServiceClick,
                            onDurationInputChange = viewModel::onDurationInputChange,
                            onSelectPresetMinutes = viewModel::selectPresetMinutes,
                            onSearchQueryChange = viewModel::onSearchQueryChange,
                            onToggleApp = viewModel::toggleApp,
                            onSelectAllFiltered = viewModel::selectAllFiltered,
                            onClearAllFiltered = viewModel::clearAllFiltered,
                            onStartSessionClick = viewModel::onStartSessionClick,
                            onNewTaskInputChange = viewModel::onNewTaskInputChange,
                            onAddTaskClick = viewModel::onAddTaskClick,
                            onToggleTask = viewModel::onToggleTask,
                            onDeleteTask = viewModel::onDeleteTask,
                            onOpenRewardPicker = viewModel::openRewardAppPicker,
                            onDismissRewardPicker = viewModel::dismissRewardAppPicker,
                            onRewardSearchChange = viewModel::onRewardAppSearchChange,
                            onSelectRewardApp = viewModel::selectRewardApp,
                            onClaimRewardClick = viewModel::onClaimRewardClick,
                            onOpenRewardAppClick = viewModel::onOpenRewardAppClick
                        )
                    }
                }
            }
        }
    }
}
