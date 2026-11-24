package com.example.quran.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.quran.audio.QuranAudioPlaybackState
import com.example.quran.audio.QuranRepeatMode
import com.example.quran.db.QuranAudioTrackEntity
import com.example.quran.db.SurahEntity
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.QuramerDeepGreen
import com.example.ui.theme.QuramerSoftGold

@Composable
fun QuranMiniPlayerBar(
    playback: QuranAudioPlaybackState,
    surahs: List<SurahEntity>,
    onTogglePlayPause: () -> Unit,
    onOpenFullPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = playback.currentTrack ?: return
    val surahName = track.surahNumber?.let { num ->
        surahs.firstOrNull { it.number == num }?.nameArabic
    }
    val progress = if (playback.durationMs > 0L) {
        (playback.positionMs.toFloat() / playback.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenFullPlayer)
            .testTag("quran_mini_player_bar")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = QuramerSoftGold
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Headphones, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = surahName ?: track.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${track.reciterName.ifBlank { "قارئ غير محدد" }} • ${formatTimerClock(playback.positionMs)} / ${formatTimerClock(playback.durationMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onTogglePlayPause) {
                    Icon(
                        imageVector = if (playback.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (playback.isPlaying) "إيقاف مؤقت" else "تشغيل",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onOpenFullPlayer) {
                    Icon(Icons.Filled.ExpandLess, contentDescription = "فتح المشغل الكامل")
                }
            }
        }
    }
}

@Composable
fun QuranPlayerTabContent(
    state: QuranUiState,
    onPlayTracks: (List<QuranAudioTrackEntity>, Int) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekBack10s: () -> Unit,
    onSeekForward10s: () -> Unit,
    onPrevSurah: () -> Unit,
    onNextSurah: () -> Unit,
    onSpeedSelected: (Float) -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onOpenLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playback = state.playbackState
    val tracks = state.audioTracks
    val activeTrack = playback.currentTrack ?: tracks.firstOrNull()

    if (activeTrack == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Headphones,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(56.dp)
                )
                Text(
                    text = "لا توجد تلاوات صوتية مرفوعة بعد",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ارفع ملفات MP3 أو M4A أو AAC أو WAV من المكتبة لتشغيلها والتحكم الكامل بها حتى مع قفل الشاشة.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onOpenLibrary,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text("رفع ملفات من المكتبة")
                }
            }
        }
        return
    }

    QuranFullPlayerBody(
        playback = playback,
        fallbackTrack = activeTrack,
        queue = playback.queue.ifEmpty { tracks },
        surahs = state.surahs,
        showCloseButton = false,
        onClose = {},
        onPlayTracks = onPlayTracks,
        onTogglePlayPause = {
            if (!playback.hasActiveTrack && tracks.isNotEmpty()) {
                onPlayTracks(tracks, 0)
            } else {
                onTogglePlayPause()
            }
        },
        onSeekTo = onSeekTo,
        onSeekBack10s = onSeekBack10s,
        onSeekForward10s = onSeekForward10s,
        onPrevSurah = onPrevSurah,
        onNextSurah = onNextSurah,
        onSpeedSelected = onSpeedSelected,
        onCycleRepeat = onCycleRepeat,
        onToggleShuffle = onToggleShuffle,
        modifier = modifier
    )
}

@Composable
fun QuranFullPlayerModal(
    state: QuranUiState,
    onClose: () -> Unit,
    onPlayTracks: (List<QuranAudioTrackEntity>, Int) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekBack10s: () -> Unit,
    onSeekForward10s: () -> Unit,
    onPrevSurah: () -> Unit,
    onNextSurah: () -> Unit,
    onSpeedSelected: (Float) -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit
) {
    BackHandler { onClose() }
    val playback = state.playbackState
    val fallback = playback.currentTrack ?: state.audioTracks.firstOrNull() ?: return
    QuranFullPlayerBody(
        playback = playback,
        fallbackTrack = fallback,
        queue = playback.queue.ifEmpty { state.audioTracks },
        surahs = state.surahs,
        showCloseButton = true,
        onClose = onClose,
        onPlayTracks = onPlayTracks,
        onTogglePlayPause = onTogglePlayPause,
        onSeekTo = onSeekTo,
        onSeekBack10s = onSeekBack10s,
        onSeekForward10s = onSeekForward10s,
        onPrevSurah = onPrevSurah,
        onNextSurah = onNextSurah,
        onSpeedSelected = onSpeedSelected,
        onCycleRepeat = onCycleRepeat,
        onToggleShuffle = onToggleShuffle,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun QuranFullPlayerBody(
    playback: QuranAudioPlaybackState,
    fallbackTrack: QuranAudioTrackEntity,
    queue: List<QuranAudioTrackEntity>,
    surahs: List<SurahEntity>,
    showCloseButton: Boolean,
    onClose: () -> Unit,
    onPlayTracks: (List<QuranAudioTrackEntity>, Int) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekBack10s: () -> Unit,
    onSeekForward10s: () -> Unit,
    onPrevSurah: () -> Unit,
    onNextSurah: () -> Unit,
    onSpeedSelected: (Float) -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = playback.currentTrack ?: fallbackTrack
    val surahName = track.surahNumber?.let { num ->
        surahs.firstOrNull { it.number == num }?.nameArabic
    }
    val duration = playback.durationMs.coerceAtLeast(track.durationMs).coerceAtLeast(1L)
    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderDragPos by remember { mutableFloatStateOf(0f) }
    val sliderFraction = if (isDraggingSlider) {
        sliderDragPos
    } else {
        (playback.positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(QuramerDeepGreen, Color(0xFF0B2B21))
                )
            ),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (showCloseButton) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "تصغير المشغل", tint = Color.White)
                    }
                }
            }
        }

        item {
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                border = BorderStroke(1.dp, QuramerSoftGold.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = QuramerSoftGold.copy(alpha = 0.2f),
                        modifier = Modifier.size(68.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Headphones, contentDescription = null, tint = QuramerSoftGold, modifier = Modifier.size(34.dp))
                        }
                    }

                    Text(
                        text = surahName ?: track.displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${track.reciterName.ifBlank { "قارئ غير محدد" }} • ${track.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    playback.errorMessage?.let { err ->
                        Text(err, style = MaterialTheme.typography.bodySmall, color = Color(0xFFFCA5A5))
                    }

                    Slider(
                        value = sliderFraction,
                        onValueChange = { frac ->
                            isDraggingSlider = true
                            sliderDragPos = frac
                        },
                        onValueChangeFinished = {
                            val targetMs = (sliderDragPos * duration.toFloat()).toLong()
                            isDraggingSlider = false
                            onSeekTo(targetMs)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quran_player_seek_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTimerClock(playback.positionMs),
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                        Text(
                            text = "المتبقي -${formatTimerClock(playback.remainingMs)} / ${formatTimerClock(duration)}",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFCBD5E1)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onPrevSurah) {
                            Icon(Icons.Filled.SkipPrevious, contentDescription = "السورة السابقة", tint = Color.White)
                        }
                        IconButton(onClick = onSeekBack10s) {
                            Icon(Icons.Filled.Replay10, contentDescription = "رجوع 10 ثوانٍ", tint = Color.White)
                        }
                        FilledIconButton(
                            onClick = onTogglePlayPause,
                            modifier = Modifier
                                .size(62.dp)
                                .testTag("quran_player_play_pause")
                        ) {
                            Icon(
                                imageVector = if (playback.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (playback.isPlaying) "إيقاف مؤقت" else "تشغيل",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        IconButton(onClick = onSeekForward10s) {
                            Icon(Icons.Filled.Forward10, contentDescription = "تقديم 10 ثوانٍ", tint = Color.White)
                        }
                        IconButton(onClick = onNextSurah) {
                            Icon(Icons.Filled.SkipNext, contentDescription = "السورة التالية", tint = Color.White)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onToggleShuffle) {
                            Icon(
                                Icons.Filled.Shuffle,
                                contentDescription = "عشوائي",
                                tint = if (playback.shuffleEnabled) QuramerSoftGold else Color.White.copy(alpha = 0.6f)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "عشوائي",
                                color = if (playback.shuffleEnabled) QuramerSoftGold else Color.White.copy(alpha = 0.8f)
                            )
                        }

                        TextButton(onClick = onCycleRepeat) {
                            val icon = if (playback.repeatMode == QuranRepeatMode.REPEAT_SURAH) {
                                Icons.Filled.RepeatOne
                            } else {
                                Icons.Filled.Repeat
                            }
                            val label = when (playback.repeatMode) {
                                QuranRepeatMode.OFF -> "بدون تكرار"
                                QuranRepeatMode.REPEAT_SURAH -> "تكرار السورة"
                                QuranRepeatMode.REPEAT_PLAYLIST -> "تكرار القائمة"
                            }
                            Icon(
                                icon,
                                contentDescription = label,
                                tint = if (playback.repeatMode != QuranRepeatMode.OFF) QuramerSoftGold else Color.White.copy(alpha = 0.6f)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                label,
                                color = if (playback.repeatMode != QuranRepeatMode.OFF) QuramerSoftGold else Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                            val selected = kotlin.math.abs(playback.speed - spd) < 0.05f
                            FilterChip(
                                selected = selected,
                                onClick = { onSpeedSelected(spd) },
                                label = { Text("${spd}x") }
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "قائمة التشغيل (${queue.size})",
                style = MaterialTheme.typography.titleSmall,
                color = QuramerSoftGold,
                fontWeight = FontWeight.Bold
            )
        }

        itemsIndexed(items = queue, key = { _, t -> t.id }) { idx, item ->
            val isCurrent = item.id == track.id
            val itemSurah = item.surahNumber?.let { n -> surahs.firstOrNull { it.number == n }?.nameArabic }
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (isCurrent) QuramerSoftGold.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.06f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .clickable { onPlayTracks(queue, idx) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = itemSurah ?: item.displayName,
                            color = Color.White,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = item.reciterName.ifBlank { "قارئ غير محدد" },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                    if (item.durationMs > 0L) {
                        Text(
                            text = formatTimerClock(item.durationMs),
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }
            }
        }
    }
}
