package com.example.data

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.edit
import androidx.core.graphics.drawable.toBitmap
import com.example.BlockerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?
)

data class DailyTask(
    val id: String,
    val title: String,
    val isCompleted: Boolean
)

data class PlantedTree(
    val id: String,
    val plantedAtMs: Long,
    val variant: Int
)

data class UploadedAudioTrack(
    val id: String,
    val uriString: String,
    val displayName: String
)

data class DailyStudyRecord(
    val dateKey: String,
    val durationMs: Long
)

data class WeeklyStudyArchiveEntry(
    val weekKey: String,
    val label: String,
    val startDateKey: String,
    val endDateKey: String,
    val studyMs: Long,
    val treesCount: Int,
    val activeDays: Int
)

data class MonthlyStudyArchiveEntry(
    val monthKey: String,
    val label: String,
    val studyMs: Long,
    val treesCount: Int,
    val activeDays: Int
)

enum class BuiltInSound(val id: String) {
    RAIN("rain"),
    WHITE_NOISE("white_noise"),
    ALPHA("alpha"),
    BETA("beta"),
    GAMMA("gamma");

    val isBinaural: Boolean
        get() = this == ALPHA || this == BETA || this == GAMMA

    companion object {
        fun fromId(id: String?): BuiltInSound? = entries.firstOrNull { it.id == id }
    }
}

sealed class SelectedAudioSource {
    data class BuiltIn(val sound: BuiltInSound) : SelectedAudioSource()
    data class Uploaded(val track: UploadedAudioTrack) : SelectedAudioSource()
}

data class StudyProgressSnapshot(
    val dailyStudyMs: Long,
    val totalStudyMs: Long,
    val unplantedStudyMs: Long,
    val trees: List<PlantedTree>,
    val sessionEndMs: Long,
    val sessionDurationMs: Long,
    val isStopwatch: Boolean = false,
    val sessionStartMs: Long = 0L,
    val currentWeekKey: String = "",
    val currentWeekLabel: String = "",
    val weeklyStudyMs: Long = 0L,
    val weeklyTreesCount: Int = 0,
    val weeklyActiveDays: Int = 0,
    val currentMonthKey: String = "",
    val currentMonthLabel: String = "",
    val monthlyStudyMs: Long = 0L,
    val monthlyTreesCount: Int = 0,
    val monthlyActiveDays: Int = 0,
    val dailyHistory: List<DailyStudyRecord> = emptyList(),
    val weeklyArchive: List<WeeklyStudyArchiveEntry> = emptyList(),
    val monthlyArchive: List<MonthlyStudyArchiveEntry> = emptyList(),
    val hasUnseenSessionFinished: Boolean = false,
    val lastFinishedSessionDurationMs: Long = 0L,
    val currentDateKey: String = "",
    val currentDateFormatted: String = ""
)

class FocusRepository(private val context: Context) {

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun currentDateKey(nowMs: Long = System.currentTimeMillis()): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(nowMs))
    }

    fun monthKeyFor(nowMs: Long = System.currentTimeMillis()): String {
        return SimpleDateFormat("yyyy-MM", Locale.US).format(Date(nowMs))
    }

    fun weekKeyFor(nowMs: Long = System.currentTimeMillis()): String {
        return weekBoundsFor(nowMs).weekKey
    }

    private data class WeekBounds(
        val weekKey: String,
        val startDateKey: String,
        val endDateKey: String,
        val label: String
    )

    private fun weekBoundsFor(nowMs: Long): WeekBounds {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // Arabic study week starts on Saturday
            while (get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
                add(Calendar.DAY_OF_YEAR, -1)
            }
        }
        val startMs = cal.timeInMillis
        val startKey = currentDateKey(startMs)
        val endCal = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 6) }
        val endMs = endCal.timeInMillis
        val endKey = currentDateKey(endMs)
        return WeekBounds(
            weekKey = startKey,
            startDateKey = startKey,
            endDateKey = endKey,
            label = formatWeekLabelArabic(startKey, endKey)
        )
    }

    private fun weekBoundsFromKey(weekKey: String): WeekBounds {
        val parsed = runCatching {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(weekKey)?.time
        }.getOrNull()
        return if (parsed != null) {
            weekBoundsFor(parsed)
        } else {
            WeekBounds(weekKey = weekKey, startDateKey = weekKey, endDateKey = weekKey, label = weekKey)
        }
    }

    private fun nextDayStartMs(ms: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = ms
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    suspend fun loadInstalledApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(intent, 0)
            .asSequence()
            .mapNotNull { resolveInfo ->
                val pkg = resolveInfo.activityInfo?.packageName ?: return@mapNotNull null
                if (pkg == context.packageName) return@mapNotNull null
                val label = resolveInfo.loadLabel(pm).toString().ifBlank { pkg }
                val iconBitmap = runCatching {
                    resolveInfo.loadIcon(pm).toBitmap(width = 96, height = 96).asImageBitmap()
                }.getOrNull()
                InstalledApp(packageName = pkg, label = label, icon = iconBitmap)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    fun getUserName(): String {
        return prefs.getString(KEY_USER_NAME, "")?.trim().orEmpty()
    }

    fun saveUserName(name: String) {
        prefs.edit {
            putString(KEY_USER_NAME, name.trim())
        }
    }

    fun getAllowedPackages(): Set<String> {
        return prefs.getStringSet(KEY_ALLOWED, emptySet())?.toSet() ?: emptySet()
    }

    fun saveAllowedPackages(allowedPackages: Set<String>) {
        prefs.edit {
            putStringSet(KEY_ALLOWED, allowedPackages.toSet())
        }
    }

    fun getSessionEndMs(): Long {
        return prefs.getLong(KEY_END, 0L)
    }

    fun getSessionDurationMs(): Long {
        return prefs.getLong(KEY_DURATION_MS, 0L)
    }

    fun startSession(
        allowedPackages: Set<String>,
        durationMinutes: Long,
        nowMs: Long = System.currentTimeMillis()
    ): Pair<Long, Long> {
        syncStudyProgress(nowMs)
        val durationMs = durationMinutes * 60_000L
        val endMs = nowMs + durationMs
        val todayKey = currentDateKey(nowMs)
        val existingStudyDate = prefs.getString(KEY_STUDY_DATE, "") ?: ""
        prefs.edit {
            putStringSet(KEY_ALLOWED, allowedPackages.toSet())
            putBoolean(KEY_IS_STOPWATCH, false)
            putLong(KEY_END, endMs)
            putLong(KEY_DURATION_MS, durationMs)
            putLong(KEY_SESSION_START_MS, nowMs)
            putLong(KEY_LAST_ACCOUNTED_MS, nowMs)
            putBoolean(KEY_HAS_UNSEEN_SESSION_FINISHED, false)
            if (existingStudyDate != todayKey) {
                putString(KEY_STUDY_DATE, todayKey)
                putLong(KEY_DAILY_STUDY_MS, 0L)
            }
        }
        return endMs to durationMs
    }

    fun startStopwatchSession(
        allowedPackages: Set<String>,
        nowMs: Long = System.currentTimeMillis()
    ): Long {
        syncStudyProgress(nowMs)
        val todayKey = currentDateKey(nowMs)
        val existingStudyDate = prefs.getString(KEY_STUDY_DATE, "") ?: ""
        prefs.edit {
            putStringSet(KEY_ALLOWED, allowedPackages.toSet())
            putBoolean(KEY_IS_STOPWATCH, true)
            putLong(KEY_END, Long.MAX_VALUE)
            putLong(KEY_DURATION_MS, 0L)
            putLong(KEY_SESSION_START_MS, nowMs)
            putLong(KEY_LAST_ACCOUNTED_MS, nowMs)
            putBoolean(KEY_HAS_UNSEEN_SESSION_FINISHED, false)
            if (existingStudyDate != todayKey) {
                putString(KEY_STUDY_DATE, todayKey)
                putLong(KEY_DAILY_STUDY_MS, 0L)
            }
        }
        return nowMs
    }

    fun cancelSession(nowMs: Long = System.currentTimeMillis()): StudyProgressSnapshot {
        syncStudyProgress(nowMs)
        prefs.edit {
            putBoolean(KEY_IS_STOPWATCH, false)
            putLong(KEY_END, 0L)
            putLong(KEY_DURATION_MS, 0L)
            putLong(KEY_SESSION_START_MS, 0L)
            putLong(KEY_LAST_ACCOUNTED_MS, 0L)
        }
        return getStudyProgressSnapshot(nowMs)
    }

    fun markSessionFinishedCelebration(durationMs: Long) {
        prefs.edit {
            putBoolean(KEY_HAS_UNSEEN_SESSION_FINISHED, true)
            putLong(KEY_LAST_SESSION_FINISHED_DURATION_MS, durationMs.coerceAtLeast(0L))
        }
    }

    fun dismissSessionFinishedCelebration() {
        prefs.edit {
            putBoolean(KEY_HAS_UNSEEN_SESSION_FINISHED, false)
        }
    }

    @Synchronized
    fun syncStudyProgress(nowMs: Long = System.currentTimeMillis()): StudyProgressSnapshot {
        val todayKey = currentDateKey(nowMs)
        val savedStudyDate = prefs.getString(KEY_STUDY_DATE, "") ?: ""
        var dailyStudyMs = if (savedStudyDate == todayKey) {
            prefs.getLong(KEY_DAILY_STUDY_MS, 0L)
        } else {
            0L
        }
        var totalStudyMs = prefs.getLong(KEY_TOTAL_STUDY_MS, 0L)
        var unplantedStudyMs = prefs.getLong(KEY_UNPLANTED_STUDY_MS, 0L)
        val trees = loadTrees().toMutableList()

        val dailyHistoryMap = loadDailyHistory().associate { it.dateKey to it.durationMs }.toMutableMap()
        val weeklyArchive = loadWeeklyArchive().toMutableList()
        val monthlyArchive = loadMonthlyArchive().toMutableList()

        val nowWeek = weekBoundsFor(nowMs)
        val nowMonthKey = monthKeyFor(nowMs)

        var currentWeekKey = prefs.getString(KEY_CURRENT_WEEK_KEY, "") ?: ""
        var weeklyStudyMs = prefs.getLong(KEY_WEEKLY_STUDY_MS, 0L)
        var weeklyTreesCount = prefs.getInt(KEY_WEEKLY_TREES_COUNT, 0)
        val weeklyActiveDays = decodeStringSet(prefs.getString(KEY_WEEKLY_ACTIVE_DAYS, null)).toMutableSet()

        var currentMonthKey = prefs.getString(KEY_CURRENT_MONTH_KEY, "") ?: ""
        var monthlyStudyMs = prefs.getLong(KEY_MONTHLY_STUDY_MS, 0L)
        var monthlyTreesCount = prefs.getInt(KEY_MONTHLY_TREES_COUNT, 0)
        val monthlyActiveDays = decodeStringSet(prefs.getString(KEY_MONTHLY_ACTIVE_DAYS, null)).toMutableSet()

        // First-time upgrade migration: preserve pre-existing progress into current month & week
        if (currentMonthKey.isBlank()) {
            currentMonthKey = nowMonthKey
            monthlyStudyMs = totalStudyMs
            monthlyTreesCount = trees.size
            if (dailyStudyMs > 0L) {
                monthlyActiveDays.add(todayKey)
            }
        }
        if (currentWeekKey.isBlank()) {
            currentWeekKey = nowWeek.weekKey
            weeklyStudyMs = dailyStudyMs
            if (dailyStudyMs > 0L) {
                weeklyActiveDays.add(todayKey)
                dailyHistoryMap[todayKey] = maxOf(dailyHistoryMap[todayKey] ?: 0L, dailyStudyMs)
            }
        }

        fun rolloverToTimestamp(targetMs: Long) {
            val targetWeek = weekBoundsFor(targetMs)
            if (currentWeekKey.isNotBlank() && currentWeekKey != targetWeek.weekKey) {
                if (weeklyStudyMs > 0L || weeklyTreesCount > 0) {
                    val oldBounds = weekBoundsFromKey(currentWeekKey)
                    upsertWeeklyArchive(
                        weeklyArchive,
                        WeeklyStudyArchiveEntry(
                            weekKey = currentWeekKey,
                            label = oldBounds.label,
                            startDateKey = oldBounds.startDateKey,
                            endDateKey = oldBounds.endDateKey,
                            studyMs = weeklyStudyMs,
                            treesCount = weeklyTreesCount,
                            activeDays = weeklyActiveDays.size
                        )
                    )
                }
                currentWeekKey = targetWeek.weekKey
                weeklyStudyMs = 0L
                weeklyTreesCount = 0
                weeklyActiveDays.clear()
            }

            val targetMonth = monthKeyFor(targetMs)
            if (currentMonthKey.isNotBlank() && currentMonthKey != targetMonth) {
                if (monthlyStudyMs > 0L || monthlyTreesCount > 0) {
                    upsertMonthlyArchive(
                        monthlyArchive,
                        MonthlyStudyArchiveEntry(
                            monthKey = currentMonthKey,
                            label = formatMonthLabelArabic(currentMonthKey),
                            studyMs = monthlyStudyMs,
                            treesCount = monthlyTreesCount,
                            activeDays = monthlyActiveDays.size
                        )
                    )
                }
                currentMonthKey = targetMonth
                monthlyStudyMs = 0L
                monthlyTreesCount = 0
                monthlyActiveDays.clear()
            }
        }

        fun applyIntervalSlice(startMs: Long, endMs: Long) {
            var cursor = startMs
            while (cursor < endMs) {
                val sliceEnd = minOf(endMs, nextDayStartMs(cursor))
                val sliceDelta = sliceEnd - cursor
                if (sliceDelta > 0L) {
                    rolloverToTimestamp(cursor)
                    val sliceDateKey = currentDateKey(cursor)
                    dailyHistoryMap[sliceDateKey] = (dailyHistoryMap[sliceDateKey] ?: 0L) + sliceDelta
                    if (sliceDateKey == todayKey) {
                        dailyStudyMs += sliceDelta
                    }
                    weeklyStudyMs += sliceDelta
                    weeklyActiveDays.add(sliceDateKey)
                    monthlyStudyMs += sliceDelta
                    monthlyActiveDays.add(sliceDateKey)
                    totalStudyMs += sliceDelta
                    unplantedStudyMs += sliceDelta

                    while (unplantedStudyMs >= ONE_HOUR_MS) {
                        unplantedStudyMs -= ONE_HOUR_MS
                        val plantedAt = sliceEnd - unplantedStudyMs
                        val variant = trees.size % 4
                        trees.add(
                            PlantedTree(
                                id = UUID.randomUUID().toString(),
                                plantedAtMs = plantedAt,
                                variant = variant
                            )
                        )
                        val plantedWeek = weekKeyFor(plantedAt)
                        if (plantedWeek == currentWeekKey) {
                            weeklyTreesCount++
                        } else {
                            val idx = weeklyArchive.indexOfFirst { it.weekKey == plantedWeek }
                            if (idx >= 0) {
                                val existing = weeklyArchive[idx]
                                weeklyArchive[idx] = existing.copy(treesCount = existing.treesCount + 1)
                            }
                        }
                        val plantedMonth = monthKeyFor(plantedAt)
                        if (plantedMonth == currentMonthKey) {
                            monthlyTreesCount++
                        } else {
                            val idx = monthlyArchive.indexOfFirst { it.monthKey == plantedMonth }
                            if (idx >= 0) {
                                val existing = monthlyArchive[idx]
                                monthlyArchive[idx] = existing.copy(treesCount = existing.treesCount + 1)
                            }
                        }
                    }
                }
                cursor = sliceEnd
            }
        }

        var isStopwatch = prefs.getBoolean(KEY_IS_STOPWATCH, false)
        val sessionStartMs = prefs.getLong(KEY_SESSION_START_MS, 0L)
        val sessionEndMs = prefs.getLong(KEY_END, 0L)
        val lastAccountedMs = prefs.getLong(KEY_LAST_ACCOUNTED_MS, 0L)
            .coerceAtLeast(sessionStartMs)

        var newSessionStartMs = sessionStartMs
        var newSessionEndMs = sessionEndMs
        var newSessionDurationMs = prefs.getLong(KEY_DURATION_MS, 0L)
        var newLastAccountedMs = lastAccountedMs
        var hasUnseenFinished = prefs.getBoolean(KEY_HAS_UNSEEN_SESSION_FINISHED, false)
        var lastFinishedDurationMs = prefs.getLong(KEY_LAST_SESSION_FINISHED_DURATION_MS, 0L)

        if (isStopwatch && sessionStartMs > 0L) {
            val effectiveEnd = nowMs.coerceAtLeast(lastAccountedMs)
            if (effectiveEnd > lastAccountedMs) {
                applyIntervalSlice(lastAccountedMs, effectiveEnd)
                newLastAccountedMs = effectiveEnd
            }
        } else if (sessionStartMs > 0L && sessionEndMs > sessionStartMs) {
            val effectiveEnd = minOf(nowMs, sessionEndMs)
            if (effectiveEnd > lastAccountedMs) {
                applyIntervalSlice(lastAccountedMs, effectiveEnd)
                newLastAccountedMs = effectiveEnd
            }

            if (nowMs >= sessionEndMs) {
                hasUnseenFinished = true
                lastFinishedDurationMs = newSessionDurationMs.coerceAtLeast(sessionEndMs - sessionStartMs)
                isStopwatch = false
                newSessionStartMs = 0L
                newSessionEndMs = 0L
                newSessionDurationMs = 0L
                newLastAccountedMs = 0L
            }
        }

        // Ensure rollover also executes when opening the app in a brand new week or month
        rolloverToTimestamp(nowMs)

        val trimmedHistory = dailyHistoryMap.entries
            .sortedByDescending { it.key }
            .take(366)
            .map { DailyStudyRecord(dateKey = it.key, durationMs = it.value) }
            .sortedBy { it.dateKey }

        val sortedWeeklyArchive = weeklyArchive.sortedByDescending { it.weekKey }
        val sortedMonthlyArchive = monthlyArchive.sortedByDescending { it.monthKey }

        prefs.edit {
            putString(KEY_STUDY_DATE, todayKey)
            putLong(KEY_DAILY_STUDY_MS, dailyStudyMs)
            putLong(KEY_TOTAL_STUDY_MS, totalStudyMs)
            putLong(KEY_UNPLANTED_STUDY_MS, unplantedStudyMs)
            putString(KEY_TREES_JSON, encodeTrees(trees))
            putBoolean(KEY_IS_STOPWATCH, isStopwatch)
            putLong(KEY_SESSION_START_MS, newSessionStartMs)
            putLong(KEY_END, newSessionEndMs)
            putLong(KEY_DURATION_MS, newSessionDurationMs)
            putLong(KEY_LAST_ACCOUNTED_MS, newLastAccountedMs)
            putBoolean(KEY_HAS_UNSEEN_SESSION_FINISHED, hasUnseenFinished)
            putLong(KEY_LAST_SESSION_FINISHED_DURATION_MS, lastFinishedDurationMs)
            putString(KEY_CURRENT_WEEK_KEY, currentWeekKey)
            putLong(KEY_WEEKLY_STUDY_MS, weeklyStudyMs)
            putInt(KEY_WEEKLY_TREES_COUNT, weeklyTreesCount)
            putString(KEY_WEEKLY_ACTIVE_DAYS, encodeStringSet(weeklyActiveDays))
            putString(KEY_CURRENT_MONTH_KEY, currentMonthKey)
            putLong(KEY_MONTHLY_STUDY_MS, monthlyStudyMs)
            putInt(KEY_MONTHLY_TREES_COUNT, monthlyTreesCount)
            putString(KEY_MONTHLY_ACTIVE_DAYS, encodeStringSet(monthlyActiveDays))
            putString(KEY_DAILY_HISTORY_JSON, encodeDailyHistory(trimmedHistory))
            putString(KEY_WEEKLY_ARCHIVE_JSON, encodeWeeklyArchive(sortedWeeklyArchive))
            putString(KEY_MONTHLY_ARCHIVE_JSON, encodeMonthlyArchive(sortedMonthlyArchive))
        }

        val currentWeekBounds = weekBoundsFromKey(currentWeekKey)
        return StudyProgressSnapshot(
            dailyStudyMs = dailyStudyMs,
            totalStudyMs = totalStudyMs,
            unplantedStudyMs = unplantedStudyMs,
            trees = trees,
            sessionEndMs = newSessionEndMs,
            sessionDurationMs = newSessionDurationMs,
            isStopwatch = isStopwatch,
            sessionStartMs = newSessionStartMs,
            currentWeekKey = currentWeekKey,
            currentWeekLabel = currentWeekBounds.label,
            weeklyStudyMs = weeklyStudyMs,
            weeklyTreesCount = weeklyTreesCount,
            weeklyActiveDays = weeklyActiveDays.size,
            currentMonthKey = currentMonthKey,
            currentMonthLabel = formatMonthLabelArabic(currentMonthKey),
            monthlyStudyMs = monthlyStudyMs,
            monthlyTreesCount = monthlyTreesCount,
            monthlyActiveDays = monthlyActiveDays.size,
            dailyHistory = trimmedHistory,
            weeklyArchive = sortedWeeklyArchive,
            monthlyArchive = sortedMonthlyArchive,
            hasUnseenSessionFinished = hasUnseenFinished,
            lastFinishedSessionDurationMs = lastFinishedDurationMs,
            currentDateKey = todayKey,
            currentDateFormatted = formatFullDateArabic(nowMs)
        )
    }

    @Synchronized
    fun creditExternalStudyProgress(durationMs: Long, nowMs: Long = System.currentTimeMillis()): StudyProgressSnapshot {
        val current = syncStudyProgress(nowMs)
        if (durationMs <= 0L) return current
        val todayKey = currentDateKey(nowMs)
        val dailyStudyMs = current.dailyStudyMs + durationMs
        val totalStudyMs = current.totalStudyMs + durationMs
        var unplantedStudyMs = current.unplantedStudyMs + durationMs
        val trees = current.trees.toMutableList()
        var newlyPlanted = 0

        while (unplantedStudyMs >= ONE_HOUR_MS) {
            unplantedStudyMs -= ONE_HOUR_MS
            newlyPlanted++
            trees.add(
                PlantedTree(
                    id = UUID.randomUUID().toString(),
                    plantedAtMs = nowMs - unplantedStudyMs,
                    variant = trees.size % 4
                )
            )
        }

        val weeklyStudyMs = current.weeklyStudyMs + durationMs
        val weeklyTreesCount = current.weeklyTreesCount + newlyPlanted
        val weeklyDays = decodeStringSet(prefs.getString(KEY_WEEKLY_ACTIVE_DAYS, null)).toMutableSet().apply {
            add(todayKey)
        }
        val monthlyStudyMs = current.monthlyStudyMs + durationMs
        val monthlyTreesCount = current.monthlyTreesCount + newlyPlanted
        val monthlyDays = decodeStringSet(prefs.getString(KEY_MONTHLY_ACTIVE_DAYS, null)).toMutableSet().apply {
            add(todayKey)
        }
        val historyMap = current.dailyHistory.associate { it.dateKey to it.durationMs }.toMutableMap()
        historyMap[todayKey] = (historyMap[todayKey] ?: 0L) + durationMs
        val updatedHistory = historyMap.entries
            .sortedByDescending { it.key }
            .take(366)
            .map { DailyStudyRecord(it.key, it.value) }
            .sortedBy { it.dateKey }

        prefs.edit {
            putString(KEY_STUDY_DATE, todayKey)
            putLong(KEY_DAILY_STUDY_MS, dailyStudyMs)
            putLong(KEY_TOTAL_STUDY_MS, totalStudyMs)
            putLong(KEY_UNPLANTED_STUDY_MS, unplantedStudyMs)
            putString(KEY_TREES_JSON, encodeTrees(trees))
            putLong(KEY_WEEKLY_STUDY_MS, weeklyStudyMs)
            putInt(KEY_WEEKLY_TREES_COUNT, weeklyTreesCount)
            putString(KEY_WEEKLY_ACTIVE_DAYS, encodeStringSet(weeklyDays))
            putLong(KEY_MONTHLY_STUDY_MS, monthlyStudyMs)
            putInt(KEY_MONTHLY_TREES_COUNT, monthlyTreesCount)
            putString(KEY_MONTHLY_ACTIVE_DAYS, encodeStringSet(monthlyDays))
            putString(KEY_DAILY_HISTORY_JSON, encodeDailyHistory(updatedHistory))
        }

        return current.copy(
            dailyStudyMs = dailyStudyMs,
            totalStudyMs = totalStudyMs,
            unplantedStudyMs = unplantedStudyMs,
            trees = trees,
            weeklyStudyMs = weeklyStudyMs,
            weeklyTreesCount = weeklyTreesCount,
            weeklyActiveDays = weeklyDays.size,
            monthlyStudyMs = monthlyStudyMs,
            monthlyTreesCount = monthlyTreesCount,
            monthlyActiveDays = monthlyDays.size,
            dailyHistory = updatedHistory
        )
    }

    fun getStudyProgressSnapshot(nowMs: Long = System.currentTimeMillis()): StudyProgressSnapshot {
        val todayKey = currentDateKey(nowMs)
        val savedStudyDate = prefs.getString(KEY_STUDY_DATE, "") ?: ""
        val dailyStudyMs = if (savedStudyDate == todayKey) {
            prefs.getLong(KEY_DAILY_STUDY_MS, 0L)
        } else {
            0L
        }
        val currentWeekKey = prefs.getString(KEY_CURRENT_WEEK_KEY, "").orEmpty().ifBlank { weekKeyFor(nowMs) }
        val currentMonthKey = prefs.getString(KEY_CURRENT_MONTH_KEY, "").orEmpty().ifBlank { monthKeyFor(nowMs) }
        val weekBounds = weekBoundsFromKey(currentWeekKey)
        val weeklyDays = decodeStringSet(prefs.getString(KEY_WEEKLY_ACTIVE_DAYS, null))
        val monthlyDays = decodeStringSet(prefs.getString(KEY_MONTHLY_ACTIVE_DAYS, null))

        return StudyProgressSnapshot(
            dailyStudyMs = dailyStudyMs,
            totalStudyMs = prefs.getLong(KEY_TOTAL_STUDY_MS, 0L),
            unplantedStudyMs = prefs.getLong(KEY_UNPLANTED_STUDY_MS, 0L),
            trees = loadTrees(),
            sessionEndMs = prefs.getLong(KEY_END, 0L),
            sessionDurationMs = prefs.getLong(KEY_DURATION_MS, 0L),
            isStopwatch = prefs.getBoolean(KEY_IS_STOPWATCH, false),
            sessionStartMs = prefs.getLong(KEY_SESSION_START_MS, 0L),
            currentWeekKey = currentWeekKey,
            currentWeekLabel = weekBounds.label,
            weeklyStudyMs = prefs.getLong(KEY_WEEKLY_STUDY_MS, 0L),
            weeklyTreesCount = prefs.getInt(KEY_WEEKLY_TREES_COUNT, 0),
            weeklyActiveDays = weeklyDays.size,
            currentMonthKey = currentMonthKey,
            currentMonthLabel = formatMonthLabelArabic(currentMonthKey),
            monthlyStudyMs = prefs.getLong(KEY_MONTHLY_STUDY_MS, 0L),
            monthlyTreesCount = prefs.getInt(KEY_MONTHLY_TREES_COUNT, 0),
            monthlyActiveDays = monthlyDays.size,
            dailyHistory = loadDailyHistory(),
            weeklyArchive = loadWeeklyArchive(),
            monthlyArchive = loadMonthlyArchive(),
            hasUnseenSessionFinished = prefs.getBoolean(KEY_HAS_UNSEEN_SESSION_FINISHED, false),
            lastFinishedSessionDurationMs = prefs.getLong(KEY_LAST_SESSION_FINISHED_DURATION_MS, 0L),
            currentDateKey = todayKey,
            currentDateFormatted = formatFullDateArabic(nowMs)
        )
    }

    private fun upsertWeeklyArchive(
        list: MutableList<WeeklyStudyArchiveEntry>,
        entry: WeeklyStudyArchiveEntry
    ) {
        val idx = list.indexOfFirst { it.weekKey == entry.weekKey }
        if (idx >= 0) {
            val prev = list[idx]
            list[idx] = entry.copy(
                studyMs = maxOf(prev.studyMs, entry.studyMs),
                treesCount = maxOf(prev.treesCount, entry.treesCount),
                activeDays = maxOf(prev.activeDays, entry.activeDays)
            )
        } else {
            list.add(entry)
        }
    }

    private fun upsertMonthlyArchive(
        list: MutableList<MonthlyStudyArchiveEntry>,
        entry: MonthlyStudyArchiveEntry
    ) {
        val idx = list.indexOfFirst { it.monthKey == entry.monthKey }
        if (idx >= 0) {
            val prev = list[idx]
            list[idx] = entry.copy(
                studyMs = maxOf(prev.studyMs, entry.studyMs),
                treesCount = maxOf(prev.treesCount, entry.treesCount),
                activeDays = maxOf(prev.activeDays, entry.activeDays)
            )
        } else {
            list.add(entry)
        }
    }

    private fun loadDailyHistory(): List<DailyStudyRecord> {
        val raw = prefs.getString(KEY_DAILY_HISTORY_JSON, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val dateKey = obj.optString("dateKey").ifBlank { continue }
                    val durationMs = obj.optLong("durationMs", 0L).coerceAtLeast(0L)
                    add(DailyStudyRecord(dateKey = dateKey, durationMs = durationMs))
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeDailyHistory(items: List<DailyStudyRecord>): String {
        val array = JSONArray()
        items.forEach { record ->
            array.put(
                JSONObject()
                    .put("dateKey", record.dateKey)
                    .put("durationMs", record.durationMs)
            )
        }
        return array.toString()
    }

    private fun loadWeeklyArchive(): List<WeeklyStudyArchiveEntry> {
        val raw = prefs.getString(KEY_WEEKLY_ARCHIVE_JSON, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val weekKey = obj.optString("weekKey").ifBlank { continue }
                    add(
                        WeeklyStudyArchiveEntry(
                            weekKey = weekKey,
                            label = obj.optString("label", weekKey),
                            startDateKey = obj.optString("startDateKey", weekKey),
                            endDateKey = obj.optString("endDateKey", weekKey),
                            studyMs = obj.optLong("studyMs", 0L),
                            treesCount = obj.optInt("treesCount", 0),
                            activeDays = obj.optInt("activeDays", 0)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeWeeklyArchive(items: List<WeeklyStudyArchiveEntry>): String {
        val array = JSONArray()
        items.forEach { entry ->
            array.put(
                JSONObject()
                    .put("weekKey", entry.weekKey)
                    .put("label", entry.label)
                    .put("startDateKey", entry.startDateKey)
                    .put("endDateKey", entry.endDateKey)
                    .put("studyMs", entry.studyMs)
                    .put("treesCount", entry.treesCount)
                    .put("activeDays", entry.activeDays)
            )
        }
        return array.toString()
    }

    private fun loadMonthlyArchive(): List<MonthlyStudyArchiveEntry> {
        val raw = prefs.getString(KEY_MONTHLY_ARCHIVE_JSON, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val monthKey = obj.optString("monthKey").ifBlank { continue }
                    add(
                        MonthlyStudyArchiveEntry(
                            monthKey = monthKey,
                            label = obj.optString("label", formatMonthLabelArabic(monthKey)),
                            studyMs = obj.optLong("studyMs", 0L),
                            treesCount = obj.optInt("treesCount", 0),
                            activeDays = obj.optInt("activeDays", 0)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeMonthlyArchive(items: List<MonthlyStudyArchiveEntry>): String {
        val array = JSONArray()
        items.forEach { entry ->
            array.put(
                JSONObject()
                    .put("monthKey", entry.monthKey)
                    .put("label", entry.label)
                    .put("studyMs", entry.studyMs)
                    .put("treesCount", entry.treesCount)
                    .put("activeDays", entry.activeDays)
            )
        }
        return array.toString()
    }

    private fun decodeStringSet(raw: String?): Set<String> {
        if (raw.isNullOrBlank()) return emptySet()
        return runCatching {
            val arr = JSONArray(raw)
            buildSet {
                for (i in 0 until arr.length()) {
                    val v = arr.optString(i)
                    if (v.isNotBlank()) add(v)
                }
            }
        }.getOrDefault(emptySet())
    }

    private fun encodeStringSet(items: Set<String>): String {
        val arr = JSONArray()
        items.forEach { arr.put(it) }
        return arr.toString()
    }

    fun loadTrees(): List<PlantedTree> {
        val raw = prefs.getString(KEY_TREES_JSON, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val id = obj.optString("id").ifBlank { continue }
                    val plantedAtMs = obj.optLong("plantedAtMs", 0L)
                    val variant = obj.optInt("variant", i % 4)
                    add(PlantedTree(id = id, plantedAtMs = plantedAtMs, variant = variant))
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeTrees(trees: List<PlantedTree>): String {
        val array = JSONArray()
        trees.forEach { tree ->
            array.put(
                JSONObject()
                    .put("id", tree.id)
                    .put("plantedAtMs", tree.plantedAtMs)
                    .put("variant", tree.variant)
            )
        }
        return array.toString()
    }

    fun tryConsumeTasksAchievement(
        allTasksCompleted: Boolean,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        if (!allTasksCompleted) return false
        val todayKey = currentDateKey(nowMs)
        val shownDate = prefs.getString(KEY_ACHIEVEMENT_TASKS_DATE, null)
        if (shownDate == todayKey) return false
        prefs.edit { putString(KEY_ACHIEVEMENT_TASKS_DATE, todayKey) }
        return true
    }

    fun tryConsumeHoursAchievement(
        dailyStudyMs: Long,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        if (dailyStudyMs <= FOUR_HOURS_MS) return false
        val todayKey = currentDateKey(nowMs)
        val shownDate = prefs.getString(KEY_ACHIEVEMENT_HOURS_DATE, null)
        if (shownDate == todayKey) return false
        prefs.edit { putString(KEY_ACHIEVEMENT_HOURS_DATE, todayKey) }
        return true
    }

    fun loadDailyTasks(todayKey: String = currentDateKey()): List<DailyTask> {
        val raw = prefs.getString(KEY_DAILY_TASKS, null) ?: return emptyList()
        val savedDate = prefs.getString(KEY_TASKS_DATE, "") ?: ""
        val isNewDay = savedDate.isNotBlank() && savedDate != todayKey

        val parsed = runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val id = obj.optString("id").ifBlank { continue }
                    val title = obj.optString("title").ifBlank { continue }
                    val completed = if (isNewDay) false else obj.optBoolean("isCompleted", false)
                    add(DailyTask(id = id, title = title, isCompleted = completed))
                }
            }
        }.getOrDefault(emptyList())

        if (isNewDay) {
            saveDailyTasks(parsed, todayKey)
        }
        return parsed
    }

    fun saveDailyTasks(tasks: List<DailyTask>, todayKey: String = currentDateKey()) {
        val array = JSONArray()
        tasks.forEach { task ->
            val obj = JSONObject()
                .put("id", task.id)
                .put("title", task.title)
                .put("isCompleted", task.isCompleted)
            array.put(obj)
        }
        prefs.edit {
            putString(KEY_DAILY_TASKS, array.toString())
            putString(KEY_TASKS_DATE, todayKey)
        }
    }

    // Audio & Full-Screen Timer Preferences
    fun loadUploadedTracks(): List<UploadedAudioTrack> {
        val raw = prefs.getString(KEY_UPLOADED_TRACKS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val id = obj.optString("id").ifBlank { continue }
                    val uri = obj.optString("uri").ifBlank { continue }
                    val name = obj.optString("name").ifBlank { continue }
                    add(UploadedAudioTrack(id = id, uriString = uri, displayName = name))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveUploadedTracks(tracks: List<UploadedAudioTrack>) {
        val array = JSONArray()
        tracks.forEach { track ->
            array.put(
                JSONObject()
                    .put("id", track.id)
                    .put("uri", track.uriString)
                    .put("name", track.displayName)
            )
        }
        prefs.edit { putString(KEY_UPLOADED_TRACKS, array.toString()) }
    }

    fun getSelectedBuiltInSoundId(): String {
        return prefs.getString(KEY_SELECTED_SOUND_ID, BuiltInSound.RAIN.id) ?: BuiltInSound.RAIN.id
    }

    fun getSelectedUploadedTrackId(): String? {
        return prefs.getString(KEY_SELECTED_UPLOADED_ID, null)?.takeIf { it.isNotBlank() }
    }

    fun saveSelectedBuiltInSound(sound: BuiltInSound) {
        prefs.edit {
            putString(KEY_SELECTED_SOUND_ID, sound.id)
            remove(KEY_SELECTED_UPLOADED_ID)
        }
    }

    fun saveSelectedUploadedTrack(trackId: String) {
        prefs.edit {
            putString(KEY_SELECTED_UPLOADED_ID, trackId)
        }
    }

    fun getAudioVolume(): Float {
        return prefs.getFloat(KEY_AUDIO_VOLUME, 0.65f).coerceIn(0f, 1f)
    }

    fun saveAudioVolume(volume: Float) {
        prefs.edit { putFloat(KEY_AUDIO_VOLUME, volume.coerceIn(0f, 1f)) }
    }

    fun isAudioLoopEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUDIO_LOOP, true)
    }

    fun saveAudioLoopEnabled(loop: Boolean) {
        prefs.edit { putBoolean(KEY_AUDIO_LOOP, loop) }
    }

    fun isAutoFullscreenTimerEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_FULLSCREEN_TIMER, false)
    }

    fun saveAutoFullscreenTimerEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_AUTO_FULLSCREEN_TIMER, enabled) }
    }

    fun isStudyAppLockEnabled(): Boolean {
        return prefs.getBoolean(KEY_STUDY_APP_LOCK_ENABLED, true)
    }

    fun saveStudyAppLockEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_STUDY_APP_LOCK_ENABLED, enabled) }
    }

    fun isBlockerServiceEnabled(): Boolean {
        if (BlockerService.isServiceConnected) return true

        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        val expectedComponent = ComponentName(context, BlockerService::class.java)
        val enabledByManager = am
            ?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            ?.any { info ->
                val si = info.resolveInfo?.serviceInfo
                si != null && si.packageName == context.packageName &&
                    si.name == BlockerService::class.java.name
            } == true
        if (enabledByManager) return true

        val enabledSetting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabledSetting.contains(expectedComponent.flattenToString()) ||
            enabledSetting.contains(expectedComponent.flattenToShortString()) ||
            enabledSetting.contains(context.packageName)
    }

    companion object {
        const val PREFS_NAME = "focus"
        const val KEY_ALLOWED = "allowed"
        const val KEY_END = "end"
        const val KEY_DURATION_MS = "duration_ms"
        const val KEY_USER_NAME = "user_name"
        const val KEY_DAILY_TASKS = "daily_tasks"
        const val KEY_TASKS_DATE = "tasks_date"

        const val KEY_SESSION_START_MS = "session_start_ms"
        const val KEY_LAST_ACCOUNTED_MS = "last_accounted_ms"
        const val KEY_STUDY_DATE = "study_date"
        const val KEY_DAILY_STUDY_MS = "daily_study_ms"
        const val KEY_TOTAL_STUDY_MS = "total_study_ms"
        const val KEY_UNPLANTED_STUDY_MS = "unplanted_study_ms"
        const val KEY_TREES_JSON = "tree_farm_json"
        const val KEY_ACHIEVEMENT_TASKS_DATE = "achievement_tasks_date"
        const val KEY_ACHIEVEMENT_HOURS_DATE = "achievement_hours_date"

        const val KEY_CURRENT_WEEK_KEY = "current_week_key"
        const val KEY_WEEKLY_STUDY_MS = "weekly_study_ms"
        const val KEY_WEEKLY_TREES_COUNT = "weekly_trees_count"
        const val KEY_WEEKLY_ACTIVE_DAYS = "weekly_active_days"
        const val KEY_CURRENT_MONTH_KEY = "current_month_key"
        const val KEY_MONTHLY_STUDY_MS = "monthly_study_ms"
        const val KEY_MONTHLY_TREES_COUNT = "monthly_trees_count"
        const val KEY_MONTHLY_ACTIVE_DAYS = "monthly_active_days"
        const val KEY_DAILY_HISTORY_JSON = "daily_history_json"
        const val KEY_WEEKLY_ARCHIVE_JSON = "weekly_archive_json"
        const val KEY_MONTHLY_ARCHIVE_JSON = "monthly_archive_json"
        const val KEY_HAS_UNSEEN_SESSION_FINISHED = "has_unseen_session_finished"
        const val KEY_LAST_SESSION_FINISHED_DURATION_MS = "last_session_finished_duration_ms"

        const val KEY_UPLOADED_TRACKS = "uploaded_audio_tracks"
        const val KEY_SELECTED_SOUND_ID = "selected_sound_id"
        const val KEY_SELECTED_UPLOADED_ID = "selected_uploaded_id"
        const val KEY_AUDIO_VOLUME = "audio_volume"
        const val KEY_AUDIO_LOOP = "audio_loop"
        const val KEY_AUTO_FULLSCREEN_TIMER = "auto_fullscreen_timer"
        const val KEY_STUDY_APP_LOCK_ENABLED = "study_app_lock_enabled"
        const val KEY_IS_STOPWATCH = "is_stopwatch"

        const val ONE_HOUR_MS = 60L * 60_000L
        const val FOUR_HOURS_MS = 4L * ONE_HOUR_MS

        private val ARABIC_MONTHS = listOf(
            "يناير",
            "فبراير",
            "مارس",
            "أبريل",
            "مايو",
            "يونيو",
            "يوليو",
            "أغسطس",
            "سبتمبر",
            "أكتوبر",
            "نوفمبر",
            "ديسمبر"
        )

        fun formatMonthLabelArabic(monthKey: String): String {
            val parts = monthKey.split("-")
            if (parts.size != 2) return monthKey
            val year = parts[0]
            val monthIdx = (parts[1].toIntOrNull() ?: return monthKey) - 1
            val monthName = ARABIC_MONTHS.getOrNull(monthIdx) ?: return monthKey
            return "$monthName $year"
        }

        fun formatWeekLabelArabic(startDateKey: String, endDateKey: String): String {
            val sParts = startDateKey.split("-")
            val eParts = endDateKey.split("-")
            return if (sParts.size == 3 && eParts.size == 3) {
                "${sParts[2]}/${sParts[1]} – ${eParts[2]}/${eParts[1]}/${eParts[0]}"
            } else {
                "$startDateKey – $endDateKey"
            }
        }

        private val ARABIC_WEEKDAYS = mapOf(
            Calendar.SATURDAY to "السبت",
            Calendar.SUNDAY to "الأحد",
            Calendar.MONDAY to "الإثنين",
            Calendar.TUESDAY to "الثلاثاء",
            Calendar.WEDNESDAY to "الأربعاء",
            Calendar.THURSDAY to "الخميس",
            Calendar.FRIDAY to "الجمعة"
        )

        fun formatFullDateArabic(nowMs: Long = System.currentTimeMillis()): String {
            val cal = Calendar.getInstance().apply { timeInMillis = nowMs }
            val dayName = ARABIC_WEEKDAYS[cal.get(Calendar.DAY_OF_WEEK)] ?: ""
            val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
            val monthName = ARABIC_MONTHS.getOrNull(cal.get(Calendar.MONTH)) ?: ""
            val year = cal.get(Calendar.YEAR)
            return "$dayName، $dayOfMonth $monthName $year"
        }

        fun formatFullDateArabicFromKey(dateKey: String): String {
            val parts = dateKey.split("-")
            if (parts.size != 3) return dateKey
            val year = parts[0].toIntOrNull() ?: return dateKey
            val month = parts[1].toIntOrNull() ?: return dateKey
            val day = parts[2].toIntOrNull() ?: return dateKey
            val cal = Calendar.getInstance().apply {
                clear()
                set(year, month - 1, day, 12, 0, 0)
            }
            return formatFullDateArabic(cal.timeInMillis)
        }
    }
}
