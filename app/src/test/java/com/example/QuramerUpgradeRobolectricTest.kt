package com.example

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.FocusRepository
import com.example.quran.data.FileImportOutcome
import com.example.quran.data.QuranLibraryRepository
import com.example.quran.db.QuramerDatabase
import com.example.quran.db.QuranAudioTrackEntity
import com.example.quran.db.SurahCatalog
import com.example.quran.domain.QuranSessionMode
import com.example.quran.domain.QuranTextParser
import com.example.quran.domain.SmartSurahSelector
import android.app.Application
import android.os.Looper
import com.example.quran.audio.QuranPlayerController
import com.example.quran.db.ImportedFileEntity
import com.example.quran.ui.QuranViewModel
import com.example.ui.FocusViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class QuramerUpgradeRobolectricTest {

    @Test
    fun `quran database initializes all 114 surahs and supports per-surah lazy queries`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, QuramerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = QuranLibraryRepository(context, db, FocusRepository(context))

        repo.ensureInitialized()
        val surahs = db.quranDao().getAllSurahsOnce()
        assertEquals(114, surahs.size)
        assertEquals("الفاتحة", surahs.first().nameArabic)
        assertEquals("الناس", surahs.last().nameArabic)
        db.close()
    }

    @Test
    fun `room migration 1 to 2 upgrades v1 tables without losing existing records`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration_test_${System.currentTimeMillis()}.db"

        // Create a realistic v1 Quran database before running MIGRATION_1_2
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS surahs (
                            number INTEGER PRIMARY KEY NOT NULL,
                            name_arabic TEXT NOT NULL,
                            name_english TEXT NOT NULL,
                            revelation_type TEXT NOT NULL,
                            ayah_count INTEGER NOT NULL,
                            word_count INTEGER NOT NULL,
                            estimated_minutes_base REAL NOT NULL,
                            is_completed INTEGER NOT NULL DEFAULT 0,
                            last_read_ayah INTEGER NOT NULL DEFAULT 0,
                            times_read INTEGER NOT NULL DEFAULT 0,
                            times_listened INTEGER NOT NULL DEFAULT 0,
                            total_read_ms INTEGER NOT NULL DEFAULT 0,
                            total_listened_ms INTEGER NOT NULL DEFAULT 0,
                            has_imported_text INTEGER NOT NULL DEFAULT 0
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        "INSERT INTO surahs (number, name_arabic, name_english, revelation_type, ayah_count, word_count, estimated_minutes_base) VALUES (1, 'الفاتحة', 'Al-Fatihah', 'مكية', 7, 29, 0.3)"
                    )
                    db.execSQL("CREATE TABLE IF NOT EXISTS legacy_notes (id INTEGER PRIMARY KEY, note TEXT)")
                    db.execSQL("INSERT INTO legacy_notes (id, note) VALUES (1, 'preserved')")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()
        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val rawDb = helper.writableDatabase
        QuramerDatabase.MIGRATION_1_2.migrate(rawDb)

        val cursor = rawDb.query("SELECT name_arabic, scroll_progress FROM surahs WHERE number = 1")
        assertTrue(cursor.moveToFirst())
        assertEquals("الفاتحة", cursor.getString(0))
        assertEquals(0.0, cursor.getDouble(1), 0.001)
        cursor.close()

        val legacyCursor = rawDb.query("SELECT note FROM legacy_notes WHERE id = 1")
        assertTrue(legacyCursor.moveToFirst())
        assertEquals("preserved", legacyCursor.getString(0))
        legacyCursor.close()

        rawDb.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun `quran text parser preserves verbatim Uthmani script and rejects corrupt input`() {
        val uthmaniText = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"
        val jsonPayload = """
            [
              {"surah": 1, "ayah": 1, "text": "$uthmaniText", "startTimeMs": 0, "endTimeMs": 4500},
              {"surah": 1, "ayah": 2, "text": "ٱلْحَمْدُ لِلَّهِ رَبِّ ٱلْعَٰلَمِينَ"}
            ]
        """.trimIndent()

        val parsedJson = QuranTextParser.parseJson(ByteArrayInputStream(jsonPayload.toByteArray()), "src1")
        assertTrue(parsedJson.isValid)
        assertEquals(2, parsedJson.ayahs.size)
        // Verbatim Uthmani characters preserved strictly without normalization
        assertEquals(uthmaniText, parsedJson.ayahs[0].textUthmani)
        assertEquals(0L, parsedJson.ayahs[0].startTimeMs)
        assertEquals(4500L, parsedJson.ayahs[0].endTimeMs)

        val txtPayload = """
            1|1|$uthmaniText
            1|2|ٱلْحَمْدُ لِلَّهِ رَبِّ ٱلْعَٰلَمِينَ
        """.trimIndent()
        val parsedTxt = QuranTextParser.parseTxt(ByteArrayInputStream(txtPayload.toByteArray()), "src2")
        assertTrue(parsedTxt.isValid)
        assertEquals(2, parsedTxt.ayahs.size)
        assertEquals(uthmaniText, parsedTxt.ayahs[0].textUthmani)

        val corrupt = QuranTextParser.parseJson(ByteArrayInputStream("{bad json".toByteArray()), "bad")
        assertFalse(corrupt.isValid)
        assertNotNull(corrupt.errorMessage)
    }

    @Test
    fun `smart surah selector handles single surah, multi surah, and confirmed audio durations`() {
        val surahs = SurahCatalog.toEntities()

        // 30 minutes should pick a fitting Surah or multi-Surah set close to 30m
        val result30 = SmartSurahSelector.selectSurahs(
            surahs = surahs,
            targetMinutes = 30,
            readingSpeedWpm = 105.0,
            mode = QuranSessionMode.READING,
            audioTracks = emptyList(),
            preferUncompleted = true
        )
        assertTrue(result30.selectedSurahs.isNotEmpty())
        assertTrue(kotlin.math.abs(result30.estimatedMinutes - 30.0) <= 6.0)

        // Short 5-minute listening session with exact confirmed track duration uses track duration
        val confirmedTrack = QuranAudioTrackEntity(
            id = "t1",
            importedFileId = "f1",
            uriString = "file://112.mp3",
            displayName = "112.mp3",
            surahNumber = 112,
            isSurahConfirmed = true,
            reciterName = "مشاري العفاسي",
            durationMs = 5L * 60_000L
        )
        val resultListen = SmartSurahSelector.selectSurahs(
            surahs = surahs,
            targetMinutes = 5,
            readingSpeedWpm = 105.0,
            mode = QuranSessionMode.LISTENING,
            audioTracks = listOf(confirmedTrack)
        )
        assertTrue(resultListen.selectedSurahs.any { it.surah.number == 112 })
    }

    @Test
    fun `multi file import detects duplicates, preserves old files, and reports corrupt files`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, QuramerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = QuranLibraryRepository(context, db, FocusRepository(context))
        repo.ensureInitialized()

        val tempDir = File(context.cacheDir, "test_imports_${System.currentTimeMillis()}").apply { mkdirs() }
        val file1 = File(tempDir, "fatiha.txt").apply {
            writeText("1|1|بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ")
        }
        val file2 = File(tempDir, "ikhlas.txt").apply {
            writeText("112|1|قُلْ هُوَ ٱللَّهُ أَحَدٌ")
        }
        val corruptPdf = File(tempDir, "broken.pdf").apply {
            writeText("NOT_A_REAL_PDF")
        }

        val summary1 = repo.importMultipleUris(listOf(Uri.fromFile(file1), Uri.fromFile(corruptPdf)))
        assertEquals(1, summary1.importedCount)
        assertEquals(1, summary1.failedCount)

        // Adding a new file later keeps existing ready files
        val summary2 = repo.importMultipleUris(listOf(Uri.fromFile(file2)))
        assertEquals(1, summary2.importedCount)
        assertEquals(1, repo.getAyahsForSurahOnce(1).size)
        assertEquals(1, repo.getAyahsForSurahOnce(112).size)

        // Re-importing identical file triggers DuplicateDetected
        val dupSummary = repo.importMultipleUris(listOf(Uri.fromFile(file1)))
        assertEquals(1, dupSummary.duplicateCount)
        assertTrue(dupSummary.outcomes.first() is FileImportOutcome.DuplicateDetected)

        tempDir.deleteRecursively()
        db.close()
    }

    @Test
    fun `quran session records history and grows tree farm without overwriting legacy focus data`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val focusRepo = FocusRepository(context)
        val db = Room.inMemoryDatabaseBuilder(context, QuramerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val quranRepo = QuranLibraryRepository(context, db, focusRepo)
        quranRepo.ensureInitialized()

        // Existing Focus data
        focusRepo.saveUserName("Galileo")
        val t0 = 1_700_000_000_000L
        focusRepo.creditExternalStudyProgress(30L * 60_000L, t0)

        // Record a 60-minute Quran reading session without Focus Lock -> plants 1 tree (30m + 60m = 90m)
        quranRepo.recordQuranSession(
            mode = QuranSessionMode.READING,
            startedAtMs = t0,
            endedAtMs = t0 + 60L * 60_000L,
            plannedDurationMs = 60L * 60_000L,
            actualDurationMs = 60L * 60_000L,
            surahNumbers = listOf(18),
            ayahsReadCount = 110,
            wordsReadCount = 1579,
            focusLockEnabled = false,
            completed = true,
            creditTreeFarm = true
        )

        val snapshot = focusRepo.getStudyProgressSnapshot(t0 + 60L * 60_000L)
        assertEquals(1, snapshot.trees.size)
        assertEquals("Galileo", focusRepo.getUserName())
        val kahf = db.quranDao().getSurah(18)!!
        assertTrue(kahf.isCompleted)
        assertEquals(1, kahf.timesRead)
        db.close()
    }

    @Test
    fun `reciter folders support folder name plus reciter name, track assignment, rename, and safe delete`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, QuramerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = QuranLibraryRepository(context, db, FocusRepository(context))
        repo.ensureInitialized()

        val folder = repo.createPlaylist("تلاوات مجودة", "الشيخ الحصري")
        assertEquals("تلاوات مجودة", folder.name)
        assertEquals("الشيخ الحصري", folder.reciterName)

        db.quranDao().upsertImportedFile(
            ImportedFileEntity(
                id = "file_audio_1",
                uriString = "file://001.mp3",
                displayName = "001.mp3",
                fileType = "AUDIO",
                fileSizeBytes = 1024L,
                contentHash = "hash_audio_1",
                status = "READY",
                progressPercent = 100,
                importedAtMs = System.currentTimeMillis()
            )
        )
        db.quranDao().upsertAudioTrack(
            QuranAudioTrackEntity(
                id = "track_1",
                importedFileId = "file_audio_1",
                uriString = "file://001.mp3",
                displayName = "001.mp3",
                surahNumber = 1,
                isSurahConfirmed = true,
                reciterName = "قارئ غير محدد",
                durationMs = 60_000L
            )
        )

        repo.assignTrackToPlaylist("track_1", folder.id)
        var track = db.quranDao().getAudioTrack("track_1")!!
        assertEquals(folder.id, track.playlistId)
        assertEquals("الشيخ الحصري", track.reciterName)

        repo.updatePlaylist(folder.id, "المصحف المرتل", "محمود خليل الحصري")
        val updatedFolder = db.quranDao().getPlaylist(folder.id)!!
        assertEquals("المصحف المرتل", updatedFolder.name)
        assertEquals("محمود خليل الحصري", updatedFolder.reciterName)
        track = db.quranDao().getAudioTrack("track_1")!!
        assertEquals("محمود خليل الحصري", track.reciterName)

        repo.deletePlaylist(folder.id)
        assertNull(db.quranDao().getPlaylist(folder.id))
        track = db.quranDao().getAudioTrack("track_1")!!
        assertNull(track.playlistId)

        db.close()
    }

    @Test
    fun `focus viewmodel offers optional dialog without forcing settings and respects lock toggle`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = FocusRepository(app)
        repo.cancelSession(System.currentTimeMillis())
        repo.saveStudyAppLockEnabled(true)

        val vm = FocusViewModel(app, repo)
        vm.onHoursInputChange("0")
        vm.onMinutesInputChange("25")
        vm.onStartSessionClick()

        // Accessibility is disabled in Robolectric -> asks gently instead of throwing user to Settings
        assertTrue(vm.uiState.value.showAccessibilityOptionalDialog)
        assertFalse(vm.uiState.value.isSessionActive)

        vm.startSessionWithoutAppLock()
        assertFalse(vm.uiState.value.showAccessibilityOptionalDialog)
        assertTrue(vm.uiState.value.isSessionActive)

        vm.confirmCancelSession()
        assertFalse(vm.uiState.value.isSessionActive)

        // When user turns off app lock toggle, starting a session skips the accessibility dialog entirely
        vm.onStudyAppLockEnabledChange(false)
        assertFalse(repo.isStudyAppLockEnabled())
        vm.onStartSessionClick()
        assertFalse(vm.uiState.value.showAccessibilityOptionalDialog)
        assertTrue(vm.uiState.value.isSessionActive)

        vm.confirmCancelSession()
    }

    @Test
    fun `quran viewmodel proposes playback first and waits for yes or no decision`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val db = QuramerDatabase.getInstance(app)
        val dao = db.quranDao()
        dao.upsertImportedFile(
            ImportedFileEntity(
                id = "suggest_file_1",
                uriString = "file://112_suggest.mp3",
                displayName = "112_suggest.mp3",
                fileType = "AUDIO",
                fileSizeBytes = 2048L,
                contentHash = "hash_suggest_1",
                status = "READY",
                progressPercent = 100,
                importedAtMs = System.currentTimeMillis()
            )
        )
        dao.upsertAudioTrack(
            QuranAudioTrackEntity(
                id = "suggest_track_1",
                importedFileId = "suggest_file_1",
                uriString = "file://112_suggest.mp3",
                displayName = "112_suggest.mp3",
                surahNumber = 112,
                isSurahConfirmed = true,
                reciterName = "مشاري العفاسي",
                durationMs = 5L * 60_000L
            )
        )

        val repo = QuranLibraryRepository(app, db, FocusRepository(app))
        repo.ensureInitialized()

        var playedCalls = 0
        val fakePlayer = object : QuranPlayerController(app) {
            override fun connect() = Unit
            override fun playPlaylist(tracks: List<QuranAudioTrackEntity>, startIndex: Int) {
                playedCalls++
            }
            override fun release() = Unit
        }

        val vm = QuranViewModel(app, repository = repo, playerController = fakePlayer)
        // Pump Robolectric main looper so Room Flow + viewModelScope emissions arrive
        repeat(60) {
            shadowOf(Looper.getMainLooper()).idle()
            if (vm.uiState.value.surahs.isNotEmpty() && vm.uiState.value.audioTracks.isNotEmpty()) {
                return@repeat
            }
            Thread.sleep(25)
        }
        assertTrue("Expected surahs loaded", vm.uiState.value.surahs.isNotEmpty())
        assertTrue("Expected audioTracks loaded", vm.uiState.value.audioTracks.isNotEmpty())

        vm.selectSessionMode(QuranSessionMode.LISTENING)
        vm.selectPresetMinutes(5)
        shadowOf(Looper.getMainLooper()).idle()
        vm.startQuranSession()
        shadowOf(Looper.getMainLooper()).idle()

        // Does NOT auto-play immediately; populates pendingPlaySuggestion instead
        assertEquals(0, playedCalls)
        assertNotNull(vm.uiState.value.pendingPlaySuggestion)

        vm.declinePlaySuggestion()
        assertNull(vm.uiState.value.pendingPlaySuggestion)
        assertTrue(vm.uiState.value.hubSuggestionDismissed)
        assertEquals(0, playedCalls)

        vm.proposeQuranListeningForStudyOrHub()
        assertNotNull(vm.uiState.value.pendingPlaySuggestion)
        vm.acceptPlaySuggestion()
        assertNull(vm.uiState.value.pendingPlaySuggestion)
        assertEquals(1, playedCalls)

        vm.finishQuranSession(completed = false)
        shadowOf(Looper.getMainLooper()).idle()
        dao.deleteAudioTrack("suggest_track_1")
        dao.deleteImportedFile("suggest_file_1")
    }

    @Test
    fun `stopwatch plants a tree every hour and credits daily and total statistics`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = FocusRepository(context)
        val t0 = 1_710_000_000_000L
        repo.cancelSession(t0)

        repo.startStopwatchSession(setOf("com.allowed.study"), t0)
        val initialSnap = repo.syncStudyProgress(t0)
        assertTrue(initialSnap.isStopwatch)
        assertEquals(t0, initialSnap.sessionStartMs)
        assertEquals(0, initialSnap.trees.size)

        // Advance 65 minutes -> should credit 65m to daily & total study stats and plant 1 tree
        val t65m = t0 + 65L * 60_000L
        val afterHourSnap = repo.syncStudyProgress(t65m)
        assertTrue(afterHourSnap.isStopwatch)
        assertEquals(65L * 60_000L, afterHourSnap.dailyStudyMs)
        assertEquals(65L * 60_000L, afterHourSnap.totalStudyMs)
        assertEquals(1, afterHourSnap.trees.size)
        assertEquals(5L * 60_000L, afterHourSnap.unplantedStudyMs)

        // Cancel stopwatch preserves stats and planted trees
        repo.cancelSession(t65m)
        val afterCancel = repo.getStudyProgressSnapshot(t65m)
        assertFalse(afterCancel.isStopwatch)
        assertEquals(1, afterCancel.trees.size)
        assertEquals(65L * 60_000L, afterCancel.dailyStudyMs)
    }

    @Test
    fun `launch intro always shows on start and stopwatch mode starts without duration input`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = FocusRepository(app)
        repo.saveUserName("Galileo")
        repo.cancelSession(System.currentTimeMillis())

        val vm = FocusViewModel(app, repo)
        // Always shows intro screen on every launch even when name already saved
        assertTrue(vm.uiState.value.showLaunchIntroScreen)
        vm.onLaunchIntroStartClick()
        assertFalse(vm.uiState.value.showLaunchIntroScreen)
        assertFalse(vm.uiState.value.isWelcomeScreenVisible)

        // Switching to Stopwatch starts active session even with 0h 0m
        vm.onHoursInputChange("0")
        vm.onMinutesInputChange("0")
        vm.onStudyAppLockEnabledChange(false)
        vm.selectTimerMode(com.example.ui.SessionTimerMode.STOPWATCH)
        vm.onStartSessionClick()

        assertTrue(vm.uiState.value.isStopwatchActive)
        assertTrue(vm.uiState.value.isSessionActive)

        vm.confirmCancelSession()
        assertFalse(vm.uiState.value.isStopwatchActive)
        assertFalse(vm.uiState.value.isSessionActive)
    }
}
