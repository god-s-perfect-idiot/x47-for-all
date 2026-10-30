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
    /** True while GPS reports motion — hides Control Centre. */
    val isMoving: Boolean = false,
    /** Lifetime distance — overwritten live from GPS + persisted store. */
    val odometerKm: String = "0.0 km",
    /** Current trip distance — overwritten live; resets each app session. */
    val tripKm: String = "0.0 km",
    val energyWhPerKm: String = "054",
    val energyRecommended: String = "52",
    val rangeKm: String = "159",
    /** Device SoC — overwritten live from BatteryManager. */
    val batteryPercent: Int = 72,
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
    val signalBars: Int = 3,
    val wifiOn: Boolean = true,
    val hillHoldOn: Boolean = false,
    val tractionControlOn: Boolean = true,
    val absOn: Boolean = true,
    val tirePressureWarn: Boolean = true,
    val efficiencyLabel: String = "EFF"
) {
    val isBallistic: Boolean get() = rideMode.equals("BALLISTIC", ignoreCase = true)
}
