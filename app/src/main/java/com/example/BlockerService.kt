package com.example

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import android.view.inputmethod.InputMethodManager
import com.example.data.FocusRepository

class BlockerService : AccessibilityService() {

    private var lastLaunchElapsedMs: Long = 0L

    private fun alwaysAllowed(): Set<String> {
        val allowed = mutableSetOf("com.android.systemui", "android", packageName)
        listOf(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            Intent(Intent.ACTION_DIAL)
        ).forEach { intent ->
            packageManager.resolveActivity(intent, 0)?.activityInfo?.packageName?.let(allowed::add)
        }
        val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.enabledInputMethodList?.forEach { inputMethod ->
            allowed.add(inputMethod.packageName)
        }
        return allowed
    }

    private fun resolveForegroundPackage(event: AccessibilityEvent?): String? {
        val activeWindowPkg = runCatching {
            windows?.firstOrNull { window ->
                window.type == AccessibilityWindowInfo.TYPE_APPLICATION &&
                    (window.isActive || window.isFocused)
            }?.root?.packageName?.toString()
        }.getOrNull()

        if (!activeWindowPkg.isNullOrBlank()) {
            return activeWindowPkg
        }

        val rootPkg = runCatching {
            rootInActiveWindow?.packageName?.toString()
        }.getOrNull()

        if (!rootPkg.isNullOrBlank()) {
            return rootPkg
        }

        return event?.packageName?.toString()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val eventType = event?.eventType ?: return
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) {
            return
        }

        val prefs = getSharedPreferences(FocusRepository.PREFS_NAME, MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (now >= prefs.getLong(FocusRepository.KEY_END, 0L)) {
            BlockedActivity.dismissIfOpen()
            return
        }

        val pkg = resolveForegroundPackage(event) ?: return
        val allowed = prefs.getStringSet(FocusRepository.KEY_ALLOWED, emptySet()) ?: emptySet()
        val systemAllowed = alwaysAllowed()

        if (pkg == packageName) {
            return
        }

        if (pkg in allowed) {
            BlockedActivity.dismissIfOpen()
            return
        }

        if (pkg in systemAllowed) {
            // Only dismiss when the user actually returned to the Home launcher (exited the blocked app).
            val homePkg = packageManager.resolveActivity(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
                0
            )?.activityInfo?.packageName
            if (pkg == homePkg) {
                BlockedActivity.dismissIfOpen()
            }
            return
        }

        val elapsedNow = SystemClock.elapsedRealtime()
        if (BlockedActivity.isForeground && elapsedNow - lastLaunchElapsedMs < 400L) return
        if (!BlockedActivity.isForeground && elapsedNow - lastLaunchElapsedMs < 120L) return
        lastLaunchElapsedMs = elapsedNow

        val blockIntent = Intent(this, BlockedActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            )
        }
        startActivity(blockIntent)
    }

    override fun onInterrupt() {}
}
