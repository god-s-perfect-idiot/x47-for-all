package com.ultraviolette.navigator.model

import com.ultraviolette.navigator.weather.WeatherCondition

/**
 * Placeholder telemetry for the first build.
 * Defaults mirror docs/screen-reference-2.jpg (COMBAT).
 */
data class ClusterPlaceholders(
    val speedKmh: String = "———",
    val speedUnitLeft: String = "km",
    val speedUnitRight: String = "h",
    /** True while speed reports motion. */
    val isMoving: Boolean = false,
    /** True while a recent GNSS-quality fix is available. */
    val gpsAvailable: Boolean = false,
    /** Lifetime distance — overwritten live from GPS + persisted store. */
    val odometerKm: String = "0.0 km",
    /** Current trip distance — overwritten live; resets each app session. */
    val tripKm: String = "0.0 km",
    /** Trip average speed — overwritten live from distance ÷ elapsed trip time. */
    val avgSpeedKmh: String = "———",
    /** Moving-only trip time (h:mm) — overwritten live while speed > 0. */
    val tripTimeText: String = "0:00",
    /** Device SoC — overwritten live from BatteryManager. */
    val batteryPercent: Int = 72,
    /** True while device is on charger — overwritten live from BatteryManager. */
    val isCharging: Boolean = false,
    /** Device wall clock — overwritten live from ClockRepository. */
    val timeText: String = "06:30 pm",
    /** Outdoor temp — overwritten live from Open-Meteo when location is available. */
    val ambientTempC: String = "--",
    val weatherCondition: WeatherCondition = WeatherCondition.CLEAR,
    val weatherIsDay: Boolean = true,
    /** Absolute lean degrees for the numeric readout — live from tilt sensor. */
    val leanAngleDeg: String = "0",
    /** Signed lean for the gauge needle (+ screen-right / − screen-left). */
    val leanAngleSignedDeg: Float = 0f,
    val rideMode: String = "COMBAT",
    val regenLevel: String = "R3",
    val regenCells: Int = 3,
    val efficiencyBars: Int = 10,
    /** Cellular bars 0–5 — overwritten live from ConnectivityRepository. */
    val signalBars: Int = 0,
    /** True while associated to Wi‑Fi — overwritten live. */
    val wifiOn: Boolean = false,
    /** Wi‑Fi strength arcs 0–3 — overwritten live when [wifiOn]. */
    val wifiBars: Int = 0,
    /** True while a Bluetooth device is connected — overwritten live; hide glyph when false. */
    val bluetoothOn: Boolean = false,
    val efficiencyLabel: String = "EFF",
    /** Ambient-light driven — true when the cluster should use the inverted dark palette. */
    val isDarkMode: Boolean = false
) {
    val isBallistic: Boolean get() = rideMode.equals("BALLISTIC", ignoreCase = true)
}
