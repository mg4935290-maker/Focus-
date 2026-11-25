package com.example.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.BuiltInSound
import com.example.data.UploadedAudioTrack
import com.example.quran.db.QuranAudioTrackEntity
import com.example.quran.db.QuranPlaylistEntity
import com.example.quran.db.SurahEntity
import com.example.quran.ui.QuranUiState
import com.example.quran.ui.QuranViewModel
import com.example.quran.ui.formatTimerClock
import com.example.ui.theme.QuramerSoftGold
import com.example.ui.theme.StudySuccessGreen
import kotlin.math.roundToInt

private enum class SoundsFilterMode {
    ALL,
    QURAN_ONLY,
    FOCUS_ONLY
}

@Composable
fun AudioTabContent(
    uiState: FocusUiState,
    quranState: QuranUiState,
    quranViewModel: QuranViewModel,
    needsNotificationPermission: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onSelectBuiltInSound: (BuiltInSound) -> Unit,
    onSelectUploadedTrack: (String) -> Unit,
    onUploadAudioClick: () -> Unit,
    onDeleteUploadedTrack: (String) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onLoopToggle: (Boolean) -> Unit,
    onTogglePlayback: () -> Unit,
    modifier: Modifier = Modifier
) {
    var filterMode by remember { mutableStateOf(SoundsFilterMode.ALL) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var editingFolder by remember { mutableStateOf<QuranPlaylistEntity?>(null) }
    var editingQuranTrack by remember { mutableStateOf<QuranAudioTrackEntity?>(null) }

    val duplicate = quranState.duplicatePrompts.firstOrNull()
    if (duplicate != null) {
        AlertDialog(
            onDismissRequest = quranViewModel::dismissDuplicatePrompt,
            title = { Text("تم اكتشاف ملف صوتي مكرر") },
            text = {
                Text(
                    "الملف «${duplicate.displayName}» مضاف مسبقاً («${duplicate.existingFile.displayName}»). هل تريد استبداله أم الإبقاء على الحالي؟"
                )
            },
            confirmButton = {
                Button(onClick = quranViewModel::confirmReplaceDuplicate) {
                    Text("استبدال الملف")
                }
            },
            dismissButton = {
                TextButton(onClick = quranViewModel::dismissDuplicatePrompt) {
                    Text("إبقاء القديم")
                }
            }
        )
    }

    if (showCreateFolderDialog) {
        StudyReciterFolderDialog(
            title = "إضافة ملف قارئ جديد",
            initialFolderName = "",
            initialReciterName = "",
            confirmLabel = "إنشاء القسم",
            onConfirm = { folderName, reciterName ->
                quranViewModel.createPlaylist(folderName, reciterName)
                showCreateFolderDialog = false
            },
            onDismiss = { showCreateFolderDialog = false }
        )
    }

    editingFolder?.let { folder ->
        StudyReciterFolderDialog(
            title = "تعديل ملف واسم القارئ",
            initialFolderName = folder.name,
            initialReciterName = folder.reciterName,
            confirmLabel = "حفظ التعديلات",
            onConfirm = { folderName, reciterName ->
                quranViewModel.updatePlaylist(folder.id, folderName, reciterName)
                editingFolder = null
            },
            onDismiss = { editingFolder = null }
        )
    }

    editingQuranTrack?.let { track ->
        StudyQuranTrackDialog(
            track = track,
            surahs = quranState.surahs,
            playlists = quranState.playlists,
            onSave = { surahNum, reciter, confirmed, folderId ->
                quranViewModel.updateTrackDetails(track.id, surahNum, reciter, confirmed, folderId)
                editingQuranTrack = null
            },
            onDismiss = { editingQuranTrack = null }
        )
    }

    val filteredQuranTracks = remember(quranState.audioTracks, quranState.selectedPlaylistIdFilter) {
        val f = quranState.selectedPlaylistIdFilter
        if (f == null) quranState.audioTracks else quranState.audioTracks.filter { it.playlistId == f }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterMode == SoundsFilterMode.ALL,
                    onClick = { filterMode = SoundsFilterMode.ALL },
                    label = { Text("الكل", maxLines = 1) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("audio_filter_all")
                )
                FilterChip(
                    selected = filterMode == SoundsFilterMode.QURAN_ONLY,
                    onClick = { filterMode = SoundsFilterMode.QURAN_ONLY },
                    label = { Text("القرآن والقراء", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("audio_subtab_quran")
                )
                FilterChip(
                    selected = filterMode == SoundsFilterMode.FOCUS_ONLY,
                    onClick = { filterMode = SoundsFilterMode.FOCUS_ONLY },
                    label = { Text("أصوات التركيز", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("audio_subtab_ambient")
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (needsNotificationPermission) {
                item(key = "notif_perm") {
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = stringResource(R.string.audio_permission_rationale),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = onRequestNotificationPermission,
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Text(stringResource(R.string.audio_permission_grant))
                            }
                        }
                    }
                }
            }

            item(key = "player_controls") {
                AudioPlayerControlCard(
                    uiState = uiState,
                    quranState = quranState,
                    forceFocusControls = filterMode == SoundsFilterMode.FOCUS_ONLY && !quranState.playbackState.isPlaying,
                    onVolumeChange = onVolumeChange,
                    onLoopToggle = onLoopToggle,
                    onToggleAmbientPlayback = onTogglePlayback,
                    onToggleQuranPlayPause = quranViewModel::togglePlayPause,
                    onSeekBack10s = quranViewModel::seekBack10s,
                    onSeekForward10s = quranViewModel::seekForward10s,
                    onOpenFullQuranPlayer = quranViewModel::openFullPlayer
                )
            }

            if (filterMode == SoundsFilterMode.ALL || filterMode == SoundsFilterMode.QURAN_ONLY) {
                item(key = "quran_session_section_header") {
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, QuramerSoftGold.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quran_reciters_session_card")
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                        contentDescription = null,
                                        tint = QuramerSoftGold
                                    )
                                    Column {
                                        Text(
                                            text = "القرآن الكريم أثناء المذاكرة",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "أضف تلاوات القرآن وقسّمها بملفات القراء لتشغيلها مع جلسة المذاكرة",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        quranViewModel.requestMultiFileUpload(quranState.selectedPlaylistIdFilter)
                                    },
                                    enabled = !quranState.isImportingFiles,
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .testTag("upload_multiple_files_button")
                                ) {
                                    Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = if (quranState.isImportingFiles) "جاري الإضافة..." else "رفع تلاوات",
                                        maxLines = 1
                                    )
                                }

                                OutlinedButton(
                                    onClick = { showCreateFolderDialog = true },
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .testTag("create_reciter_folder_button")
                                ) {
                                    Icon(Icons.Filled.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("ملف قارئ", maxLines = 1)
                                }
                            }

                            if (quranState.isImportingFiles) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }

                            if (quranState.playlists.isNotEmpty()) {
                                HorizontalDivider()
                                Text(
                                    text = "ملفات القراء (${quranState.playlists.size})",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )

                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    item {
                                        FilterChip(
                                            selected = quranState.selectedPlaylistIdFilter == null,
                                            onClick = { quranViewModel.selectPlaylistFilter(null) },
                                            label = { Text("كل القراء (${quranState.audioTracks.size})") }
                                        )
                                    }
                                    items(items = quranState.playlists, key = { it.id }) { folder ->
                                        val count = quranState.audioTracks.count { it.playlistId == folder.id }
                                        FilterChip(
                                            selected = quranState.selectedPlaylistIdFilter == folder.id,
                                            onClick = { quranViewModel.selectPlaylistFilter(folder.id) },
                                            label = { Text("${folder.name} • ${folder.reciterName} ($count)") }
                                        )
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    quranState.playlists.forEach { folder ->
                                        val folderTracks = quranState.audioTracks.filter { it.playlistId == folder.id }
                                        val isSelected = quranState.selectedPlaylistIdFilter == folder.id
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
                                                .clickable { quranViewModel.selectPlaylistFilter(folder.id) }
                                                .testTag("reciter_folder_item_${folder.id}")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
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
                                                    IconButton(onClick = { quranViewModel.playTracks(folderTracks, 0) }) {
                                                        Icon(Icons.Filled.PlayArrow, contentDescription = "تشغيل القسم", tint = MaterialTheme.colorScheme.primary)
                                                    }
                                                }
                                                IconButton(onClick = { quranViewModel.requestMultiFileUpload(folder.id) }) {
                                                    Icon(Icons.Filled.Add, contentDescription = "إضافة تلاوات للقارئ", tint = StudySuccessGreen)
                                                }
                                                IconButton(onClick = { editingFolder = folder }) {
                                                    Icon(Icons.Filled.Edit, contentDescription = "تعديل ملف القارئ")
                                                }
                                                IconButton(onClick = { quranViewModel.deletePlaylist(folder.id) }) {
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

                if (filteredQuranTracks.isEmpty()) {
                    item(key = "empty_quran_tracks") {
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LibraryMusic,
                                    contentDescription = null,
                                    tint = QuramerSoftGold
                                )
                                Text(
                                    text = "لا توجد تلاوات قرآن مضافة بعد. اضغط «رفع تلاوات» أو «ملف قارئ» لتشغيل القرآن أثناء المذاكرة.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(items = filteredQuranTracks, key = { "quran_${it.id}" }) { track ->
                        val isPlayingThis = quranState.playbackState.currentTrack?.id == track.id &&
                            quranState.playbackState.isPlaying
                        val surahName = track.surahNumber?.let { n ->
                            quranState.surahs.firstOrNull { it.number == n }?.nameArabic
                        }
                        val folderName = track.playlistId?.let { pid ->
                            quranState.playlists.firstOrNull { it.id == pid }?.name
                        }
                        val trackIndex = filteredQuranTracks.indexOf(track).coerceAtLeast(0)
                        val importedFile = quranState.importedFiles.firstOrNull { it.id == track.importedFileId }

                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = if (isPlayingThis) {
                                StudySuccessGreen.copy(alpha = 0.14f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            border = BorderStroke(
                                1.dp,
                                if (isPlayingThis) StudySuccessGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .clickable {
                                    if (isPlayingThis) {
                                        quranViewModel.togglePlayPause()
                                    } else {
                                        quranViewModel.playTracks(filteredQuranTracks, trackIndex)
                                    }
                                }
                                .testTag("quran_session_track_${track.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        if (isPlayingThis) {
                                            quranViewModel.togglePlayPause()
                                        } else {
                                            quranViewModel.playTracks(filteredQuranTracks, trackIndex)
                                        }
                                    },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = if (isPlayingThis) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = if (isPlayingThis) "إيقاف مؤقت" else "تشغيل التلاوة",
                                        tint = if (isPlayingThis) StudySuccessGreen else MaterialTheme.colorScheme.primary
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = surahName ?: track.displayName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val sub = listOfNotNull(
                                        folderName,
                                        track.reciterName.ifBlank { "قارئ غير محدد" },
                                        if (track.durationMs > 0L) formatTimerClock(track.durationMs) else null
                                    ).joinToString(" • ")
                                    Text(
                                        text = sub,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { editingQuranTrack = track },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = "تعديل القارئ والسورة")
                                }
                                if (importedFile != null) {
                                    IconButton(
                                        onClick = { quranViewModel.deleteImportedFile(importedFile) },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(
                                            Icons.Filled.DeleteOutline,
                                            contentDescription = "حذف التلاوة",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (filterMode == SoundsFilterMode.ALL || filterMode == SoundsFilterMode.FOCUS_ONLY) {
                item(key = "headphones_notice") {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Headphones,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.audio_headphones_notice),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                item(key = "builtin_header") {
                    Text(
                        text = stringResource(R.string.audio_builtin_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                items(items = BuiltInSound.entries, key = { it.id }) { sound ->
                    val isSelected = uiState.selectedUploadedTrackId == null && uiState.selectedBuiltInSound == sound
                    BuiltInSoundRow(
                        sound = sound,
                        isSelected = isSelected,
                        onClick = { onSelectBuiltInSound(sound) }
                    )
                }

                item(key = "uploaded_header") {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "ملفات أصوات أخرى من الجهاز",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = onUploadAudioClick,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("upload_audio_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.UploadFile,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.audio_upload_button))
                        }
                    }
                }

                if (uiState.uploadedTracks.isEmpty()) {
                    item(key = "uploaded_empty") {
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LibraryMusic,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = stringResource(R.string.audio_uploaded_empty),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(items = uiState.uploadedTracks, key = { it.id }) { track ->
                        UploadedTrackRow(
                            track = track,
                            isSelected = uiState.selectedUploadedTrackId == track.id,
                            onSelect = { onSelectUploadedTrack(track.id) },
                            onDelete = { onDeleteUploadedTrack(track.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioPlayerControlCard(
    uiState: FocusUiState,
    quranState: QuranUiState,
    forceFocusControls: Boolean,
    onVolumeChange: (Float) -> Unit,
    onLoopToggle: (Boolean) -> Unit,
    onToggleAmbientPlayback: () -> Unit,
    onToggleQuranPlayPause: () -> Unit,
    onSeekBack10s: () -> Unit,
    onSeekForward10s: () -> Unit,
    onOpenFullQuranPlayer: () -> Unit
) {
    val quranPlayback = quranState.playbackState
    val showQuranControls = !forceFocusControls &&
        (quranPlayback.isPlaying || (!uiState.isAudioPlaying && quranPlayback.hasActiveTrack))
    val anyPlaying = uiState.isAudioPlaying || quranPlayback.isPlaying

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (anyPlaying) StudySuccessGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        ),
        modifier = Modifier.fillMaxWidth()
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
                    color = if (anyPlaying) {
                        StudySuccessGreen.copy(alpha = 0.16f)
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.GraphicEq,
                            contentDescription = null,
                            tint = if (anyPlaying) StudySuccessGreen else MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (showQuranControls) {
                            val track = quranPlayback.currentTrack
                            val surah = track?.surahNumber?.let { n ->
                                quranState.surahs.firstOrNull { it.number == n }?.nameArabic
                            }
                            surah ?: track?.displayName ?: stringResource(R.string.audio_section_title)
                        } else {
                            stringResource(R.string.audio_section_title)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (showQuranControls) {
                            val reciter = quranPlayback.currentTrack?.reciterName.orEmpty().ifBlank { "القرآن الكريم" }
                            "$reciter • ${formatTimerClock(quranPlayback.positionMs)} / ${formatTimerClock(quranPlayback.durationMs)}"
                        } else {
                            stringResource(R.string.audio_section_subtitle)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (showQuranControls) {
                    IconButton(onClick = onOpenFullQuranPlayer) {
                        Icon(
                            imageVector = Icons.Filled.OpenInFull,
                            contentDescription = "فتح المشغل الكامل",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (showQuranControls) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onSeekBack10s,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Filled.Replay10, contentDescription = "رجوع 10 ثوانٍ")
                    }
                    Button(
                        onClick = onToggleQuranPlayPause,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("quran_audio_play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (quranPlayback.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(if (quranPlayback.isPlaying) R.string.audio_pause else R.string.audio_play),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    OutlinedButton(
                        onClick = onSeekForward10s,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Filled.Forward10, contentDescription = "تقديم 10 ثوانٍ")
                    }
                }
                OutlinedButton(
                    onClick = onToggleAmbientPlayback,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("switch_to_focus_sound_button")
                ) {
                    Icon(Icons.Filled.GraphicEq, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("تشغيل صوت التركيز المحدد بدلاً من التلاوة", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            } else {
                Button(
                    onClick = onToggleAmbientPlayback,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isAudioPlaying) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    ),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("audio_play_pause_button")
                ) {
                    Icon(
                        imageVector = if (uiState.isAudioPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(
                            if (uiState.isAudioPlaying) R.string.audio_pause else R.string.audio_play
                        ),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.audio_volume_label),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = uiState.audioVolume,
                        onValueChange = onVolumeChange,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("audio_volume_slider")
                    )
                    Text(
                        text = "${(uiState.audioVolume * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.audio_loop_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = uiState.audioLoopEnabled,
                        onCheckedChange = onLoopToggle,
                        modifier = Modifier.testTag("audio_loop_switch")
                    )
                }
            }
        }
    }
}

@Composable
private fun StudyReciterFolderDialog(
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
                    label = { Text("اسم الملف / القسم (مثال: تلاوات هادئة)") },
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
                enabled = folderName.isNotBlank() || reciterName.isNotBlank(),
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
private fun StudyQuranTrackDialog(
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
        title = { Text("تعيين القارئ والسورة والقسم") },
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
                    Text("ملف القارئ:", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(items = playlists, key = { it.id }) { p ->
                            FilterChip(
                                selected = selectedPlaylistId == p.id,
                                onClick = {
                                    if (selectedPlaylistId == p.id) {
                                        selectedPlaylistId = null
                                    } else {
                                        selectedPlaylistId = p.id
                                        if (p.reciterName.isNotBlank()) reciterName = p.reciterName
                                    }
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
                    Text("تأكيد رقم السورة", style = MaterialTheme.typography.bodySmall)
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
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
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
private fun BuiltInSoundRow(
    sound: BuiltInSound,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val labelRes = when (sound) {
        BuiltInSound.RAIN -> R.string.audio_rain
        BuiltInSound.WHITE_NOISE -> R.string.audio_white_noise
        BuiltInSound.ALPHA -> R.string.audio_alpha
        BuiltInSound.BETA -> R.string.audio_beta
        BuiltInSound.GAMMA -> R.string.audio_gamma
    }
    val icon: ImageVector = when (sound) {
        BuiltInSound.RAIN -> Icons.Filled.WaterDrop
        BuiltInSound.WHITE_NOISE -> Icons.Filled.GraphicEq
        BuiltInSound.ALPHA, BuiltInSound.BETA, BuiltInSound.GAMMA -> Icons.Filled.Headphones
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .testTag("builtin_sound_${sound.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun UploadedTrackRow(
    track: UploadedAudioTrack,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) {
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onSelect)
            .testTag("uploaded_track_${track.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.AudioFile,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
            )
            Text(
                text = track.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = stringResource(R.string.cd_delete_audio),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
