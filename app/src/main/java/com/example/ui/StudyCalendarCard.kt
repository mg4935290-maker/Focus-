package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.DailyStudyRecord
import com.example.data.FocusRepository
import com.example.data.PlantedTree
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.QuramerSoftGold
import com.example.ui.theme.StudySuccessGreen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun FocusDateAndCalendarSection(
    uiState: FocusUiState,
    modifier: Modifier = Modifier
) {
    var calendarExpanded by remember { mutableStateOf(true) }
    val todayKey = uiState.currentDateKey.ifBlank {
        remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    }
    val todayFormatted = uiState.currentDateFormatted.ifBlank {
        remember(todayKey) { FocusRepository.formatFullDateArabicFromKey(todayKey) }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("focus_daily_date_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Today,
                            contentDescription = "تاريخ اليوم",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "تاريخ اليوم",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = todayFormatted,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("focus_daily_date_text")
                    )
                }

                OutlinedButton(
                    onClick = { calendarExpanded = !calendarExpanded },
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("focus_toggle_calendar_button")
                ) {
                    Icon(
                        imageVector = if (calendarExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (calendarExpanded) "إخفاء التقويم" else "التقويم",
                        maxLines = 1
                    )
                }
            }
        }

        AnimatedVisibility(visible = calendarExpanded) {
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
fun StudyCalendarCard(
    todayKey: String,
    dailyStudyMs: Long,
    dailyHistory: List<DailyStudyRecord>,
    plantedTrees: List<PlantedTree>,
    modifier: Modifier = Modifier
) {
    val todayParts = remember(todayKey) { parseDateKey(todayKey) }
    var displayedYear by remember { mutableIntStateOf(todayParts.first) }
    var displayedMonthZeroBased by remember { mutableIntStateOf(todayParts.second - 1) }
    var selectedDateKey by remember { mutableStateOf(todayKey) }
    var previousTodayKey by remember { mutableStateOf(todayKey) }

    // Keep selected date & month synced to new day automatically when midnight passes if user had Today selected
    LaunchedEffect(todayKey) {
        if (selectedDateKey.isBlank() || selectedDateKey == previousTodayKey) {
            selectedDateKey = todayKey
            val parts = parseDateKey(todayKey)
            displayedYear = parts.first
            displayedMonthZeroBased = parts.second - 1
        }
        previousTodayKey = todayKey
    }

    val studyByDate = remember(dailyHistory, dailyStudyMs, todayKey) {
        val map = dailyHistory.associate { it.dateKey to it.durationMs }.toMutableMap()
        map[todayKey] = maxOf(map[todayKey] ?: 0L, dailyStudyMs)
        map
    }

    val treesByDate = remember(plantedTrees) {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        plantedTrees.groupingBy { fmt.format(Date(it.plantedAtMs)) }.eachCount()
    }

    val monthKey = remember(displayedYear, displayedMonthZeroBased) {
        String.format(Locale.US, "%04d-%02d", displayedYear, displayedMonthZeroBased + 1)
    }
    val monthTitle = remember(monthKey) { FocusRepository.formatMonthLabelArabic(monthKey) }
    val cells = remember(displayedYear, displayedMonthZeroBased) {
        buildCalendarCells(displayedYear, displayedMonthZeroBased)
    }

    val selectedStudyMs = studyByDate[selectedDateKey] ?: 0L
    val selectedTreesCount = treesByDate[selectedDateKey] ?: 0
    val selectedFormattedDate = remember(selectedDateKey) {
        FocusRepository.formatFullDateArabicFromKey(selectedDateKey)
    }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("study_calendar_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = monthTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("calendar_month_title")
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (displayedYear != todayParts.first || displayedMonthZeroBased != (todayParts.second - 1) || selectedDateKey != todayKey) {
                        TextButton(
                            onClick = {
                                displayedYear = todayParts.first
                                displayedMonthZeroBased = todayParts.second - 1
                                selectedDateKey = todayKey
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("calendar_today_button")
                        ) {
                            Text("اليوم")
                        }
                    }
                    IconButton(
                        onClick = {
                            if (displayedMonthZeroBased == 0) {
                                displayedMonthZeroBased = 11
                                displayedYear--
                            } else {
                                displayedMonthZeroBased--
                            }
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("calendar_prev_month_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "الشهر السابق"
                        )
                    }
                    IconButton(
                        onClick = {
                            if (displayedMonthZeroBased == 11) {
                                displayedMonthZeroBased = 0
                                displayedYear++
                            } else {
                                displayedMonthZeroBased++
                            }
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("calendar_next_month_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "الشهر التالي"
                        )
                    }
                }
            }

            // Saturday-first Arabic weekday header
            Row(modifier = Modifier.fillMaxWidth()) {
                val weekdays = listOf("س", "ح", "ن", "ث", "ر", "خ", "ج")
                weekdays.forEach { dayHeader ->
                    Text(
                        text = dayHeader,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            val rows = remember(cells) { cells.chunked(7) }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                rows.forEach { weekRow ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        weekRow.forEach { cell ->
                            if (cell == null) {
                                Spacer(modifier = Modifier.weight(1f).height(42.dp))
                            } else {
                                val isToday = cell.dateKey == todayKey
                                val isSelected = cell.dateKey == selectedDateKey
                                val studyMs = studyByDate[cell.dateKey] ?: 0L
                                val dotColor = when {
                                    studyMs >= FocusRepository.ONE_HOUR_MS -> StudySuccessGreen
                                    studyMs > 0L -> QuramerSoftGold
                                    else -> Color.Transparent
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = when {
                                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                                        isToday -> StudySuccessGreen.copy(alpha = 0.14f)
                                        else -> Color.Transparent
                                    },
                                    border = when {
                                        isSelected -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                        isToday -> BorderStroke(1.dp, StudySuccessGreen.copy(alpha = 0.7f))
                                        else -> null
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { selectedDateKey = cell.dateKey }
                                        .testTag("calendar_day_${cell.dateKey}")
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = cell.dayOfMonth.toString(),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = when {
                                                isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                                                isToday -> StudySuccessGreen
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                        if (dotColor != Color.Transparent) {
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .clip(CircleShape)
                                                    .background(dotColor)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

            // Selected Day Details Strip
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calendar_selected_day_details")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = selectedFormattedDate,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (selectedDateKey == todayKey) {
                            Surface(
                                shape = CircleShape,
                                color = StudySuccessGreen.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = "اليوم",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = StudySuccessGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (selectedStudyMs > 0L) {
                                    "المذاكرة: ${formatDurationArabic(selectedStudyMs)}"
                                } else {
                                    "لا توجد مذاكرة مسجلة في هذا اليوم"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (selectedTreesCount > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Park,
                                    contentDescription = null,
                                    tint = StudySuccessGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "$selectedTreesCount شجرة",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StudySuccessGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class CalendarDayCell(
    val dayOfMonth: Int,
    val dateKey: String
)

private fun parseDateKey(dateKey: String): Triple<Int, Int, Int> {
    val parts = dateKey.split("-")
    if (parts.size == 3) {
        val y = parts[0].toIntOrNull()
        val m = parts[1].toIntOrNull()
        val d = parts[2].toIntOrNull()
        if (y != null && m != null && d != null) {
            return Triple(y, m, d)
        }
    }
    val cal = Calendar.getInstance()
    return Triple(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
}

private fun buildCalendarCells(year: Int, monthZeroBased: Int): List<CalendarDayCell?> {
    val cal = Calendar.getInstance().apply {
        clear()
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, monthZeroBased)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    // Saturday = 0, Sunday = 1, Monday = 2, ..., Friday = 6
    val leadingBlanks = when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.SATURDAY -> 0
        Calendar.SUNDAY -> 1
        Calendar.MONDAY -> 2
        Calendar.TUESDAY -> 3
        Calendar.WEDNESDAY -> 4
        Calendar.THURSDAY -> 5
        Calendar.FRIDAY -> 6
        else -> 0
    }

    val list = ArrayList<CalendarDayCell?>(42)
    repeat(leadingBlanks) { list.add(null) }
    for (day in 1..daysInMonth) {
        val key = String.format(Locale.US, "%04d-%02d-%02d", year, monthZeroBased + 1, day)
        list.add(CalendarDayCell(dayOfMonth = day, dateKey = key))
    }
    while (list.size % 7 != 0) {
        list.add(null)
    }
    return list
}
