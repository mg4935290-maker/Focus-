package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.DailyTask
import com.example.data.InstalledApp
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StudySuccessGreen
import java.util.Locale

@Composable
fun DailyTasksTabContent(
    uiState: FocusUiState,
    onNewTaskInputChange: (String) -> Unit,
    onAddTaskClick: () -> Unit,
    onToggleTask: (String) -> Unit,
    onDeleteTask: (String) -> Unit,
    onOpenRewardPicker: () -> Unit,
    onDismissRewardPicker: () -> Unit,
    onRewardSearchChange: (String) -> Unit,
    onSelectRewardApp: (String) -> Unit,
    onClaimRewardClick: () -> Unit,
    onOpenRewardAppClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.isRewardAppPickerOpen) {
        RewardAppPickerDialog(
            apps = uiState.filteredRewardPickerApps,
            selectedPackage = uiState.rewardPackageName,
            searchQuery = uiState.rewardAppSearchQuery,
            onSearchChange = onRewardSearchChange,
            onSelectApp = onSelectRewardApp,
            onDismiss = onDismissRewardPicker
        )
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 700.dp

        if (isWide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.45f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DailyRewardCard(
                        uiState = uiState,
                        onOpenRewardPicker = onOpenRewardPicker,
                        onClaimRewardClick = onClaimRewardClick,
                        onOpenRewardAppClick = onOpenRewardAppClick
                    )
                    DailyProgressSummaryCard(uiState = uiState)
                }

                Column(
                    modifier = Modifier
                        .weight(0.55f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AddTaskRow(
                        input = uiState.newTaskInput,
                        onInputChange = onNewTaskInputChange,
                        onAddClick = onAddTaskClick
                    )
                    TasksListBody(
                        tasks = uiState.dailyTasks,
                        onToggleTask = onToggleTask,
                        onDeleteTask = onDeleteTask,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item(key = "reward_card") {
                    DailyRewardCard(
                        uiState = uiState,
                        onOpenRewardPicker = onOpenRewardPicker,
                        onClaimRewardClick = onClaimRewardClick,
                        onOpenRewardAppClick = onOpenRewardAppClick
                    )
                }

                item(key = "tasks_header") {
                    DailyProgressSummaryCard(uiState = uiState)
                }

                item(key = "add_task_row") {
                    AddTaskRow(
                        input = uiState.newTaskInput,
                        onInputChange = onNewTaskInputChange,
                        onAddClick = onAddTaskClick
                    )
                }

                if (uiState.dailyTasks.isEmpty()) {
                    item(key = "empty_tasks") {
                        EmptyDailyTasksState()
                    }
                } else {
                    items(items = uiState.dailyTasks, key = { it.id }) { task ->
                        DailyTaskRow(
                            task = task,
                            onToggle = { onToggleTask(task.id) },
                            onDelete = { onDeleteTask(task.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyRewardCard(
    uiState: FocusUiState,
    onOpenRewardPicker: () -> Unit,
    onClaimRewardClick: () -> Unit,
    onOpenRewardAppClick: () -> Unit
) {
    val rewardApp = uiState.selectedRewardApp
    val borderColor = when {
        uiState.isRewardActive -> StudySuccessGreen
        uiState.canClaimReward -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("daily_reward_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (uiState.isRewardActive) {
                        StudySuccessGreen.copy(alpha = 0.16f)
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (uiState.isRewardActive) {
                                Icons.Filled.LockOpen
                            } else {
                                Icons.Filled.CardGiftcard
                            },
                            contentDescription = null,
                            tint = if (uiState.isRewardActive) {
                                StudySuccessGreen
                            } else {
                                MaterialTheme.colorScheme.secondary
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.reward_card_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val remainingTasks = (uiState.dailyTasks.size - uiState.completedTasksCount).coerceAtLeast(0)
                    val statusText = when {
                        uiState.isRewardActive -> stringResource(R.string.reward_status_active)
                        uiState.isRewardClaimedToday -> stringResource(R.string.reward_status_claimed_today)
                        uiState.dailyTasks.isEmpty() -> stringResource(R.string.reward_status_locked_no_tasks)
                        !uiState.allTasksCompleted -> stringResource(
                            R.string.reward_status_locked_incomplete,
                            remainingTasks
                        )
                        uiState.rewardPackageName.isNullOrBlank() -> stringResource(
                            R.string.reward_status_locked_pick_app
                        )
                        else -> stringResource(R.string.reward_status_ready)
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Selected Reward App Row
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (rewardApp?.icon != null) {
                        Image(
                            bitmap = rewardApp.icon,
                            contentDescription = stringResource(R.string.cd_app_icon, rewardApp.label),
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Apps,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Text(
                        text = if (rewardApp != null) {
                            stringResource(R.string.reward_selected_app, rewardApp.label)
                        } else if (!uiState.rewardPackageName.isNullOrBlank()) {
                            stringResource(R.string.reward_selected_app, uiState.rewardPackageName)
                        } else {
                            stringResource(R.string.reward_app_not_selected)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedButton(
                        onClick = onOpenRewardPicker,
                        enabled = !uiState.isRewardActive,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("choose_reward_app_button")
                    ) {
                        Text(
                            text = stringResource(
                                if (uiState.rewardPackageName.isNullOrBlank()) {
                                    R.string.reward_choose_app
                                } else {
                                    R.string.reward_change_app
                                }
                            )
                        )
                    }
                }
            }

            if (uiState.isRewardActive) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = StudySuccessGreen.copy(alpha = 0.14f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Timer,
                                contentDescription = null,
                                tint = StudySuccessGreen
                            )
                            Text(
                                text = stringResource(
                                    R.string.reward_remaining_time,
                                    formatHourCountdown(uiState.rewardRemainingMs)
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = StudySuccessGreen,
                                modifier = Modifier.testTag("reward_countdown_text")
                            )
                        }
                    }
                }

                Button(
                    onClick = onOpenRewardAppClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudySuccessGreen,
                        contentColor = Color.White
                    ),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("open_reward_app_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.reward_open_app_button))
                }
            } else if (!uiState.isRewardClaimedToday) {
                Button(
                    onClick = onClaimRewardClick,
                    enabled = uiState.canClaimReward,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("claim_reward_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.CardGiftcard,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.reward_claim_button))
                }
            }
        }
    }
}

@Composable
private fun DailyProgressSummaryCard(uiState: FocusUiState) {
    val total = uiState.dailyTasks.size
    val done = uiState.completedTasksCount
    val fraction = if (total > 0) done.toFloat() / total.toFloat() else 0f

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
                        imageVector = Icons.Filled.TaskAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.daily_tasks_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (uiState.allTasksCompleted) {
                        MaterialTheme.colorScheme.tertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    }
                ) {
                    Text(
                        text = stringResource(R.string.daily_tasks_progress, done, total),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (uiState.allTasksCompleted) {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { fraction },
                color = if (uiState.allTasksCompleted) {
                    StudySuccessGreen
                } else {
                    MaterialTheme.colorScheme.primary
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
private fun AddTaskRow(
    input: String,
    onInputChange: (String) -> Unit,
    onAddClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            placeholder = { Text(stringResource(R.string.daily_tasks_input_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onAddClick() }),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier
                .weight(1f)
                .testTag("new_task_input")
        )

        Button(
            onClick = onAddClick,
            enabled = input.trim().isNotEmpty(),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier
                .height(54.dp)
                .testTag("add_task_button")
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.daily_tasks_add_button))
        }
    }
}

@Composable
private fun TasksListBody(
    tasks: List<DailyTask>,
    onToggleTask: (String) -> Unit,
    onDeleteTask: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (tasks.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            EmptyDailyTasksState()
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            items(items = tasks, key = { it.id }) { task ->
                DailyTaskRow(
                    task = task,
                    onToggle = { onToggleTask(task.id) },
                    onDelete = { onDeleteTask(task.id) }
                )
            }
        }
    }
}

@Composable
private fun DailyTaskRow(
    task: DailyTask,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (task.isCompleted) {
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            1.dp,
            if (task.isCompleted) {
                StudySuccessGreen.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onToggle)
            .testTag("task_row_${task.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggle() },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("task_checkbox_${task.id}")
            )

            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (task.isCompleted) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("delete_task_${task.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = stringResource(R.string.cd_delete_task),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun EmptyDailyTasksState() {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = stringResource(R.string.daily_tasks_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.daily_tasks_empty_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RewardAppPickerDialog(
    apps: List<InstalledApp>,
    selectedPackage: String?,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onSelectApp: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.reward_picker_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text(stringResource(R.string.search_apps_hint)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.cd_clear_search)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(items = apps, key = { it.packageName }) { app ->
                        val isSelected = app.packageName == selectedPackage
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .clickable { onSelectApp(app.packageName) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (app.icon != null) {
                                    Image(
                                        bitmap = app.icon,
                                        contentDescription = stringResource(R.string.cd_app_icon, app.label),
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.Apps,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Text(
                                    text = app.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.reward_picker_close))
            }
        }
    )
}

private fun formatHourCountdown(remainingMs: Long): String {
    val totalSeconds = (remainingMs / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
