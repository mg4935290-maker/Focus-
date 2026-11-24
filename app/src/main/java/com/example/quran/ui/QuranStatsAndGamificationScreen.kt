package com.example.quran.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.quran.domain.QuranSessionMode
import com.example.ui.FocusUiState
import com.example.ui.formatDurationArabic
import com.example.ui.theme.QuramerSoftGold
import com.example.ui.theme.StudySuccessGreen
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuranStatsAndGamificationScreen(
    focusState: FocusUiState,
    quranState: QuranUiState,
    onUpdateGoals: (dailyMinutes: Int, weeklyMinutes: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSection by remember { mutableIntStateOf(0) } // 0: Overview, 1: Reading Log, 2: Listening Log
    var dailyGoalInput by remember(quranState.profile.dailyGoalMinutes) {
        mutableStateOf(quranState.profile.dailyGoalMinutes.toString())
    }
    var weeklyGoalInput by remember(quranState.profile.weeklyGoalMinutes) {
        mutableStateOf(quranState.profile.weeklyGoalMinutes.toString())
    }

    val totalReadMs = quranState.surahs.sumOf { it.totalReadMs }
    val totalListenMs = quranState.surahs.sumOf { it.totalListenedMs }
    val readSurahsCount = quranState.surahs.count { it.timesRead > 0 || it.lastReadAyah > 0 }
    val completedSurahsCount = quranState.surahs.count { it.isCompleted }
    val readAyahsEstimate = quranState.surahs.sumOf { s ->
        if (s.isCompleted) s.ayahCount else s.lastReadAyah.coerceIn(0, s.ayahCount)
    }
    val topSurahs = remember(quranState.surahs) {
        quranState.surahs
            .filter { it.timesRead > 0 || it.timesListened > 0 }
            .sortedByDescending { it.timesRead + it.timesListened }
            .take(5)
    }

    val now = System.currentTimeMillis()
    val todayLogs = remember(quranState.sessionLogs) {
        quranState.sessionLogs.filter { now - it.endedAtMs <= 24L * 3_600_000L }
    }
    val weekLogs = remember(quranState.sessionLogs) {
        quranState.sessionLogs.filter { now - it.endedAtMs <= 7L * 24L * 3_600_000L }
    }
    val monthLogs = remember(quranState.sessionLogs) {
        quranState.sessionLogs.filter { now - it.endedAtMs <= 30L * 24L * 3_600_000L }
    }

    val avgSessionMs = if (quranState.sessionLogs.isEmpty()) 0L
    else quranState.sessionLogs.sumOf { it.actualDurationMs } / quranState.sessionLogs.size

    val todayQuranMinutes = (todayLogs.sumOf { it.actualDurationMs } / 60_000L).toInt()
    val weekQuranMinutes = (weekLogs.sumOf { it.actualDurationMs } / 60_000L).toInt()

    val badges = remember(quranState.profile.badgesJson) {
        val arr = runCatching { JSONArray(quranState.profile.badgesJson) }.getOrDefault(JSONArray())
        buildList {
            for (i in 0 until arr.length()) {
                add(arr.optString(i))
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "stats_sub_tabs") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedSection == 0,
                    onClick = { selectedSection = 0 },
                    label = { Text("الإحصائيات والشارات") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedSection == 1,
                    onClick = { selectedSection = 1 },
                    label = { Text("سجل القراءة") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedSection == 2,
                    onClick = { selectedSection = 2 },
                    label = { Text("سجل الاستماع") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        when (selectedSection) {
            0 -> {
                // Gamification Points, Stars & Streaks
                item(key = "gamification_card") {
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, QuramerSoftGold.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quran_gamification_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("نظام التحفيز والإنجاز", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                GamificationBadgeTile("النقاط", "${quranState.profile.points}", Icons.Filled.EmojiEvents, Modifier.weight(1f))
                                GamificationBadgeTile("النجوم", "${quranState.profile.stars}", Icons.Filled.Star, Modifier.weight(1f))
                                GamificationBadgeTile("سلسلة القراءة", "${quranState.profile.readingStreakDays} يوم", Icons.Filled.LocalFireDepartment, Modifier.weight(1f))
                                GamificationBadgeTile("سلسلة الاستماع", "${quranState.profile.listeningStreakDays} يوم", Icons.Filled.LocalFireDepartment, Modifier.weight(1f))
                            }

                            Text("الأوسمة والشارات (${badges.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            if (badges.isEmpty()) {
                                Text("أكمل أول جلسة قرآن أو سورة لفتح أول وسام لك.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    badges.forEach { badge ->
                                        AssistChip(
                                            onClick = {},
                                            label = { Text(translateBadge(badge)) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Daily & Weekly Goals
                item(key = "goals_card") {
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("الأهداف اليومية والأسبوعية", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            val dailyTarget = quranState.profile.dailyGoalMinutes.coerceAtLeast(1)
                            val weeklyTarget = quranState.profile.weeklyGoalMinutes.coerceAtLeast(1)
                            Text("الهدف اليومي: $todayQuranMinutes / $dailyTarget دقيقة", style = MaterialTheme.typography.bodySmall)
                            LinearProgressIndicator(
                                progress = { (todayQuranMinutes.toFloat() / dailyTarget).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text("الهدف الأسبوعي: $weekQuranMinutes / $weeklyTarget دقيقة", style = MaterialTheme.typography.bodySmall)
                            LinearProgressIndicator(
                                progress = { (weekQuranMinutes.toFloat() / weeklyTarget).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = dailyGoalInput,
                                    onValueChange = { dailyGoalInput = it.filter { c -> c.isDigit() }.take(3) },
                                    label = { Text("هدف يومي (د)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = weeklyGoalInput,
                                    onValueChange = { weeklyGoalInput = it.filter { c -> c.isDigit() }.take(4) },
                                    label = { Text("هدف أسبوعي (د)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Button(
                                onClick = {
                                    onUpdateGoals(
                                        dailyGoalInput.toIntOrNull() ?: 20,
                                        weeklyGoalInput.toIntOrNull() ?: 120
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("حفظ الأهداف")
                            }
                        }
                    }
                }

                // Detailed Analytics
                item(key = "quran_analytics") {
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("إحصائيات القرآن والمذاكرة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            StatRowLine("إجمالي وقت القراءة", formatDurationArabic(totalReadMs))
                            StatRowLine("إجمالي وقت الاستماع", formatDurationArabic(totalListenMs))
                            StatRowLine("إجمالي وقت المذاكرة", formatDurationArabic(focusState.totalStudyMs))
                            StatRowLine("عدد جلسات القرآن", "${quranState.sessionLogs.size}")
                            StatRowLine("السور المقروءة / المكتملة", "$readSurahsCount / $completedSurahsCount من 114")
                            StatRowLine("عدد الآيات المقروءة تقريباً", "$readAyahsEstimate")
                            StatRowLine("متوسط مدة الجلسة", formatDurationArabic(avgSessionMs))
                            StatRowLine("أشجار المزرعة المزروعة", "${focusState.plantedTrees.size} شجرة")
                            StatRowLine("جلسات اليوم / الأسبوع / الشهر", "${todayLogs.size} / ${weekLogs.size} / ${monthLogs.size}")
                        }
                    }
                }

                if (topSurahs.isNotEmpty()) {
                    item(key = "top_surahs") {
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("أكثر السور قراءةً واستماعاً", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                topSurahs.forEach { s ->
                                    StatRowLine(s.nameArabic, "قراءة ${s.timesRead} • استماع ${s.timesListened}")
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                val readLogs = quranState.sessionLogs.filter {
                    it.mode == QuranSessionMode.READING.name || it.mode == QuranSessionMode.COMBINED.name
                }
                if (readLogs.isEmpty()) {
                    item { EmptyHistoryCard("لا يوجد سجل قراءة قرآن بعد.") }
                } else {
                    items(items = readLogs, key = { it.id }) { log ->
                        SessionLogRowCard(log = log, surahs = quranState.surahs)
                    }
                }
            }

            else -> {
                val listenLogs = quranState.sessionLogs.filter {
                    it.mode == QuranSessionMode.LISTENING.name || it.mode == QuranSessionMode.COMBINED.name
                }
                if (listenLogs.isEmpty()) {
                    item { EmptyHistoryCard("لا يوجد سجل استماع تلاوات بعد.") }
                } else {
                    items(items = listenLogs, key = { it.id }) { log ->
                        SessionLogRowCard(log = log, surahs = quranState.surahs)
                    }
                }
            }
        }
    }
}

@Composable
private fun GamificationBadgeTile(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, tint = QuramerSoftGold, modifier = Modifier.size(18.dp))
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatRowLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EmptyHistoryCard(message: String) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(20.dp)
        )
    }
}

@Composable
private fun SessionLogRowCard(
    log: com.example.quran.db.QuranSessionLogEntity,
    surahs: List<com.example.quran.db.SurahEntity>
) {
    val dateStr = remember(log.endedAtMs) {
        SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale.getDefault()).format(Date(log.endedAtMs))
    }
    val surahNames = log.surahNumbersCsv.split(',')
        .mapNotNull { it.toIntOrNull() }
        .mapNotNull { n -> surahs.firstOrNull { it.number == n }?.nameArabic }
        .joinToString("، ")

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = surahNames.ifBlank { "تلاوة قرآنية" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatDurationArabic(log.actualDurationMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = StudySuccessGreen
                )
            }
            Text(
                text = dateStr,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun translateBadge(id: String): String = when (id) {
    "FIRST_QURAN_SESSION" -> "بداية مباركة 🌱"
    "FIRST_SURAH_COMPLETED" -> "ختم سورة ⭐"
    "STREAK_3_DAYS" -> "مواظب 3 أيام 🔥"
    "STREAK_7_DAYS" -> "همّة أسبوع 🏆"
    else -> id
}
