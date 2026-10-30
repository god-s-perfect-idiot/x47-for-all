package com.ultraviolette.navigator.location

import kotlin.math.abs

/**
 * 1D Kalman filter on ground speed (m/s).
 *
 * Predict step uses short-term forward acceleration from the IMU.
 * Update step uses GNSS Doppler speed with R from [speedAccuracyMetersPerSecond].
 * Accel is never integrated open-loop for long — covariance grows and GNSS
 * re-anchors the estimate.
 */
internal class SpeedKalman {
    /** Estimated speed (m/s). */
    var speedMps: Double = 0.0
        private set

    /** Estimate covariance (m²/s²). */
    private var p: Double = 16.0

    fun predict(aForwardMps2: Double, dtSec: Double) {
        if (dtSec <= 0.0 || dtSec > 0.25) return

        val a = aForwardMps2.coerceIn(-MAX_A, MAX_A)
        speedMps = (speedMps + a * dtSec).coerceIn(0.0, MAX_SPEED_MPS)

        // Process noise grows with |a| (bias / orientation error) and dt.
        val q = (Q_BASE + Q_ACC * a * a) * dtSec
        p += q
        p = p.coerceIn(MIN_P, MAX_P)
    }

    /**
     * GNSS measurement update.
     * @param measuredMps Doppler speed (m/s)
     * @param sigmaMps 1σ speed uncertainty from [Location.speedAccuracyMetersPerSecond]
     */
    fun update(measuredMps: Double, sigmaMps: Double) {
        val z = measuredMps.coerceIn(0.0, MAX_SPEED_MPS)
        val r = (sigmaMps * sigmaMps).coerceIn(MIN_R, MAX_R)
        val k = p / (p + r)
        speedMps = (speedMps + k * (z - speedMps)).coerceIn(0.0, MAX_SPEED_MPS)
        p = ((1.0 - k) * p).coerceIn(MIN_P, MAX_P)
    }

    /** Parked / confirmed stop — reset state so the dial cannot stick. */
    fun forceStop() {
        speedMps = 0.0
        p = 4.0
    }

    /** Inflate uncertainty when GNSS is stale so the next fix pulls harder. */
    fun inflateForGpsGap(dtSec: Double) {
        if (dtSec <= 0.0) return
        p = (p + Q_GPS_GAP * dtSec).coerceAtMost(MAX_P)
        // Mild bleed so a dead GNSS lock cannot leave us stuck at 21 km/h.
        if (dtSec > 0.5 && speedMps > 0.0) {
            speedMps *= (1.0 - 0.04 * dtSec).coerceIn(0.85, 1.0)
            if (speedMps < 0.3) speedMps = 0.0
        }
    }

    private companion object {
        const val MAX_SPEED_MPS = 280.0 / 3.6
        const val MAX_A = 6.0
        const val Q_BASE = 0.35
        const val Q_ACC = 0.15
        const val Q_GPS_GAP = 2.5
        const val MIN_P = 0.05
        const val MAX_P = 400.0
        const val MIN_R = 0.04
        const val MAX_R = 64.0
    }
}

/**
 * Display-only smoother so integer km/h does not chatter.
 * Does not feed back into the Kalman state.
 */
internal class SpeedDisplaySmoother {
    private var shownKmh: Double = 0.0

    fun smooth(targetKmh: Double): Float {
        val alpha = if (abs(targetKmh - shownKmh) > 8.0) 0.55 else 0.28
        shownKmh += (targetKmh - shownKmh) * alpha
        if (targetKmh < 1.0 && shownKmh < 1.5) shownKmh = 0.0
        return shownKmh.toFloat().coerceAtLeast(0f)
    }

    fun reset() {
        shownKmh = 0.0
    }
}
