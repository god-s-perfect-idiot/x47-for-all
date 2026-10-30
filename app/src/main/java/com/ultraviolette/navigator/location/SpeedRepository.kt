package com.ultraviolette.navigator.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class SpeedSample(
    /** Display-smoothed fused speed in km/h. */
    val speedKmh: Float,
    /**
     * Distance advanced since the previous accepted fix, in meters.
     * From filtered lat/lng — never used for the speed reading.
     */
    val deltaMeters: Float = 0f
)

/**
 * Vehicle-style speed pipeline:
 *
 * GNSS Doppler (1–5 Hz) → primary measurement, weighted by speedAccuracy
 * Accel + gyro (high rate) → short-term predict between fixes
 * 1D Kalman → fuse predict + measure
 * Display EMA → calm integer readout
 *
 * Accelerometer is never open-loop integrated without GNSS re-anchoring.
 */
class SpeedRepository(context: Context) {
    private val appContext = context.applicationContext
    private val fusedClient = LocationServices.getFusedLocationProviderClient(appContext)
    private val locationManager =
        appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val sensorManager =
        appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarse = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        return fine == PackageManager.PERMISSION_GRANTED ||
            coarse == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun observeSpeed(): Flow<SpeedSample> = callbackFlow {
        if (!hasLocationPermission()) {
            close()
            return@callbackFlow
        }

        val kalman = SpeedKalman()
        val display = SpeedDisplaySmoother()
        var lastDistanceLocation: Location? = null
        var lastGpsElapsed = 0L
        var lastEmitElapsed = 0L
        var bearingRad = 0f
        var hasBearing = false
        var gyroRateRad = 0f

        fun publish(deltaMeters: Float = 0f, force: Boolean = false) {
            val now = SystemClock.elapsedRealtime()
            if (!force && now - lastEmitElapsed < MIN_EMIT_MS) return
            lastEmitElapsed = now

            val fusedKmh = (kalman.speedMps * 3.6).coerceIn(0.0, MAX_SPEED_KMH.toDouble())
            val shown = display.smooth(fusedKmh)
            trySend(SpeedSample(speedKmh = shown, deltaMeters = deltaMeters))
        }

        fun onGps(location: Location) {
            val deltaMeters = consumeDistanceDelta(location, lastDistanceLocation) { next ->
                lastDistanceLocation = next
            }

            if (location.provider == LocationManager.NETWORK_PROVIDER) {
                if (deltaMeters > 0f) publish(deltaMeters = deltaMeters, force = true)
                return
            }
            if (!location.hasSpeed()) {
                if (deltaMeters > 0f) publish(deltaMeters = deltaMeters, force = true)
                return
            }

            val now = SystemClock.elapsedRealtime()
            if (lastGpsElapsed > 0L) {
                kalman.inflateForGpsGap((now - lastGpsElapsed) / 1000.0)
            }
            lastGpsElapsed = now

            val measured = location.speed.toDouble().coerceAtLeast(0.0)
            val sigma = if (location.hasSpeedAccuracy()) {
                location.speedAccuracyMetersPerSecond.toDouble().coerceAtLeast(0.25)
            } else {
                DEFAULT_SPEED_SIGMA_MPS
            }

            // Untrustworthy Doppler while essentially still → measure as stop.
            val treatAsStop = measured < STOP_MPS ||
                (location.hasSpeedAccuracy() && measured < sigma && measured < 2.5)

            if (treatAsStop) {
                kalman.update(0.0, sigma.coerceAtMost(1.5))
                if (measured < STOP_MPS && sigma < 2.0) {
                    kalman.forceStop()
                    display.reset()
                }
                hasBearing = false
            } else if (sigma > MAX_SPEED_SIGMA_MPS) {
                // Very uncertain — weak pull only.
                kalman.update(measured, sigma)
            } else {
                kalman.update(measured, sigma)
                if (location.hasBearing() && measured > BEARING_MIN_MPS) {
                    bearingRad = Math.toRadians(location.bearing.toDouble()).toFloat()
                    hasBearing = true
                }
            }

            publish(deltaMeters = deltaMeters, force = true)
        }

        val fusedCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach(::onGps)
            }
        }

        // 1–5 Hz GNSS as recommended for vehicle clusters.
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, GPS_INTERVAL_MS)
            .setMinUpdateIntervalMillis(GPS_MIN_INTERVAL_MS)
            .setMaxUpdateDelayMillis(0L)
            .setMinUpdateDistanceMeters(0f)
            .setWaitForAccurateLocation(false)
            .build()

        var usingFused = false
        try {
            fusedClient.requestLocationUpdates(request, fusedCallback, Looper.getMainLooper())
            usingFused = true
        } catch (_: Exception) {
            usingFused = false
        }

        val gpsListener = LocationListener { location -> onGps(location) }
        if (!usingFused) {
            try {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    GPS_INTERVAL_MS,
                    0f,
                    gpsListener,
                    Looper.getMainLooper()
                )
            } catch (_: SecurityException) {
                close()
                return@callbackFlow
            } catch (_: IllegalArgumentException) {
                close()
                return@callbackFlow
            }
        }

        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        val linearSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        val gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        val rotationMatrix = FloatArray(9)
        var hasRotation = false
        var lastImuNs = 0L

        val sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_GYROSCOPE -> {
                        if (event.values.size >= 3) {
                            val gx = event.values[0]
                            val gy = event.values[1]
                            val gz = event.values[2]
                            gyroRateRad = sqrt(gx * gx + gy * gy + gz * gz)
                        }
                    }
                    Sensor.TYPE_ROTATION_VECTOR,
                    Sensor.TYPE_GAME_ROTATION_VECTOR -> {
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                        hasRotation = true
                    }
                    Sensor.TYPE_LINEAR_ACCELERATION -> {
                        if (event.values.size < 3) return
                        val nowNs = event.timestamp
                        val dt = if (lastImuNs == 0L) {
                            0f
                        } else {
                            ((nowNs - lastImuNs).coerceAtLeast(0L) / 1_000_000_000f)
                                .coerceIn(0f, 0.1f)
                        }
                        lastImuNs = nowNs
                        if (dt <= 0f) return

                        val nowElapsed = SystemClock.elapsedRealtime()
                        val sinceGps = if (lastGpsElapsed == 0L) {
                            Long.MAX_VALUE
                        } else {
                            nowElapsed - lastGpsElapsed
                        }

                        // Only short-term coast between GNSS fixes.
                        if (sinceGps > IMU_COAST_MS) {
                            kalman.inflateForGpsGap(dt.toDouble())
                            publish()
                            return
                        }

                        if (!hasRotation || !hasBearing) {
                            // No travel frame yet — time still advances uncertainty.
                            kalman.predict(0.0, dt.toDouble())
                            publish()
                            return
                        }

                        val ax = event.values[0]
                        val ay = event.values[1]
                        val az = event.values[2]
                        val east =
                            rotationMatrix[0] * ax + rotationMatrix[1] * ay + rotationMatrix[2] * az
                        val north =
                            rotationMatrix[3] * ax + rotationMatrix[4] * ay + rotationMatrix[5] * az
                        var aForward = east * sin(bearingRad) + north * cos(bearingRad)

                        // Yawing hard → forward-axis projection is unreliable.
                        if (gyroRateRad > 0.7f) {
                            aForward *= 0.25f
                        }

                        kalman.predict(aForward.toDouble(), dt.toDouble())
                        publish()
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        rotationSensor?.let {
            sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_GAME)
        }
        linearSensor?.let {
            sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_GAME)
        }
        gyroSensor?.let {
            sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_GAME)
        }

        awaitClose {
            if (usingFused) {
                fusedClient.removeLocationUpdates(fusedCallback)
            } else {
                locationManager.removeUpdates(gpsListener)
            }
            sensorManager.unregisterListener(sensorListener)
        }
    }

    private companion object {
        const val GPS_INTERVAL_MS = 400L
        const val GPS_MIN_INTERVAL_MS = 200L
        const val MIN_EMIT_MS = 50L
        const val IMU_COAST_MS = 2000L
        const val MAX_SPEED_KMH = 280f
        const val MAX_ACCURACY_M = 60f
        const val MIN_MOVE_M = 5f
        const val MAX_IMPLIED_SPEED_MPS = 70f
        const val STOP_MPS = 0.45
        const val BEARING_MIN_MPS = 1.5
        const val DEFAULT_SPEED_SIGMA_MPS = 1.8
        const val MAX_SPEED_SIGMA_MPS = 6.0

        fun consumeDistanceDelta(
            location: Location,
            previous: Location?,
            onAnchor: (Location) -> Unit
        ): Float {
            if (location.provider == LocationManager.NETWORK_PROVIDER) return 0f
            if (location.hasAccuracy() && location.accuracy > MAX_ACCURACY_M) return 0f

            if (previous == null) {
                onAnchor(location)
                return 0f
            }

            val dtSec = (location.time - previous.time) / 1000.0
            if (dtSec <= 0.05) return 0f

            val distanceM = previous.distanceTo(location)
            if (distanceM < MIN_MOVE_M) return 0f

            val impliedMps = distanceM / dtSec
            if (impliedMps > MAX_IMPLIED_SPEED_MPS) {
                onAnchor(location)
                return 0f
            }

            onAnchor(location)
            return distanceM
        }
    }
}
