package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import kotlin.math.ceil

@Composable
fun TreeFarmTabContent(
    uiState: FocusUiState,
    modifier: Modifier = Modifier
) {
    val unplantedMinutes = (uiState.unplantedStudyMs / 60_000L).toInt().coerceIn(0, 59)
    val nextTreeProgress = (uiState.unplantedStudyMs.toFloat() / 3_600_000f).coerceIn(0f, 1f)
    var selectedTreeIndex by remember(uiState.plantedTrees.size) {
        mutableStateOf(if (uiState.plantedTrees.isNotEmpty()) uiState.plantedTrees.lastIndex else -1)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Realistic Farm Land Hero showing all earned trees planted in soil plots
        item(key = "realistic_farm_land") {
            RealisticFarmLandCard(
                trees = uiState.plantedTrees,
                nextTreeProgress = nextTreeProgress,
                unplantedMinutes = unplantedMinutes,
                selectedTreeIndex = selectedTreeIndex,
                onSelectTree = { selectedTreeIndex = it }
            )
        }

        // Selected tree detail strip
        if (selectedTreeIndex in uiState.plantedTrees.indices) {
            item(key = "selected_tree_detail") {
                val tree = uiState.plantedTrees[selectedTreeIndex]
                val treeNum = selectedTreeIndex + 1
                val dateLabel = remember(tree.plantedAtMs) {
                    if (tree.plantedAtMs > 0L) {
                        SimpleDateFormat("dd/MM/yyyy • hh:mm a", Locale.getDefault()).format(Date(tree.plantedAtMs))
                    } else {
                        "—"
                    }
                }
                val treeSpecies = when (tree.variant % 4) {
                    0 -> "شجرة بلوط خضراء"
                    1 -> "شجرة صنوبر دائمة الخضرة"
                    2 -> "شجرة مثمرة بأزهار ذهبية"
                    else -> "شجرة زيتون عالية"
                }
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, StudySuccessGreen.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Park,
                            contentDescription = null,
                            tint = StudySuccessGreen,
                            modifier = Modifier.size(28.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "شجرة #$treeNum — $treeSpecies",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "تاريخ الزراعة: $dateLabel",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Farm stats summary
        item(key = "farm_summary") {
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
                                text = "كل ساعة من التايمر أو الستوب وتش أو جلسات القرآن تزرع شجرة حقيقية في أرضك",
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
            item(key = "farm_empty_hint") {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Spa,
                            contentDescription = null,
                            tint = StudySuccessGreen,
                            modifier = Modifier.size(30.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.farm_empty_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.farm_empty_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RealisticFarmLandCard(
    trees: List<PlantedTree>,
    nextTreeProgress: Float,
    unplantedMinutes: Int,
    selectedTreeIndex: Int,
    onSelectTree: (Int) -> Unit
) {
    // 4 trees per row across the soil terraces; always reserve at least 2 rows (8 plots) so the land feels full & real
    val columns = 4
    val totalSlots = (trees.size + 1).coerceAtLeast(8)
    val rows = ceil(totalSlots / columns.toFloat()).toInt().coerceAtLeast(2)
    val landHeightDp = (170 + rows * 92).dp

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.5.dp, Color(0xFF4D7C0F).copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("realistic_farm_land_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(landHeightDp)
            ) {
                // Background Sky, Distant Hills, Wooden Fence, Lush Grass & Terraced Soil Plots
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val horizonY = (h * 0.24f).coerceAtMost(115.dp.toPx())

                    // Sky gradient
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFBAE6FD),
                                Color(0xFFE0F2FE),
                                Color(0xFFFEF3C7)
                            ),
                            startY = 0f,
                            endY = horizonY
                        ),
                        size = Size(w, horizonY)
                    )

                    // Warm Sun & Glow
                    drawCircle(
                        color = Color(0xFFFDE68A).copy(alpha = 0.5f),
                        radius = 34.dp.toPx(),
                        center = Offset(w * 0.84f, horizonY * 0.38f)
                    )
                    drawCircle(
                        color = Color(0xFFFACC15),
                        radius = 18.dp.toPx(),
                        center = Offset(w * 0.84f, horizonY * 0.38f)
                    )

                    // Soft clouds
                    drawCircle(color = Color.White.copy(alpha = 0.85f), radius = 16.dp.toPx(), center = Offset(w * 0.20f, horizonY * 0.34f))
                    drawCircle(color = Color.White.copy(alpha = 0.85f), radius = 21.dp.toPx(), center = Offset(w * 0.27f, horizonY * 0.32f))
                    drawCircle(color = Color.White.copy(alpha = 0.85f), radius = 15.dp.toPx(), center = Offset(w * 0.35f, horizonY * 0.36f))

                    // Distant emerald rolling hills
                    val backHill = Path().apply {
                        moveTo(0f, horizonY)
                        quadraticTo(w * 0.25f, horizonY - 28.dp.toPx(), w * 0.55f, horizonY - 8.dp.toPx())
                        quadraticTo(w * 0.80f, horizonY - 26.dp.toPx(), w, horizonY - 4.dp.toPx())
                        lineTo(w, horizonY + 12.dp.toPx())
                        lineTo(0f, horizonY + 12.dp.toPx())
                        close()
                    }
                    drawPath(backHill, color = Color(0xFF4ADE80).copy(alpha = 0.55f))

                    // Main farm meadow earth gradient below horizon
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF65A30D),
                                Color(0xFF4D7C0F),
                                Color(0xFF3F6212)
                            ),
                            startY = horizonY,
                            endY = h
                        ),
                        topLeft = Offset(0f, horizonY),
                        size = Size(w, h - horizonY)
                    )

                    // Rich tilled farm field earth bed
                    val fieldTop = horizonY + 26.dp.toPx()
                    val fieldMargin = 14.dp.toPx()
                    drawRoundRect(
                        color = Color(0xFF5C3A21),
                        topLeft = Offset(fieldMargin, fieldTop),
                        size = Size(w - fieldMargin * 2, h - fieldTop - 16.dp.toPx()),
                        cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx())
                    )
                    drawRoundRect(
                        color = Color(0xFF71492A),
                        topLeft = Offset(fieldMargin + 5.dp.toPx(), fieldTop + 5.dp.toPx()),
                        size = Size(w - (fieldMargin + 5.dp.toPx()) * 2, h - fieldTop - 26.dp.toPx()),
                        cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx())
                    )

                    // Grass border tufts & wooden farm fence along the top edge
                    val postCount = 9
                    val fenceY = horizonY + 6.dp.toPx()
                    drawRect(
                        color = Color(0xFF92400E),
                        topLeft = Offset(fieldMargin, fenceY + 6.dp.toPx()),
                        size = Size(w - fieldMargin * 2, 4.dp.toPx())
                    )
                    for (p in 0 until postCount) {
                        val px = fieldMargin + ((w - fieldMargin * 2) * p / (postCount - 1).toFloat())
                        drawRoundRect(
                            color = Color(0xFF78350F),
                            topLeft = Offset(px - 3.dp.toPx(), fenceY),
                            size = Size(6.dp.toPx(), 20.dp.toPx()),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        )
                    }
                }

                // Interactive grid of soil plots positioned right on top of the farm earth bed
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 132.dp, start = 20.dp, end = 20.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (row in 0 until rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            for (col in 0 until columns) {
                                val slotIdx = row * columns + col
                                if (slotIdx < trees.size) {
                                    val tree = trees[slotIdx]
                                    val isSelected = slotIdx == selectedTreeIndex
                                    Box(
                                        modifier = Modifier
                                            .size(width = 72.dp, height = 84.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable { onSelectTree(slotIdx) }
                                            .testTag("farm_land_tree_${slotIdx + 1}"),
                                        contentAlignment = Alignment.BottomCenter
                                    ) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            drawSoilPlotMound(highlighted = isSelected)
                                            drawRealisticPlantedTree(variant = tree.variant)
                                        }
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isSelected) Color(0xFFFACC15) else Color(0xCC052E16),
                                            modifier = Modifier.align(Alignment.TopEnd)
                                        ) {
                                            Text(
                                                text = "#${slotIdx + 1}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color(0xFF1C1917) else Color.White,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                } else if (slotIdx == trees.size) {
                                    // Next soil plot showing sprouting seedling growing toward 60 minutes!
                                    Box(
                                        modifier = Modifier
                                            .size(width = 72.dp, height = 84.dp)
                                            .testTag("farm_land_sprout"),
                                        contentAlignment = Alignment.BottomCenter
                                    ) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            drawSoilPlotMound(highlighted = false)
                                            drawSproutingSeedling(progress = nextTreeProgress)
                                        }
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xBB1E293B),
                                            modifier = Modifier.align(Alignment.TopCenter)
                                        ) {
                                            Text(
                                                text = "$unplantedMinutes/60د",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFFA7F3D0),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                } else {
                                    // Empty prepared fertile soil plot waiting for future trees
                                    Box(
                                        modifier = Modifier.size(width = 72.dp, height = 84.dp),
                                        contentAlignment = Alignment.BottomCenter
                                    ) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            drawSoilPlotMound(highlighted = false)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Land Header Pill overlaid neatly in sky
                Surface(
                    shape = CircleShape,
                    color = Color(0xCC06281E),
                    border = BorderStroke(1.dp, Color(0xFFA7F3D0).copy(alpha = 0.5f)),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(14.dp)
                ) {
                    Text(
                        text = "أرض مزرعتك (${trees.size} شجرة مزروعة)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawSoilPlotMound(highlighted: Boolean) {
    val w = size.width
    val h = size.height
    if (highlighted) {
        drawOval(
            color = Color(0xFFFACC15).copy(alpha = 0.45f),
            topLeft = Offset(w * 0.08f, h * 0.74f),
            size = Size(w * 0.84f, h * 0.24f)
        )
    }
    drawOval(
        color = Color(0xFF3E2723),
        topLeft = Offset(w * 0.14f, h * 0.77f),
        size = Size(w * 0.72f, h * 0.18f)
    )
    drawOval(
        color = Color(0xFF4E342E),
        topLeft = Offset(w * 0.20f, h * 0.78f),
        size = Size(w * 0.60f, h * 0.12f)
    )
    // Grass ring around soil plot
    drawOval(
        color = Color(0xFF65A30D).copy(alpha = 0.45f),
        topLeft = Offset(w * 0.12f, h * 0.80f),
        size = Size(w * 0.16f, h * 0.08f)
    )
    drawOval(
        color = Color(0xFF65A30D).copy(alpha = 0.45f),
        topLeft = Offset(w * 0.72f, h * 0.80f),
        size = Size(w * 0.16f, h * 0.08f)
    )
}

private fun DrawScope.drawSproutingSeedling(progress: Float) {
    val w = size.width
    val h = size.height
    val growth = (0.35f + progress * 0.65f).coerceIn(0.35f, 1.0f)
    val stemHeight = h * 0.22f * growth
    val baseY = h * 0.82f

    // Green stem
    drawRoundRect(
        color = Color(0xFF22C55E),
        topLeft = Offset(w * 0.48f, baseY - stemHeight),
        size = Size(w * 0.05f, stemHeight),
        cornerRadius = CornerRadius(4f, 4f)
    )
    // Two sprouting leaves
    drawOval(
        color = Color(0xFF4ADE80),
        topLeft = Offset(w * 0.34f, baseY - stemHeight - h * 0.06f),
        size = Size(w * 0.18f, h * 0.10f)
    )
    drawOval(
        color = Color(0xFF86EFAC),
        topLeft = Offset(w * 0.49f, baseY - stemHeight - h * 0.05f),
        size = Size(w * 0.18f, h * 0.09f)
    )
}

private fun DrawScope.drawRealisticPlantedTree(variant: Int) {
    val w = size.width
    val h = size.height

    // Soft tree shadow on soil
    drawOval(
        color = Color.Black.copy(alpha = 0.24f),
        topLeft = Offset(w * 0.24f, h * 0.81f),
        size = Size(w * 0.52f, h * 0.09f)
    )

    // Rooted trunk
    drawRoundRect(
        color = Color(0xFF6D2809),
        topLeft = Offset(w * 0.43f, h * 0.50f),
        size = Size(w * 0.14f, h * 0.34f),
        cornerRadius = CornerRadius(6f, 6f)
    )
    drawRoundRect(
        color = Color(0xFF854D0E),
        topLeft = Offset(w * 0.46f, h * 0.52f),
        size = Size(w * 0.05f, h * 0.30f),
        cornerRadius = CornerRadius(3f, 3f)
    )

    when (variant % 4) {
        0 -> {
            // Lush Oak
            drawCircle(color = Color(0xFF065F46), radius = w * 0.29f, center = Offset(w * 0.50f, h * 0.40f))
            drawCircle(color = Color(0xFF059669), radius = w * 0.23f, center = Offset(w * 0.34f, h * 0.44f))
            drawCircle(color = Color(0xFF10B981), radius = w * 0.22f, center = Offset(w * 0.65f, h * 0.43f))
            drawCircle(color = Color(0xFF34D399), radius = w * 0.18f, center = Offset(w * 0.49f, h * 0.29f))
        }
        1 -> {
            // Layered Pine
            val bottomTri = Path().apply {
                moveTo(w * 0.50f, h * 0.26f)
                lineTo(w * 0.18f, h * 0.62f)
                lineTo(w * 0.82f, h * 0.62f)
                close()
            }
            val midTri = Path().apply {
                moveTo(w * 0.50f, h * 0.15f)
                lineTo(w * 0.24f, h * 0.48f)
                lineTo(w * 0.76f, h * 0.48f)
                close()
            }
            val topTri = Path().apply {
                moveTo(w * 0.50f, h * 0.06f)
                lineTo(w * 0.31f, h * 0.33f)
                lineTo(w * 0.69f, h * 0.33f)
                close()
            }
            drawPath(bottomTri, color = Color(0xFF065F46))
            drawPath(midTri, color = Color(0xFF047857))
            drawPath(topTri, color = Color(0xFF10B981))
        }
        2 -> {
            // Golden Fruit / Blossom Tree
            drawCircle(color = Color(0xFF047857), radius = w * 0.28f, center = Offset(w * 0.50f, h * 0.38f))
            drawCircle(color = Color(0xFF10B981), radius = w * 0.20f, center = Offset(w * 0.38f, h * 0.33f))
            drawCircle(color = Color(0xFFFBBF24), radius = w * 0.055f, center = Offset(w * 0.36f, h * 0.36f))
            drawCircle(color = Color(0xFFFDE68A), radius = w * 0.05f, center = Offset(w * 0.59f, h * 0.29f))
            drawCircle(color = Color(0xFFFBBF24), radius = w * 0.055f, center = Offset(w * 0.56f, h * 0.45f))
            drawCircle(color = Color(0xFFF59E0B), radius = w * 0.045f, center = Offset(w * 0.47f, h * 0.25f))
        }
        else -> {
            // Olive / Cypress Tree
            drawOval(
                color = Color(0xFF14532D),
                topLeft = Offset(w * 0.26f, h * 0.12f),
                size = Size(w * 0.48f, h * 0.54f)
            )
            drawOval(
                color = Color(0xFF16A34A),
                topLeft = Offset(w * 0.31f, h * 0.15f),
                size = Size(w * 0.32f, h * 0.44f)
            )
            drawOval(
                color = Color(0xFF4ADE80).copy(alpha = 0.55f),
                topLeft = Offset(w * 0.37f, h * 0.18f),
                size = Size(w * 0.15f, h * 0.30f)
            )
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
