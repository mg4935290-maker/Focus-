package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Park
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.PlantedTree
import com.example.ui.theme.StudySuccessGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TreeFarmTabContent(
    uiState: FocusUiState,
    modifier: Modifier = Modifier
) {
    val unplantedMinutes = (uiState.unplantedStudyMs / 60_000L).toInt().coerceIn(0, 59)
    val nextTreeProgress = (uiState.unplantedStudyMs.toFloat() / 3_600_000f).coerceIn(0f, 1f)

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 148.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }, key = "farm_summary") {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, StudySuccessGreen.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("farm_summary_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = StudySuccessGreen.copy(alpha = 0.16f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Park,
                                    contentDescription = null,
                                    tint = StudySuccessGreen,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.farm_title),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.farm_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = stringResource(R.string.farm_total_trees, uiState.plantedTrees.size),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                    .testTag("farm_total_trees_text")
                            )
                        }

                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.farm_today_study_time,
                                    formatDurationArabic(uiState.dailyStudyMs)
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }
                    }

                    Text(
                        text = stringResource(
                            R.string.farm_total_study_time,
                            formatDurationArabic(uiState.totalStudyMs)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(R.string.farm_next_tree_progress, unplantedMinutes),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        LinearProgressIndicator(
                            progress = { nextTreeProgress },
                            color = StudySuccessGreen,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        }

        if (uiState.plantedTrees.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "farm_empty") {
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
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Park,
                            contentDescription = null,
                            tint = StudySuccessGreen.copy(alpha = 0.65f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = stringResource(R.string.farm_empty_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.farm_empty_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            itemsIndexed(
                items = uiState.plantedTrees.reversed(),
                key = { _, tree -> tree.id }
            ) { reverseIndex, tree ->
                val treeNumber = uiState.plantedTrees.size - reverseIndex
                TreeCard(treeNumber = treeNumber, tree = tree)
            }
        }
    }
}

@Composable
private fun TreeCard(
    treeNumber: Int,
    tree: PlantedTree
) {
    val dateLabel = remember(tree.plantedAtMs) {
        if (tree.plantedAtMs > 0L) {
            SimpleDateFormat("dd/MM", Locale.US).format(Date(tree.plantedAtMs))
        } else {
            ""
        }
    }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, StudySuccessGreen.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TreeCanvasIllustration(
                variant = tree.variant,
                modifier = Modifier.size(86.dp)
            )
            Text(
                text = stringResource(R.string.farm_tree_label, treeNumber),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (dateLabel.isNotEmpty()) {
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TreeCanvasIllustration(
    variant: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Soft grass mound
        drawOval(
            color = Color(0xFF86EFAC).copy(alpha = 0.45f),
            topLeft = Offset(w * 0.12f, h * 0.78f),
            size = Size(w * 0.76f, h * 0.16f)
        )

        // Trunk
        drawRoundRect(
            color = Color(0xFF78350F),
            topLeft = Offset(w * 0.44f, h * 0.50f),
            size = Size(w * 0.12f, h * 0.36f),
            cornerRadius = CornerRadius(6f, 6f)
        )

        when (variant % 4) {
            0 -> {
                // Lush round oak
                drawCircle(color = Color(0xFF059669), radius = w * 0.28f, center = Offset(w * 0.50f, h * 0.38f))
                drawCircle(color = Color(0xFF10B981), radius = w * 0.21f, center = Offset(w * 0.36f, h * 0.44f))
                drawCircle(color = Color(0xFF34D399), radius = w * 0.19f, center = Offset(w * 0.63f, h * 0.43f))
            }
            1 -> {
                // Layered evergreen pine
                val topTri = Path().apply {
                    moveTo(w * 0.50f, h * 0.12f)
                    lineTo(w * 0.26f, h * 0.44f)
                    lineTo(w * 0.74f, h * 0.44f)
                    close()
                }
                val midTri = Path().apply {
                    moveTo(w * 0.50f, h * 0.28f)
                    lineTo(w * 0.20f, h * 0.64f)
                    lineTo(w * 0.80f, h * 0.64f)
                    close()
                }
                drawPath(midTri, color = Color(0xFF047857))
                drawPath(topTri, color = Color(0xFF10B981))
            }
            2 -> {
                // Golden blossom study tree
                drawCircle(color = Color(0xFF0D9488), radius = w * 0.27f, center = Offset(w * 0.50f, h * 0.38f))
                drawCircle(color = Color(0xFFFBBF24), radius = w * 0.05f, center = Offset(w * 0.38f, h * 0.34f))
                drawCircle(color = Color(0xFFFDE68A), radius = w * 0.045f, center = Offset(w * 0.58f, h * 0.29f))
                drawCircle(color = Color(0xFFFBBF24), radius = w * 0.045f, center = Offset(w * 0.55f, h * 0.45f))
            }
            else -> {
                // Tall cypress / olive
                drawOval(
                    color = Color(0xFF15803D),
                    topLeft = Offset(w * 0.28f, h * 0.12f),
                    size = Size(w * 0.44f, h * 0.54f)
                )
                drawOval(
                    color = Color(0xFF22C55E).copy(alpha = 0.65f),
                    topLeft = Offset(w * 0.35f, h * 0.17f),
                    size = Size(w * 0.22f, h * 0.40f)
                )
            }
        }
    }
}

fun formatDurationArabic(ms: Long): String {
    val totalMinutes = (ms / 60_000L).coerceAtLeast(0L)
    val hours = totalMinutes / 60L
    val mins = totalMinutes % 60L
    return when {
        hours > 0L && mins > 0L -> "$hours س و $mins د"
        hours > 0L -> "$hours س"
        else -> "$mins د"
    }
}
