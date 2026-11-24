package com.example.quran.ui

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.activity.compose.BackHandler
import androidx.core.graphics.createBitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.quran.db.ImportedFileEntity
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.QuramerSoftGold
import com.example.ui.theme.StudySuccessGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun QuranReaderScreen(
    state: QuranUiState,
    onSelectSurah: (Int) -> Unit,
    onPrevSurah: () -> Unit,
    onNextSurah: () -> Unit,
    onAdjustFontSize: (Float) -> Unit,
    onToggleNightMode: () -> Unit,
    onToggleControlsHidden: () -> Unit,
    onSaveReadingPosition: (surahNumber: Int, ayahNumber: Int, progress: Float) -> Unit,
    onToggleBookmark: (surahNumber: Int, ayahNumber: Int) -> Unit,
    onToggleSurahCompleted: (surahNumber: Int, completed: Boolean) -> Unit,
    onOpenLibraryUpload: () -> Unit,
    onOpenPdfFile: (ImportedFileEntity) -> Unit,
    onClosePdfViewer: () -> Unit,
    onToggleAudioPlayPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activePdf = state.activePdfFile
    if (activePdf != null) {
        PdfMushafViewerScreen(
            file = activePdf,
            onClose = onClosePdfViewer
        )
        return
    }

    var showSurahPicker by remember { mutableStateOf(false) }
    var showBookmarksDialog by remember { mutableStateOf(false) }

    val surah = state.currentReaderSurah
    val ayahs = state.readerAyahs
    val listState = rememberLazyListState()

    val readerBg = if (state.readerNightMode) Color(0xFF061B14) else Color(0xFFFDFBF4)
    val readerFg = if (state.readerNightMode) Color(0xFFF5EFE0) else Color(0xFF102A20)
    val cardBg = if (state.readerNightMode) Color(0xFF0B291F) else Color(0xFFF7F2E4)

    // Scroll to saved ayah or timed ayah when surah loads
    LaunchedEffect(state.readerSurahNumber, ayahs.size) {
        val targetAyah = surah?.lastReadAyah ?: 0
        if (targetAyah > 1 && ayahs.isNotEmpty()) {
            val idx = ayahs.indexOfFirst { it.ayahNumber >= targetAyah }
            if (idx > 0) {
                listState.scrollToItem(idx)
            }
        }
    }

    LaunchedEffect(state.activeTimedAyahNumber) {
        val activeNum = state.activeTimedAyahNumber ?: return@LaunchedEffect
        val idx = ayahs.indexOfFirst { it.ayahNumber == activeNum }
        if (idx >= 0) {
            listState.animateScrollToItem(idx)
        }
    }

    val scrollProgress by remember(ayahs.size) {
        derivedStateOf {
            if (ayahs.isEmpty()) 0f
            else {
                val firstVisible = listState.firstVisibleItemIndex
                val total = ayahs.size.coerceAtLeast(1)
                ((firstVisible + 1).toFloat() / total.toFloat()).coerceIn(0f, 1f)
            }
        }
    }

    val visibleFirstAyah by remember(ayahs) {
        derivedStateOf {
            ayahs.getOrNull(listState.firstVisibleItemIndex)?.ayahNumber ?: 1
        }
    }

    LaunchedEffect(state.readerSurahNumber, visibleFirstAyah, ayahs.size) {
        if (ayahs.isNotEmpty()) {
            onSaveReadingPosition(state.readerSurahNumber, visibleFirstAyah, scrollProgress)
        }
    }

    if (showSurahPicker) {
        AlertDialog(
            onDismissRequest = { showSurahPicker = false },
            title = { Text("اختر السورة") },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(items = state.surahs, key = { it.number }) { item ->
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = if (item.number == state.readerSurahNumber) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                Color.Transparent
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectSurah(item.number)
                                    showSurahPicker = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${item.number}. ${item.nameArabic}",
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${item.ayahCount} آية" + if (item.hasImportedText) " • نص جاهز" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (item.hasImportedText) StudySuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSurahPicker = false }) { Text("إغلاق") }
            }
        )
    }

    if (showBookmarksDialog) {
        AlertDialog(
            onDismissRequest = { showBookmarksDialog = false },
            title = { Text("العلامات المرجعية") },
            text = {
                if (state.bookmarks.isEmpty()) {
                    Text("لا توجد علامات مرجعية محفوظة بعد. اضغط على أيقونة العلامة بجانب أي آية لحفظها.")
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 340.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(items = state.bookmarks, key = { it.id }) { bm ->
                            val bmSurah = state.surahs.firstOrNull { it.number == bm.surahNumber }
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectSurah(bm.surahNumber)
                                        showBookmarksDialog = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${bmSurah?.nameArabic ?: "سورة ${bm.surahNumber}"} — آية ${bm.ayahNumber}")
                                    Icon(Icons.Filled.Bookmark, contentDescription = null, tint = QuramerSoftGold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBookmarksDialog = false }) { Text("إغلاق") }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(readerBg)
    ) {
        AnimatedVisibility(visible = !state.readerControlsHidden) {
            Surface(
                color = cardBg,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onPrevSurah) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "السورة السابقة", tint = readerFg)
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.clickable { showSurahPicker = true }
                        ) {
                            Text(
                                text = surah?.let { "${it.number}. ${it.nameArabic}" } ?: "سورة ${state.readerSurahNumber}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }

                        IconButton(onClick = onNextSurah) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "السورة التالية", tint = readerFg)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onAdjustFontSize(-2f) }) {
                                Icon(Icons.Filled.TextDecrease, contentDescription = "تصغير الخط", tint = readerFg)
                            }
                            Text(
                                text = "${state.readerFontSizeSp.toInt()}",
                                style = MaterialTheme.typography.labelMedium,
                                color = readerFg
                            )
                            IconButton(onClick = { onAdjustFontSize(2f) }) {
                                Icon(Icons.Filled.TextIncrease, contentDescription = "تكبير الخط", tint = readerFg)
                            }
                            IconButton(onClick = onToggleNightMode) {
                                Icon(
                                    imageVector = if (state.readerNightMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                    contentDescription = "الوضع الليلي والنهاري",
                                    tint = readerFg
                                )
                            }
                            IconButton(onClick = { showBookmarksDialog = true }) {
                                Icon(Icons.Filled.Bookmarks, contentDescription = "العلامات المرجعية", tint = QuramerSoftGold)
                            }
                        }

                        if (surah != null) {
                            TextButton(
                                onClick = { onToggleSurahCompleted(surah.number, !surah.isCompleted) }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = if (surah.isCompleted) StudySuccessGreen else readerFg.copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (surah.isCompleted) "مكتملة" else "إتمام",
                                    color = if (surah.isCompleted) StudySuccessGreen else readerFg
                                )
                            }
                        }
                    }

                    LinearProgressIndicator(
                        progress = { if (ayahs.isNotEmpty()) scrollProgress else (surah?.scrollProgress ?: 0f) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Active session Surahs quick switcher
                    val activeSession = state.activeSession
                    if (activeSession != null && activeSession.surahNumbers.size > 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            activeSession.surahNumbers.take(5).forEach { sNum ->
                                val sName = state.surahs.firstOrNull { it.number == sNum }?.nameArabic ?: "$sNum"
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (sNum == state.readerSurahNumber) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                    modifier = Modifier.clickable { onSelectSurah(sNum) }
                                ) {
                                    Text(
                                        text = sName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (sNum == state.readerSurahNumber) MaterialTheme.colorScheme.onPrimary else readerFg,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Combined / Audio follow-along indicator (honest timing sync disclosure)
                    if (state.playbackState.hasActiveTrack) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (state.readerHasTimingSync) {
                                        "مزامنة الآيات مفعّلة مع التلاوة"
                                    } else {
                                        "قراءة ومتابعة مع التلاوة (بدون مزامنة زمنية تلقائية للآيات)"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = onToggleAudioPlayPause,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (state.playbackState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = "تشغيل/إيقاف"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Main Reader Content Area (Tap toggles distraction-free controls)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onToggleControlsHidden
                )
        ) {
            if (ayahs.isEmpty()) {
                val importedPdfs = state.importedFiles.filter { it.fileType == "PDF" && it.status == "READY" }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = QuramerSoftGold,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    item {
                        Text(
                            text = surah?.let { "${it.nameArabic} — ${it.ayahCount} آية" } ?: "لم يُستورد نص لهذه السورة بعد",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = readerFg,
                            textAlign = TextAlign.Center
                        )
                    }
                    item {
                        Text(
                            text = "لضمان صحة الرسم العثماني ١٠٠٪ بدون أي تحريف، لا يعرض القارئ نصوصاً آلية غير موثقة. يمكنك رفع ملفات المصحف (JSON أو TXT) أو تصفح ملفات PDF المرفوعة مباشرة.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = readerFg.copy(alpha = 0.78f),
                            textAlign = TextAlign.Center
                        )
                    }
                    item {
                        Button(
                            onClick = onOpenLibraryUpload,
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("reader_import_files_cta")
                        ) {
                            Icon(Icons.Filled.UploadFile, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("استيراد ملفات المصحف (TXT / JSON / PDF)")
                        }
                    }

                    if (importedPdfs.isNotEmpty()) {
                        item {
                            Text(
                                text = "ملفات PDF المرفوعة جاهزة للقراءة:",
                                style = MaterialTheme.typography.titleSmall,
                                color = readerFg
                            )
                        }
                        items(items = importedPdfs, key = { it.id }) { pdf ->
                            Card(
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                border = BorderStroke(1.dp, QuramerSoftGold.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenPdfFile(pdf) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(pdf.displayName, fontWeight = FontWeight.Bold, color = readerFg, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${pdf.ayahCountImported} صفحة", style = MaterialTheme.typography.bodySmall, color = readerFg.copy(alpha = 0.7f))
                                    }
                                    Text("فتح PDF", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (surah != null && surah.number != 1 && surah.number != 9) {
                        item(key = "bismillah_${surah.number}") {
                            Text(
                                text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                                fontFamily = AmiriFontFamily,
                                fontSize = (state.readerFontSizeSp + 2f).sp,
                                color = QuramerSoftGold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            )
                        }
                    }

                    items(items = ayahs, key = { it.id }) { ayah ->
                        val isBookmarked = state.bookmarks.any {
                            it.surahNumber == ayah.surahNumber && it.ayahNumber == ayah.ayahNumber
                        }
                        val isHighlighted = state.activeTimedAyahNumber == ayah.ayahNumber

                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = if (isHighlighted) {
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                            } else {
                                cardBg
                            },
                            border = BorderStroke(
                                1.dp,
                                if (isHighlighted) QuramerSoftGold else Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${ayah.textUthmani} ﴿${ayah.ayahNumber}﴾",
                                    fontFamily = AmiriFontFamily,
                                    fontSize = state.readerFontSizeSp.sp,
                                    lineHeight = (state.readerFontSizeSp * 1.72f).sp,
                                    color = readerFg,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "آية ${ayah.ayahNumber}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = readerFg.copy(alpha = 0.65f)
                                    )
                                    IconButton(
                                        onClick = { onToggleBookmark(ayah.surahNumber, ayah.ayahNumber) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                            contentDescription = "حفظ علامة مرجعية",
                                            tint = if (isBookmarked) QuramerSoftGold else readerFg.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfMushafViewerScreen(
    file: ImportedFileEntity,
    onClose: () -> Unit
) {
    BackHandler { onClose() }

    var pageIndex by remember(file.id) { mutableIntStateOf(0) }
    val totalPages = file.ayahCountImported.coerceAtLeast(1)

    val bitmapState by produceState<Bitmap?>(initialValue = null, key1 = file.localCopyPath, key2 = pageIndex) {
        value = withContext(Dispatchers.IO) {
            val path = file.localCopyPath ?: return@withContext null
            val pdfFile = File(path)
            if (!pdfFile.exists()) return@withContext null
            runCatching {
                ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        if (renderer.pageCount <= 0) return@withContext null
                        val safeIdx = pageIndex.coerceIn(0, renderer.pageCount - 1)
                        renderer.openPage(safeIdx).use { page ->
                            val targetWidth = (page.width * 2).coerceIn(1, 2048)
                            val targetHeight = (page.height * 2).coerceIn(1, 2048)
                            val bmp = createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                            bmp.eraseColor(android.graphics.Color.WHITE)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            bmp
                        }
                    }
                }
            }.getOrNull()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9F6EC))
    ) {
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "إغلاق عارض PDF")
                }
                Text(
                    text = "${file.displayName} (${pageIndex + 1}/$totalPages)",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Row {
                    IconButton(
                        onClick = { if (pageIndex > 0) pageIndex-- },
                        enabled = pageIndex > 0
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "الصفحة السابقة")
                    }
                    IconButton(
                        onClick = { if (pageIndex + 1 < totalPages) pageIndex++ },
                        enabled = pageIndex + 1 < totalPages
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "الصفحة التالية")
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            val bmp = bitmapState
            if (bmp == null) {
                CircularProgressIndicator()
            } else {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "صفحة المصحف ${pageIndex + 1}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
