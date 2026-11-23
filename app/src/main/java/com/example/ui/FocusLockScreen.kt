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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SettingsAccessibility
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.InstalledApp
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StudySuccessGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DURATION_PRESETS = listOf(15, 25, 45, 60, 90)

@Composable
fun FocusLockScreen(
    uiState: FocusUiState,
    snackbarHostState: SnackbarHostState,
    onSelectTab: (MainTab) -> Unit,
    onEditNameClick: () -> Unit,
    onEnableServiceClick: () -> Unit,
    onDurationInputChange: (String) -> Unit,
    onSelectPresetMinutes: (Int) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleApp: (String) -> Unit,
    onSelectAllFiltered: () -> Unit,
    onClearAllFiltered: () -> Unit,
    onStartSessionClick: () -> Unit,
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
                        if (uiState.selectedTab == MainTab.FOCUS) {
                            BottomStartSessionPanel(
                                isSessionActive = uiState.isSessionActive,
                                onStartSessionClick = onStartSessionClick
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

                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (uiState.selectedTab) {
                            MainTab.FOCUS -> {
                                FocusSessionTabContent(
                                    uiState = uiState,
                                    showInlineStartButton = useNavRail,
                                    onEditNameClick = onEditNameClick,
                                    onEnableServiceClick = onEnableServiceClick,
                                    onDurationInputChange = onDurationInputChange,
                                    onSelectPresetMinutes = onSelectPresetMinutes,
                                    onSearchQueryChange = onSearchQueryChange,
                                    onToggleApp = onToggleApp,
                                    onSelectAllFiltered = onSelectAllFiltered,
                                    onClearAllFiltered = onClearAllFiltered,
                                    onStartSessionClick = onStartSessionClick
                                )
                            }
                            MainTab.TASKS_REWARD -> {
                                DailyTasksTabContent(
                                    uiState = uiState,
                                    onNewTaskInputChange = onNewTaskInputChange,
                                    onAddTaskClick = onAddTaskClick,
                                    onToggleTask = onToggleTask,
                                    onDeleteTask = onDeleteTask,
                                    onOpenRewardPicker = onOpenRewardPicker,
                                    onDismissRewardPicker = onDismissRewardPicker,
                                    onRewardSearchChange = onRewardSearchChange,
                                    onSelectRewardApp = onSelectRewardApp,
                                    onClaimRewardClick = onClaimRewardClick,
                                    onOpenRewardAppClick = onOpenRewardAppClick
                                )
                            }
                        }
                    }

                    if (useNavRail) {
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
private fun FocusBottomNavigationBar(
    selectedTab: MainTab,
    onSelectTab: (MainTab) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = selectedTab == MainTab.FOCUS,
            onClick = { onSelectTab(MainTab.FOCUS) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == MainTab.FOCUS) Icons.Filled.Lock else Icons.Outlined.Lock,
                    contentDescription = null
                )
            },
            label = { Text(stringResource(R.string.tab_focus)) },
            modifier = Modifier.testTag("nav_tab_focus")
        )
        NavigationBarItem(
            selected = selectedTab == MainTab.TASKS_REWARD,
            onClick = { onSelectTab(MainTab.TASKS_REWARD) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == MainTab.TASKS_REWARD) {
                        Icons.Filled.TaskAlt
                    } else {
                        Icons.Outlined.TaskAlt
                    },
                    contentDescription = null
                )
            },
            label = { Text(stringResource(R.string.tab_tasks_reward)) },
            modifier = Modifier.testTag("nav_tab_tasks_reward")
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
            selected = selectedTab == MainTab.TASKS_REWARD,
            onClick = { onSelectTab(MainTab.TASKS_REWARD) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == MainTab.TASKS_REWARD) {
                        Icons.Filled.TaskAlt
                    } else {
                        Icons.Outlined.TaskAlt
                    },
                    contentDescription = null
                )
            },
            label = { Text(stringResource(R.string.tab_tasks_reward)) },
            modifier = Modifier.testTag("rail_tab_tasks_reward")
        )
    }
}

@Composable
private fun FocusSessionTabContent(
    uiState: FocusUiState,
    showInlineStartButton: Boolean,
    onEditNameClick: () -> Unit,
    onEnableServiceClick: () -> Unit,
    onDurationInputChange: (String) -> Unit,
    onSelectPresetMinutes: (Int) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleApp: (String) -> Unit,
    onSelectAllFiltered: () -> Unit,
    onClearAllFiltered: () -> Unit,
    onStartSessionClick: () -> Unit
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
                        onEditNameClick = onEditNameClick
                    )
                    AccessibilityStatusCard(
                        isEnabled = uiState.isServiceEnabled,
                        onEnableClick = onEnableServiceClick
                    )
                    DurationSelectorCard(
                        durationInput = uiState.durationInput,
                        onDurationInputChange = onDurationInputChange,
                        onSelectPreset = onSelectPresetMinutes
                    )
                    if (showInlineStartButton) {
                        StartSessionButton(
                            isSessionActive = uiState.isSessionActive,
                            onStartSessionClick = onStartSessionClick
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
                        searchQuery = uiState.searchQuery,
                        onSearchQueryChange = onSearchQueryChange,
                        onSelectAll = onSelectAllFiltered,
                        onClearAll = onClearAllFiltered
                    )
                    AllowedAppsListContainer(
                        isLoading = uiState.isLoadingApps,
                        apps = uiState.filteredApps,
                        allowedPackages = uiState.allowedPackages,
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
                        onEditNameClick = onEditNameClick
                    )
                }
                item(key = "service_status") {
                    AccessibilityStatusCard(
                        isEnabled = uiState.isServiceEnabled,
                        onEnableClick = onEnableServiceClick
                    )
                }
                item(key = "duration") {
                    DurationSelectorCard(
                        durationInput = uiState.durationInput,
                        onDurationInputChange = onDurationInputChange,
                        onSelectPreset = onSelectPresetMinutes
                    )
                }
                item(key = "apps_header") {
                    AllowedAppsHeader(
                        allowedCount = uiState.allowedPackages.size,
                        searchQuery = uiState.searchQuery,
                        onSearchQueryChange = onSearchQueryChange,
                        onSelectAll = onSelectAllFiltered,
                        onClearAll = onClearAllFiltered
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
    onEditNameClick: () -> Unit
) {
    val progress = remember(uiState.remainingMs, uiState.sessionDurationMs) {
        if (uiState.sessionDurationMs > 0L && uiState.remainingMs > 0L) {
            (uiState.remainingMs.toFloat() / uiState.sessionDurationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    }

    val ringTrackColor = Color.White.copy(alpha = 0.20f)
    val ringActiveColor = Color(0xFF34D399)
    val ringAccentColor = Color(0xFFFBBF24)

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0E2244)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("focus_hero_card")
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_study_hero_1795442323794),
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
                                Color(0xB30B1B3A),
                                Color(0xE60B1730)
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
                                text = stringResource(R.string.session_active_title),
                                style = MaterialTheme.typography.labelLarge,
                                color = ringActiveColor
                            )
                            Text(
                                text = formatCountdown(uiState.remainingMs),
                                style = MaterialTheme.typography.displaySmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = Color.White,
                                modifier = Modifier.testTag("countdown_timer_text")
                            )
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

                Text(
                    text = stringResource(
                        R.string.session_allowed_summary,
                        uiState.allowedPackages.size
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFA7F3D0)
                )

                CreatedByGalileoFooter(
                    modifier = Modifier.fillMaxWidth(),
                    textColor = Color(0xFFCBD5E1)
                )
            }
        }
    }
}

@Composable
private fun AccessibilityStatusCard(
    isEnabled: Boolean,
    onEnableClick: () -> Unit
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
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DurationSelectorCard(
    durationInput: String,
    onDurationInputChange: (String) -> Unit,
    onSelectPreset: (Int) -> Unit
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

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DURATION_PRESETS.forEach { preset ->
                    val selected = durationInput == preset.toString()
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectPreset(preset) },
                        label = {
                            Text(
                                text = "$preset ${stringResource(R.string.unit_minutes_short)}",
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("duration_preset_$preset")
                    )
                }
            }

            OutlinedTextField(
                value = durationInput,
                onValueChange = onDurationInputChange,
                label = { Text(stringResource(R.string.hint_duration_minutes)) },
                suffix = { Text(stringResource(R.string.unit_minutes)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("duration_input")
            )
        }
    }
}

@Composable
private fun AllowedAppsHeader(
    allowedCount: Int,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearAll: () -> Unit
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
                    imageVector = Icons.Filled.Apps,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.section_allowed_apps_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
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
}

@Composable
private fun AllowedAppsListContainer(
    isLoading: Boolean,
    apps: List<InstalledApp>,
    allowedPackages: Set<String>,
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
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onToggle)
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
                onCheckedChange = { onToggle() },
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
private fun BottomStartSessionPanel(
    isSessionActive: Boolean,
    onStartSessionClick: () -> Unit
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
            StartSessionButton(
                isSessionActive = isSessionActive,
                onStartSessionClick = onStartSessionClick
            )
        }
    }
}

@Composable
private fun StartSessionButton(
    isSessionActive: Boolean,
    onStartSessionClick: () -> Unit
) {
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
            text = stringResource(
                if (isSessionActive) R.string.btn_update_session else R.string.btn_start_session
            ),
            style = MaterialTheme.typography.titleMedium
        )
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
