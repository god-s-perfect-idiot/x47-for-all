package com.ultraviolette.navigator.location

import android.content.Context
import com.ultraviolette.navigator.model.RideMode
import com.ultraviolette.navigator.model.SavedTrip
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persisted ride history + in-progress draft.
 * Survives process death so unfinished rides are not lost.
 */
class TripStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun readTrips(): List<SavedTrip> {
        val raw = prefs.getString(KEY_TRIPS, null) ?: return emptyList()
        return runCatching { parseTrips(raw) }.getOrDefault(emptyList())
    }

    fun writeTrips(trips: List<SavedTrip>) {
        val capped = trips
            .sortedByDescending { it.endedAtEpochMs }
            .take(MAX_TRIPS)
        prefs.edit()
            .putString(KEY_TRIPS, encodeTrips(capped))
            .apply()
    }

    fun readDraft(): TripDraft? {
        val raw = prefs.getString(KEY_DRAFT, null) ?: return null
        return runCatching { parseDraft(raw) }.getOrNull()
    }

    fun writeDraft(draft: TripDraft) {
        prefs.edit()
            .putString(KEY_DRAFT, encodeDraft(draft))
            .apply()
    }

    fun clearDraft() {
        prefs.edit().remove(KEY_DRAFT).apply()
    }

    data class TripDraft(
        val startedAtEpochMs: Long,
        val distanceMeters: Double,
        val movingMs: Long,
        val rideMode: String,
        val efficiencyBars: Int,
        val disciplineBars: Int
    )

    private companion object {
        const val PREFS_NAME = "cluster_trips"
        const val KEY_TRIPS = "trips_json"
        const val KEY_DRAFT = "draft_json"
        const val MAX_TRIPS = 200

        fun encodeTrips(trips: List<SavedTrip>): String {
            val arr = JSONArray()
            trips.forEach { arr.put(encodeTrip(it)) }
            return arr.toString()
        }

        fun parseTrips(raw: String): List<SavedTrip> {
            val arr = JSONArray(raw)
            return buildList {
                for (i in 0 until arr.length()) {
                    add(parseTrip(arr.getJSONObject(i)))
                }
            }
        }

        fun encodeTrip(trip: SavedTrip): JSONObject =
            JSONObject()
                .put("id", trip.id)
                .put("startedAt", trip.startedAtEpochMs)
                .put("endedAt", trip.endedAtEpochMs)
                .put("meters", trip.distanceMeters)
                .put("movingMs", trip.movingMs)
                .put("rideMode", trip.rideMode)
                .put("eff", trip.efficiencyBars)
                .put("spd", trip.disciplineBars)

        fun parseTrip(obj: JSONObject): SavedTrip =
            SavedTrip(
                id = obj.getString("id"),
                startedAtEpochMs = obj.getLong("startedAt"),
                endedAtEpochMs = obj.getLong("endedAt"),
                distanceMeters = obj.getDouble("meters"),
                movingMs = obj.getLong("movingMs"),
                rideMode = RideMode.canonicalize(obj.optString("rideMode")) ?: RideMode.COMBAT,
                efficiencyBars = obj.optInt("eff", 0),
                disciplineBars = obj.optInt("spd", 0)
            )

        fun encodeDraft(draft: TripDraft): String =
            JSONObject()
                .put("startedAt", draft.startedAtEpochMs)
                .put("meters", draft.distanceMeters)
                .put("movingMs", draft.movingMs)
                .put("rideMode", draft.rideMode)
                .put("eff", draft.efficiencyBars)
                .put("spd", draft.disciplineBars)
                .toString()

        fun parseDraft(raw: String): TripDraft {
            val obj = JSONObject(raw)
            return TripDraft(
                startedAtEpochMs = obj.getLong("startedAt"),
                distanceMeters = obj.getDouble("meters"),
                movingMs = obj.getLong("movingMs"),
                rideMode = RideMode.canonicalize(obj.optString("rideMode")) ?: RideMode.COMBAT,
                efficiencyBars = obj.optInt("eff", 0),
                disciplineBars = obj.optInt("spd", 0)
            )
        }
    }
}
