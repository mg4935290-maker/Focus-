package com.example.quran.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.FocusUiState
import com.example.ui.formatDurationArabic
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.QuramerDeepGreen
import com.example.ui.theme.QuramerSoftGold
import com.example.ui.theme.StudySuccessGreen

@Composable
fun QuramerHubScreen(
    focusState: FocusUiState,
    quranState: QuranUiState,
    onEditNameClick: () -> Unit,
    onOpenTimerMode: () -> Unit,
    onOpenStopwatchMode: () -> Unit,
    onOpenSmartTimer: () -> Unit,
    onResumeLastReading: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenTreeFarm: () -> Unit,
    onOpenStats: () -> Unit,
    onAcceptHubQuranSuggestion: () -> Unit,
    onDismissHubQuranSuggestion: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lastSurah = quranState.surahs.firstOrNull { it.number == quranState.profile.lastViewedSurah }
    val totalReadMs = quranState.surahs.sumOf { it.totalReadMs }
    val totalListenMs = quranState.surahs.sumOf { it.totalListenedMs }
    val completedSurahsCount = quranState.surahs.count { it.isCompleted }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "quramer_hero") {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = QuramerDeepGreen),
                border = BorderStroke(1.dp, QuramerSoftGold.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quramer_hub_hero")
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
                                        Color(0xA6062018),
                                        Color(0xED062018)
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = StudySuccessGreen.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, StudySuccessGreen.copy(alpha = 0.5f)),
                                modifier = Modifier.testTag("hero_salawat_banner")
                            ) {
                                Text(
                                    text = stringResource(R.string.salawat_banner_text),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color(0xFFD1FAE5),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }

                            if (focusState.userName.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color.White.copy(alpha = 0.14f),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable(onClick = onEditNameClick)
                                        .testTag("hub_edit_name_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = focusState.userName,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color(0xFFFAF0D1),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Icon(
                                            imageVector = Icons.Filled.Edit,
                                            contentDescription = stringResource(R.string.cd_edit_name),
                                            tint = Color(0xFFFAF0D1),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.app_slogan),
                            style = MaterialTheme.typography.labelLarge,
                            color = QuramerSoftGold
                        )
                        Text(
                            text = stringResource(R.string.app_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFDCEAE3)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onOpenSmartTimer,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = QuramerSoftGold,
                                    contentColor = Color(0xFF281C02)
                                ),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("hub_smart_timer_cta")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.smart_select_button),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }

                            OutlinedButton(
                                onClick = onResumeLastReading,
                                border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFA7F3D0)),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("hub_resume_quran_cta")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = lastSurah?.nameArabic?.let { "متابعة $it" } ?: "المصحف",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Active Session Banner (Focus Lock or Quran Session)
        if (focusState.isSessionActive || quranState.activeSession != null) {
            item(key = "active_session_banner") {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = when {
                                    quranState.activeSession != null -> "جلسة قرآن نشطة الآن"
                                    focusState.isStopwatchActive -> stringResource(R.string.stopwatch_active_title)
                                    else -> stringResource(R.string.session_active_title)
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            val ms = when {
                                quranState.activeSession != null -> quranState.activeSession.remainingMs
                                focusState.isStopwatchActive -> focusState.stopwatchElapsedMs
                                else -> focusState.remainingMs
                            }
                            Text(
                                text = formatTimerClock(ms),
                                style = MaterialTheme.typography.headlineSmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Button(
                            onClick = if (quranState.activeSession != null) onOpenSmartTimer else onOpenTimerMode,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text("فتح الجلسة")
                        }
                    }
                }
            }
        }

        // Optional Quran Suggestion Card (never auto-plays without user saying Yes)
        if (!quranState.hubSuggestionDismissed &&
            quranState.activeSession == null &&
            !quranState.playbackState.isPlaying
        ) {
            item(key = "hub_quran_suggestion") {
                val suggestedTrack = quranState.audioTracks.firstOrNull()
                val suggestedSurah = suggestedTrack?.surahNumber?.let { num ->
                    quranState.surahs.firstOrNull { it.number == num }
                } ?: quranState.surahs.firstOrNull { !it.isCompleted }
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                    ),
                    border = BorderStroke(1.dp, QuramerSoftGold.copy(alpha = 0.65f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hub_quran_suggestion_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "اقتراح قرآني لك الآن (اختياري)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = if (suggestedTrack != null) {
                                val reciter = suggestedTrack.reciterName.ifBlank { "من مكتبتك الصوتية" }
                                val title = suggestedSurah?.nameArabic ?: suggestedTrack.displayName
                                "تحب نشغّل تلاوة «$title» ($reciter) ولا تخليها بعدين؟"
                            } else {
                                val title = suggestedSurah?.nameArabic ?: "سورة الكهف"
                                "نقترح لك ورداً لطيفاً من القرآن الكريم («$title» مثلاً). تحب تبدأ الآن؟"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onAcceptHubQuranSuggestion,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("hub_suggestion_yes")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(if (suggestedTrack != null) "أه، شغّل الآن" else "أه، افتح الاقتراح")
                            }
                            OutlinedButton(
                                onClick = onDismissHubQuranSuggestion,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("hub_suggestion_no")
                            ) {
                                Text("لا، لاحقاً")
                            }
                        }
                    }
                }
            }
        }

        // Two Main Modes: Timer & Stopwatch (no Study Mode / Quran Mode)
        item(key = "main_modes_grid") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModeActionCard(
                    title = stringResource(R.string.timer_mode_title),
                    subtitle = stringResource(R.string.timer_mode_desc),
                    icon = Icons.Filled.Timer,
                    accent = MaterialTheme.colorScheme.primary,
                    onClick = onOpenTimerMode,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("hub_timer_mode_card")
                )
                ModeActionCard(
                    title = stringResource(R.string.stopwatch_mode_title),
                    subtitle = stringResource(R.string.stopwatch_mode_desc),
                    icon = Icons.Filled.PlayArrow,
                    accent = MaterialTheme.colorScheme.secondary,
                    onClick = onOpenStopwatchMode,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("hub_stopwatch_mode_card")
                )
            }
        }

        // Focus Lock Status & Tree Farm Quick Cards
        item(key = "lock_and_farm_row") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(
                        1.dp,
                        if (focusState.isServiceEnabled) StudySuccessGreen.copy(alpha = 0.45f)
                        else MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onOpenTimerMode)
                        .testTag("hub_focus_lock_card")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (focusState.isServiceEnabled) Icons.Filled.CheckCircle else Icons.Filled.WarningAmber,
                                contentDescription = null,
                                tint = if (focusState.isServiceEnabled) StudySuccessGreen else MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Focus Lock",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = if (focusState.isServiceEnabled) "الحظر مفعل ومستعد" else "بحاجة لتفعيل الخدمة",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "التطبيقات المسموحة: ${focusState.allowedPackages.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onOpenTreeFarm)
                        .testTag("hub_tree_farm_card")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Park,
                                contentDescription = null,
                                tint = StudySuccessGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.farm_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = stringResource(R.string.stat_trees_value, focusState.plantedTrees.size),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "كل ساعة مذاكرة أو قرآن = شجرة",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Combined Summary Statistics Card
        item(key = "hub_summary_stats") {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenStats)
                    .testTag("hub_stats_summary_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
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
                                text = "ملخص الإنجاز والقرآن",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "عرض التفاصيل ←",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HubMiniMetric("مذاكرة اليوم", formatDurationArabic(focusState.dailyStudyMs), Modifier.weight(1f))
                        HubMiniMetric("قراءة القرآن", formatDurationArabic(totalReadMs), Modifier.weight(1f))
                        HubMiniMetric("استماع القرآن", formatDurationArabic(totalListenMs), Modifier.weight(1f))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HubMiniMetric("السور المكتملة", "$completedSurahsCount / 114", Modifier.weight(1f))
                        HubMiniMetric("النقاط", "${quranState.profile.points}", Modifier.weight(1f))
                        HubMiniMetric("الملفات المرفوعة", "${quranState.importedFiles.size}", Modifier.weight(1f))
                    }

                    if (quranState.importedFiles.isEmpty()) {
                        OutlinedButton(
                            onClick = onOpenLibrary,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                        ) {
                            Text("استيراد ملفات المصحف أو التلاوات الصوتية")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accent.copy(alpha = 0.14f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HubMiniMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

fun formatTimerClock(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val h = totalSeconds / 3600L
    val m = (totalSeconds % 3600L) / 60L
    val s = totalSeconds % 60L
    return if (h > 0L) {
        String.format(java.util.Locale.US, "%02d:%02d:%02d", h, m, s)
    } else {
        String.format(java.util.Locale.US, "%02d:%02d", m, s)
    }
}
