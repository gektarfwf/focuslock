package com.focuslock.app

import android.content.Context

object Prefs {
    private const val FILE = "focuslock_prefs"
    private const val KEY_BLOCKED = "blocked_pkgs"
    private const val KEY_UNLOCK_AT = "unlock_at"

    private fun sp(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun blocked(ctx: Context): MutableSet<String> =
        sp(ctx).getStringSet(KEY_BLOCKED, emptySet())!!.toMutableSet()

    fun setBlocked(ctx: Context, pkgs: Set<String>) {
        sp(ctx).edit().putStringSet(KEY_BLOCKED, pkgs).apply()
    }

    fun unlockAt(ctx: Context): Long = sp(ctx).getLong(KEY_UNLOCK_AT, 0L)

    fun setUnlockAt(ctx: Context, at: Long) {
        sp(ctx).edit().putLong(KEY_UNLOCK_AT, at).apply()
    }

    fun isActive(ctx: Context): Boolean {
        val at = unlockAt(ctx)
        return at > System.currentTimeMillis() && blocked(ctx).isNotEmpty()
    }

    fun clear(ctx: Context) {
        sp(ctx).edit().remove(KEY_UNLOCK_AT).apply()
    }
}