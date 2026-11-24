package com.example.quran.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.quran.domain.QuranSessionMode
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StudySuccessGreen
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SmartQuranSessionScreen(
    state: QuranUiState,
    onSelectPresetMinutes: (Int) -> Unit,
    onCustomMinutesChange: (String) -> Unit,
    onSelectMode: (QuranSessionMode) -> Unit,
    onPreferUncompletedChange: (Boolean) -> Unit,
    onWithFocusLockChange: (Boolean) -> Unit,
    onOnlyImportedTextChange: (Boolean) -> Unit,
    onSmartSelectClick: () -> Unit,
    onReselectClick: () -> Unit,
    onOpenEditSurahsDialog: () -> Unit,
    onCloseEditSurahsDialog: () -> Unit,
    onToggleManualSurah: (Int) -> Unit,
    onStartSession: () -> Unit,
    onPauseResumeSession: () -> Unit,
    onCompleteCurrentSurah: () -> Unit,
    onFinishSession: () -> Unit,
    onDismissTimeExpiredKeepReading: () -> Unit,
    onOpenReaderForSurah: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.showEditSurahsDialog) {
        EditSelectedSurahsDialog(
            state = state,
            onToggleSurah = onToggleManualSurah,
            onDismiss = onCloseEditSurahsDialog
        )
    }

    val active = state.activeSession
    if (active != null && active.showTimeExpiredModal) {
        val currentSurahName = state.surahs.firstOrNull { it.number == active.currentSurahNumber }?.nameArabic
        AlertDialog(
            onDismissRequest = onDismissTimeExpiredKeepReading,
            icon = {
                Icon(
                    imageVector = Icons.Filled.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            },
            title = { Text("انتهى وقت جلسة القرآن!") },
            text = {
                Text(
                    text = if (currentSurahName != null) {
                        "انتهت المدة المحددة بينما ما زلت في $currentSurahName. هل تريد إكمال السورة الحالية أم إنهاء الجلسة الآن؟"
                    } else {
                        "انتهت المدة المحددة للجلسة. هل تريد إكمال السورة أم إنهاء الجلسة وحفظ تقدمك؟"
                    }
                )
            },
            confirmButton = {
                Button(onClick = onDismissTimeExpiredKeepReading) {
                    Text(stringResource(R.string.quran_complete_surah))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onFinishSession) {
                    Text(stringResource(R.string.quran_end_session))
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (active != null) {
            item(key = "active_quran_session") {
                ActiveQuranSessionCard(
                    state = state,
                    active = active,
                    onPauseResume = onPauseResumeSession,
                    onCompleteSurah = onCompleteCurrentSurah,
                    onFinishSession = onFinishSession,
                    onOpenReaderForSurah = onOpenReaderForSurah
                )
            }
        }

        item(key = "smart_timer_config") {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
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
                            text = "مؤقت القرآن الذكي",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "اختر مدة القراءة أو الاستماع ليقوم التطبيق بتحليل السور واختيار الأنسب لوقتك:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val presets = listOf(10, 15, 20, 30, 45, 60)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presets.forEach { preset ->
                            val isSelected = state.customMinutesInput.isEmpty() && state.targetPresetMinutes == preset
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectPresetMinutes(preset) },
                                enabled = active == null,
                                label = { Text("$preset دقيقة") },
                                modifier = Modifier.testTag("preset_minutes_$preset")
                            )
                        }
                    }

                    OutlinedTextField(
                        value = state.customMinutesInput,
                        onValueChange = onCustomMinutesChange,
                        enabled = active == null,
                        label = { Text("مدة مخصصة (بالدقائق)") },
                        placeholder = { Text("مثال: 25 أو 90") },
                        suffix = { Text(stringResource(R.string.unit_minutes)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_quran_minutes_input")
                    )

                    Text(
                        text = "نوع الجلسة",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ModeChip(
                            label = stringResource(R.string.quran_mode_reading),
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            selected = state.sessionMode == QuranSessionMode.READING,
                            enabled = active == null,
                            onClick = { onSelectMode(QuranSessionMode.READING) },
                            modifier = Modifier.weight(1f)
                        )
                        ModeChip(
                            label = stringResource(R.string.quran_mode_listening),
                            icon = Icons.Filled.Headphones,
                            selected = state.sessionMode == QuranSessionMode.LISTENING,
                            enabled = active == null,
                            onClick = { onSelectMode(QuranSessionMode.LISTENING) },
                            modifier = Modifier.weight(1f)
                        )
                        ModeChip(
                            label = stringResource(R.string.quran_mode_combined),
                            icon = Icons.Filled.AutoAwesome,
                            selected = state.sessionMode == QuranSessionMode.COMBINED,
                            enabled = active == null,
                            onClick = { onSelectMode(QuranSessionMode.COMBINED) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تفضيل السور غير المكتملة أو المفتوحة مؤخراً",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = state.preferUncompleted,
                            onCheckedChange = onPreferUncompletedChange,
                            enabled = active == null
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ربط الجلسة بقفل التطبيقات (Focus Lock)",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = state.withFocusLock,
                            onCheckedChange = onWithFocusLockChange,
                            enabled = active == null
                        )
                    }

                    val importedSurahCount = state.surahs.count { it.hasImportedText }
                    if (importedSurahCount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "الاقتصار على السور المستورد نصها ($importedSurahCount)",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = state.onlyImportedTextSurahs,
                                onCheckedChange = onOnlyImportedTextChange,
                                enabled = active == null
                            )
                        }
                    }

                    Button(
                        onClick = onSmartSelectClick,
                        enabled = active == null,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("smart_select_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.smart_select_button),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Smart Select Results Card
        item(key = "smart_selection_result") {
            val selectedList = state.effectiveSelectedSurahs
            val result = state.smartSelection
            val isManual = state.manualSurahOverrides != null
            val totalAyahs = selectedList.sumOf { it.ayahCount }
            val estimatedMinutes = if (isManual) {
                selectedList.sumOf { it.wordCount / state.profile.readingSpeedWpm.coerceAtLeast(60.0) }
            } else {
                result?.estimatedMinutes ?: 0.0
            }
            val diffMinutes = estimatedMinutes - state.effectiveTargetMinutes.toDouble()

            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("smart_selection_result_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "السور المقترحة للجلسة",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = if (isManual) "اختيار يدوي"
                                else if (result?.isApproximate == true) "تقدير تقريبي"
                                else "مُعاير بسرعتك",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricBox("المدة المطلوبة", "${state.effectiveTargetMinutes} د", Modifier.weight(1f))
                        MetricBox("الوقت المقدر", "${estimatedMinutes.roundToInt()} د", Modifier.weight(1f))
                        MetricBox("إجمالي الآيات", "$totalAyahs", Modifier.weight(1f))
                    }

                    val diffRounded = abs(diffMinutes).roundToInt()
                    val diffLabel = when {
                        diffRounded == 0 -> "مطابق تماماً للوقت المحدد"
                        diffMinutes > 0 -> "فارق تقريبي: +$diffRounded دقيقة"
                        else -> "فارق تقريبي: -$diffRounded دقيقة"
                    }
                    Text(
                        text = diffLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    selectedList.forEach { surah ->
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenReaderForSurah(surah.number) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${surah.number}. ${surah.nameArabic}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${surah.ayahCount} آية • ${surah.wordCount} كلمة",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = if (surah.isCompleted) "مكتملة ✓" else "فتح",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (surah.isCompleted) StudySuccessGreen else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onReselectClick,
                            enabled = active == null,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("reselect_surahs_button")
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.quran_reselect), maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = onOpenEditSurahsDialog,
                            enabled = active == null,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("edit_surahs_button")
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.quran_edit_surahs), maxLines = 1)
                        }
                    }

                    if (active == null) {
                        Button(
                            onClick = onStartSession,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudySuccessGreen,
                                contentColor = Color.White
                            ),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("start_quran_session_button")
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.quran_session_start),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveQuranSessionCard(
    state: QuranUiState,
    active: ActiveQuranSession,
    onPauseResume: () -> Unit,
    onCompleteSurah: () -> Unit,
    onFinishSession: () -> Unit,
    onOpenReaderForSurah: (Int) -> Unit
) {
    val currentSurah = state.surahs.firstOrNull { it.number == active.currentSurahNumber }
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_quran_session_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (active.isPaused) "الجلسة متوقفة مؤقتاً" else "جلسة القرآن نشطة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (active.focusLockEnabled) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = StudySuccessGreen.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(12.dp))
                            Text("قفل المشتتات نشط", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            Text(
                text = formatTimerClock(active.remainingMs),
                style = MaterialTheme.typography.displaySmall,
                fontFamily = JetBrainsMonoFontFamily,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "السورة الحالية (${active.currentSurahIndex + 1}/${active.surahNumbers.size}): " +
                    (currentSurah?.nameArabic ?: "سورة ${active.currentSurahNumber ?: 1}"),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onPauseResume,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = if (active.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (active.isPaused) "استئناف" else "إيقاف مؤقت", maxLines = 1)
                }

                Button(
                    onClick = onCompleteSurah,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Filled.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.quran_complete_surah), maxLines = 1)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                currentSurah?.let { s ->
                    OutlinedButton(
                        onClick = { onOpenReaderForSurah(s.number) },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                    ) {
                        Text("فتح بالمصحف")
                    }
                }

                OutlinedButton(
                    onClick = onFinishSession,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.quran_end_session), maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun EditSelectedSurahsDialog(
    state: QuranUiState,
    onToggleSurah: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val selectedNumbers = state.effectiveSelectedSurahs.map { it.number }.toSet()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعديل سور الجلسة يدوياً") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(items = state.surahs, key = { it.number }) { surah ->
                    val checked = surah.number in selectedNumbers
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleSurah(surah.number) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = checked, onCheckedChange = { onToggleSurah(surah.number) })
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${surah.number}. ${surah.nameArabic}", fontWeight = FontWeight.Medium)
                            Text(
                                text = "${surah.ayahCount} آية",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("تم")
            }
        }
    )
}

@Composable
private fun ModeChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
        ),
        modifier = modifier
            .clickable(enabled = enabled, onClick = onClick)
            .minimumInteractiveComponentSize()
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MetricBox(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}
