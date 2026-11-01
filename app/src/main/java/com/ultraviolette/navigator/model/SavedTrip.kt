package com.ultraviolette.navigator.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

/**
 * One completed ride — distance, moving time, and scores at the moment it ended.
 */
data class SavedTrip(
    val id: String = UUID.randomUUID().toString(),
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long,
    val distanceMeters: Double,
    val movingMs: Long,
    val rideMode: String = RideMode.COMBAT,
    val efficiencyBars: Int = 0,
    val disciplineBars: Int = 0
) {
    val distanceKmText: String get() = formatDistanceKm(distanceMeters)
    val avgSpeedText: String get() = formatAvgSpeed(distanceMeters, movingMs)
    val durationText: String get() = formatTripTime(movingMs)
    val whenText: String get() = DAY_TIME.format(Date(startedAtEpochMs))
    val dateText: String get() = DAY_ONLY.format(Date(startedAtEpochMs))
    val timeRangeText: String
        get() {
            val start = CLOCK.format(Date(startedAtEpochMs))
            val end = CLOCK.format(Date(endedAtEpochMs))
            return "$start – $end"
        }

    companion object {
        private val DAY_TIME = SimpleDateFormat("d MMM · h:mm a", Locale.US)
        private val DAY_ONLY = SimpleDateFormat("d MMM yyyy", Locale.US)
        private val CLOCK = SimpleDateFormat("h:mm a", Locale.US)

        /** Minimum distance before a ride is kept in history. */
        const val MIN_SAVE_METERS = 50.0

        fun formatDistanceKm(meters: Double): String {
            val km = (meters / 1000.0).coerceAtLeast(0.0)
            return if (km < 10.0) {
                "%.1f km".format(km)
            } else {
                "%d km".format(km.roundToInt())
            }
        }

        fun formatAvgSpeed(meters: Double, movingMs: Long): String {
            if (movingMs < 1_000L || meters <= 0.0) return "——"
            val hours = movingMs / 3_600_000.0
            val kmh = ((meters / 1000.0) / hours).roundToInt().coerceIn(0, 999)
            return "%d".format(kmh)
        }

        fun formatTripTime(movingMs: Long): String {
            val totalMin = (movingMs / 60_000L).coerceAtLeast(0L)
            val hours = totalMin / 60L
            val minutes = totalMin % 60L
            return "%d:%02d".format(hours, minutes)
        }
    }
}
