package com.ultraviolette.navigator.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.os.SystemClock
import android.view.Display
import android.view.Surface
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

data class LeanSample(
    /**
     * Signed ride lean in degrees after display-axis remap + smoothing.
     * Positive = lean toward screen-right, negative = screen-left.
     */
    val leanAngleDeg: Float
)

/**
 * Phone roll → motorcycle lean for the cluster gauge.
 *
 * Uses gravity (accelerometer fallback) remapped to the current display
 * rotation so landscape left/right tilt reads as lean, independent of
 * which way `sensorLandscape` flipped the activity.
 *
 * Mount tilt is auto-zeroed: when the phone sits at a steady angle for
 * 15 seconds (bar mount recline, cradle skew), that angle becomes the
 * new upright and lean is reported relative to it.
 */
class LeanAngleRepository(context: Context) {
    private val appContext = context.applicationContext
    private val sensorManager =
        appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val displayManager =
        appContext.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager

    fun observeLean(): Flow<LeanSample> = callbackFlow {
        val gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val sensor = gravitySensor ?: accelerometer
        if (sensor == null) {
            trySend(LeanSample(0f))
            close()
            return@callbackFlow
        }

        val filter = LeanFilter()

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type != sensor.type) return
                if (event.values.size < 3) return

                val remapped = remapGravityToDisplay(
                    event.values[0],
                    event.values[1],
                    event.values[2],
                    displayRotation()
                )
                val dx = remapped[0]
                val dy = remapped[1]
                val dz = remapped[2]
                // Lateral bank vs down-in-plane — stable when the phone is
                // upright or slightly reclined like a bar-mounted cluster.
                val raw = Math.toDegrees(
                    atan2(dx.toDouble(), hypot(dy.toDouble(), dz.toDouble()))
                ).toFloat()
                val smoothed = filter.accept(raw)
                trySend(LeanSample(leanAngleDeg = smoothed))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        val registered = sensorManager.registerListener(
            listener,
            sensor,
            SensorManager.SENSOR_DELAY_GAME
        )
        if (!registered) {
            trySend(LeanSample(0f))
            close()
            return@callbackFlow
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }

    /** Application context has no Activity display — use DisplayManager. */
    private fun displayRotation(): Int =
        displayManager.getDisplay(Display.DEFAULT_DISPLAY)?.rotation
            ?: Surface.ROTATION_0

    private companion object {
        /**
         * Map device-frame gravity into display axes where +X is screen-right
         * and +Y is screen-up (Android natural → current rotation).
         */
        fun remapGravityToDisplay(
            gx: Float,
            gy: Float,
            gz: Float,
            rotation: Int
        ): FloatArray = when (rotation) {
            // Matches SensorManager.remapCoordinateSystem for each Surface rotation.
            Surface.ROTATION_90 -> floatArrayOf(gy, -gx, gz)
            Surface.ROTATION_180 -> floatArrayOf(-gx, -gy, gz)
            Surface.ROTATION_270 -> floatArrayOf(-gy, gx, gz)
            else -> floatArrayOf(gx, gy, gz)
        }
    }
}

/**
 * EMA + mount-zero: smooth the physical bank, then subtract a baseline that
 * locks in whenever the phone holds a steady tilt for [STABLE_MS].
 */
private class LeanFilter {
    private var ema: Float? = null
    /** Physical lean treated as upright (mount / cradle bias). */
    private var zeroOffsetDeg: Float = 0f
    /** Running candidate while tilt looks unchanged. */
    private var stableCandidateDeg: Float? = null
    private var stableSinceElapsedMs: Long = 0L

    fun accept(rawDeg: Float, nowElapsedMs: Long = SystemClock.elapsedRealtime()): Float {
        val clamped = rawDeg.coerceIn(-MAX_LEAN_DEG, MAX_LEAN_DEG)
        val previous = ema
        val smoothed = if (previous == null) {
            clamped
        } else {
            previous * (1f - EMA_ALPHA) + clamped * EMA_ALPHA
        }
        ema = smoothed

        updateMountZero(smoothed, nowElapsedMs)

        val relative = (smoothed - zeroOffsetDeg).coerceIn(-MAX_LEAN_DEG, MAX_LEAN_DEG)
        // Snap tiny residual to zero so idle / calibrated mount reads 0°.
        return if (abs(relative) < DEAD_ZONE_DEG) 0f else relative
    }

    private fun updateMountZero(smoothedDeg: Float, nowElapsedMs: Long) {
        val candidate = stableCandidateDeg
        if (candidate == null || abs(smoothedDeg - candidate) > STABLE_DELTA_DEG) {
            // Tilt moved — restart the settle window around the new angle.
            stableCandidateDeg = smoothedDeg
            stableSinceElapsedMs = nowElapsedMs
            return
        }

        // Keep the candidate as a light average so noise doesn't freeze an outlier.
        val settled = candidate * (1f - STABLE_BLEND) + smoothedDeg * STABLE_BLEND
        stableCandidateDeg = settled

        if (nowElapsedMs - stableSinceElapsedMs < STABLE_MS) return
        if (abs(settled - zeroOffsetDeg) < DEAD_ZONE_DEG) return
        zeroOffsetDeg = settled
    }

    private companion object {
        const val EMA_ALPHA = 0.22f
        const val MAX_LEAN_DEG = 60f
        const val DEAD_ZONE_DEG = 0.6f
        /** How long tilt must hold still before it becomes the new upright. */
        const val STABLE_MS = 15_000L
        /** Max wobble still counted as "unchanged" while settling. */
        const val STABLE_DELTA_DEG = 1.5f
        const val STABLE_BLEND = 0.15f
    }
}

fun formatLeanAngleDigits(leanAngleDeg: Float): String =
    kotlin.math.abs(leanAngleDeg).roundToInt().coerceIn(0, 99).toString()
