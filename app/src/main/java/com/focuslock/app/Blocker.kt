package com.focuslock.app

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build

object Blocker {

    fun dpm(ctx: Context): DevicePolicyManager =
        ctx.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    fun admin(ctx: Context): ComponentName =
        ComponentName(ctx, FocusDeviceAdminReceiver::class.java)

    fun isDeviceOwner(ctx: Context): Boolean =
        dpm(ctx).isDeviceOwnerApp(ctx.packageName)

    fun applySelfProtection(ctx: Context, enable: Boolean) {
        if (!isDeviceOwner(ctx)) return
        try {
            if (Build.VERSION.SDK_INT >= 23) {
                dpm(ctx).setUninstallBlocked(admin(ctx), ctx.packageName, enable)
            }
        } catch (_: Throwable) {}
    }

    fun applyHiddenState(ctx: Context, pkgs: Set<String>, hide: Boolean) {
        if (!isDeviceOwner(ctx)) return
        val d = dpm(ctx)
        val c = admin(ctx)
        pkgs.forEach { pkg ->
            try {
                d.setApplicationHidden(c, pkg, hide)
            } catch (_: Throwable) {}
        }
    }

    fun applyAll(ctx: Context, pkgs: Set<String>, hide: Boolean) {
        applyHiddenState(ctx, pkgs, hide)
        applySelfProtection(ctx, hide)
    }
}