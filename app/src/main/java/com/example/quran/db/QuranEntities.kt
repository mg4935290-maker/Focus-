package com.example.quran.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "surahs")
data class SurahEntity(
    @PrimaryKey val number: Int,
    @ColumnInfo(name = "name_arabic") val nameArabic: String,
    @ColumnInfo(name = "name_english") val nameEnglish: String,
    @ColumnInfo(name = "revelation_type") val revelationType: String,
    @ColumnInfo(name = "ayah_count") val ayahCount: Int,
    @ColumnInfo(name = "word_count") val wordCount: Int,
    @ColumnInfo(name = "estimated_minutes_base") val estimatedMinutesBase: Double,
    @ColumnInfo(name = "is_completed") val isCompleted: Boolean = false,
    @ColumnInfo(name = "last_read_ayah") val lastReadAyah: Int = 0,
    @ColumnInfo(name = "scroll_progress") val scrollProgress: Float = 0f,
    @ColumnInfo(name = "times_read") val timesRead: Int = 0,
    @ColumnInfo(name = "times_listened") val timesListened: Int = 0,
    @ColumnInfo(name = "total_read_ms") val totalReadMs: Long = 0L,
    @ColumnInfo(name = "total_listened_ms") val totalListenedMs: Long = 0L,
    @ColumnInfo(name = "has_imported_text") val hasImportedText: Boolean = false
)

@Entity(
    tableName = "ayahs",
    indices = [
        Index(value = ["surah_number"]),
        Index(value = ["surah_number", "ayah_number"], unique = true)
    ]
)
data class AyahEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "surah_number") val surahNumber: Int,
    @ColumnInfo(name = "ayah_number") val ayahNumber: Int,
    @ColumnInfo(name = "text_uthmani") val textUthmani: String,
    @ColumnInfo(name = "word_count") val wordCount: Int,
    @ColumnInfo(name = "start_time_ms") val startTimeMs: Long? = null,
    @ColumnInfo(name = "end_time_ms") val endTimeMs: Long? = null,
    @ColumnInfo(name = "source_file_id") val sourceFileId: String = ""
)

@Entity(
    tableName = "imported_files",
    indices = [Index(value = ["content_hash"])]
)
data class ImportedFileEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "uri_string") val uriString: String,
    @ColumnInfo(name = "local_copy_path") val localCopyPath: String? = null,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "content_hash") val contentHash: String,
    @ColumnInfo(name = "file_type") val fileType: String, // PDF, TXT, JSON, AUDIO
    @ColumnInfo(name = "file_size_bytes") val fileSizeBytes: Long,
    @ColumnInfo(name = "status") val status: String, // PENDING, PROCESSING, READY, FAILED
    @ColumnInfo(name = "progress_percent") val progressPercent: Int = 0,
    @ColumnInfo(name = "error_message") val errorMessage: String? = null,
    @ColumnInfo(name = "imported_at_ms") val importedAtMs: Long,
    @ColumnInfo(name = "surah_count_imported") val surahCountImported: Int = 0,
    @ColumnInfo(name = "ayah_count_imported") val ayahCountImported: Int = 0
)

@Entity(
    tableName = "quran_audio_tracks",
    indices = [Index(value = ["surah_number"]), Index(value = ["playlist_id"])]
)
data class QuranAudioTrackEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "imported_file_id") val importedFileId: String,
    @ColumnInfo(name = "uri_string") val uriString: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "surah_number") val surahNumber: Int? = null,
    @ColumnInfo(name = "is_surah_confirmed") val isSurahConfirmed: Boolean = false,
    @ColumnInfo(name = "reciter_name") val reciterName: String = "",
    @ColumnInfo(name = "duration_ms") val durationMs: Long = 0L,
    @ColumnInfo(name = "bitrate_kbps") val bitrateKbps: Int = 0,
    @ColumnInfo(name = "quality_label") val qualityLabel: String = "",
    @ColumnInfo(name = "playlist_id") val playlistId: String? = null,
    @ColumnInfo(name = "created_at_ms") val createdAtMs: Long = 0L
)

@Entity(tableName = "quran_playlists")
data class QuranPlaylistEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "reciter_name") val reciterName: String,
    @ColumnInfo(name = "created_at_ms") val createdAtMs: Long
)

@Entity(
    tableName = "bookmarks",
    indices = [Index(value = ["surah_number", "ayah_number"], unique = true)]
)
data class BookmarkEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "surah_number") val surahNumber: Int,
    @ColumnInfo(name = "ayah_number") val ayahNumber: Int,
    @ColumnInfo(name = "note") val note: String = "",
    @ColumnInfo(name = "created_at_ms") val createdAtMs: Long
)

@Entity(tableName = "quran_session_logs")
data class QuranSessionLogEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "mode") val mode: String, // READING, LISTENING, COMBINED
    @ColumnInfo(name = "started_at_ms") val startedAtMs: Long,
    @ColumnInfo(name = "ended_at_ms") val endedAtMs: Long,
    @ColumnInfo(name = "planned_duration_ms") val plannedDurationMs: Long,
    @ColumnInfo(name = "actual_duration_ms") val actualDurationMs: Long,
    @ColumnInfo(name = "surah_numbers_csv") val surahNumbersCsv: String,
    @ColumnInfo(name = "ayahs_read_count") val ayahsReadCount: Int = 0,
    @ColumnInfo(name = "focus_lock_enabled") val focusLockEnabled: Boolean = false,
    @ColumnInfo(name = "completed") val completed: Boolean = false
)

@Entity(tableName = "user_quran_profile")
data class UserQuranProfileEntity(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "reading_speed_wpm") val readingSpeedWpm: Double = 105.0,
    @ColumnInfo(name = "points") val points: Int = 0,
    @ColumnInfo(name = "stars") val stars: Int = 0,
    @ColumnInfo(name = "daily_goal_minutes") val dailyGoalMinutes: Int = 20,
    @ColumnInfo(name = "weekly_goal_minutes") val weeklyGoalMinutes: Int = 120,
    @ColumnInfo(name = "reading_streak_days") val readingStreakDays: Int = 0,
    @ColumnInfo(name = "listening_streak_days") val listeningStreakDays: Int = 0,
    @ColumnInfo(name = "last_read_date_key") val lastReadDateKey: String = "",
    @ColumnInfo(name = "last_listen_date_key") val lastListenDateKey: String = "",
    @ColumnInfo(name = "badges_json") val badgesJson: String = "[]",
    @ColumnInfo(name = "last_viewed_surah") val lastViewedSurah: Int = 1,
    @ColumnInfo(name = "last_viewed_ayah") val lastViewedAyah: Int = 1
)
