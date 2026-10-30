package com.ultraviolette.navigator.model

/**
 * Placeholder telemetry for the first build.
 * Defaults mirror docs/screen-reference-2.jpg (COMBAT).
 */
data class ClusterPlaceholders(
    val speedKmh: String = "---",
    val speedUnitLeft: String = "km",
    val speedUnitRight: String = "h",
    val odometerKm: String = "12 km",
    val tripKm: String = "0 km",
    val energyWhPerKm: String = "054",
    val energyRecommended: String = "52",
    val rangeKm: String = "159",
    val batteryPercent: Int = 72,
    val timeText: String = "06:30 pm",
    val ambientTempC: String = "24",
    val leanAngleDeg: String = "1",
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
