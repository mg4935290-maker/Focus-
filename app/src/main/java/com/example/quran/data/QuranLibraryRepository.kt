package com.example.quran.data

import android.content.Context
import android.content.Intent
import android.graphics.pdf.PdfRenderer
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.core.net.toUri
import com.example.data.FocusRepository
import com.example.quran.db.AyahEntity
import com.example.quran.db.BookmarkEntity
import com.example.quran.db.ImportedFileEntity
import com.example.quran.db.QuramerDatabase
import com.example.quran.db.QuranAudioTrackEntity
import com.example.quran.db.QuranPlaylistEntity
import com.example.quran.db.QuranSessionLogEntity
import com.example.quran.db.SurahCatalog
import com.example.quran.db.SurahEntity
import com.example.quran.db.UserQuranProfileEntity
import com.example.quran.domain.QuranSessionMode
import com.example.quran.domain.QuranTextParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

sealed class FileImportOutcome {
    data class Imported(val file: ImportedFileEntity) : FileImportOutcome()
    data class DuplicateDetected(
        val displayName: String,
        val uriString: String,
        val existingFile: ImportedFileEntity
    ) : FileImportOutcome()
    data class Failed(val displayName: String, val reason: String) : FileImportOutcome()
}

data class MultiImportSummary(
    val outcomes: List<FileImportOutcome>
) {
    val importedCount: Int get() = outcomes.count { it is FileImportOutcome.Imported }
    val duplicateCount: Int get() = outcomes.count { it is FileImportOutcome.DuplicateDetected }
    val failedCount: Int get() = outcomes.count { it is FileImportOutcome.Failed }
}

class QuranLibraryRepository(
    private val context: Context,
    private val database: QuramerDatabase = QuramerDatabase.getInstance(context),
    private val focusRepository: FocusRepository = FocusRepository(context)
) {
    private val dao = database.quranDao()

    val surahsFlow: Flow<List<SurahEntity>> = dao.observeAllSurahs()
    val importedFilesFlow: Flow<List<ImportedFileEntity>> = dao.observeImportedFiles()
    val audioTracksFlow: Flow<List<QuranAudioTrackEntity>> = dao.observeAudioTracks()
    val playlistsFlow: Flow<List<QuranPlaylistEntity>> = dao.observePlaylists()
    val bookmarksFlow: Flow<List<BookmarkEntity>> = dao.observeBookmarks()
    val sessionLogsFlow: Flow<List<QuranSessionLogEntity>> = dao.observeSessionLogs()
    val profileFlow: Flow<UserQuranProfileEntity> = dao.observeProfile().map {
        it ?: UserQuranProfileEntity()
    }

    suspend fun ensureInitialized() = withContext(Dispatchers.IO) {
        if (dao.getSurahCount() < 114) {
            dao.insertSurahsIfAbsent(SurahCatalog.toEntities())
        }
        if (dao.getProfileOnce() == null) {
            dao.upsertProfile(UserQuranProfileEntity())
        }
    }

    fun observeAyahsForSurah(surahNumber: Int): Flow<List<AyahEntity>> =
        dao.observeAyahsForSurah(surahNumber)

    suspend fun getAyahsForSurahOnce(surahNumber: Int): List<AyahEntity> =
        dao.getAyahsForSurahOnce(surahNumber)

    suspend fun hasTimingDataForSurah(surahNumber: Int): Boolean =
        dao.getTimedAyahCountForSurah(surahNumber) > 0

    suspend fun importMultipleUris(
        uris: List<Uri>,
        replaceExistingId: String? = null,
        targetPlaylistId: String? = null
    ): MultiImportSummary = withContext(Dispatchers.IO) {
        ensureInitialized()
        val outcomes = mutableListOf<FileImportOutcome>()
        for (uri in uris) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            outcomes.add(importSingleUriInternal(uri, replaceExistingId, targetPlaylistId))
        }
        MultiImportSummary(outcomes)
    }

    suspend fun replaceDuplicateUri(
        uriString: String,
        existingFileId: String,
        targetPlaylistId: String? = null
    ): FileImportOutcome =
        withContext(Dispatchers.IO) {
            val uri = uriString.toUri()
            importSingleUriInternal(uri, replaceExistingId = existingFileId, targetPlaylistId = targetPlaylistId)
        }

    private suspend fun importSingleUriInternal(
        uri: Uri,
        replaceExistingId: String?,
        targetPlaylistId: String? = null
    ): FileImportOutcome {
        val (displayName, reportedSize) = queryDisplayNameAndSize(uri)
        val mime = context.contentResolver.getType(uri).orEmpty().lowercase()
        val fileType = detectSupportedFileType(displayName, mime)
            ?: return FileImportOutcome.Failed(
                displayName,
                "نوع الملف غير مدعوم. الأنواع المدعومة: PDF, TXT, JSON, MP3, M4A, AAC, WAV"
            )

        val fileId = replaceExistingId ?: UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val initialRecord = ImportedFileEntity(
            id = fileId,
            uriString = uri.toString(),
            displayName = displayName,
            contentHash = "",
            fileType = fileType,
            fileSizeBytes = reportedSize,
            status = "PROCESSING",
            progressPercent = 10,
            importedAtMs = now
        )
        dao.upsertImportedFile(initialRecord)

        return try {
            val hash = computeContentHash(uri)
            if (replaceExistingId == null) {
                val duplicate = dao.findFileByHash(hash)
                if (duplicate != null) {
                    dao.deleteImportedFile(fileId)
                    return FileImportOutcome.DuplicateDetected(
                        displayName = displayName,
                        uriString = uri.toString(),
                        existingFile = duplicate
                    )
                }
            }

            dao.updateImportProgress(fileId, "PROCESSING", 35)

            when (fileType) {
                "JSON" -> {
                    val parsed = context.contentResolver.openInputStream(uri)?.use { stream ->
                        QuranTextParser.parseJson(stream, fileId)
                    } ?: return failImport(fileId, displayName, "تعذر فتح الملف للقراءة.")

                    if (!parsed.isValid) {
                        return failImport(fileId, displayName, parsed.errorMessage ?: "ملف JSON غير صالح.")
                    }
                    dao.updateImportProgress(fileId, "PROCESSING", 75)
                    dao.replaceAyahsForSource(fileId, parsed.ayahs)
                    val ready = initialRecord.copy(
                        contentHash = hash,
                        status = "READY",
                        progressPercent = 100,
                        surahCountImported = parsed.surahCount,
                        ayahCountImported = parsed.ayahs.size
                    )
                    dao.upsertImportedFile(ready)
                    FileImportOutcome.Imported(ready)
                }
                "TXT" -> {
                    val parsed = context.contentResolver.openInputStream(uri)?.use { stream ->
                        QuranTextParser.parseTxt(stream, fileId)
                    } ?: return failImport(fileId, displayName, "تعذر فتح الملف النصي للقراءة.")

                    if (!parsed.isValid) {
                        return failImport(fileId, displayName, parsed.errorMessage ?: "ملف نصي غير صالح.")
                    }
                    dao.updateImportProgress(fileId, "PROCESSING", 75)
                    dao.replaceAyahsForSource(fileId, parsed.ayahs)
                    val ready = initialRecord.copy(
                        contentHash = hash,
                        status = "READY",
                        progressPercent = 100,
                        surahCountImported = parsed.surahCount,
                        ayahCountImported = parsed.ayahs.size
                    )
                    dao.upsertImportedFile(ready)
                    FileImportOutcome.Imported(ready)
                }
                "PDF" -> {
                    val pdfDir = File(context.filesDir, "quran_pdfs").apply { mkdirs() }
                    val localPdf = File(pdfDir, "$fileId.pdf")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(localPdf).use { output ->
                            input.copyTo(output, bufferSize = 16 * 1024)
                        }
                    } ?: return failImport(fileId, displayName, "تعذر نسخ ملف PDF.")

                    dao.updateImportProgress(fileId, "PROCESSING", 70)
                    val pageCount = validatePdfAndCountPages(localPdf)
                    if (pageCount <= 0) {
                        localPdf.delete()
                        return failImport(fileId, displayName, "ملف PDF تالف أو لا يحتوي على صفحات صالحة.")
                    }

                    // Per Quran accuracy policy: PDF is stored for direct Mushaf page viewing;
                    // we do NOT auto-commit unverified text extraction into the ayah database.
                    val ready = initialRecord.copy(
                        localCopyPath = localPdf.absolutePath,
                        contentHash = hash,
                        status = "READY",
                        progressPercent = 100,
                        surahCountImported = 0,
                        ayahCountImported = pageCount
                    )
                    dao.upsertImportedFile(ready)
                    FileImportOutcome.Imported(ready)
                }
                else -> { // AUDIO
                    dao.updateImportProgress(fileId, "PROCESSING", 60)
                    val meta = extractAudioMetadata(uri, displayName)
                    val trackId = if (replaceExistingId != null) {
                        dao.deleteAudioTracksByFile(fileId)
                        UUID.randomUUID().toString()
                    } else {
                        UUID.randomUUID().toString()
                    }
                    val targetPlaylist = targetPlaylistId?.let { dao.getPlaylist(it) }
                    val resolvedReciter = targetPlaylist?.reciterName?.takeIf { it.isNotBlank() } ?: meta.reciterName
                    val playlistId = targetPlaylist?.id ?: ensureReciterPlaylist(resolvedReciter)
                    val track = QuranAudioTrackEntity(
                        id = trackId,
                        importedFileId = fileId,
                        uriString = uri.toString(),
                        displayName = displayName,
                        // Never lock a filename guess as verified; user confirms or assigns manually
                        surahNumber = meta.suggestedSurahNumber,
                        isSurahConfirmed = false,
                        reciterName = resolvedReciter,
                        durationMs = meta.durationMs,
                        bitrateKbps = meta.bitrateKbps,
                        qualityLabel = meta.qualityLabel,
                        playlistId = playlistId,
                        createdAtMs = now
                    )
                    dao.upsertAudioTrack(track)
                    val ready = initialRecord.copy(
                        contentHash = hash,
                        status = "READY",
                        progressPercent = 100,
                        surahCountImported = if (meta.suggestedSurahNumber != null) 1 else 0
                    )
                    dao.upsertImportedFile(ready)
                    FileImportOutcome.Imported(ready)
                }
            }
        } catch (t: Throwable) {
            failImport(fileId, displayName, t.localizedMessage ?: "حدث خطأ أثناء معالجة الملف.")
        }
    }

    private suspend fun failImport(fileId: String, displayName: String, reason: String): FileImportOutcome.Failed {
        dao.updateImportProgress(
            id = fileId,
            status = "FAILED",
            progress = 0,
            errorMessage = reason
        )
        return FileImportOutcome.Failed(displayName, reason)
    }

    suspend fun deleteImportedFile(file: ImportedFileEntity) = withContext(Dispatchers.IO) {
        if (!file.localCopyPath.isNullOrBlank()) {
            runCatching { File(file.localCopyPath).delete() }
        }
        dao.deleteAyahsBySourceFile(file.id)
        dao.refreshSurahTextAvailability()
        dao.deleteAudioTracksByFile(file.id)
        dao.deleteImportedFile(file.id)
    }

    suspend fun updateAudioTrackDetails(
        trackId: String,
        surahNumber: Int?,
        reciterName: String,
        confirmed: Boolean,
        playlistIdOverride: String? = null
    ) = withContext(Dispatchers.IO) {
        val cleanReciter = reciterName.trim().ifEmpty { "قارئ غير محدد" }
        val playlistId = playlistIdOverride ?: ensureReciterPlaylist(cleanReciter)
        dao.updateTrackAssignment(
            trackId = trackId,
            surahNumber = surahNumber,
            confirmed = confirmed && surahNumber != null,
            reciterName = cleanReciter,
            playlistId = playlistId
        )
    }

    suspend fun createPlaylist(name: String, reciterName: String): QuranPlaylistEntity =
        withContext(Dispatchers.IO) {
            val cleanName = name.trim().ifEmpty { reciterName.trim() }
            val cleanReciter = reciterName.trim().ifEmpty { cleanName }
            val playlist = QuranPlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = cleanName,
                reciterName = cleanReciter,
                createdAtMs = System.currentTimeMillis()
            )
            dao.upsertPlaylist(playlist)
            playlist
        }

    suspend fun updatePlaylist(playlistId: String, name: String, reciterName: String) =
        withContext(Dispatchers.IO) {
            val existing = dao.getPlaylist(playlistId) ?: return@withContext
            val cleanName = name.trim().ifEmpty { existing.name }
            val cleanReciter = reciterName.trim().ifEmpty { cleanName }
            dao.upsertPlaylist(
                existing.copy(
                    name = cleanName,
                    reciterName = cleanReciter
                )
            )
            dao.updateTracksReciterByPlaylist(playlistId, cleanReciter)
        }

    suspend fun deletePlaylist(playlistId: String) = withContext(Dispatchers.IO) {
        dao.clearTrackPlaylistReferences(playlistId)
        dao.deletePlaylist(playlistId)
    }

    suspend fun assignTrackToPlaylist(trackId: String, playlistId: String?) = withContext(Dispatchers.IO) {
        val track = dao.getAudioTrack(trackId) ?: return@withContext
        val folder = playlistId?.let { dao.getPlaylist(it) }
        val nextReciter = folder?.reciterName?.takeIf { it.isNotBlank() } ?: track.reciterName
        dao.updateTrackAssignment(
            trackId = trackId,
            surahNumber = track.surahNumber,
            confirmed = track.isSurahConfirmed,
            reciterName = nextReciter,
            playlistId = folder?.id
        )
    }

    suspend fun deleteAudioTrack(trackId: String) = withContext(Dispatchers.IO) {
        dao.deleteAudioTrack(trackId)
    }

    private suspend fun ensureReciterPlaylist(reciterName: String): String {
        val clean = reciterName.trim().ifEmpty { "تلاوات عامة" }
        val existingId = "reciter_${clean.lowercase().hashCode()}"
        dao.upsertPlaylist(
            QuranPlaylistEntity(
                id = existingId,
                name = clean,
                reciterName = clean,
                createdAtMs = System.currentTimeMillis()
            )
        )
        return existingId
    }

    suspend fun saveReadingPosition(surahNumber: Int, ayahNumber: Int, scrollProgress: Float) =
        withContext(Dispatchers.IO) {
            ensureInitialized()
            dao.updateSurahReadingPosition(surahNumber, ayahNumber, scrollProgress.coerceIn(0f, 1f))
            val profile = dao.getProfileOnce() ?: UserQuranProfileEntity()
            dao.upsertProfile(
                profile.copy(
                    lastViewedSurah = surahNumber,
                    lastViewedAyah = ayahNumber
                )
            )
        }

    suspend fun toggleBookmark(surahNumber: Int, ayahNumber: Int, currentlyBookmarked: Boolean) =
        withContext(Dispatchers.IO) {
            if (currentlyBookmarked) {
                dao.deleteBookmarkAt(surahNumber, ayahNumber)
            } else {
                dao.upsertBookmark(
                    BookmarkEntity(
                        id = "${surahNumber}_${ayahNumber}",
                        surahNumber = surahNumber,
                        ayahNumber = ayahNumber,
                        createdAtMs = System.currentTimeMillis()
                    )
                )
            }
        }

    suspend fun toggleSurahCompleted(surahNumber: Int, completed: Boolean) =
        withContext(Dispatchers.IO) {
            dao.setSurahCompleted(surahNumber, completed)
            if (completed) {
                val profile = dao.getProfileOnce() ?: UserQuranProfileEntity()
                val updatedBadges = unlockBadge(profile.badgesJson, "FIRST_SURAH_COMPLETED")
                dao.upsertProfile(
                    profile.copy(
                        points = profile.points + 50,
                        stars = profile.stars + 1,
                        badgesJson = updatedBadges
                    )
                )
            }
        }

    suspend fun recordQuranSession(
        mode: QuranSessionMode,
        startedAtMs: Long,
        endedAtMs: Long,
        plannedDurationMs: Long,
        actualDurationMs: Long,
        surahNumbers: List<Int>,
        ayahsReadCount: Int,
        wordsReadCount: Int,
        focusLockEnabled: Boolean,
        completed: Boolean,
        creditTreeFarm: Boolean = !focusLockEnabled
    ) = withContext(Dispatchers.IO) {
        ensureInitialized()
        if (actualDurationMs < 5_000L && !completed) return@withContext

        val log = QuranSessionLogEntity(
            id = UUID.randomUUID().toString(),
            mode = mode.name,
            startedAtMs = startedAtMs,
            endedAtMs = endedAtMs,
            plannedDurationMs = plannedDurationMs,
            actualDurationMs = actualDurationMs,
            surahNumbersCsv = surahNumbers.joinToString(","),
            ayahsReadCount = ayahsReadCount,
            focusLockEnabled = focusLockEnabled,
            completed = completed
        )
        dao.insertSessionLog(log)

        if (surahNumbers.isNotEmpty()) {
            val perSurahMs = (actualDurationMs / surahNumbers.size).coerceAtLeast(0L)
            surahNumbers.forEach { num ->
                when (mode) {
                    QuranSessionMode.READING -> dao.recordSurahReadStats(num, perSurahMs)
                    QuranSessionMode.LISTENING -> dao.recordSurahListenStats(num, perSurahMs)
                    QuranSessionMode.COMBINED -> {
                        dao.recordSurahReadStats(num, perSurahMs)
                        dao.recordSurahListenStats(num, perSurahMs)
                    }
                }
                if (completed) {
                    dao.setSurahCompleted(num, true)
                }
            }
        }

        val profile = dao.getProfileOnce() ?: UserQuranProfileEntity()
        val todayKey = currentDateKey(endedAtMs)
        val yesterdayKey = currentDateKey(endedAtMs - 24L * 3_600_000L)

        var newReadStreak = profile.readingStreakDays
        var newLastReadDate = profile.lastReadDateKey
        if (mode == QuranSessionMode.READING || mode == QuranSessionMode.COMBINED) {
            newReadStreak = when (profile.lastReadDateKey) {
                todayKey -> profile.readingStreakDays.coerceAtLeast(1)
                yesterdayKey -> profile.readingStreakDays + 1
                else -> 1
            }
            newLastReadDate = todayKey
        }

        var newListenStreak = profile.listeningStreakDays
        var newLastListenDate = profile.lastListenDateKey
        if (mode == QuranSessionMode.LISTENING || mode == QuranSessionMode.COMBINED) {
            newListenStreak = when (profile.lastListenDateKey) {
                todayKey -> profile.listeningStreakDays.coerceAtLeast(1)
                yesterdayKey -> profile.listeningStreakDays + 1
                else -> 1
            }
            newLastListenDate = todayKey
        }

        // Adapt user's personal WPM when real reading word counts exist
        var updatedWpm = profile.readingSpeedWpm
        if (wordsReadCount >= 30 && actualDurationMs >= 20_000L) {
            val observedWpm = (wordsReadCount / (actualDurationMs / 60_000.0)).coerceIn(55.0, 210.0)
            updatedWpm = (profile.readingSpeedWpm * 0.75) + (observedWpm * 0.25)
        }

        val minutesEarned = (actualDurationMs / 60_000L).toInt().coerceAtLeast(1)
        val earnedPoints = minutesEarned * 10 + (if (completed) 25 else 0)
        val earnedStars = if (completed) 1 else 0

        var badges = unlockBadge(profile.badgesJson, "FIRST_QURAN_SESSION")
        if (newReadStreak >= 3 || newListenStreak >= 3) {
            badges = unlockBadge(badges, "STREAK_3_DAYS")
        }
        if (newReadStreak >= 7 || newListenStreak >= 7) {
            badges = unlockBadge(badges, "STREAK_7_DAYS")
        }
        if (completed && surahNumbers.isNotEmpty()) {
            badges = unlockBadge(badges, "FIRST_SURAH_COMPLETED")
        }

        dao.upsertProfile(
            profile.copy(
                readingSpeedWpm = updatedWpm,
                points = profile.points + earnedPoints,
                stars = profile.stars + earnedStars,
                readingStreakDays = newReadStreak,
                listeningStreakDays = newListenStreak,
                lastReadDateKey = newLastReadDate,
                lastListenDateKey = newLastListenDate,
                badgesJson = badges
            )
        )

        // Bridge Quran-only session time into Tree Farm + Study Stats so Tree Farm grows with Quran & Study
        if (creditTreeFarm) {
            focusRepository.creditExternalStudyProgress(actualDurationMs, endedAtMs)
        }
    }

    suspend fun updateGoals(dailyMinutes: Int, weeklyMinutes: Int) = withContext(Dispatchers.IO) {
        val profile = dao.getProfileOnce() ?: UserQuranProfileEntity()
        dao.upsertProfile(
            profile.copy(
                dailyGoalMinutes = dailyMinutes.coerceIn(5, 300),
                weeklyGoalMinutes = weeklyMinutes.coerceIn(15, 2000)
            )
        )
    }

    private fun unlockBadge(currentJson: String, badgeId: String): String {
        val arr = runCatching { JSONArray(currentJson) }.getOrDefault(JSONArray())
        for (i in 0 until arr.length()) {
            if (arr.optString(i) == badgeId) return arr.toString()
        }
        arr.put(badgeId)
        return arr.toString()
    }

    private fun currentDateKey(ms: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(ms))

    private fun queryDisplayNameAndSize(uri: Uri): Pair<String, Long> {
        var name: String? = null
        var size = 0L
        runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0) name = cursor.getString(nameIdx)
                    if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
                }
            }
        }
        val cleanName = name?.takeIf { it.isNotBlank() }
            ?: uri.lastPathSegment
            ?: "ملف_قرآن"
        return cleanName to size
    }

    private fun detectSupportedFileType(displayName: String, mime: String): String? {
        val lowerName = displayName.lowercase()
        return when {
            lowerName.endsWith(".json") || mime.contains("json") -> "JSON"
            lowerName.endsWith(".txt") || mime.startsWith("text/") -> "TXT"
            lowerName.endsWith(".pdf") || mime == "application/pdf" -> "PDF"
            lowerName.endsWith(".mp3") || lowerName.endsWith(".m4a") ||
                lowerName.endsWith(".aac") || lowerName.endsWith(".wav") ||
                mime.startsWith("audio/") -> "AUDIO"
            else -> null
        }
    }

    private fun computeContentHash(uri: Uri): String {
        val digest = MessageDigest.getInstance("SHA-256")
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val buf = ByteArray(16 * 1024)
            var read = stream.read(buf)
            while (read > 0) {
                digest.update(buf, 0, read)
                read = stream.read(buf)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun validatePdfAndCountPages(file: File): Int {
        return runCatching {
            val header = ByteArray(5)
            file.inputStream().use { it.read(header) }
            if (!String(header, Charsets.US_ASCII).startsWith("%PDF-")) {
                return 0
            }
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    renderer.pageCount
                }
            }
        }.getOrDefault(0)
    }

    private data class AudioExtractedMeta(
        val durationMs: Long,
        val bitrateKbps: Int,
        val qualityLabel: String,
        val reciterName: String,
        val suggestedSurahNumber: Int?
    )

    private fun extractAudioMetadata(uri: Uri, displayName: String): AudioExtractedMeta {
        val mmr = MediaMetadataRetriever()
        return try {
            mmr.setDataSource(context, uri)
            val durationMs = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
            val bitrateBps = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                ?.toIntOrNull()?.coerceAtLeast(0) ?: 0
            val bitrateKbps = bitrateBps / 1000
            val qualityLabel = when {
                bitrateKbps >= 256 -> "جودة عالية (${bitrateKbps} kbps)"
                bitrateKbps >= 128 -> "جودة متوسطة (${bitrateKbps} kbps)"
                bitrateKbps > 0 -> "${bitrateKbps} kbps"
                else -> "ملف صوتي"
            }
            val artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.trim()?.takeIf { it.isNotBlank() }
                ?: mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                    ?.trim()?.takeIf { it.isNotBlank() }
                ?: "قارئ غير محدد"

            // Tentative suggestion only (marked unconfirmed so user must verify/adjust)
            val numMatch = Regex("""(?:^|\D)(0?\d{1,3})(?:\D|$)""").find(displayName)
            val guessedNum = numMatch?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it in 1..114 }

            AudioExtractedMeta(
                durationMs = durationMs,
                bitrateKbps = bitrateKbps,
                qualityLabel = qualityLabel,
                reciterName = artist,
                suggestedSurahNumber = guessedNum
            )
        } catch (_: Throwable) {
            AudioExtractedMeta(0L, 0, "ملف صوتي", "قارئ غير محدد", null)
        } finally {
            runCatching { mmr.release() }
        }
    }
}
