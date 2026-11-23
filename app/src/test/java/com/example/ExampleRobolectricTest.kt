package com.example

import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.example.data.DailyTask
import com.example.data.FocusRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Focus Lock", appName)
        assertEquals("created by Galileo", context.getString(R.string.created_by_galileo))
    }

    @Test
    fun `saveUserName trims and persists name`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = FocusRepository(context)

        assertEquals("", repo.getUserName())
        repo.saveUserName("  Galileo  ")
        assertEquals("Galileo", repo.getUserName())
    }

    @Test
    fun `daily tasks persist and reset completion on new day`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = FocusRepository(context)
        val tasks = listOf(
            DailyTask(id = "1", title = "مراجعة الفصل الأول", isCompleted = true),
            DailyTask(id = "2", title = "حل تمارين الرياضيات", isCompleted = false)
        )

        repo.saveDailyTasks(tasks, todayKey = "2026-11-23")
        val sameDay = repo.loadDailyTasks(todayKey = "2026-11-23")
        assertEquals(2, sameDay.size)
        assertTrue(sameDay[0].isCompleted)

        val nextDay = repo.loadDailyTasks(todayKey = "2026-11-24")
        assertEquals(2, nextDay.size)
        assertFalse(nextDay[0].isCompleted)
        assertFalse(nextDay[1].isCompleted)
    }

    @Test
    fun `claimDailyHourReward sets one hour unlock and marks today claimed`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = FocusRepository(context)
        val now = 1_700_000_000_000L
        val todayKey = repo.currentDateKey(now)

        assertFalse(repo.isRewardClaimedToday(todayKey))
        val endMs = repo.claimDailyHourReward("com.google.android.youtube", nowMs = now)

        assertEquals(now + FocusRepository.ONE_HOUR_MS, endMs)
        assertEquals("com.google.android.youtube", repo.getRewardPackage())
        assertTrue(repo.isRewardClaimedToday(todayKey))
        assertFalse(repo.isRewardClaimedToday("2099-01-01"))
    }

    @Test
    fun `startSession persists allowed packages and future end time`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = FocusRepository(context)
        val allowed = setOf("com.android.calculator2", "com.whatsapp")

        val (endMs, durationMs) = repo.startSession(allowed, 15L)

        assertEquals(allowed, repo.getAllowedPackages())
        assertEquals(15L * 60_000L, durationMs)
        assertEquals(endMs, repo.getSessionEndMs())
        assertTrue(endMs > System.currentTimeMillis())
    }

    @Test
    fun `isBlockerServiceEnabled detects enabled accessibility service setting`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = FocusRepository(context)

        assertFalse(repo.isBlockerServiceEnabled())

        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            "${context.packageName}/${BlockerService::class.java.name}"
        )

        assertTrue(repo.isBlockerServiceEnabled())
    }
}
