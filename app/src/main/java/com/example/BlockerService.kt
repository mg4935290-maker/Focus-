package com.example

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager

class BlockerService : AccessibilityService() {

    private var lastBlockedPkg: String? = null
    private var lastBlockedElapsedMs: Long = 0L

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

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        val prefs = getSharedPreferences("focus", MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (now >= prefs.getLong("end", 0L)) return

        val allowed = prefs.getStringSet("allowed", emptySet()) ?: emptySet()
        if (pkg in allowed || pkg in alwaysAllowed()) return

        val elapsedNow = SystemClock.elapsedRealtime()
        if (pkg == lastBlockedPkg && elapsedNow - lastBlockedElapsedMs < 450L) return
        lastBlockedPkg = pkg
        lastBlockedElapsedMs = elapsedNow

        performGlobalAction(GLOBAL_ACTION_HOME)
        val blockIntent = Intent(this, BlockedActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        }
        startActivity(blockIntent)
    }

    override fun onInterrupt() {}
}
