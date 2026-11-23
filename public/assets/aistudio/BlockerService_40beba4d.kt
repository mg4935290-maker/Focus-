package com.example.focuslock

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager

class BlockerService : AccessibilityService() {

    private fun alwaysAllowed(): Set<String> {
        val s = mutableSetOf("com.android.systemui", "android", packageName)
        listOf(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), Intent(Intent.ACTION_DIAL)).forEach {
            packageManager.resolveActivity(it, 0)?.activityInfo?.packageName?.let(s::add)
        }
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
            .enabledInputMethodList.forEach { s.add(it.packageName) }
        return s
    }

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        if (e?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = e.packageName?.toString() ?: return
        val p = getSharedPreferences("focus", MODE_PRIVATE)
        if (System.currentTimeMillis() >= p.getLong("end", 0L)) return
        val allowed = p.getStringSet("allowed", emptySet()) ?: emptySet()
        if (pkg in allowed || pkg in alwaysAllowed()) return
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    override fun onInterrupt() {}
}
