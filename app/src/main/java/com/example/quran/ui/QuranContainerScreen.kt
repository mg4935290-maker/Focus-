package com.example.quran.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R

@Composable
fun QuranContainerScreen(
    quranState: QuranUiState,
    quranViewModel: QuranViewModel,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (!quranState.readerControlsHidden || quranState.activeSubTab != QuranSubTab.READER) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QuranSubTabChip(
                        label = stringResource(R.string.quran_subtab_smart),
                        selected = quranState.activeSubTab == QuranSubTab.SMART_TIMER,
                        onClick = { quranViewModel.selectSubTab(QuranSubTab.SMART_TIMER) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quran_subtab_smart")
                    )
                    QuranSubTabChip(
                        label = stringResource(R.string.quran_subtab_reader),
                        selected = quranState.activeSubTab == QuranSubTab.READER,
                        onClick = { quranViewModel.selectSubTab(QuranSubTab.READER) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quran_subtab_reader")
                    )
                    QuranSubTabChip(
                        label = stringResource(R.string.quran_subtab_library),
                        selected = quranState.activeSubTab == QuranSubTab.LIBRARY,
                        onClick = { quranViewModel.selectSubTab(QuranSubTab.LIBRARY) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quran_subtab_library")
                    )
                    QuranSubTabChip(
                        label = stringResource(R.string.quran_subtab_player),
                        selected = quranState.activeSubTab == QuranSubTab.PLAYER,
                        onClick = { quranViewModel.selectSubTab(QuranSubTab.PLAYER) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quran_subtab_player")
                    )
                }
            }
        }

        when (quranState.activeSubTab) {
            QuranSubTab.SMART_TIMER -> {
                SmartQuranSessionScreen(
                    state = quranState,
                    onSelectPresetMinutes = quranViewModel::selectPresetMinutes,
                    onCustomMinutesChange = quranViewModel::onCustomMinutesChange,
                    onSelectMode = quranViewModel::selectSessionMode,
                    onPreferUncompletedChange = quranViewModel::setPreferUncompleted,
                    onWithFocusLockChange = quranViewModel::setWithFocusLock,
                    onOnlyImportedTextChange = quranViewModel::setOnlyImportedTextSurahs,
                    onSmartSelectClick = quranViewModel::runSmartSelect,
                    onReselectClick = quranViewModel::reselectSmart,
                    onOpenEditSurahsDialog = quranViewModel::openEditSurahsDialog,
                    onCloseEditSurahsDialog = quranViewModel::closeEditSurahsDialog,
                    onToggleManualSurah = quranViewModel::toggleManualSurahSelection,
                    onStartSession = quranViewModel::startQuranSession,
                    onPauseResumeSession = quranViewModel::pauseOrResumeQuranSession,
                    onCompleteCurrentSurah = quranViewModel::advanceOrCompleteSurahInSession,
                    onFinishSession = { quranViewModel.finishQuranSession(completed = false) },
                    onDismissTimeExpiredKeepReading = quranViewModel::dismissTimeExpiredModalKeepReading,
                    onOpenReaderForSurah = { surahNum ->
                        quranViewModel.openSurahInReader(surahNum)
                        quranViewModel.selectSubTab(QuranSubTab.READER)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            QuranSubTab.READER -> {
                QuranReaderScreen(
                    state = quranState,
                    onSelectSurah = quranViewModel::openSurahInReader,
                    onPrevSurah = quranViewModel::previousSurahInReader,
                    onNextSurah = quranViewModel::nextSurahInReader,
                    onAdjustFontSize = quranViewModel::adjustReaderFontSize,
                    onToggleNightMode = quranViewModel::toggleReaderNightMode,
                    onToggleControlsHidden = quranViewModel::toggleReaderControlsHidden,
                    onSaveReadingPosition = quranViewModel::saveReadingPosition,
                    onToggleBookmark = quranViewModel::toggleBookmark,
                    onToggleSurahCompleted = quranViewModel::toggleSurahCompleted,
                    onOpenLibraryUpload = { quranViewModel.selectSubTab(QuranSubTab.LIBRARY) },
                    onOpenPdfFile = quranViewModel::openPdfViewer,
                    onClosePdfViewer = quranViewModel::closePdfViewer,
                    onToggleAudioPlayPause = quranViewModel::togglePlayPause,
                    modifier = Modifier.weight(1f)
                )
            }
            QuranSubTab.LIBRARY -> {
                QuranLibraryScreen(
                    state = quranState,
                    onUploadMultiFilesClick = quranViewModel::requestMultiFileUpload,
                    onConfirmReplaceDuplicate = quranViewModel::confirmReplaceDuplicate,
                    onDismissDuplicate = quranViewModel::dismissDuplicatePrompt,
                    onDeleteFile = quranViewModel::deleteImportedFile,
                    onOpenPdfFile = { file ->
                        quranViewModel.openPdfViewer(file)
                        quranViewModel.selectSubTab(QuranSubTab.READER)
                    },
                    onPlayTracks = quranViewModel::playTracks,
                    onUpdateTrackDetails = quranViewModel::updateTrackDetails,
                    onCreatePlaylist = quranViewModel::createPlaylist,
                    onUpdatePlaylist = quranViewModel::updatePlaylist,
                    onDeletePlaylist = quranViewModel::deletePlaylist,
                    onSelectPlaylistFilter = quranViewModel::selectPlaylistFilter,
                    modifier = Modifier.weight(1f)
                )
            }
            QuranSubTab.PLAYER -> {
                QuranPlayerTabContent(
                    state = quranState,
                    onPlayTracks = quranViewModel::playTracks,
                    onTogglePlayPause = quranViewModel::togglePlayPause,
                    onSeekTo = quranViewModel::seekTo,
                    onSeekBack10s = quranViewModel::seekBack10s,
                    onSeekForward10s = quranViewModel::seekForward10s,
                    onPrevSurah = quranViewModel::previousSurahAudio,
                    onNextSurah = quranViewModel::nextSurahAudio,
                    onSpeedSelected = quranViewModel::setPlaybackSpeed,
                    onCycleRepeat = quranViewModel::cycleRepeatMode,
                    onToggleShuffle = quranViewModel::toggleShuffle,
                    onOpenLibrary = { quranViewModel.selectSubTab(QuranSubTab.LIBRARY) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun QuranSubTabChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        modifier = modifier
    )
}
