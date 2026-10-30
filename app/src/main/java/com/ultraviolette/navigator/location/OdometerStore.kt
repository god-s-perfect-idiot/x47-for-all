package com.ultraviolette.navigator.location

import android.content.Context

/**
 * Lifetime odometer meters — survives process death.
 * Trip distance stays in memory only (resets each app session).
 */
class OdometerStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun readTotalMeters(): Double =
        prefs.getFloat(KEY_TOTAL_METERS, 0f).toDouble().coerceAtLeast(0.0)

    fun writeTotalMeters(meters: Double) {
        prefs.edit()
            .putFloat(KEY_TOTAL_METERS, meters.coerceAtLeast(0.0).toFloat())
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "cluster_odometer"
        const val KEY_TOTAL_METERS = "total_meters"
    }
}
