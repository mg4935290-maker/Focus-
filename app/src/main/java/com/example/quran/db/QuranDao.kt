package com.example.quran.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface QuranDao {

    // Surahs
    @Query("SELECT * FROM surahs ORDER BY number ASC")
    fun observeAllSurahs(): Flow<List<SurahEntity>>

    @Query("SELECT * FROM surahs ORDER BY number ASC")
    suspend fun getAllSurahsOnce(): List<SurahEntity>

    @Query("SELECT * FROM surahs WHERE number = :number LIMIT 1")
    suspend fun getSurah(number: Int): SurahEntity?

    @Query("SELECT COUNT(*) FROM surahs")
    suspend fun getSurahCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSurahsIfAbsent(surahs: List<SurahEntity>)

    @Query("UPDATE surahs SET last_read_ayah = :ayah, scroll_progress = :progress, has_imported_text = has_imported_text WHERE number = :surahNumber")
    suspend fun updateSurahReadingPosition(surahNumber: Int, ayah: Int, progress: Float)

    @Query("UPDATE surahs SET has_imported_text = 1 WHERE number IN (:surahNumbers)")
    suspend fun markSurahsHaveText(surahNumbers: List<Int>)

    @Query("UPDATE surahs SET has_imported_text = 0 WHERE number NOT IN (SELECT DISTINCT surah_number FROM ayahs)")
    suspend fun refreshSurahTextAvailability()

    @Query("UPDATE surahs SET is_completed = :completed WHERE number = :surahNumber")
    suspend fun setSurahCompleted(surahNumber: Int, completed: Boolean)

    @Query("UPDATE surahs SET times_read = times_read + 1, total_read_ms = total_read_ms + :durationMs WHERE number = :surahNumber")
    suspend fun recordSurahReadStats(surahNumber: Int, durationMs: Long)

    @Query("UPDATE surahs SET times_listened = times_listened + 1, total_listened_ms = total_listened_ms + :durationMs WHERE number = :surahNumber")
    suspend fun recordSurahListenStats(surahNumber: Int, durationMs: Long)

    // Ayahs (queried strictly per-Surah so full Quran is never loaded into memory at once)
    @Query("SELECT * FROM ayahs WHERE surah_number = :surahNumber ORDER BY ayah_number ASC")
    fun observeAyahsForSurah(surahNumber: Int): Flow<List<AyahEntity>>

    @Query("SELECT * FROM ayahs WHERE surah_number = :surahNumber ORDER BY ayah_number ASC")
    suspend fun getAyahsForSurahOnce(surahNumber: Int): List<AyahEntity>

    @Query("SELECT COUNT(*) FROM ayahs WHERE surah_number = :surahNumber")
    suspend fun getAyahCountForSurah(surahNumber: Int): Int

    @Query("SELECT COUNT(*) FROM ayahs WHERE surah_number = :surahNumber AND start_time_ms IS NOT NULL")
    suspend fun getTimedAyahCountForSurah(surahNumber: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAyahs(ayahs: List<AyahEntity>)

    @Query("DELETE FROM ayahs WHERE source_file_id = :sourceFileId")
    suspend fun deleteAyahsBySourceFile(sourceFileId: String)

    @Transaction
    suspend fun replaceAyahsForSource(sourceFileId: String, ayahs: List<AyahEntity>) {
        deleteAyahsBySourceFile(sourceFileId)
        // Batch inserts to keep SQLite bind variable limits safe for large Mushaf imports
        ayahs.chunked(400).forEach { chunk ->
            insertAyahs(chunk)
        }
        val affectedSurahs = ayahs.map { it.surahNumber }.distinct()
        if (affectedSurahs.isNotEmpty()) {
            markSurahsHaveText(affectedSurahs)
        }
        refreshSurahTextAvailability()
    }

    // Imported Files
    @Query("SELECT * FROM imported_files ORDER BY imported_at_ms DESC")
    fun observeImportedFiles(): Flow<List<ImportedFileEntity>>

    @Query("SELECT * FROM imported_files WHERE content_hash = :hash LIMIT 1")
    suspend fun findFileByHash(hash: String): ImportedFileEntity?

    @Query("SELECT * FROM imported_files WHERE id = :id LIMIT 1")
    suspend fun getImportedFile(id: String): ImportedFileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertImportedFile(file: ImportedFileEntity)

    @Query("UPDATE imported_files SET status = :status, progress_percent = :progress, error_message = :errorMessage, surah_count_imported = :surahs, ayah_count_imported = :ayahs WHERE id = :id")
    suspend fun updateImportProgress(
        id: String,
        status: String,
        progress: Int,
        errorMessage: String? = null,
        surahs: Int = 0,
        ayahs: Int = 0
    )

    @Query("DELETE FROM imported_files WHERE id = :id")
    suspend fun deleteImportedFile(id: String)

    // Audio Tracks & Playlists
    @Query("SELECT * FROM quran_audio_tracks ORDER BY COALESCE(surah_number, 999) ASC, created_at_ms DESC")
    fun observeAudioTracks(): Flow<List<QuranAudioTrackEntity>>

    @Query("SELECT * FROM quran_audio_tracks WHERE id = :id LIMIT 1")
    suspend fun getAudioTrack(id: String): QuranAudioTrackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAudioTrack(track: QuranAudioTrackEntity)

    @Query("UPDATE quran_audio_tracks SET surah_number = :surahNumber, is_surah_confirmed = :confirmed, reciter_name = :reciterName, playlist_id = :playlistId WHERE id = :trackId")
    suspend fun updateTrackAssignment(
        trackId: String,
        surahNumber: Int?,
        confirmed: Boolean,
        reciterName: String,
        playlistId: String?
    )

    @Query("DELETE FROM quran_audio_tracks WHERE imported_file_id = :fileId")
    suspend fun deleteAudioTracksByFile(fileId: String)

    @Query("DELETE FROM quran_audio_tracks WHERE id = :id")
    suspend fun deleteAudioTrack(id: String)

    @Query("SELECT * FROM quran_playlists ORDER BY created_at_ms DESC")
    fun observePlaylists(): Flow<List<QuranPlaylistEntity>>

    @Query("SELECT * FROM quran_playlists WHERE id = :id LIMIT 1")
    suspend fun getPlaylist(id: String): QuranPlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlaylist(playlist: QuranPlaylistEntity)

    @Query("UPDATE quran_audio_tracks SET reciter_name = :reciterName WHERE playlist_id = :playlistId")
    suspend fun updateTracksReciterByPlaylist(playlistId: String, reciterName: String)

    @Query("UPDATE quran_audio_tracks SET playlist_id = NULL WHERE playlist_id = :playlistId")
    suspend fun clearTrackPlaylistReferences(playlistId: String)

    @Query("DELETE FROM quran_playlists WHERE id = :id")
    suspend fun deletePlaylist(id: String)

    // Bookmarks
    @Query("SELECT * FROM bookmarks ORDER BY created_at_ms DESC")
    fun observeBookmarks(): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE surah_number = :surahNumber AND ayah_number = :ayahNumber")
    suspend fun deleteBookmarkAt(surahNumber: Int, ayahNumber: Int)

    // Session Logs & Statistics
    @Query("SELECT * FROM quran_session_logs ORDER BY started_at_ms DESC")
    fun observeSessionLogs(): Flow<List<QuranSessionLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessionLog(log: QuranSessionLogEntity)

    // Profile & Gamification
    @Query("SELECT * FROM user_quran_profile WHERE id = 1 LIMIT 1")
    fun observeProfile(): Flow<UserQuranProfileEntity?>

    @Query("SELECT * FROM user_quran_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfileOnce(): UserQuranProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: UserQuranProfileEntity)
}
