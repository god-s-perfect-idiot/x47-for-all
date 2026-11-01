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
    /** Current trip distance — overwritten live; sealed into trip history when the ride ends. */
    val tripKm: String = "0.0 km",
    /** Trip average speed — overwritten live from distance ÷ elapsed trip time. */
    val avgSpeedKmh: String = "———",
    /** Moving-only trip time (h:mm) — overwritten live while speed > 0. */
    val tripTimeText: String = "0:00",
    /** Device SoC — overwritten live from BatteryManager. */
    val batteryPercent: Int = 72,
    /** True while device is on charger — overwritten live from BatteryManager. */
    val isCharging: Boolean = false,
    /**
     * True when phone battery pack is hot (≥ 45 °C) — live from BatteryManager.
     * Drives the top-bar mode badge red while [isMoving].
     */
    val phoneTempHigh: Boolean = false,
    /** Device wall clock — overwritten live from ClockRepository. */
    val timeText: String = "06:30 pm",
    /** Outdoor temp — overwritten live from Open-Meteo when location is available. */
    val ambientTempC: String = "--",
    val weatherCondition: WeatherCondition = WeatherCondition.CLEAR,
    val weatherIsDay: Boolean = true,
    /** 10 m wind speed — overwritten live from Open-Meteo when location is available. */
    val windSpeedText: String = "--",
    /** GNSS MSL altitude — overwritten live from SpeedRepository when available. */
    val altitudeText: String = "--",
    /** Absolute lean degrees for the numeric readout — live from tilt sensor. */
    val leanAngleDeg: String = "0",
    /** Signed lean for the gauge needle (+ screen-right / − screen-left). */
    val leanAngleSignedDeg: Float = 0f,
    /** Display label for the bottom mode notch — GLIDE / COMBAT / BALLISTIC. */
    val rideMode: String = RideMode.COMBAT,
    /**
     * EFF filled stripe count 0–10 — live riding-smoothness score from
     * [com.ultraviolette.navigator.sensor.RidingSmoothnessRepository].
     */
    val efficiencyBars: Int = 0,
    /**
     * False while idle / stopped — EFF ticks render darker grey.
     * True only while moving with a score — right-side filled ticks use
     * the ride accent, left remainder stay light grey.
     */
    val efficiencyActive: Boolean = false,
    /**
     * SPD filled stripe count 0–10 — live speed-discipline score from
     * [com.ultraviolette.navigator.sensor.SpeedDisciplineRepository].
     */
    val disciplineBars: Int = 0,
    /**
     * False while idle / stopped — SPD ticks render darker grey.
     * True only while moving with a score — filled ticks use the ride accent.
     */
    val disciplineActive: Boolean = false,
    /** Cellular bars 0–5 — overwritten live from ConnectivityRepository. */
    val signalBars: Int = 0,
    /** True while associated to Wi‑Fi — overwritten live. */
    val wifiOn: Boolean = false,
    /** Wi‑Fi strength arcs 0–3 — overwritten live when [wifiOn]. */
    val wifiBars: Int = 0,
    /** True while a Bluetooth device is connected — overwritten live; hide glyph when false. */
    val bluetoothOn: Boolean = false,
    val efficiencyLabel: String = "EFF",
    val disciplineLabel: String = "SPD",
    /** Ambient-light driven — true when the cluster should use the inverted dark palette. */
    val isDarkMode: Boolean = false,
    /** Bike display name on the Control Centre identity tile — persisted via BikeNameStore. */
    val bikeName: String = BikeNameStore.DEFAULT_BIKE_NAME,
    /** Brand accent pack — UV / KTM / BMW / APRILIA — persisted via BrandThemeStore. */
    val brandTheme: String = BrandTheme.DEFAULT
) {
    val isGlide: Boolean get() = rideMode.equals(RideMode.GLIDE, ignoreCase = true)
    val isCombat: Boolean get() = rideMode.equals(RideMode.COMBAT, ignoreCase = true)
    val isBallistic: Boolean get() = rideMode.equals(RideMode.BALLISTIC, ignoreCase = true)
}

/** Tap-cycle order on the bottom mode bar: Glide → Combat → Ballistic. */
object RideMode {
    const val GLIDE = "GLIDE"
    const val COMBAT = "COMBAT"
    const val BALLISTIC = "BALLISTIC"

    private val order = listOf(GLIDE, COMBAT, BALLISTIC)

    /** Canonical label, or null when [raw] is not a known mode. */
    fun canonicalize(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return order.firstOrNull { it.equals(raw, ignoreCase = true) }
    }

    fun next(current: String): String {
        val idx = order.indexOfFirst { it.equals(current, ignoreCase = true) }
        return if (idx < 0) COMBAT else order[(idx + 1) % order.size]
    }
}
