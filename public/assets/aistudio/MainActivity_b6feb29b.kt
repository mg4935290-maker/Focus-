package com.example.focuslock

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.widget.*

class MainActivity : Activity() {
    private val checks = mutableListOf<Pair<CheckBox, String>>()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val prefs = getSharedPreferences("focus", MODE_PRIVATE)
        val saved = prefs.getStringSet("allowed", emptySet()) ?: emptySet()

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 64, 32, 32) }
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val minutes = EditText(this).apply { hint = "المدة بالدقائق"; inputType = InputType.TYPE_CLASS_NUMBER }

        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        packageManager.queryIntentActivities(i, 0)
            .map { it.activityInfo.packageName to it.loadLabel(packageManager).toString() }
            .distinctBy { it.first }.filter { it.first != packageName }.sortedBy { it.second.lowercase() }
            .forEach { (pkg, label) ->
                val cb = CheckBox(this).apply { text = label; isChecked = pkg in saved }
                checks.add(cb to pkg); list.addView(cb)
            }

        val enable = Button(this).apply {
            text = "1) تفعيل خدمة الحظر"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }
        val start = Button(this).apply { text = "2) ابدأ الجلسة" }
        start.setOnClickListener {
            val m = minutes.text.toString().toLongOrNull() ?: 0L
            val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
                ?.contains(packageName) == true
            when {
                m <= 0 -> toast("اكتب مدة صحيحة")
                !enabled -> { toast("فعّل الخدمة أولاً"); startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                else -> {
                    prefs.edit()
                        .putStringSet("allowed", checks.filter { it.first.isChecked }.map { it.second }.toSet())
                        .putLong("end", System.currentTimeMillis() + m * 60_000).apply()
                    toast("بدأت الجلسة لمدة $m دقيقة")
                    startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
        }

        root.addView(TextView(this).apply { text = "اختر التطبيقات المسموحة:"; textSize = 18f })
        root.addView(ScrollView(this).apply { addView(list) }, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(minutes); root.addView(enable); root.addView(start)
        setContentView(root)
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()
}
