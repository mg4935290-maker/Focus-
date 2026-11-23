package com.example

import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.example.data.BuiltInSound
import com.example.data.DailyTask
import com.example.data.FocusRepository
import com.example.data.UploadedAudioTrack
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
    fun `required exact strings match specification`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Focus Lock", context.getString(R.string.app_name))
        assertEquals("created by Galileo", context.getString(R.string.created_by_galileo))
        assertEquals(
            "حلمك يستاهل انك تحارب عشانه ،ارجع ذاكر🩵🩵",
            context.getString(R.string.blocked_screen_message)
        )
        assertEquals(
            "عاش ياملك يابن الملوك باموووووو🤯🤯🤯",
            context.getString(R.string.achievement_message)
        )
        assertEquals("تفعيل خدمة الحظر", context.getString(R.string.btn_enable_service))
        assertEquals("ابدأ الجلسة", context.getString(R.string.btn_start_session))
        assertEquals("صلي على النبي ﷺ", context.getString(R.string.salawat_phrase))
        assertEquals("تأكيد", context.getString(R.string.action_confirm_apps))
        assertEquals("الإحصائيات", context.getString(R.string.home_stats_title))
        assertEquals("تأكيد التطبيقات المسموحة", context.getString(R.string.confirm_apps_title))
    }

    @Test
    fun `allowed packages confirmation persists independently of starting session`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = FocusRepository(context)
        val selection = setOf("com.example.reader", "com.example.notes")

        repo.saveAllowedPackages(selection)
        assertEquals(selection, repo.getAllowedPackages())
        assertEquals(15, com.example.notifications.SalawatReminderScheduler.AUTO_INTERVAL_MINUTES)
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
    fun `tree farm plants one tree only per full completed hour`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = FocusRepository(context)
        val t0 = 1_700_000_000_000L

        // 40-minute session -> 0 trees planted, 40m carried
        repo.startSession(emptySet(), durationMinutes = 40L, nowMs = t0)
        val after40m = repo.syncStudyProgress(t0 + 40L * 60_000L)
        assertEquals(0, after40m.trees.size)
        assertEquals(40L * 60_000L, after40m.unplantedStudyMs)

        // Next 2h 30m session (total 3h 10m) -> 3 trees planted, 10m carried
        val t1 = t0 + 50L * 60_000L
        repo.startSession(emptySet(), durationMinutes = 150L, nowMs = t1)
        val afterLongSession = repo.syncStudyProgress(t1 + 150L * 60_000L)
        assertEquals(3, afterLongSession.trees.size)
        assertEquals(10L * 60_000L, afterLongSession.unplantedStudyMs)
        assertEquals(190L * 60_000L, afterLongSession.totalStudyMs)
    }

    @Test
    fun `achievements trigger once per day per condition`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = FocusRepository(context)
        val day1 = 1_700_000_000_000L
        val day2 = day1 + 24L * 3_600_000L

        assertFalse(repo.tryConsumeTasksAchievement(allTasksCompleted = false, nowMs = day1))
        assertTrue(repo.tryConsumeTasksAchievement(allTasksCompleted = true, nowMs = day1))
        assertFalse(repo.tryConsumeTasksAchievement(allTasksCompleted = true, nowMs = day1))
        assertTrue(repo.tryConsumeTasksAchievement(allTasksCompleted = true, nowMs = day2))

        assertFalse(repo.tryConsumeHoursAchievement(dailyStudyMs = FocusRepository.FOUR_HOURS_MS, nowMs = day1))
        assertTrue(repo.tryConsumeHoursAchievement(dailyStudyMs = FocusRepository.FOUR_HOURS_MS + 1000L, nowMs = day1))
        assertFalse(repo.tryConsumeHoursAchievement(dailyStudyMs = FocusRepository.FOUR_HOURS_MS + 60_000L, nowMs = day1))
        assertTrue(repo.tryConsumeHoursAchievement(dailyStudyMs = FocusRepository.FOUR_HOURS_MS + 1000L, nowMs = day2))
    }

    @Test
    fun `uploaded audio tracks and built-in selection persist`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = FocusRepository(context)
        val tracks = listOf(
            UploadedAudioTrack(id = "t1", uriString = "content://audio/1", displayName = "سورة الكهف.mp3")
        )
        repo.saveUploadedTracks(tracks)
        repo.saveSelectedUploadedTrack("t1")
        assertEquals(tracks, repo.loadUploadedTracks())
        assertEquals("t1", repo.getSelectedUploadedTrackId())

        repo.saveSelectedBuiltInSound(BuiltInSound.GAMMA)
        assertEquals(BuiltInSound.GAMMA.id, repo.getSelectedBuiltInSoundId())
        assertEquals(null, repo.getSelectedUploadedTrackId())
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
