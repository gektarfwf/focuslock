package com.focuslock.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent?) {
        if (Prefs.isActive(ctx)) {
            val pkgs = Prefs.blocked(ctx)
            Blocker.applyAll(ctx, pkgs, true)
            TimerService.start(ctx, Prefs.unlockAt(ctx))
        } else {
            val pkgs = Prefs.blocked(ctx)
            Blocker.applyAll(ctx, pkgs, false)
            Prefs.clear(ctx)
        }
    }
}