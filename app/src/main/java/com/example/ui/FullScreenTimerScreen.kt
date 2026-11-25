package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.quran.audio.QuranAudioPlaybackState
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.util.Locale

@Composable
fun FullScreenTimerScreen(
    uiState: FocusUiState,
    quranPlayback: QuranAudioPlaybackState = QuranAudioPlaybackState(),
    onToggleAudioPlayback: () -> Unit,
    onExitFullScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onExitFullScreen()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "timer_bg")
    val topColor by infiniteTransition.animateColor(
        initialValue = Color(0xFF0F172A),
        targetValue = Color(0xFF1E3A8A),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "top_color"
    )
    val midColor by infiniteTransition.animateColor(
        initialValue = Color(0xFF0D9488),
        targetValue = Color(0xFF065F46),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mid_color"
    )
    val bottomColor by infiniteTransition.animateColor(
        initialValue = Color(0xFF1E293B),
        targetValue = Color(0xFF312E81),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bottom_color"
    )

    val displayMs = if (uiState.isStopwatchActive) uiState.stopwatchElapsedMs else uiState.remainingMs
    val progress = remember(displayMs, uiState.sessionDurationMs, uiState.isStopwatchActive) {
        if (uiState.isStopwatchActive) {
            ((displayMs % 3_600_000L).toFloat() / 3_600_000f).coerceIn(0.05f, 1f)
        } else if (uiState.sessionDurationMs > 0L && uiState.remainingMs > 0L) {
            (uiState.remainingMs.toFloat() / uiState.sessionDurationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(topColor, midColor, bottomColor)
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .testTag("fullscreen_timer_screen")
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (uiState.userName.isNotBlank()) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = stringResource(R.string.greeting_user_study, uiState.userName),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFFDE68A),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            } else {
                Spacer(Modifier.width(1.dp))
            }

            OutlinedButton(
                onClick = onExitFullScreen,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("exit_fullscreen_timer_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.CloseFullscreen,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.fullscreen_timer_close_button))
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF34D399).copy(alpha = 0.18f),
                border = BorderStroke(1.dp, Color(0xFF6EE7B7).copy(alpha = 0.5f))
            ) {
                Text(
                    text = stringResource(R.string.salawat_banner_text),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFA7F3D0),
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                )
            }

            Box(
                modifier = Modifier.size(250.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 12.dp.toPx()
                    drawArc(
                        color = Color.White.copy(alpha = 0.18f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color(0xFF34D399),
                                Color(0xFF38BDF8),
                                Color(0xFFFBBF24),
                                Color(0xFF34D399)
                            )
                        ),
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }

                Text(
                    text = formatTimerDisplay(displayMs),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White,
                    modifier = Modifier.testTag("fullscreen_timer_digits")
                )
            }

            Text(
                text = if (uiState.isStopwatchActive) {
                    "ستوب وتش نشط — كل ساعة كاملة تزرع شجرة وتُضاف للإحصائيات"
                } else {
                    stringResource(R.string.fullscreen_timer_subtitle)
                },
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFE2E8F0),
                textAlign = TextAlign.Center
            )

            val isQuranActive = !uiState.isAudioPlaying &&
                (quranPlayback.isPlaying || quranPlayback.hasActiveTrack)
            val anyAudioPlaying = uiState.isAudioPlaying || quranPlayback.isPlaying
            val activeAudioTitle: String? = when {
                uiState.isAudioPlaying -> {
                    val uploaded = uiState.uploadedTracks.firstOrNull { it.id == uiState.selectedUploadedTrackId }
                    uploaded?.displayName ?: stringResource(builtInSoundLabelRes(uiState.selectedBuiltInSound))
                }
                isQuranActive -> quranPlayback.currentTrack?.displayName?.ifBlank { "تلاوة القرآن" } ?: "تلاوة القرآن"
                else -> null
            }

            if (activeAudioTitle != null) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = if (isQuranActive) "القرآن: $activeAudioTitle" else activeAudioTitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFFDE68A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            OutlinedButton(
                onClick = onToggleAudioPlayback,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("fullscreen_audio_toggle_button")
            ) {
                Icon(
                    imageVector = if (anyAudioPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(
                        if (anyAudioPlaying) R.string.audio_pause else R.string.audio_play
                    )
                )
            }
        }

        CreatedByGalileoFooter(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .fillMaxWidth(),
            textColor = Color(0xFFE2E8F0)
        )
    }
}

private fun formatTimerDisplay(remainingMs: Long): String {
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
