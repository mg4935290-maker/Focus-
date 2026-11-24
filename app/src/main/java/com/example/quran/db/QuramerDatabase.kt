package com.example.quran.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SurahEntity::class,
        AyahEntity::class,
        ImportedFileEntity::class,
        QuranAudioTrackEntity::class,
        QuranPlaylistEntity::class,
        BookmarkEntity::class,
        QuranSessionLogEntity::class,
        UserQuranProfileEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class QuramerDatabase : RoomDatabase() {

    abstract fun quranDao(): QuranDao

    companion object {
        const val DB_NAME = "quramer_database.db"

        /**
         * Explicit schema migration v1 -> v2 preserving all existing Quran library,
         * imported files, bookmarks, session history, and user profile records.
         */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (tableExists(db, "surahs")) {
                    addColumnIfMissing(db, "surahs", "scroll_progress", "REAL NOT NULL DEFAULT 0.0")
                }
                if (tableExists(db, "ayahs")) {
                    addColumnIfMissing(db, "ayahs", "start_time_ms", "INTEGER DEFAULT NULL")
                    addColumnIfMissing(db, "ayahs", "end_time_ms", "INTEGER DEFAULT NULL")
                }
                if (tableExists(db, "quran_audio_tracks")) {
                    addColumnIfMissing(db, "quran_audio_tracks", "is_surah_confirmed", "INTEGER NOT NULL DEFAULT 0")
                }
                if (tableExists(db, "user_quran_profile")) {
                    addColumnIfMissing(db, "user_quran_profile", "weekly_goal_minutes", "INTEGER NOT NULL DEFAULT 120")
                }
            }

            private fun tableExists(db: SupportSQLiteDatabase, table: String): Boolean {
                db.query("SELECT name FROM sqlite_master WHERE type='table' AND name=?", arrayOf(table)).use { c ->
                    return c.moveToFirst()
                }
            }

            private fun addColumnIfMissing(
                db: SupportSQLiteDatabase,
                table: String,
                column: String,
                definition: String
            ) {
                var exists = false
                db.query("PRAGMA table_info($table)").use { c ->
                    val nameIdx = c.getColumnIndex("name")
                    while (c.moveToNext()) {
                        if (nameIdx >= 0 && c.getString(nameIdx) == column) {
                            exists = true
                            break
                        }
                    }
                }
                if (!exists) {
                    db.execSQL("ALTER TABLE $table ADD COLUMN $column $definition")
                }
            }
        }

        @Volatile
        private var instance: QuramerDatabase? = null

        fun getInstance(context: Context): QuramerDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    QuramerDatabase::class.java,
                    DB_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
