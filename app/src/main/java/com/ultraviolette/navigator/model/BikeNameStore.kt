package com.ultraviolette.navigator.model

import android.content.Context

/**
 * Persisted bike display name for the Control Centre identity tile —
 * survives process death so reopen keeps the last chosen name.
 */
class BikeNameStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun readBikeName(default: String = DEFAULT_BIKE_NAME): String {
        val stored = prefs.getString(KEY_BIKE_NAME, null)?.trim().orEmpty()
        return stored.ifEmpty { default }
    }

    fun writeBikeName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        prefs.edit().putString(KEY_BIKE_NAME, trimmed).apply()
    }

    companion object {
        const val DEFAULT_BIKE_NAME = "MY X-47"
        private const val PREFS_NAME = "cluster_bike_name"
        private const val KEY_BIKE_NAME = "bike_name"
    }
}
