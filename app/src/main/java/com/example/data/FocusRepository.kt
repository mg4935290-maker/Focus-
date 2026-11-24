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
    val sessionStartMs: Long = 0L
)

class FocusRepository(private val context: Context) {

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun currentDateKey(nowMs: Long = System.currentTimeMillis()): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(nowMs))
    }

    private fun startOfDayMs(nowMs: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
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

        var isStopwatch = prefs.getBoolean(KEY_IS_STOPWATCH, false)
        val sessionStartMs = prefs.getLong(KEY_SESSION_START_MS, 0L)
        val sessionEndMs = prefs.getLong(KEY_END, 0L)
        val lastAccountedMs = prefs.getLong(KEY_LAST_ACCOUNTED_MS, 0L)
            .coerceAtLeast(sessionStartMs)

        var newSessionStartMs = sessionStartMs
        var newSessionEndMs = sessionEndMs
        var newSessionDurationMs = prefs.getLong(KEY_DURATION_MS, 0L)
        var newLastAccountedMs = lastAccountedMs

        if (isStopwatch && sessionStartMs > 0L) {
            val effectiveEnd = nowMs.coerceAtLeast(lastAccountedMs)
            if (effectiveEnd > lastAccountedMs) {
                val deltaMs = effectiveEnd - lastAccountedMs
                val dayStart = startOfDayMs(nowMs)
                val todayDeltaMs = if (lastAccountedMs < dayStart) {
                    (effectiveEnd - dayStart).coerceAtLeast(0L)
                } else {
                    deltaMs
                }
                dailyStudyMs += todayDeltaMs
                totalStudyMs += deltaMs
                unplantedStudyMs += deltaMs

                while (unplantedStudyMs >= ONE_HOUR_MS) {
                    unplantedStudyMs -= ONE_HOUR_MS
                    val plantedAt = effectiveEnd - unplantedStudyMs
                    val variant = trees.size % 4
                    trees.add(
                        PlantedTree(
                            id = UUID.randomUUID().toString(),
                            plantedAtMs = plantedAt,
                            variant = variant
                        )
                    )
                }
                newLastAccountedMs = effectiveEnd
            }
        } else if (sessionStartMs > 0L && sessionEndMs > sessionStartMs) {
            val effectiveEnd = minOf(nowMs, sessionEndMs)
            if (effectiveEnd > lastAccountedMs) {
                val deltaMs = effectiveEnd - lastAccountedMs
                val dayStart = startOfDayMs(nowMs)
                val todayDeltaMs = if (lastAccountedMs < dayStart) {
                    (effectiveEnd - dayStart).coerceAtLeast(0L)
                } else {
                    deltaMs
                }
                dailyStudyMs += todayDeltaMs
                totalStudyMs += deltaMs
                unplantedStudyMs += deltaMs

                while (unplantedStudyMs >= ONE_HOUR_MS) {
                    unplantedStudyMs -= ONE_HOUR_MS
                    val plantedAt = effectiveEnd - unplantedStudyMs
                    val variant = trees.size % 4
                    trees.add(
                        PlantedTree(
                            id = UUID.randomUUID().toString(),
                            plantedAtMs = plantedAt,
                            variant = variant
                        )
                    )
                }
                newLastAccountedMs = effectiveEnd
            }

            if (nowMs >= sessionEndMs) {
                isStopwatch = false
                newSessionStartMs = 0L
                newSessionEndMs = 0L
                newSessionDurationMs = 0L
                newLastAccountedMs = 0L
            }
        }

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
        }

        return StudyProgressSnapshot(
            dailyStudyMs = dailyStudyMs,
            totalStudyMs = totalStudyMs,
            unplantedStudyMs = unplantedStudyMs,
            trees = trees,
            sessionEndMs = newSessionEndMs,
            sessionDurationMs = newSessionDurationMs,
            isStopwatch = isStopwatch,
            sessionStartMs = newSessionStartMs
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

        while (unplantedStudyMs >= ONE_HOUR_MS) {
            unplantedStudyMs -= ONE_HOUR_MS
            trees.add(
                PlantedTree(
                    id = UUID.randomUUID().toString(),
                    plantedAtMs = nowMs - unplantedStudyMs,
                    variant = trees.size % 4
                )
            )
        }

        prefs.edit {
            putString(KEY_STUDY_DATE, todayKey)
            putLong(KEY_DAILY_STUDY_MS, dailyStudyMs)
            putLong(KEY_TOTAL_STUDY_MS, totalStudyMs)
            putLong(KEY_UNPLANTED_STUDY_MS, unplantedStudyMs)
            putString(KEY_TREES_JSON, encodeTrees(trees))
        }

        return StudyProgressSnapshot(
            dailyStudyMs = dailyStudyMs,
            totalStudyMs = totalStudyMs,
            unplantedStudyMs = unplantedStudyMs,
            trees = trees,
            sessionEndMs = current.sessionEndMs,
            sessionDurationMs = current.sessionDurationMs,
            isStopwatch = current.isStopwatch,
            sessionStartMs = current.sessionStartMs
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
        return StudyProgressSnapshot(
            dailyStudyMs = dailyStudyMs,
            totalStudyMs = prefs.getLong(KEY_TOTAL_STUDY_MS, 0L),
            unplantedStudyMs = prefs.getLong(KEY_UNPLANTED_STUDY_MS, 0L),
            trees = loadTrees(),
            sessionEndMs = prefs.getLong(KEY_END, 0L),
            sessionDurationMs = prefs.getLong(KEY_DURATION_MS, 0L),
            isStopwatch = prefs.getBoolean(KEY_IS_STOPWATCH, false),
            sessionStartMs = prefs.getLong(KEY_SESSION_START_MS, 0L)
        )
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
    }
}
