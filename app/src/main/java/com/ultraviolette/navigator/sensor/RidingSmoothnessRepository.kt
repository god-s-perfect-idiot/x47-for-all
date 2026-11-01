package com.ultraviolette.navigator.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Display
import android.view.Surface
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Live riding-smoothness score (0–10) for the EFF stripe bar.
 *
 * Four weighted components (acceleration / speed / cornering / vibration),
 * refreshed once per second from phone IMU + GPS speed pushed by the
 * ViewModel. This is smoothness, not fuel efficiency — see scoring notes
 * in the companion object.
 */
data class SmoothnessSample(
    /** Rolling score in 0–10. */
    val score: Float,
    /** Discrete EFF stripes 0–10. */
    val bars: Int,
    /** True when enough moving-window data was available to trust [score]. */
    val confident: Boolean
)

class RidingSmoothnessRepository(context: Context) {
    private val appContext = context.applicationContext
    private val sensorManager =
        appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val displayManager =
        appContext.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager

    @Volatile
    private var latestSpeedKmh: Float = 0f

    /** Feed GPS / fused speed from [com.ultraviolette.navigator.ui.ClusterViewModel]. */
    fun updateSpeed(speedKmh: Float, isMoving: Boolean) {
        latestSpeedKmh = if (isMoving) speedKmh.coerceAtLeast(0f) else 0f
    }

    fun observeSmoothness(): Flow<SmoothnessSample> = callbackFlow {
        val linearSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        if (linearSensor == null) {
            trySend(SmoothnessSample(score = 0f, bars = 0, confident = false))
            close()
            return@callbackFlow
        }

        val engine = SmoothnessEngine(hasGyro = gyroSensor != null)
        var lastAccelNanos = 0L
        var lastGyroNanos = 0L
        var gravityX = 0f
        var gravityY = 0f
        var gravityZ = SensorManager.GRAVITY_EARTH
        var hasGravity = linearSensor.type == Sensor.TYPE_LINEAR_ACCELERATION

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_LINEAR_ACCELERATION,
                    Sensor.TYPE_ACCELEROMETER -> {
                        if (event.values.size < 3) return
                        val dtSec = deltaSec(event.timestamp, lastAccelNanos)
                        lastAccelNanos = event.timestamp
                        if (dtSec <= 0f || dtSec > MAX_SAMPLE_DT_SEC) return

                        var ax = event.values[0]
                        var ay = event.values[1]
                        var az = event.values[2]

                        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                            // Strip gravity with a slow EMA so orientation changes
                            // do not look like rider jerk.
                            val gAlpha = 0.08f
                            gravityX = gravityX * (1f - gAlpha) + ax * gAlpha
                            gravityY = gravityY * (1f - gAlpha) + ay * gAlpha
                            gravityZ = gravityZ * (1f - gAlpha) + az * gAlpha
                            ax -= gravityX
                            ay -= gravityY
                            az -= gravityZ
                            hasGravity = true
                        }

                        val remapped = remapToDisplay(ax, ay, az, displayRotation())
                        // Rider-facing landscape mount: +X lateral, −Z forward.
                        val aLat = remapped[0]
                        val aFwd = -remapped[2]
                        engine.onImu(
                            aFwd = aFwd,
                            aLat = aLat,
                            aMag = sqrt(ax * ax + ay * ay + az * az),
                            dtSec = dtSec,
                            speedKmh = latestSpeedKmh,
                            nowElapsedMs = SystemClock.elapsedRealtime()
                        )
                    }

                    Sensor.TYPE_GYROSCOPE -> {
                        if (event.values.size < 3) return
                        val dtSec = deltaSec(event.timestamp, lastGyroNanos)
                        lastGyroNanos = event.timestamp
                        if (dtSec <= 0f || dtSec > MAX_SAMPLE_DT_SEC) return
                        val remapped = remapToDisplay(
                            event.values[0],
                            event.values[1],
                            event.values[2],
                            displayRotation()
                        )
                        // Display-frame yaw rate ≈ bike yaw when phone faces rider.
                        engine.onGyroYaw(remapped[1], SystemClock.elapsedRealtime())
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        val registeredLinear = sensorManager.registerListener(
            listener,
            linearSensor,
            SensorManager.SENSOR_DELAY_GAME
        )
        if (!registeredLinear) {
            trySend(SmoothnessSample(score = 0f, bars = 0, confident = false))
            close()
            return@callbackFlow
        }
        gyroSensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
        }

        val mainHandler = Handler(Looper.getMainLooper())
        val ticker = object : Runnable {
            override fun run() {
                val sample = engine.score(
                    speedKmh = latestSpeedKmh,
                    imuReady = hasGravity,
                    nowElapsedMs = SystemClock.elapsedRealtime()
                )
                trySend(sample)
                mainHandler.postDelayed(this, SCORE_INTERVAL_MS)
            }
        }
        mainHandler.post(ticker)

        awaitClose {
            mainHandler.removeCallbacks(ticker)
            sensorManager.unregisterListener(listener)
        }
    }

    private fun displayRotation(): Int =
        displayManager.getDisplay(Display.DEFAULT_DISPLAY)?.rotation
            ?: Surface.ROTATION_0

    private companion object {
        const val SCORE_INTERVAL_MS = 1_000L
        const val MAX_SAMPLE_DT_SEC = 0.25f

        fun deltaSec(timestampNanos: Long, previousNanos: Long): Float {
            if (previousNanos <= 0L) return 0f
            return ((timestampNanos - previousNanos) / 1_000_000_000.0).toFloat()
        }

        fun remapToDisplay(x: Float, y: Float, z: Float, rotation: Int): FloatArray =
            when (rotation) {
                Surface.ROTATION_90 -> floatArrayOf(y, -x, z)
                Surface.ROTATION_180 -> floatArrayOf(-x, -y, z)
                Surface.ROTATION_270 -> floatArrayOf(-y, x, z)
                else -> floatArrayOf(x, y, z)
            }
    }
}

/**
 * Rolling-window feature extraction + exponential component scores.
 *
 * Weights (max 10): accel 3.0 · speed 2.5 · cornering 2.5 · vibration 2.0.
 * Calibration constants are engineering starting points — tune on real rides.
 */
internal class SmoothnessEngine(
    private val hasGyro: Boolean
) {
    private val accelJerk = RingStats(FEATURE_WINDOW_MS)
    private val lateralJerk = RingStats(FEATURE_WINDOW_MS)
    private val vibration = RingStats(FEATURE_WINDOW_MS)
    private val speeds = RingStats(SPEED_WINDOW_MS)

    private var prevAFwd: Float? = null
    private var prevALat: Float? = null
    private var vibLowPass = 0f
    private var vibReady = false
    private var lastYawRate = 0f
    private var lastYawElapsedMs = 0L
    private var heldScore = 0f
    private var hasHeldScore = false

    fun onGyroYaw(yawRateRadPerSec: Float, nowElapsedMs: Long) {
        lastYawRate = yawRateRadPerSec
        lastYawElapsedMs = nowElapsedMs
    }

    fun onImu(
        aFwd: Float,
        aLat: Float,
        aMag: Float,
        dtSec: Float,
        speedKmh: Float,
        nowElapsedMs: Long
    ) {
        if (dtSec < MIN_DT_SEC) return

        val moving = speedKmh >= MIN_SPEED_KMH
        if (moving) {
            speeds.add(speedKmh, nowElapsedMs)

            val previousFwd = prevAFwd
            if (previousFwd != null) {
                val jerk = (aFwd - previousFwd) / dtSec
                accelJerk.add(jerk, nowElapsedMs)
            }

            val cornering = isCornering(aLat, nowElapsedMs)
            val previousLat = prevALat
            if (cornering && previousLat != null) {
                val latJerk = (aLat - previousLat) / dtSec
                lateralJerk.add(latJerk, nowElapsedMs)
            }
        }

        // High-pass ≈ residual after slow EMA (road / mount vibration).
        if (!vibReady) {
            vibLowPass = aMag
            vibReady = true
        } else {
            vibLowPass = vibLowPass * (1f - VIB_LP_ALPHA) + aMag * VIB_LP_ALPHA
        }
        if (moving) {
            vibration.add(aMag - vibLowPass, nowElapsedMs)
        }

        prevAFwd = aFwd
        prevALat = aLat
    }

    fun score(speedKmh: Float, imuReady: Boolean, nowElapsedMs: Long): SmoothnessSample {
        accelJerk.evict(nowElapsedMs)
        lateralJerk.evict(nowElapsedMs)
        vibration.evict(nowElapsedMs)
        speeds.evict(nowElapsedMs)

        val movingNow = speedKmh >= MIN_SPEED_KMH
        // Idle / stopped → dark-grey off state. Keep the last score so it
        // can reappear when motion resumes, but never leave bars lit blue.
        if (!movingNow) {
            return idleSample()
        }

        if (!imuReady || accelJerk.size < MIN_JERK_SAMPLES) {
            return idleSample()
        }

        val jRms = accelJerk.rms()
        val sAccel = MAX_ACCEL * exp((-K_ACCEL * jRms).toDouble()).toFloat()

        val speedSigma = speeds.stdDev()
        val sSpeed = if (speeds.size >= MIN_SPEED_SAMPLES) {
            MAX_SPEED * exp((-K_SPEED * speedSigma).toDouble()).toFloat()
        } else {
            null
        }

        val sCorner = when {
            !hasGyro && lateralJerk.size < MIN_LAT_SAMPLES -> null
            lateralJerk.size >= MIN_LAT_SAMPLES -> {
                val latRms = lateralJerk.rms()
                MAX_CORNER * exp((-K_CORNER * latRms).toDouble()).toFloat()
            }
            else -> MAX_CORNER // no cornering events → do not penalize
        }

        val sVibe = if (vibration.size >= MIN_VIB_SAMPLES) {
            val vRms = vibration.rms()
            MAX_VIBE * exp((-K_VIBE * vRms).toDouble()).toFloat()
        } else {
            null
        }

        // Drop unavailable components and renormalize so missing sensors
        // cannot silently mint a perfect 10.
        var earned = sAccel
        var maxPossible = MAX_ACCEL
        if (sSpeed != null) {
            earned += sSpeed
            maxPossible += MAX_SPEED
        }
        if (sCorner != null) {
            earned += sCorner
            maxPossible += MAX_CORNER
        }
        if (sVibe != null) {
            earned += sVibe
            maxPossible += MAX_VIBE
        }

        if (maxPossible <= 0f) return idleSample()

        val score = ((earned / maxPossible) * 10f).coerceIn(0f, 10f)
        // Require IMU jerk + speed consistency before lighting the bar.
        val ready = sSpeed != null
        if (ready) {
            heldScore = score
            hasHeldScore = true
        }

        val publish = when {
            ready -> score
            hasHeldScore -> heldScore
            else -> score
        }
        return SmoothnessSample(
            score = publish,
            bars = publish.barCount(),
            // Only light accent bars while actually moving with a score.
            confident = ready || hasHeldScore
        )
    }

    private fun isCornering(aLat: Float, nowElapsedMs: Long): Boolean {
        if (kotlin.math.abs(aLat) >= LAT_ACCEL_CORNER) return true
        if (!hasGyro) return false
        if (nowElapsedMs - lastYawElapsedMs > GYRO_STALE_MS) return false
        return kotlin.math.abs(lastYawRate) >= YAW_CORNER_RAD_S
    }

    /** Parked / below scoring speed — EFF ticks render off (dark grey). */
    private fun idleSample(): SmoothnessSample {
        if (!hasHeldScore) {
            return SmoothnessSample(score = 0f, bars = 0, confident = false)
        }
        return SmoothnessSample(
            score = heldScore,
            bars = heldScore.barCount(),
            confident = false
        )
    }

    private companion object {
        const val FEATURE_WINDOW_MS = 10_000L
        const val SPEED_WINDOW_MS = 30_000L
        const val MIN_SPEED_KMH = 10f
        const val MIN_DT_SEC = 0.008f

        const val MIN_JERK_SAMPLES = 40
        const val MIN_SPEED_SAMPLES = 8
        const val MIN_LAT_SAMPLES = 20
        const val MIN_VIB_SAMPLES = 40

        const val MAX_ACCEL = 3.0f
        const val MAX_SPEED = 2.5f
        const val MAX_CORNER = 2.5f
        const val MAX_VIBE = 2.0f

        /** Tuned so ~1 m/s³ RMS jerk ≈ 2.4 / 3. */
        const val K_ACCEL = 0.22f
        /** Tuned so ~3 km/h σ ≈ 2.0 / 2.5. */
        const val K_SPEED = 0.074f
        const val K_CORNER = 0.18f
        /** Soft vibration penalty — mounts / road surfaces vary a lot. */
        const val K_VIBE = 0.12f

        const val LAT_ACCEL_CORNER = 0.8f
        const val YAW_CORNER_RAD_S = 0.25f
        const val GYRO_STALE_MS = 250L
        const val VIB_LP_ALPHA = 0.05f

        fun Float.barCount(): Int = roundToInt().coerceIn(0, 10)
    }
}

/** Timestamped float ring used for RMS / σ over a sliding time window. */
internal class RingStats(private val windowMs: Long) {
    private val values = ArrayDeque<Float>()
    private val times = ArrayDeque<Long>()

    val size: Int get() = values.size

    fun add(value: Float, elapsedMs: Long) {
        values.addLast(value)
        times.addLast(elapsedMs)
        evict(elapsedMs)
    }

    fun evict(nowElapsedMs: Long) {
        val cutoff = nowElapsedMs - windowMs
        while (times.isNotEmpty() && times.first() < cutoff) {
            times.removeFirst()
            values.removeFirst()
        }
    }

    fun rms(): Float {
        if (values.isEmpty()) return 0f
        var sumSq = 0.0
        for (v in values) sumSq += (v * v).toDouble()
        return sqrt(sumSq / values.size).toFloat()
    }

    fun stdDev(): Float {
        if (values.size < 2) return 0f
        var sum = 0.0
        for (v in values) sum += v
        val mean = sum / values.size
        var varSum = 0.0
        for (v in values) {
            val d = v - mean
            varSum += d * d
        }
        return sqrt(varSum / values.size).toFloat()
    }
}
