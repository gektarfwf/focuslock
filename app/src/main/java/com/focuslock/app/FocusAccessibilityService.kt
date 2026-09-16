package com.focuslock.app

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class FocusAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val e = event ?: return
        if (e.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            e.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED) return

        if (!Prefs.isActive(this)) return

        val pkg = e.packageName?.toString() ?: return
        if (pkg == packageName) return
        if (pkg in SYSTEM_WHITELIST) return

        if (pkg in Prefs.blocked(this)) {
            val i = Intent(this, BlockActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(i)
        }
    }

    override fun onInterrupt() {}

    companion object {
        private val SYSTEM_WHITELIST = setOf(
            "com.android.systemui",
            "android",
            "com.android.settings",
            "com.android.phone",
            "com.android.dialer",
            "com.android.incallui",
            "com.google.android.dialer"
        )
    }
}