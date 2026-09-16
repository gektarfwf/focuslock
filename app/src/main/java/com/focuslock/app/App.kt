package com.focuslock.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel("focuslock", "FocusLock", NotificationManager.IMPORTANCE_LOW)
            )
        }
        if (Prefs.isActive(this)) {
            Blocker.applyAll(this, Prefs.blocked(this), true)
            TimerService.start(this, Prefs.unlockAt(this))
        }
    }
}