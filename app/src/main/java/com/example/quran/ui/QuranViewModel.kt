package com.example.quran.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.FocusAudioService
import com.example.data.FocusRepository
import com.example.quran.audio.QuranAudioPlaybackState
import com.example.quran.audio.QuranPlayerController
import com.example.quran.data.FileImportOutcome
import com.example.quran.data.QuranLibraryRepository
import com.example.quran.db.AyahEntity
import com.example.quran.db.BookmarkEntity
import com.example.quran.db.ImportedFileEntity
import com.example.quran.db.QuramerDatabase
import com.example.quran.db.QuranAudioTrackEntity
import com.example.quran.db.QuranPlaylistEntity
import com.example.quran.db.QuranSessionLogEntity
import com.example.quran.db.SurahEntity
import com.example.quran.db.UserQuranProfileEntity
import com.example.quran.domain.QuranSessionMode
import com.example.quran.domain.SmartSelectionResult
import com.example.quran.domain.SmartSurahSelector
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class QuranSubTab {
    SMART_TIMER,
    READER,
    LIBRARY,
    PLAYER
}

data class DuplicateFilePrompt(
    val displayName: String,
    val uriString: String,
    val existingFile: ImportedFileEntity
)

data class ActiveQuranSession(
    val startedAtMs: Long,
    val plannedDurationMs: Long,
    val activeElapsedMs: Long,
    val isPaused: Boolean,
    val lastResumedAtMs: Long,
    val mode: QuranSessionMode,
    val surahNumbers: List<Int>,
    val currentSurahIndex: Int,
    val focusLockEnabled: Boolean,
    val showTimeExpiredModal: Boolean = false
) {
    val remainingMs: Long
        get() = (plannedDurationMs - activeElapsedMs).coerceAtLeast(0L)

    val currentSurahNumber: Int?
        get() = surahNumbers.getOrNull(currentSurahIndex)
}

data class QuranPlaySuggestionPrompt(
    val title: String,
    val message: String,
    val tracksToPlay: List<QuranAudioTrackEntity>,
    val openFullPlayerOnAccept: Boolean = false
)

data class QuranUiState(
    val activeSubTab: QuranSubTab = QuranSubTab.SMART_TIMER,
    val surahs: List<SurahEntity> = emptyList(),
    val importedFiles: List<ImportedFileEntity> = emptyList(),
    val audioTracks: List<QuranAudioTrackEntity> = emptyList(),
    val playlists: List<QuranPlaylistEntity> = emptyList(),
    val bookmarks: List<BookmarkEntity> = emptyList(),
    val sessionLogs: List<QuranSessionLogEntity> = emptyList(),
    val profile: UserQuranProfileEntity = UserQuranProfileEntity(),

    // Smart Quran Timer
    val targetPresetMinutes: Int = 20,
    val customMinutesInput: String = "",
    val sessionMode: QuranSessionMode = QuranSessionMode.READING,
    val preferUncompleted: Boolean = true,
    val withFocusLock: Boolean = false,
    val onlyImportedTextSurahs: Boolean = false,
    val reselectVariantIndex: Int = 0,
    val smartSelection: SmartSelectionResult? = null,
    val manualSurahOverrides: List<Int>? = null,
    val showEditSurahsDialog: Boolean = false,

    // Active Decoupled Session
    val activeSession: ActiveQuranSession? = null,

    // Quran Reader (single Surah loaded lazily)
    val readerSurahNumber: Int = 1,
    val readerAyahs: List<AyahEntity> = emptyList(),
    val readerHasTimingSync: Boolean = false,
    val readerFontSizeSp: Float = 24f,
    val readerNightMode: Boolean = false,
    val readerControlsHidden: Boolean = false,
    val activePdfFile: ImportedFileEntity? = null,

    // Multi-file Import
    val isImportingFiles: Boolean = false,
    val duplicatePrompts: List<DuplicateFilePrompt> = emptyList(),
    val targetUploadPlaylistId: String? = null,

    // Audio Player & Suggestion
    val playbackState: QuranAudioPlaybackState = QuranAudioPlaybackState(),
    val isFullPlayerVisible: Boolean = false,
    val selectedPlaylistIdFilter: String? = null,
    val pendingPlaySuggestion: QuranPlaySuggestionPrompt? = null,
    val hubSuggestionDismissed: Boolean = false
) {
    val effectiveTargetMinutes: Int
        get() = customMinutesInput.toIntOrNull()?.takeIf { it > 0 } ?: targetPresetMinutes

    val currentReaderSurah: SurahEntity?
        get() = surahs.firstOrNull { it.number == readerSurahNumber }

    val effectiveSelectedSurahs: List<SurahEntity>
        get() {
            val manual = manualSurahOverrides
            if (manual != null) {
                return manual.mapNotNull { num -> surahs.firstOrNull { it.number == num } }
            }
            return smartSelection?.selectedSurahs?.map { it.surah }.orEmpty()
        }

    val activeTimedAyahNumber: Int?
        get() {
            if (!readerHasTimingSync) return null
            val track = playbackState.currentTrack ?: return null
            if (!track.isSurahConfirmed || track.surahNumber != readerSurahNumber) return null
            val pos = playbackState.positionMs
            return readerAyahs.firstOrNull { ayah ->
                val s = ayah.startTimeMs ?: return@firstOrNull false
                val e = ayah.endTimeMs ?: Long.MAX_VALUE
                pos in s..e
            }?.ayahNumber
        }
}

sealed interface QuranUiEvent {
    data class ShowMessage(val message: String) : QuranUiEvent
    data object LaunchMultiFilePicker : QuranUiEvent
    data class OpenPdfViewer(val file: ImportedFileEntity) : QuranUiEvent
    data class NotifyFocusLockRefresh(val started: Boolean) : QuranUiEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class QuranViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: QuranLibraryRepository = QuranLibraryRepository(
        context = application,
        database = QuramerDatabase.getInstance(application),
        focusRepository = FocusRepository(application)
    ),
    private val focusRepository: FocusRepository = FocusRepository(application),
    private val playerController: QuranPlayerController = QuranPlayerController(application)
) : AndroidViewModel(application) {

    private val sessionPrefs = application.getSharedPreferences(PREFS_QURAN_SESSION, Context.MODE_PRIVATE)

    private val _selectedReaderSurah = MutableStateFlow(1)
    private val _uiState = MutableStateFlow(QuranUiState())
    val uiState: StateFlow<QuranUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<QuranUiEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<QuranUiEvent> = _events.asSharedFlow()

    private var sessionTickerJob: Job? = null

    init {
        viewModelScope.launch {
            repository.ensureInitialized()
        }
        playerController.connect()
        observeDatabaseFlows()
        observeReaderAyahs()
        observeAudioPlayer()
        restorePersistedSession()
    }

    private fun observeDatabaseFlows() {
        viewModelScope.launch {
            combine(
                repository.surahsFlow,
                repository.importedFilesFlow,
                repository.audioTracksFlow,
                repository.playlistsFlow
            ) { surahs, files, tracks, playlists ->
                DatabaseBundleA(surahs, files, tracks, playlists)
            }.combine(
                combine(
                    repository.bookmarksFlow,
                    repository.sessionLogsFlow,
                    repository.profileFlow
                ) { bookmarks, logs, profile ->
                    DatabaseBundleB(bookmarks, logs, profile)
                }
            ) { a, b -> a to b }
                .collect { (a, b) ->
                    _uiState.update { state ->
                        val updatedState = state.copy(
                            surahs = a.surahs,
                            importedFiles = a.files,
                            audioTracks = a.tracks,
                            playlists = a.playlists,
                            bookmarks = b.bookmarks,
                            sessionLogs = b.logs,
                            profile = b.profile
                        )
                        if (updatedState.smartSelection == null && a.surahs.isNotEmpty()) {
                            updatedState.copy(
                                smartSelection = computeSmartSelection(updatedState)
                            )
                        } else {
                            updatedState
                        }
                    }
                    // Initialize reader surah on first launch to last viewed surah
                    if (b.profile.lastViewedSurah in 1..114 && _uiState.value.activeSession == null) {
                        if (_selectedReaderSurah.value == 1 && b.profile.lastViewedSurah != 1 && _uiState.value.readerAyahs.isEmpty()) {
                            _selectedReaderSurah.value = b.profile.lastViewedSurah
                            _uiState.update { it.copy(readerSurahNumber = b.profile.lastViewedSurah) }
                        }
                    }
                }
        }
    }

    private fun observeReaderAyahs() {
        viewModelScope.launch {
            _selectedReaderSurah
                .flatMapLatest { surahNum ->
                    repository.observeAyahsForSurah(surahNum)
                }
                .collect { ayahs ->
                    val hasTiming = ayahs.any { it.startTimeMs != null }
                    _uiState.update {
                        it.copy(
                            readerAyahs = ayahs,
                            readerHasTimingSync = hasTiming
                        )
                    }
                }
        }
    }

    private fun observeAudioPlayer() {
        viewModelScope.launch {
            playerController.state.collect { playback ->
                _uiState.update { it.copy(playbackState = playback) }
            }
        }
    }

    private fun computeSmartSelection(state: QuranUiState): SmartSelectionResult {
        return SmartSurahSelector.selectSurahs(
            surahs = state.surahs,
            targetMinutes = state.effectiveTargetMinutes,
            readingSpeedWpm = state.profile.readingSpeedWpm,
            mode = state.sessionMode,
            audioTracks = state.audioTracks,
            preferUncompleted = state.preferUncompleted,
            onlySurahsWithImportedText = state.onlyImportedTextSurahs,
            variantIndex = state.reselectVariantIndex
        )
    }

    fun selectSubTab(subTab: QuranSubTab) {
        _uiState.update { it.copy(activeSubTab = subTab) }
    }

    // Smart Quran Timer Actions
    fun selectPresetMinutes(minutes: Int) {
        _uiState.update { state ->
            val updated = state.copy(
                targetPresetMinutes = minutes,
                customMinutesInput = "",
                manualSurahOverrides = null,
                reselectVariantIndex = 0
            )
            updated.copy(smartSelection = computeSmartSelection(updated))
        }
    }

    fun onCustomMinutesChange(raw: String) {
        val digits = raw.filter { it.isDigit() }.take(3)
        _uiState.update { state ->
            val updated = state.copy(
                customMinutesInput = digits,
                manualSurahOverrides = null
            )
            updated.copy(smartSelection = computeSmartSelection(updated))
        }
    }

    fun selectSessionMode(mode: QuranSessionMode) {
        _uiState.update { state ->
            val updated = state.copy(
                sessionMode = mode,
                manualSurahOverrides = null
            )
            updated.copy(smartSelection = computeSmartSelection(updated))
        }
    }

    fun setPreferUncompleted(prefer: Boolean) {
        _uiState.update { state ->
            val updated = state.copy(
                preferUncompleted = prefer,
                manualSurahOverrides = null
            )
            updated.copy(smartSelection = computeSmartSelection(updated))
        }
    }

    fun setWithFocusLock(enabled: Boolean) {
        _uiState.update { it.copy(withFocusLock = enabled) }
    }

    fun setOnlyImportedTextSurahs(onlyImported: Boolean) {
        _uiState.update { state ->
            val updated = state.copy(
                onlyImportedTextSurahs = onlyImported,
                manualSurahOverrides = null
            )
            updated.copy(smartSelection = computeSmartSelection(updated))
        }
    }

    fun runSmartSelect() {
        _uiState.update { state ->
            val updated = state.copy(
                manualSurahOverrides = null,
                reselectVariantIndex = 0
            )
            updated.copy(smartSelection = computeSmartSelection(updated))
        }
    }

    fun reselectSmart() {
        _uiState.update { state ->
            val updated = state.copy(
                manualSurahOverrides = null,
                reselectVariantIndex = state.reselectVariantIndex + 1
            )
            updated.copy(smartSelection = computeSmartSelection(updated))
        }
    }

    fun openEditSurahsDialog() {
        _uiState.update { it.copy(showEditSurahsDialog = true) }
    }

    fun closeEditSurahsDialog() {
        _uiState.update { it.copy(showEditSurahsDialog = false) }
    }

    fun toggleManualSurahSelection(surahNumber: Int) {
        _uiState.update { state ->
            val current = (state.manualSurahOverrides
                ?: state.smartSelection?.selectedSurahs?.map { it.surah.number }.orEmpty()).toMutableSet()
            if (surahNumber in current) {
                current.remove(surahNumber)
            } else {
                current.add(surahNumber)
            }
            state.copy(manualSurahOverrides = current.sorted())
        }
    }

    // Active Decoupled Quran Session
    fun startQuranSession() {
        val state = _uiState.value
        if (state.activeSession != null) return
        val selected = state.effectiveSelectedSurahs
        if (selected.isEmpty()) {
            _events.tryEmit(QuranUiEvent.ShowMessage("اختر سورة واحدة على الأقل لبدء الجلسة"))
            return
        }
        val targetMins = state.effectiveTargetMinutes.coerceAtLeast(1)
        val now = System.currentTimeMillis()
        val plannedMs = targetMins * 60_000L

        val serviceReady = focusRepository.isBlockerServiceEnabled()
        val actualLockEnabled = state.withFocusLock && serviceReady
        if (actualLockEnabled) {
            val allowed = focusRepository.getAllowedPackages()
            focusRepository.startSession(allowedPackages = allowed, durationMinutes = targetMins.toLong(), nowMs = now)
            _events.tryEmit(QuranUiEvent.NotifyFocusLockRefresh(started = true))
        } else if (state.withFocusLock && !serviceReady) {
            _events.tryEmit(QuranUiEvent.ShowMessage("بدأت الجلسة بنجاح بدون الحاجة لإعدادات الحظر"))
        }

        val session = ActiveQuranSession(
            startedAtMs = now,
            plannedDurationMs = plannedMs,
            activeElapsedMs = 0L,
            isPaused = false,
            lastResumedAtMs = now,
            mode = state.sessionMode,
            surahNumbers = selected.map { it.number },
            currentSurahIndex = 0,
            focusLockEnabled = actualLockEnabled,
            showTimeExpiredModal = false
        )
        persistSession(session)
        _uiState.update { it.copy(activeSession = session) }
        startSessionTicker()

        val firstSurah = selected.first().number
        openSurahInReader(firstSurah)

        // Never force/auto-play Quran immediately: propose to the user first so they decide Yes or No
        val matchingTracks = findAudioTracksForSurahs(selected.map { it.number })
        if ((state.sessionMode == QuranSessionMode.LISTENING || state.sessionMode == QuranSessionMode.COMBINED) &&
            matchingTracks.isNotEmpty()
        ) {
            val firstTrack = matchingTracks.first()
            val surahTitle = selected.first().nameArabic
            _uiState.update {
                it.copy(
                    pendingPlaySuggestion = QuranPlaySuggestionPrompt(
                        title = "هل تحب تشغيل التلاوة الآن؟",
                        message = "نقترح تشغيل «$surahTitle» (${firstTrack.reciterName.ifBlank { firstTrack.displayName }}) أثناء جلستك. هل توافق؟",
                        tracksToPlay = matchingTracks,
                        openFullPlayerOnAccept = state.sessionMode == QuranSessionMode.LISTENING
                    )
                )
            }
        }

        if (state.sessionMode != QuranSessionMode.LISTENING) {
            _uiState.update { it.copy(activeSubTab = QuranSubTab.READER) }
        }
    }

    fun proposeQuranListeningForStudyOrHub() {
        val state = _uiState.value
        val candidateTracks = state.audioTracks
        if (candidateTracks.isEmpty()) return
        val firstTrack = candidateTracks.first()
        val surahName = firstTrack.surahNumber?.let { num ->
            state.surahs.firstOrNull { it.number == num }?.nameArabic
        } ?: firstTrack.displayName

        _uiState.update {
            it.copy(
                pendingPlaySuggestion = QuranPlaySuggestionPrompt(
                    title = "اقتراح تلاوة قرآنية",
                    message = "تحب نشغّل تلاوة «$surahName» (${firstTrack.reciterName.ifBlank { "من مكتبتك" }}) الآن ولا نخليها بعدين؟",
                    tracksToPlay = candidateTracks,
                    openFullPlayerOnAccept = false
                )
            )
        }
    }

    fun acceptPlaySuggestion() {
        val suggestion = _uiState.value.pendingPlaySuggestion ?: return
        _uiState.update {
            it.copy(
                pendingPlaySuggestion = null,
                isFullPlayerVisible = suggestion.openFullPlayerOnAccept
            )
        }
        if (suggestion.tracksToPlay.isNotEmpty()) {
            playerController.playPlaylist(suggestion.tracksToPlay, 0)
        }
    }

    fun declinePlaySuggestion() {
        _uiState.update {
            it.copy(
                pendingPlaySuggestion = null,
                hubSuggestionDismissed = true
            )
        }
    }

    fun dismissHubSuggestion() {
        _uiState.update { it.copy(hubSuggestionDismissed = true) }
    }

    private fun findAudioTracksForSurahs(surahNumbers: List<Int>): List<QuranAudioTrackEntity> {
        val tracks = _uiState.value.audioTracks
        return surahNumbers.mapNotNull { surahNum ->
            tracks.firstOrNull { it.surahNumber == surahNum && it.isSurahConfirmed }
                ?: tracks.firstOrNull { it.surahNumber == surahNum }
        }
    }

    fun pauseOrResumeQuranSession() {
        val current = _uiState.value.activeSession ?: return
        val now = System.currentTimeMillis()
        val updated = if (current.isPaused) {
            current.copy(
                isPaused = false,
                lastResumedAtMs = now
            )
        } else {
            val elapsedNow = computeElapsed(current, now)
            current.copy(
                isPaused = true,
                activeElapsedMs = elapsedNow,
                lastResumedAtMs = now
            )
        }
        persistSession(updated)
        _uiState.update { it.copy(activeSession = updated) }
    }

    fun advanceOrCompleteSurahInSession() {
        val current = _uiState.value.activeSession ?: return
        val currentSurah = current.currentSurahNumber
        if (currentSurah != null) {
            viewModelScope.launch {
                repository.toggleSurahCompleted(currentSurah, true)
            }
        }
        val nextIndex = current.currentSurahIndex + 1
        if (nextIndex < current.surahNumbers.size) {
            val updated = current.copy(
                currentSurahIndex = nextIndex,
                showTimeExpiredModal = false
            )
            persistSession(updated)
            _uiState.update { it.copy(activeSession = updated) }
            openSurahInReader(updated.surahNumbers[nextIndex])
            _events.tryEmit(QuranUiEvent.ShowMessage("ما شاء الله! انتقلت للسورة التالية في خطة الجلسة"))
        } else {
            finishQuranSession(completed = true)
        }
    }

    fun dismissTimeExpiredModalKeepReading() {
        val current = _uiState.value.activeSession ?: return
        val updated = current.copy(showTimeExpiredModal = false)
        persistSession(updated)
        _uiState.update { it.copy(activeSession = updated) }
    }

    fun finishQuranSession(completed: Boolean = true) {
        val current = _uiState.value.activeSession ?: return
        val now = System.currentTimeMillis()
        val elapsedMs = computeElapsed(current, now).coerceAtLeast(1_000L)

        sessionTickerJob?.cancel()
        clearPersistedSession()
        _uiState.update { it.copy(activeSession = null) }

        if (current.focusLockEnabled) {
            focusRepository.cancelSession(now)
            _events.tryEmit(QuranUiEvent.NotifyFocusLockRefresh(started = false))
        }

        viewModelScope.launch {
            val sessionSurahs = current.surahNumbers
            var ayahsCount = 0
            var wordsCount = 0
            sessionSurahs.forEach { num ->
                val surahMeta = _uiState.value.surahs.firstOrNull { it.number == num }
                ayahsCount += surahMeta?.ayahCount ?: 0
                wordsCount += surahMeta?.wordCount ?: 0
            }
            repository.recordQuranSession(
                mode = current.mode,
                startedAtMs = current.startedAtMs,
                endedAtMs = now,
                plannedDurationMs = current.plannedDurationMs,
                actualDurationMs = elapsedMs,
                surahNumbers = sessionSurahs,
                ayahsReadCount = ayahsCount,
                wordsReadCount = wordsCount,
                focusLockEnabled = current.focusLockEnabled,
                completed = completed,
                creditTreeFarm = !current.focusLockEnabled
            )
            _events.tryEmit(
                QuranUiEvent.ShowMessage(
                    if (completed) "تقبّل الله منك! تم حفظ الجلسة وإنجاز السور"
                    else "تم إنهاء الجلسة وحفظ وقتك بنجاح"
                )
            )
        }
    }

    private fun computeElapsed(session: ActiveQuranSession, nowMs: Long): Long {
        return if (session.isPaused) {
            session.activeElapsedMs
        } else {
            (session.activeElapsedMs + (nowMs - session.lastResumedAtMs).coerceAtLeast(0L))
        }
    }

    private fun startSessionTicker() {
        sessionTickerJob?.cancel()
        sessionTickerJob = viewModelScope.launch {
            while (isActive) {
                val current = _uiState.value.activeSession ?: break
                val now = System.currentTimeMillis()
                if (!current.isPaused) {
                    val elapsed = computeElapsed(current, now)
                    val remaining = (current.plannedDurationMs - elapsed).coerceAtLeast(0L)
                    val shouldShowModal = remaining == 0L &&
                        !current.showTimeExpiredModal &&
                        elapsed >= current.plannedDurationMs &&
                        current.plannedDurationMs > 0L

                    val updated = current.copy(
                        activeElapsedMs = elapsed,
                        lastResumedAtMs = now,
                        showTimeExpiredModal = current.showTimeExpiredModal || shouldShowModal
                    )
                    if (shouldShowModal) {
                        persistSession(updated)
                    }
                    _uiState.update { it.copy(activeSession = updated) }
                }
                delay(1000L)
            }
        }
    }

    private fun persistSession(session: ActiveQuranSession) {
        sessionPrefs.edit {
            putLong(KEY_START, session.startedAtMs)
            putLong(KEY_PLANNED, session.plannedDurationMs)
            putLong(KEY_ELAPSED, session.activeElapsedMs)
            putBoolean(KEY_PAUSED, session.isPaused)
            putLong(KEY_LAST_RESUMED, session.lastResumedAtMs)
            putString(KEY_MODE, session.mode.name)
            putString(KEY_SURAHS, session.surahNumbers.joinToString(","))
            putInt(KEY_INDEX, session.currentSurahIndex)
            putBoolean(KEY_FOCUS_LOCK, session.focusLockEnabled)
            putBoolean(KEY_EXPIRED_SHOWN, session.showTimeExpiredModal)
        }
    }

    private fun restorePersistedSession() {
        val start = sessionPrefs.getLong(KEY_START, 0L)
        if (start <= 0L) return
        val planned = sessionPrefs.getLong(KEY_PLANNED, 0L)
        val elapsed = sessionPrefs.getLong(KEY_ELAPSED, 0L)
        val paused = sessionPrefs.getBoolean(KEY_PAUSED, false)
        val lastResumed = sessionPrefs.getLong(KEY_LAST_RESUMED, start)
        val mode = runCatching {
            QuranSessionMode.valueOf(sessionPrefs.getString(KEY_MODE, QuranSessionMode.READING.name)!!)
        }.getOrDefault(QuranSessionMode.READING)
        val surahs = sessionPrefs.getString(KEY_SURAHS, "")
            .orEmpty()
            .split(',')
            .mapNotNull { it.toIntOrNull() }
        if (surahs.isEmpty()) {
            clearPersistedSession()
            return
        }
        val idx = sessionPrefs.getInt(KEY_INDEX, 0).coerceIn(0, surahs.lastIndex)
        val focusLock = sessionPrefs.getBoolean(KEY_FOCUS_LOCK, false)
        val expiredModal = sessionPrefs.getBoolean(KEY_EXPIRED_SHOWN, false)
        val now = System.currentTimeMillis()
        val adjustedElapsed = if (paused) elapsed else elapsed + (now - lastResumed).coerceAtLeast(0L)

        val restored = ActiveQuranSession(
            startedAtMs = start,
            plannedDurationMs = planned,
            activeElapsedMs = adjustedElapsed,
            isPaused = paused,
            lastResumedAtMs = now,
            mode = mode,
            surahNumbers = surahs,
            currentSurahIndex = idx,
            focusLockEnabled = focusLock,
            showTimeExpiredModal = expiredModal || (planned > 0L && adjustedElapsed >= planned)
        )
        _uiState.update { it.copy(activeSession = restored) }
        _selectedReaderSurah.value = restored.surahNumbers[idx]
        _uiState.update { it.copy(readerSurahNumber = restored.surahNumbers[idx]) }
        startSessionTicker()
    }

    private fun clearPersistedSession() {
        sessionPrefs.edit { clear() }
    }

    // Reader Operations
    fun openSurahInReader(surahNumber: Int) {
        val safe = surahNumber.coerceIn(1, 114)
        _selectedReaderSurah.value = safe
        _uiState.update { it.copy(readerSurahNumber = safe) }
    }

    fun nextSurahInReader() {
        val next = (_uiState.value.readerSurahNumber + 1).coerceAtMost(114)
        openSurahInReader(next)
    }

    fun previousSurahInReader() {
        val prev = (_uiState.value.readerSurahNumber - 1).coerceAtLeast(1)
        openSurahInReader(prev)
    }

    fun adjustReaderFontSize(deltaSp: Float) {
        _uiState.update { state ->
            state.copy(readerFontSizeSp = (state.readerFontSizeSp + deltaSp).coerceIn(16f, 40f))
        }
    }

    fun toggleReaderNightMode() {
        _uiState.update { it.copy(readerNightMode = !it.readerNightMode) }
    }

    fun toggleReaderControlsHidden() {
        _uiState.update { it.copy(readerControlsHidden = !it.readerControlsHidden) }
    }

    fun saveReadingPosition(surahNumber: Int, ayahNumber: Int, progress: Float) {
        viewModelScope.launch {
            repository.saveReadingPosition(surahNumber, ayahNumber, progress)
        }
    }

    fun toggleBookmark(surahNumber: Int, ayahNumber: Int) {
        val currentlyBookmarked = _uiState.value.bookmarks.any {
            it.surahNumber == surahNumber && it.ayahNumber == ayahNumber
        }
        viewModelScope.launch {
            repository.toggleBookmark(surahNumber, ayahNumber, currentlyBookmarked)
        }
    }

    fun toggleSurahCompleted(surahNumber: Int, completed: Boolean) {
        viewModelScope.launch {
            repository.toggleSurahCompleted(surahNumber, completed)
        }
    }

    fun openPdfViewer(file: ImportedFileEntity) {
        _uiState.update { it.copy(activePdfFile = file) }
    }

    fun closePdfViewer() {
        _uiState.update { it.copy(activePdfFile = null) }
    }

    // Multi-file SAF Upload
    fun requestMultiFileUpload(targetPlaylistId: String? = _uiState.value.selectedPlaylistIdFilter) {
        _uiState.update { it.copy(targetUploadPlaylistId = targetPlaylistId) }
        _events.tryEmit(QuranUiEvent.LaunchMultiFilePicker)
    }

    fun onMultipleFilesSelected(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val folderId = _uiState.value.targetUploadPlaylistId
        _uiState.update { it.copy(isImportingFiles = true) }
        viewModelScope.launch {
            val summary = repository.importMultipleUris(uris, targetPlaylistId = folderId)
            val duplicates = summary.outcomes.filterIsInstance<FileImportOutcome.DuplicateDetected>().map {
                DuplicateFilePrompt(
                    displayName = it.displayName,
                    uriString = it.uriString,
                    existingFile = it.existingFile
                )
            }
            val failed = summary.outcomes.filterIsInstance<FileImportOutcome.Failed>()
            val importedCount = summary.importedCount

            _uiState.update { state ->
                state.copy(
                    isImportingFiles = false,
                    targetUploadPlaylistId = null,
                    duplicatePrompts = state.duplicatePrompts + duplicates
                )
            }

            when {
                failed.isNotEmpty() && importedCount > 0 ->
                    _events.tryEmit(
                        QuranUiEvent.ShowMessage("تم استيراد $importedCount ملف(ات)، وتعذر ${failed.size}: ${failed.first().reason}")
                    )
                failed.isNotEmpty() ->
                    _events.tryEmit(QuranUiEvent.ShowMessage(failed.first().reason))
                importedCount > 0 ->
                    _events.tryEmit(QuranUiEvent.ShowMessage("تم استيراد $importedCount ملف(ات) بنجاح وحفظها في المكتبة"))
            }
        }
    }

    fun confirmReplaceDuplicate() {
        val prompt = _uiState.value.duplicatePrompts.firstOrNull() ?: return
        val folderId = _uiState.value.targetUploadPlaylistId ?: _uiState.value.selectedPlaylistIdFilter
        _uiState.update { it.copy(duplicatePrompts = it.duplicatePrompts.drop(1), isImportingFiles = true) }
        viewModelScope.launch {
            val outcome = repository.replaceDuplicateUri(prompt.uriString, prompt.existingFile.id, folderId)
            _uiState.update { it.copy(isImportingFiles = false) }
            when (outcome) {
                is FileImportOutcome.Imported ->
                    _events.tryEmit(QuranUiEvent.ShowMessage("تم استبدال الملف «${prompt.displayName}» بنجاح"))
                is FileImportOutcome.Failed ->
                    _events.tryEmit(QuranUiEvent.ShowMessage(outcome.reason))
                is FileImportOutcome.DuplicateDetected -> Unit
            }
        }
    }

    fun dismissDuplicatePrompt() {
        _uiState.update { it.copy(duplicatePrompts = it.duplicatePrompts.drop(1)) }
    }

    fun deleteImportedFile(file: ImportedFileEntity) {
        if (playerController.state.value.currentTrack?.importedFileId == file.id) {
            playerController.pauseIfPlaying()
        }
        viewModelScope.launch {
            repository.deleteImportedFile(file)
            _events.tryEmit(QuranUiEvent.ShowMessage("تم حذف «${file.displayName}» من المكتبة"))
        }
    }

    // Audio Tracks, Playlists & Player Controls
    fun updateTrackDetails(
        trackId: String,
        surahNumber: Int?,
        reciterName: String,
        confirmed: Boolean,
        playlistIdOverride: String? = null
    ) {
        viewModelScope.launch {
            repository.updateAudioTrackDetails(trackId, surahNumber, reciterName, confirmed, playlistIdOverride)
            _events.tryEmit(QuranUiEvent.ShowMessage("تم تحديث بيانات المقطع الصوتي وتأكيدها"))
        }
    }

    fun assignTrackToPlaylist(trackId: String, playlistId: String?) {
        viewModelScope.launch {
            repository.assignTrackToPlaylist(trackId, playlistId)
            _events.tryEmit(QuranUiEvent.ShowMessage("تم نقل التلاوة إلى القسم المحدد"))
        }
    }

    fun createPlaylist(folderName: String, reciterName: String) {
        if (folderName.isBlank() && reciterName.isBlank()) return
        val resolvedFolder = folderName.trim().ifEmpty { reciterName.trim() }
        val resolvedReciter = reciterName.trim().ifEmpty { resolvedFolder }
        viewModelScope.launch {
            val created = repository.createPlaylist(resolvedFolder, resolvedReciter)
            _uiState.update { it.copy(selectedPlaylistIdFilter = created.id) }
            _events.tryEmit(QuranUiEvent.ShowMessage("تم إنشاء ملف «$resolvedFolder» للقارئ $resolvedReciter"))
        }
    }

    fun updatePlaylist(playlistId: String, folderName: String, reciterName: String) {
        viewModelScope.launch {
            repository.updatePlaylist(playlistId, folderName, reciterName)
            _events.tryEmit(QuranUiEvent.ShowMessage("تم تحديث اسم القسم والقارئ"))
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            if (_uiState.value.selectedPlaylistIdFilter == playlistId) {
                _uiState.update { it.copy(selectedPlaylistIdFilter = null) }
            }
            _events.tryEmit(QuranUiEvent.ShowMessage("تم حذف القسم دون حذف الملفات الصوتية"))
        }
    }

    fun selectPlaylistFilter(playlistId: String?) {
        _uiState.update { it.copy(selectedPlaylistIdFilter = playlistId) }
    }

    fun playTracks(tracks: List<QuranAudioTrackEntity>, startIndex: Int = 0) {
        if (tracks.isEmpty()) {
            _events.tryEmit(QuranUiEvent.ShowMessage("أضف ملفاً صوتياً أولاً من المكتبة"))
            return
        }
        FocusAudioService.stop(getApplication())
        playerController.playPlaylist(tracks, startIndex)
    }

    fun togglePlayPause() {
        if (!_uiState.value.playbackState.isPlaying) {
            FocusAudioService.stop(getApplication())
        }
        playerController.togglePlayPause()
    }

    fun pauseQuranAudio() = playerController.pauseIfPlaying()
    fun seekTo(positionMs: Long) = playerController.seekTo(positionMs)
    fun seekForward10s() = playerController.seekForward10s()
    fun seekBack10s() = playerController.seekBack10s()
    fun nextSurahAudio() = playerController.nextSurah()
    fun previousSurahAudio() = playerController.previousSurah()
    fun setPlaybackSpeed(speed: Float) = playerController.setPlaybackSpeed(speed)
    fun cycleRepeatMode() = playerController.cycleRepeatMode()
    fun toggleShuffle() = playerController.toggleShuffle()
    fun openFullPlayer() = _uiState.update { it.copy(isFullPlayerVisible = true) }
    fun closeFullPlayer() = _uiState.update { it.copy(isFullPlayerVisible = false) }

    fun updateGoals(dailyMinutes: Int, weeklyMinutes: Int) {
        viewModelScope.launch {
            repository.updateGoals(dailyMinutes, weeklyMinutes)
            _events.tryEmit(QuranUiEvent.ShowMessage("تم تحديث أهداف القراءة والاستماع"))
        }
    }

    override fun onCleared() {
        super.onCleared()
        sessionTickerJob?.cancel()
        playerController.release()
    }

    private data class DatabaseBundleA(
        val surahs: List<SurahEntity>,
        val files: List<ImportedFileEntity>,
        val tracks: List<QuranAudioTrackEntity>,
        val playlists: List<QuranPlaylistEntity>
    )

    private data class DatabaseBundleB(
        val bookmarks: List<BookmarkEntity>,
        val logs: List<QuranSessionLogEntity>,
        val profile: UserQuranProfileEntity
    )

    companion object {
        private const val PREFS_QURAN_SESSION = "quramer_session"
        private const val KEY_START = "start_ms"
        private const val KEY_PLANNED = "planned_ms"
        private const val KEY_ELAPSED = "elapsed_ms"
        private const val KEY_PAUSED = "paused"
        private const val KEY_LAST_RESUMED = "last_resumed"
        private const val KEY_MODE = "mode"
        private const val KEY_SURAHS = "surahs"
        private const val KEY_INDEX = "index"
        private const val KEY_FOCUS_LOCK = "focus_lock"
        private const val KEY_EXPIRED_SHOWN = "expired_shown"
    }
}
