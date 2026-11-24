package com.example.quran.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.quran.db.ImportedFileEntity
import com.example.quran.db.QuranAudioTrackEntity
import com.example.quran.db.QuranPlaylistEntity
import com.example.quran.db.SurahEntity
import com.example.ui.theme.QuramerSoftGold
import com.example.ui.theme.StudySuccessGreen

@Composable
fun QuranLibraryScreen(
    state: QuranUiState,
    onUploadMultiFilesClick: (targetPlaylistId: String?) -> Unit,
    onConfirmReplaceDuplicate: () -> Unit,
    onDismissDuplicate: () -> Unit,
    onDeleteFile: (ImportedFileEntity) -> Unit,
    onOpenPdfFile: (ImportedFileEntity) -> Unit,
    onPlayTracks: (List<QuranAudioTrackEntity>, Int) -> Unit,
    onUpdateTrackDetails: (trackId: String, surahNumber: Int?, reciterName: String, confirmed: Boolean, playlistIdOverride: String?) -> Unit,
    onCreatePlaylist: (folderName: String, reciterName: String) -> Unit,
    onUpdatePlaylist: (playlistId: String, folderName: String, reciterName: String) -> Unit,
    onDeletePlaylist: (playlistId: String) -> Unit,
    onSelectPlaylistFilter: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingTrack by remember { mutableStateOf<QuranAudioTrackEntity?>(null) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var editingFolder by remember { mutableStateOf<QuranPlaylistEntity?>(null) }

    val duplicate = state.duplicatePrompts.firstOrNull()
    if (duplicate != null) {
        AlertDialog(
            onDismissRequest = onDismissDuplicate,
            title = { Text("تم اكتشاف ملف مكرر") },
            text = {
                Text(
                    "الملف «${duplicate.displayName}» مطابق بمحتواه لملف مستورد مسبقاً («${duplicate.existingFile.displayName}»). هل تريد استبداله أم الإبقاء على الملف الحالي؟"
                )
            },
            confirmButton = {
                Button(onClick = onConfirmReplaceDuplicate) {
                    Text("استبدال الملف")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDuplicate) {
                    Text("إبقاء القديم")
                }
            }
        )
    }

    editingTrack?.let { track ->
        TrackAssignmentDialog(
            track = track,
            surahs = state.surahs,
            playlists = state.playlists,
            onSave = { surahNum, reciter, confirmed, folderId ->
                onUpdateTrackDetails(track.id, surahNum, reciter, confirmed, folderId)
                editingTrack = null
            },
            onDismiss = { editingTrack = null }
        )
    }

    if (showCreateFolderDialog) {
        ReciterFolderDialog(
            title = "إضافة ملف / قسم قارئ جديد",
            initialFolderName = "",
            initialReciterName = "",
            confirmLabel = "إنشاء القسم",
            onConfirm = { folderName, reciterName ->
                onCreatePlaylist(folderName, reciterName)
                showCreateFolderDialog = false
            },
            onDismiss = { showCreateFolderDialog = false }
        )
    }

    editingFolder?.let { folder ->
        ReciterFolderDialog(
            title = "تعديل ملف واسم القارئ",
            initialFolderName = folder.name,
            initialReciterName = folder.reciterName,
            confirmLabel = "حفظ التعديلات",
            onConfirm = { folderName, reciterName ->
                onUpdatePlaylist(folder.id, folderName, reciterName)
                editingFolder = null
            },
            onDismiss = { editingFolder = null }
        )
    }

    val filteredTracks = remember(state.audioTracks, state.selectedPlaylistIdFilter) {
        val f = state.selectedPlaylistIdFilter
        if (f == null) state.audioTracks else state.audioTracks.filter { it.playlistId == f }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "multi_upload_header") {
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
                    Text(
                        text = "مكتبة القرآن والملفات",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "ارفع نصوص المصحف (JSON, TXT, PDF) أو التلاوات الصوتية (MP3, M4A, AAC, WAV) مع الحفاظ على كل الملفات السابقة.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { onUploadMultiFilesClick(state.selectedPlaylistIdFilter) },
                        enabled = !state.isImportingFiles,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("upload_multiple_files_button")
                    ) {
                        Icon(Icons.Filled.UploadFile, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (state.isImportingFiles) "جاري استيراد الملفات..."
                            else stringResource(R.string.quran_upload_multi),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (state.isImportingFiles) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }

        // Reciter Folders Section (Write File/Folder Name + Reciter Name)
        item(key = "reciter_folders_header") {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, QuramerSoftGold.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reciter_folders_card")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
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
                            Icon(Icons.Filled.Folder, contentDescription = null, tint = QuramerSoftGold)
                            Text(
                                text = "تقسيم القراء (ملفات القراء)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = { showCreateFolderDialog = true },
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("create_reciter_folder_button")
                        ) {
                            Icon(Icons.Filled.CreateNewFolder, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("إضافة ملف قارئ", maxLines = 1)
                        }
                    }

                    Text(
                        text = "اكتب اسم الملف أو القسم واكتب بعده اسم القارئ لتنظيم التلاوات ورفع الصوتيات داخل كل قارئ بسهولة.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (state.playlists.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = state.selectedPlaylistIdFilter == null,
                                    onClick = { onSelectPlaylistFilter(null) },
                                    label = { Text("كل القراء (${state.audioTracks.size})") }
                                )
                            }
                            items(items = state.playlists, key = { it.id }) { folder ->
                                val count = state.audioTracks.count { it.playlistId == folder.id }
                                FilterChip(
                                    selected = state.selectedPlaylistIdFilter == folder.id,
                                    onClick = { onSelectPlaylistFilter(folder.id) },
                                    label = { Text("${folder.name} • ${folder.reciterName} ($count)") }
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.playlists.forEach { folder ->
                                val folderTracks = state.audioTracks.filter { it.playlistId == folder.id }
                                val isSelected = state.selectedPlaylistIdFilter == folder.id
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectPlaylistFilter(folder.id) }
                                        .testTag("reciter_folder_item_${folder.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Folder,
                                            contentDescription = null,
                                            tint = QuramerSoftGold
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = folder.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "القارئ: ${folder.reciterName} • ${folderTracks.size} تلاوة",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (folderTracks.isNotEmpty()) {
                                            IconButton(onClick = { onPlayTracks(folderTracks, 0) }) {
                                                Icon(Icons.Filled.PlayArrow, contentDescription = "تشغيل القسم", tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                        IconButton(onClick = { onUploadMultiFilesClick(folder.id) }) {
                                            Icon(Icons.Filled.Add, contentDescription = "رفع ملفات داخل القسم", tint = StudySuccessGreen)
                                        }
                                        IconButton(onClick = { editingFolder = folder }) {
                                            Icon(Icons.Filled.Edit, contentDescription = "تعديل اسم الملف والقارئ")
                                        }
                                        IconButton(onClick = { onDeletePlaylist(folder.id) }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "حذف القسم", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Audio Tracks List
        item(key = "tracks_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Filled.LibraryMusic, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Text(
                        text = "التلاوات الصوتية (${filteredTracks.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        items(items = filteredTracks, key = { it.id }) { track ->
            val surah = track.surahNumber?.let { num -> state.surahs.firstOrNull { it.number == num } }
            val trackIndex = filteredTracks.indexOf(track).coerceAtLeast(0)
            val folderName = track.playlistId?.let { pid -> state.playlists.firstOrNull { it.id == pid }?.name }
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    1.dp,
                    if (track.isSurahConfirmed) StudySuccessGreen.copy(alpha = 0.45f)
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = { onPlayTracks(filteredTracks, trackIndex) },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "تشغيل",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = track.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val surahBadge = when {
                            surah != null && track.isSurahConfirmed -> "${surah.nameArabic} (مؤكدة ✓)"
                            surah != null -> "مقترحة: ${surah.nameArabic} — بحاجة لتأكيد"
                            else -> "لم تُحدد السورة بعد"
                        }
                        Text(
                            text = listOfNotNull(
                                folderName,
                                track.reciterName.ifBlank { "قارئ غير محدد" },
                                surahBadge
                            ).joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (track.isSurahConfirmed) StudySuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (track.durationMs > 0L) {
                            Text(
                                text = "${formatTimerClock(track.durationMs)} • ${track.qualityLabel}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = { editingTrack = track }) {
                        Icon(Icons.Filled.Edit, contentDescription = "تحديد السورة والقارئ والقسم")
                    }
                }
            }
        }

        // Imported Files List
        item(key = "imported_files_section_title") {
            Text(
                text = "الملفات المستوردة (${state.importedFiles.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        if (state.importedFiles.isEmpty()) {
            item(key = "empty_imported_files") {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text(
                        text = "لم تستورد أي ملفات قرآن أو تلاوات بعد. اضغط على «رفع ملفات متعددة» بالأعلى لاختيار ملف أو مجموعة ملفات معاً.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(items = state.importedFiles, key = { it.id }) { file ->
                ImportedFileCard(
                    file = file,
                    onOpenPdf = { onOpenPdfFile(file) },
                    onDelete = { onDeleteFile(file) }
                )
            }
        }
    }
}

@Composable
private fun ReciterFolderDialog(
    title: String,
    initialFolderName: String,
    initialReciterName: String,
    confirmLabel: String,
    onConfirm: (folderName: String, reciterName: String) -> Unit,
    onDismiss: () -> Unit
) {
    var folderName by remember { mutableStateOf(initialFolderName) }
    var reciterName by remember { mutableStateOf(initialReciterName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("اسم الملف / القسم (مثال: مصحف مرتل)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("folder_name_input")
                )
                OutlinedTextField(
                    value = reciterName,
                    onValueChange = { reciterName = it },
                    label = { Text("اسم القارئ (مثال: محمود خليل الحصري)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("folder_reciter_name_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (folderName.isNotBlank() || reciterName.isNotBlank()) {
                        onConfirm(folderName, reciterName)
                    }
                },
                modifier = Modifier.testTag("confirm_reciter_folder_button")
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
private fun ImportedFileCard(
    file: ImportedFileEntity,
    onOpenPdf: () -> Unit,
    onDelete: () -> Unit
) {
    val icon = when (file.fileType) {
        "PDF" -> Icons.Filled.PictureAsPdf
        "AUDIO" -> Icons.Filled.AudioFile
        else -> Icons.Filled.Description
    }
    val statusColor = when (file.status) {
        "READY" -> StudySuccessGreen
        "FAILED" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.secondary
    }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("imported_file_card_${file.id}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(icon, contentDescription = null, tint = statusColor)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = file.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val meta = when (file.status) {
                        "READY" -> when (file.fileType) {
                            "PDF" -> "PDF جاهز (${file.ayahCountImported} صفحة)"
                            "AUDIO" -> "ملف صوتي جاهز"
                            else -> "تم استيراد ${file.ayahCountImported} آية من ${file.surahCountImported} سورة"
                        }
                        "FAILED" -> file.errorMessage ?: "فشل الاستيراد"
                        else -> "جاري المعالجة (${file.progressPercent}%)"
                    }
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (file.status == "FAILED") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (file.fileType == "PDF" && file.status == "READY") {
                    OutlinedButton(onClick = onOpenPdf, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Text("فتح")
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "حذف الملف", tint = MaterialTheme.colorScheme.error)
                }
            }

            if (file.status == "PROCESSING") {
                LinearProgressIndicator(
                    progress = { (file.progressPercent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun TrackAssignmentDialog(
    track: QuranAudioTrackEntity,
    surahs: List<SurahEntity>,
    playlists: List<QuranPlaylistEntity>,
    onSave: (surahNumber: Int?, reciterName: String, confirmed: Boolean, playlistId: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var reciterName by remember(track.id) { mutableStateOf(track.reciterName) }
    var selectedSurahNum by remember(track.id) { mutableStateOf(track.surahNumber) }
    var selectedPlaylistId by remember(track.id) { mutableStateOf(track.playlistId) }
    var confirmed by remember(track.id) { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعيين السورة والقارئ والقسم") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = reciterName,
                    onValueChange = { reciterName = it },
                    label = { Text("اسم القارئ") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (playlists.isNotEmpty()) {
                    Text("نقل إلى ملف / قسم:", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(items = playlists, key = { it.id }) { p ->
                            FilterChip(
                                selected = selectedPlaylistId == p.id,
                                onClick = {
                                    selectedPlaylistId = p.id
                                    if (p.reciterName.isNotBlank()) reciterName = p.reciterName
                                },
                                label = { Text("${p.name} (${p.reciterName})") }
                            )
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { confirmed = !confirmed }
                ) {
                    Checkbox(checked = confirmed, onCheckedChange = { confirmed = it })
                    Text("تأكيد صحة رقم السورة المختارة", style = MaterialTheme.typography.bodySmall)
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                ) {
                    items(items = surahs, key = { it.number }) { s ->
                        val isSel = selectedSurahNum == s.number
                        Surface(
                            color = if (isSel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedSurahNum = s.number }
                        ) {
                            Text(
                                text = "${s.number}. ${s.nameArabic}",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(selectedSurahNum, reciterName, confirmed, selectedPlaylistId) }) {
                Text("حفظ وتأكيد")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
