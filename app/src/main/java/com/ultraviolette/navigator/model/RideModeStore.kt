package com.ultraviolette.navigator.model

import android.content.Context

/**
 * Persisted ride-mode theme (GLIDE / COMBAT / BALLISTIC) — survives process death
 * so reopen keeps the last chosen accent instead of resetting to COMBAT.
 */
class RideModeStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun readRideMode(default: String = RideMode.COMBAT): String =
        RideMode.canonicalize(prefs.getString(KEY_RIDE_MODE, null)) ?: default

    fun writeRideMode(mode: String) {
        val canonical = RideMode.canonicalize(mode) ?: return
        prefs.edit().putString(KEY_RIDE_MODE, canonical).apply()
    }

    private companion object {
        const val PREFS_NAME = "cluster_ride_mode"
        const val KEY_RIDE_MODE = "ride_mode"
    }
}
