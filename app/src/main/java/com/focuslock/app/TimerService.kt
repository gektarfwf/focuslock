package com.focuslock.app

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class TimerService : Service() {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotif("Фокус активен"))
        val until = Prefs.unlockAt(this)
        job?.cancel()
        job = scope.launch {
            while (isActive) {
                val left = until - System.currentTimeMillis()
                if (left <= 0) {
                    withContext(Dispatchers.Main) { finishLock() }
                    return@launch
                }
                updateNotif(left)
                delay(1000)
            }
        }
        return START_STICKY
    }

    private fun finishLock() {
        val pkgs = Prefs.blocked(this)
        Blocker.applyAll(this, pkgs, false)
        Prefs.clear(this)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotif(text: String): Notification {
        val ch = "focuslock"
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(ch) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(ch, "FocusLock", NotificationManager.IMPORTANCE_LOW)
                )
            }
        }
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, ch)
            .setContentTitle("FocusLock")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .setContentIntent(pi)
            .build()
    }

    private fun updateNotif(leftMs: Long) {
        val s = leftMs / 1000
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        val text = String.format("Осталось %02d:%02d:%02d", h, m, sec)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotif(text))
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val NOTIF_ID = 4711
        fun start(ctx: Context, until: Long) {
            Prefs.setUnlockAt(ctx, until)
            val i = Intent(ctx, TimerService::class.java)
            if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i)
            else ctx.startService(i)
        }
        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, TimerService::class.java))
        }
    }
}