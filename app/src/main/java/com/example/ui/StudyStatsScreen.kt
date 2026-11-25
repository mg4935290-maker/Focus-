package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.DailyStudyRecord
import com.example.data.MonthlyStudyArchiveEntry
import com.example.data.WeeklyStudyArchiveEntry
import com.example.ui.theme.QuramerSoftGold
import com.example.ui.theme.StudySuccessGreen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class StudyStatsTab {
    DAILY_WEEKLY,
    WEEKLY_COMPARE,
    MONTHLY_ARCHIVE
}

@Composable
fun StudyStatsAndArchiveScreen(
    uiState: FocusUiState,
    modifier: Modifier = Modifier
) {
    var activeStatsTab by remember { mutableStateOf(StudyStatsTab.DAILY_WEEKLY) }

    Column(modifier = modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = activeStatsTab == StudyStatsTab.DAILY_WEEKLY,
                    onClick = { activeStatsTab = StudyStatsTab.DAILY_WEEKLY },
                    label = {
                        Text(
                            text = "اليومي والأسبوعي",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("stats_subtab_daily_weekly")
                )
                FilterChip(
                    selected = activeStatsTab == StudyStatsTab.WEEKLY_COMPARE,
                    onClick = { activeStatsTab = StudyStatsTab.WEEKLY_COMPARE },
                    label = {
                        Text(
                            text = "مقارنة الأسابيع",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("stats_subtab_weekly_compare")
                )
                FilterChip(
                    selected = activeStatsTab == StudyStatsTab.MONTHLY_ARCHIVE,
                    onClick = { activeStatsTab = StudyStatsTab.MONTHLY_ARCHIVE },
                    label = {
                        Text(
                            text = "الأرشيف الشهري",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("stats_subtab_monthly_archive")
                )
            }
        }

        when (activeStatsTab) {
            StudyStatsTab.DAILY_WEEKLY -> DailyAndWeeklyOverviewTab(uiState = uiState)
            StudyStatsTab.WEEKLY_COMPARE -> WeeklyComparisonArchiveTab(uiState = uiState)
            StudyStatsTab.MONTHLY_ARCHIVE -> MonthlyArchiveAndComparisonTab(uiState = uiState)
        }
    }
}

@Composable
private fun DailyAndWeeklyOverviewTab(uiState: FocusUiState) {
    val last7Days = remember(uiState.dailyHistory, uiState.dailyStudyMs) {
        buildLast7DaysBars(uiState.dailyHistory, uiState.dailyStudyMs)
    }
    val todayMs = last7Days.lastOrNull()?.durationMs ?: uiState.dailyStudyMs
    val yesterdayMs = if (last7Days.size >= 2) last7Days[last7Days.lastIndex - 1].durationMs else 0L

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("study_stats_daily_weekly_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "overview_summary_cards") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StudyStatMetricCard(
                        icon = Icons.Filled.Schedule,
                        title = stringResource(R.string.stat_today_study_label),
                        value = formatDurationArabic(todayMs),
                        subtitle = "إنجاز اليوم",
                        accent = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stats_today_card")
                    )
                    StudyStatMetricCard(
                        icon = Icons.Filled.DateRange,
                        title = stringResource(R.string.stat_week_study_label),
                        value = formatDurationArabic(uiState.weeklyStudyMs),
                        subtitle = "${uiState.weeklyActiveDays} أيام نشاط • ${uiState.weeklyTreesCount} شجرة",
                        accent = StudySuccessGreen,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stats_week_card")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StudyStatMetricCard(
                        icon = Icons.Filled.CalendarMonth,
                        title = uiState.currentMonthLabel.ifBlank { stringResource(R.string.stat_month_study_label) },
                        value = formatDurationArabic(uiState.monthlyStudyMs),
                        subtitle = "${uiState.monthlyActiveDays} يوم • ${uiState.monthlyTreesCount} شجرة",
                        accent = QuramerSoftGold,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stats_month_card")
                    )
                    StudyStatMetricCard(
                        icon = Icons.Filled.Park,
                        title = stringResource(R.string.stat_trees_label),
                        value = stringResource(R.string.stat_trees_value, uiState.plantedTrees.size),
                        subtitle = "إجمالي المذاكرة: ${formatDurationArabic(uiState.totalStudyMs)}",
                        accent = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stats_trees_total_card")
                    )
                }
            }
        }

        item(key = "today_vs_yesterday") {
            PeriodDeltaCard(
                title = "مقارنة اليوم مع الأمس",
                currentLabel = "اليوم",
                currentMs = todayMs,
                previousLabel = "الأمس",
                previousMs = yesterdayMs
            )
        }

        item(key = "weekly_bar_chart") {
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
                        Column {
                            Text(
                                text = "مذاكرة آخر ٧ أيام",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (uiState.currentWeekLabel.isNotBlank()) {
                                Text(
                                    text = "الأسبوع الحالي: ${uiState.currentWeekLabel}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    val maxDayMs = last7Days.maxOfOrNull { it.durationMs }?.coerceAtLeast(1L) ?: 1L
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(148.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        last7Days.forEach { day ->
                            val frac = (day.durationMs.toFloat() / maxDayMs.toFloat()).coerceIn(0f, 1f)
                            val minutes = (day.durationMs / 60_000L).toInt()
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                Text(
                                    text = if (minutes > 0) "${minutes}د" else "0",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .width(22.dp)
                                        .height((92.dp * frac).coerceAtLeast(6.dp))
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(
                                            if (day.isToday) {
                                                StudySuccessGreen
                                            } else if (day.durationMs > 0L) {
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant
                                            }
                                        )
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = day.dayLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        item(key = "stats_study_calendar") {
            val todayKey = uiState.currentDateKey.ifBlank {
                java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            }
            StudyCalendarCard(
                todayKey = todayKey,
                dailyStudyMs = uiState.dailyStudyMs,
                dailyHistory = uiState.dailyHistory,
                plantedTrees = uiState.plantedTrees
            )
        }
    }
}

@Composable
private fun WeeklyComparisonArchiveTab(uiState: FocusUiState) {
    val currentWeekEntry = remember(
        uiState.currentWeekLabel,
        uiState.weeklyStudyMs,
        uiState.weeklyTreesCount,
        uiState.weeklyActiveDays
    ) {
        WeeklyStudyArchiveEntry(
            weekKey = "CURRENT_WEEK",
            label = "الأسبوع الحالي (${uiState.currentWeekLabel.ifBlank { "الآن" }})",
            startDateKey = "",
            endDateKey = "",
            studyMs = uiState.weeklyStudyMs,
            treesCount = uiState.weeklyTreesCount,
            activeDays = uiState.weeklyActiveDays
        )
    }

    val allWeeks = remember(currentWeekEntry, uiState.weeklyArchive) {
        listOf(currentWeekEntry) + uiState.weeklyArchive
    }

    var firstWeekKey by remember(allWeeks) {
        mutableStateOf(allWeeks.firstOrNull()?.weekKey ?: "")
    }
    var secondWeekKey by remember(allWeeks) {
        mutableStateOf(allWeeks.getOrNull(1)?.weekKey ?: allWeeks.firstOrNull()?.weekKey ?: "")
    }

    val firstWeek = allWeeks.firstOrNull { it.weekKey == firstWeekKey } ?: currentWeekEntry
    val secondWeek = allWeeks.firstOrNull { it.weekKey == secondWeekKey }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("study_stats_weekly_compare_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "weekly_auto_compare") {
            val prevWeek = uiState.weeklyArchive.firstOrNull()
            if (prevWeek != null) {
                PeriodDeltaCard(
                    title = "هذا الأسبوع مقارنةً بالأسبوع الماضي",
                    currentLabel = "هذا الأسبوع",
                    currentMs = uiState.weeklyStudyMs,
                    previousLabel = prevWeek.label,
                    previousMs = prevWeek.studyMs,
                    extraNote = "الأشجار: ${uiState.weeklyTreesCount} مقابل ${prevWeek.treesCount} • أيام النشاط: ${uiState.weeklyActiveDays} مقابل ${prevWeek.activeDays}"
                )
            } else {
                InfoEmptyCard(
                    icon = Icons.AutoMirrored.Filled.CompareArrows,
                    title = "مقارنة الأسابيع التلقائية",
                    body = "عند نهاية الأسبوع الحالي (الجمعة)، سيُحفظ تلقائياً في الأرشيف لتظهر مقارنة فورية بين كل أسبوع والذي يليه."
                )
            }
        }

        if (allWeeks.size >= 2) {
            item(key = "weekly_interactive_compare") {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "قارن بين أي أسبوعين من الأرشيف",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text("الأسبوع الأول:", style = MaterialTheme.typography.labelMedium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(items = allWeeks, key = { "w1_${it.weekKey}" }) { week ->
                                FilterChip(
                                    selected = week.weekKey == firstWeek.weekKey,
                                    onClick = { firstWeekKey = week.weekKey },
                                    label = { Text(week.label, maxLines = 1) }
                                )
                            }
                        }

                        Text("الأسبوع الثاني:", style = MaterialTheme.typography.labelMedium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(items = allWeeks, key = { "w2_${it.weekKey}" }) { week ->
                                FilterChip(
                                    selected = week.weekKey == secondWeek?.weekKey,
                                    onClick = { secondWeekKey = week.weekKey },
                                    label = { Text(week.label, maxLines = 1) }
                                )
                            }
                        }

                        if (secondWeek != null) {
                            HorizontalDivider()
                            TwoPeriodComparisonTable(
                                labelA = firstWeek.label,
                                studyMsA = firstWeek.studyMs,
                                treesA = firstWeek.treesCount,
                                daysA = firstWeek.activeDays,
                                labelB = secondWeek.label,
                                studyMsB = secondWeek.studyMs,
                                treesB = secondWeek.treesCount,
                                daysB = secondWeek.activeDays
                            )
                        }
                    }
                }
            }
        }

        item(key = "weekly_archive_header") {
            Text(
                text = "سجل الأسابيع المؤرشفة (${uiState.weeklyArchive.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (uiState.weeklyArchive.isEmpty()) {
            item(key = "weekly_archive_empty") {
                InfoEmptyCard(
                    icon = Icons.Filled.Archive,
                    title = "لا توجد أسابيع مؤرشفة بعد",
                    body = "الأسبوع الحالي (${uiState.currentWeekLabel}) قيد التسجيل الآن وسيُضاف للأرشيف فور انتهائه."
                )
            }
        } else {
            items(items = uiState.weeklyArchive, key = { it.weekKey }) { week ->
                val idx = uiState.weeklyArchive.indexOf(week)
                val olderWeek = uiState.weeklyArchive.getOrNull(idx + 1)
                ArchiveEntryCard(
                    title = week.label,
                    studyMs = week.studyMs,
                    treesCount = week.treesCount,
                    activeDays = week.activeDays,
                    previousStudyMs = olderWeek?.studyMs
                )
            }
        }
    }
}

@Composable
private fun MonthlyArchiveAndComparisonTab(uiState: FocusUiState) {
    val currentMonthEntry = remember(
        uiState.currentMonthLabel,
        uiState.monthlyStudyMs,
        uiState.monthlyTreesCount,
        uiState.monthlyActiveDays
    ) {
        MonthlyStudyArchiveEntry(
            monthKey = "CURRENT_MONTH",
            label = "${uiState.currentMonthLabel.ifBlank { "الشهر الحالي" }} (الحالي)",
            studyMs = uiState.monthlyStudyMs,
            treesCount = uiState.monthlyTreesCount,
            activeDays = uiState.monthlyActiveDays
        )
    }

    val allMonths = remember(currentMonthEntry, uiState.monthlyArchive) {
        listOf(currentMonthEntry) + uiState.monthlyArchive
    }

    var firstMonthKey by remember(allMonths) {
        mutableStateOf(allMonths.firstOrNull()?.monthKey ?: "")
    }
    var secondMonthKey by remember(allMonths) {
        mutableStateOf(allMonths.getOrNull(1)?.monthKey ?: allMonths.firstOrNull()?.monthKey ?: "")
    }

    val firstMonth = allMonths.firstOrNull { it.monthKey == firstMonthKey } ?: currentMonthEntry
    val secondMonth = allMonths.firstOrNull { it.monthKey == secondMonthKey }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("study_stats_monthly_archive_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "monthly_reset_notice") {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "تُصفَّر إحصائيات الشهر الحالي تلقائياً مع بداية كل شهر ميلادي، ويُحفظ سجل كل شهر في الأرشيف الشهري للمقارنة.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        item(key = "current_month_card") {
            ArchiveEntryCard(
                title = "الشهر الحالي: ${uiState.currentMonthLabel}",
                studyMs = uiState.monthlyStudyMs,
                treesCount = uiState.monthlyTreesCount,
                activeDays = uiState.monthlyActiveDays,
                previousStudyMs = uiState.monthlyArchive.firstOrNull()?.studyMs,
                isCurrentHighlighted = true
            )
        }

        if (allMonths.size >= 2) {
            item(key = "monthly_interactive_compare") {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, QuramerSoftGold.copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = null, tint = QuramerSoftGold)
                            Text(
                                text = "مقارنة بين أي شهرين من الأرشيف",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text("الشهر الأول:", style = MaterialTheme.typography.labelMedium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(items = allMonths, key = { "m1_${it.monthKey}" }) { m ->
                                FilterChip(
                                    selected = m.monthKey == firstMonth.monthKey,
                                    onClick = { firstMonthKey = m.monthKey },
                                    label = { Text(m.label, maxLines = 1) }
                                )
                            }
                        }

                        Text("الشهر الثاني:", style = MaterialTheme.typography.labelMedium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(items = allMonths, key = { "m2_${it.monthKey}" }) { m ->
                                FilterChip(
                                    selected = m.monthKey == secondMonth?.monthKey,
                                    onClick = { secondMonthKey = m.monthKey },
                                    label = { Text(m.label, maxLines = 1) }
                                )
                            }
                        }

                        if (secondMonth != null) {
                            HorizontalDivider()
                            TwoPeriodComparisonTable(
                                labelA = firstMonth.label,
                                studyMsA = firstMonth.studyMs,
                                treesA = firstMonth.treesCount,
                                daysA = firstMonth.activeDays,
                                labelB = secondMonth.label,
                                studyMsB = secondMonth.studyMs,
                                treesB = secondMonth.treesCount,
                                daysB = secondMonth.activeDays
                            )
                        }
                    }
                }
            }
        }

        item(key = "monthly_archive_list_header") {
            Text(
                text = "أرشيف الشهور السابقة (${uiState.monthlyArchive.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (uiState.monthlyArchive.isEmpty()) {
            item(key = "monthly_archive_empty") {
                InfoEmptyCard(
                    icon = Icons.Filled.Archive,
                    title = "لا توجد شهور مؤرشفة بعد",
                    body = "سيظهر هنا أرشيف كل شهر تلقائياً بمجرد انتهاء ${uiState.currentMonthLabel.ifBlank { "هذا الشهر" }} وبدء الشهر الجديد."
                )
            }
        } else {
            items(items = uiState.monthlyArchive, key = { it.monthKey }) { month ->
                val idx = uiState.monthlyArchive.indexOf(month)
                val olderMonth = uiState.monthlyArchive.getOrNull(idx + 1)
                ArchiveEntryCard(
                    title = "أرشيف ${month.label}",
                    studyMs = month.studyMs,
                    treesCount = month.treesCount,
                    activeDays = month.activeDays,
                    previousStudyMs = olderMonth?.studyMs
                )
            }
        }
    }
}

@Composable
private fun StudyStatMetricCard(
    icon: ImageVector,
    title: String,
    value: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = accent.copy(alpha = 0.14f),
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PeriodDeltaCard(
    title: String,
    currentLabel: String,
    currentMs: Long,
    previousLabel: String,
    previousMs: Long,
    extraNote: String? = null
) {
    val diffMs = currentMs - previousMs
    val isUp = diffMs >= 0L
    val deltaColor = if (isUp) StudySuccessGreen else MaterialTheme.colorScheme.error
    val percentText = formatPercentChange(currentMs, previousMs)

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, deltaColor.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = CircleShape,
                    color = deltaColor.copy(alpha = 0.14f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isUp) {
                                Icons.AutoMirrored.Filled.TrendingUp
                            } else {
                                Icons.AutoMirrored.Filled.TrendingDown
                            },
                            contentDescription = null,
                            tint = deltaColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = percentText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = deltaColor
                        )
                    }
                }
            }

            Text(
                text = "$currentLabel: ${formatDurationArabic(currentMs)} • $previousLabel: ${formatDurationArabic(previousMs)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val diffFormatted = formatDurationArabic(abs(diffMs))
            Text(
                text = when {
                    diffMs > 0L -> "زيادة بمقدار $diffFormatted مقارنة بـ $previousLabel"
                    diffMs < 0L -> "أقل بمقدار $diffFormatted مقارنة بـ $previousLabel"
                    else -> "نفس مدة المذاكرة المسجلة في $previousLabel"
                },
                style = MaterialTheme.typography.labelMedium,
                color = deltaColor,
                fontWeight = FontWeight.SemiBold
            )

            if (!extraNote.isNullOrBlank()) {
                Text(
                    text = extraNote,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TwoPeriodComparisonTable(
    labelA: String,
    studyMsA: Long,
    treesA: Int,
    daysA: Int,
    labelB: String,
    studyMsB: Long,
    treesB: Int,
    daysB: Int
) {
    val diffMs = studyMsA - studyMsB
    val diffColor = if (diffMs >= 0L) StudySuccessGreen else MaterialTheme.colorScheme.error

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ComparisonColumnBox(
                title = labelA,
                duration = formatDurationArabic(studyMsA),
                trees = "$treesA شجرة",
                days = "$daysA يوم نشاط",
                highlight = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                modifier = Modifier.weight(1f)
            )
            ComparisonColumnBox(
                title = labelB,
                duration = formatDurationArabic(studyMsB),
                trees = "$treesB شجرة",
                days = "$daysB يوم نشاط",
                highlight = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                modifier = Modifier.weight(1f)
            )
        }

        Surface(
            shape = MaterialTheme.shapes.small,
            color = diffColor.copy(alpha = 0.12f),
            modifier = Modifier.fillMaxWidth()
        ) {
            val prefix = if (diffMs > 0L) "تفوق الأول بـ +" else if (diffMs < 0L) "أقل بـ -" else "متساويان ("
            val suffix = if (diffMs == 0L) ")" else ""
            val treesDiff = treesA - treesB
            val treesNote = if (treesDiff >= 0) "+$treesDiff شجرة" else "$treesDiff شجرة"
            Text(
                text = "$prefix${formatDurationArabic(abs(diffMs))}$suffix (${formatPercentChange(studyMsA, studyMsB)}) • الفرق في الأشجار: $treesNote",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = diffColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(10.dp)
            )
        }
    }
}

@Composable
private fun ComparisonColumnBox(
    title: String,
    duration: String,
    trees: String,
    days: String,
    highlight: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = highlight,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = duration,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(text = trees, style = MaterialTheme.typography.labelSmall)
            Text(text = days, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ArchiveEntryCard(
    title: String,
    studyMs: Long,
    treesCount: Int,
    activeDays: Int,
    previousStudyMs: Long?,
    isCurrentHighlighted: Boolean = false
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (isCurrentHighlighted) StudySuccessGreen.copy(alpha = 0.55f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (previousStudyMs != null) {
                    val up = studyMs >= previousStudyMs
                    val badgeColor = if (up) StudySuccessGreen else MaterialTheme.colorScheme.error
                    Surface(
                        shape = CircleShape,
                        color = badgeColor.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = formatPercentChange(studyMs, previousStudyMs),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "المذاكرة: ${formatDurationArabic(studyMs)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "$treesCount شجرة • $activeDays يوم",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InfoEmptyCard(
    icon: ImageVector,
    title: String,
    body: String
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(text = body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private data class DayChartBar(
    val dateKey: String,
    val dayLabel: String,
    val durationMs: Long,
    val isToday: Boolean
)

private fun buildLast7DaysBars(
    history: List<DailyStudyRecord>,
    todayFallbackMs: Long
): List<DayChartBar> {
    val map = history.associate { it.dateKey to it.durationMs }
    val sdfKey = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val todayKey = sdfKey.format(Date())
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        add(Calendar.DAY_OF_YEAR, -6)
    }

    return buildList {
        for (i in 0 until 7) {
            val key = sdfKey.format(cal.time)
            val label = when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SATURDAY -> "سبت"
                Calendar.SUNDAY -> "أحد"
                Calendar.MONDAY -> "إثنين"
                Calendar.TUESDAY -> "ثلاثاء"
                Calendar.WEDNESDAY -> "أربعاء"
                Calendar.THURSDAY -> "خميس"
                Calendar.FRIDAY -> "جمعة"
                else -> ""
            }
            val duration = if (key == todayKey) {
                maxOf(map[key] ?: 0L, todayFallbackMs)
            } else {
                map[key] ?: 0L
            }
            add(
                DayChartBar(
                    dateKey = key,
                    dayLabel = label,
                    durationMs = duration,
                    isToday = key == todayKey
                )
            )
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
    }
}

private fun formatPercentChange(currentMs: Long, previousMs: Long): String {
    if (previousMs <= 0L) {
        return if (currentMs > 0L) "+100%" else "0%"
    }
    val pct = (((currentMs - previousMs).toDouble() / previousMs.toDouble()) * 100.0).roundToInt()
    return if (pct >= 0) "+$pct%" else "$pct%"
}
