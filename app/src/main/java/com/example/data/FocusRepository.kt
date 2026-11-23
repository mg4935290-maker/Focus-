package com.example.data

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.example.BlockerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

class FocusRepository(private val context: Context) {

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun currentDateKey(nowMs: Long = System.currentTimeMillis()): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(nowMs))
    }

    suspend fun loadInstalledApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(intent, 0)
            .asSequence()
            .mapNotNull { resolveInfo ->
                val pkg = resolveInfo.activityInfo?.packageName ?: return@mapNotNull null
                if (pkg == context.packageName) return@mapNotNull null
                val label = resolveInfo.loadLabel(pm)?.toString()?.ifBlank { pkg } ?: pkg
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
        prefs.edit()
            .putString(KEY_USER_NAME, name.trim())
            .apply()
    }

    fun getAllowedPackages(): Set<String> {
        return prefs.getStringSet(KEY_ALLOWED, emptySet())?.toSet() ?: emptySet()
    }

    fun getSessionEndMs(): Long {
        return prefs.getLong(KEY_END, 0L)
    }

    fun getSessionDurationMs(): Long {
        return prefs.getLong(KEY_DURATION_MS, 0L)
    }

    fun startSession(allowedPackages: Set<String>, durationMinutes: Long): Pair<Long, Long> {
        val durationMs = durationMinutes * 60_000L
        val endMs = System.currentTimeMillis() + durationMs
        prefs.edit()
            .putStringSet(KEY_ALLOWED, allowedPackages.toSet())
            .putLong(KEY_END, endMs)
            .putLong(KEY_DURATION_MS, durationMs)
            .apply()
        return endMs to durationMs
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
        prefs.edit()
            .putString(KEY_DAILY_TASKS, array.toString())
            .putString(KEY_TASKS_DATE, todayKey)
            .apply()
    }

    fun getRewardPackage(): String? {
        return prefs.getString(KEY_REWARD_PACKAGE, null)?.takeIf { it.isNotBlank() }
    }

    fun setRewardPackage(packageName: String) {
        prefs.edit()
            .putString(KEY_REWARD_PACKAGE, packageName)
            .apply()
    }

    fun isRewardClaimedToday(todayKey: String = currentDateKey()): Boolean {
        return prefs.getString(KEY_REWARD_CLAIMED_DATE, null) == todayKey
    }

    fun getRewardEndMs(): Long {
        return prefs.getLong(KEY_REWARD_END, 0L)
    }

    fun claimDailyHourReward(
        rewardPackage: String,
        nowMs: Long = System.currentTimeMillis()
    ): Long {
        val todayKey = currentDateKey(nowMs)
        val endMs = nowMs + ONE_HOUR_MS
        prefs.edit()
            .putString(KEY_REWARD_PACKAGE, rewardPackage)
            .putLong(KEY_REWARD_END, endMs)
            .putString(KEY_REWARD_CLAIMED_DATE, todayKey)
            .apply()
        return endMs
    }

    fun isBlockerServiceEnabled(): Boolean {
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
        const val KEY_REWARD_PACKAGE = "reward_package"
        const val KEY_REWARD_END = "reward_end"
        const val KEY_REWARD_CLAIMED_DATE = "reward_claimed_date"
        const val ONE_HOUR_MS = 60L * 60_000L
    }
}
