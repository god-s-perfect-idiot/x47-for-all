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
import android.os.Build
import android.os.Handler
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
import kotlin.math.abs
import kotlin.math.sqrt
import android.location.LocationRequest as PlatformLocationRequest

data class SpeedSample(
    /** Display speed in km/h (0 when parked / untrustworthy). */
    val speedKmh: Float,
    /** Meters advanced since the previous accepted fix (odo/trip). */
    val deltaMeters: Float = 0f,
    /** True when the dial should leave idle chrome. */
    val isMoving: Boolean = false,
    /** True while a recent GNSS-quality fix is available. */
    val gpsAvailable: Boolean = false
)

/**
 * GNSS Doppler speed when the chip has a lock; coarse network / fused
 * position-delta when it does not — dumb, but the dial still moves.
 *
 * IMU remains a still-veto only.
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

        var lastDistanceLocation: Location? = null
        var lastFixNanos = 0L
        var lastGpsElapsedMs = 0L
        var shownKmh = 0f
        var gpsAvailable = false
        var motionEnergy = 0f
        var stillTicks = 0

        fun physicallyStill(): Boolean = motionEnergy < STILL_ENERGY && stillTicks >= STILL_TICKS

        fun emit(deltaMeters: Float = 0f) {
            val moving = shownKmh >= MOVE_KMH
            trySend(
                SpeedSample(
                    speedKmh = if (moving) shownKmh else 0f,
                    deltaMeters = deltaMeters,
                    isMoving = moving,
                    gpsAvailable = gpsAvailable
                )
            )
        }

        fun refreshGpsAvailability(nowMs: Long = SystemClock.elapsedRealtime()) {
            val next = lastGpsElapsedMs > 0L && (nowMs - lastGpsElapsedMs) <= GPS_STALE_MS
            if (next != gpsAvailable) {
                gpsAvailable = next
                emit()
            } else {
                gpsAvailable = next
            }
        }

        fun applySpeed(kmhRaw: Float, deltaMeters: Float, fromGps: Boolean) {
            var kmh = kmhRaw.coerceAtLeast(0f)
            if (kmh > MAX_SPEED_KMH) {
                if (deltaMeters > 0f) emit(deltaMeters)
                return
            }

            if (physicallyStill() && kmh < PARK_NOISE_CEILING_KMH) {
                kmh = 0f
            }

            val alpha = if (fromGps) GPS_EMA_ALPHA else FALLBACK_EMA_ALPHA
            shownKmh = when {
                kmh < STOP_KMH -> 0f
                shownKmh < STOP_KMH -> kmh
                else -> shownKmh * (1f - alpha) + kmh * alpha
            }
            emit(deltaMeters)
        }

        fun isGnssQuality(location: Location): Boolean {
            if (location.provider == LocationManager.GPS_PROVIDER) return true
            if (location.provider == LocationManager.NETWORK_PROVIDER) return false
            // Fused / other: treat tight accuracy + Doppler as GNSS-quality.
            if (location.hasAccuracy() && location.accuracy <= GPS_QUALITY_ACCURACY_M && location.hasSpeed()) {
                return true
            }
            return false
        }

        fun onLocation(location: Location) {
            val nanos = location.elapsedRealtimeNanos
            if (nanos > 0L && nanos < lastFixNanos) return
            if (nanos > 0L) lastFixNanos = nanos

            val gnss = isGnssQuality(location)
            if (gnss) {
                lastGpsElapsedMs = SystemClock.elapsedRealtime()
                gpsAvailable = true
            } else {
                refreshGpsAvailability()
                // Prefer live GNSS; ignore coarse fixes while the chip is locked.
                if (gpsAvailable) return
            }

            val maxAcc = if (gnss) MAX_GPS_ACCURACY_M else MAX_FALLBACK_ACCURACY_M
            if (location.hasAccuracy() && location.accuracy > maxAcc) return

            val odoDelta = if (gnss) {
                consumeDistanceDelta(location, lastDistanceLocation, maxAcc) { next ->
                    lastDistanceLocation = next
                }
            } else {
                0f // Don't pollute lifetime odo with cell/wifi jumps.
            }

            if (location.hasSpeed()) {
                var kmh = location.speed * 3.6f
                if (gnss && location.hasSpeedAccuracy()) {
                    val sigmaKmh = location.speedAccuracyMetersPerSecond * 3.6f
                    if (kmh > 0f && kmh < sigmaKmh * 0.5f) kmh = 0f
                }
                applySpeed(kmh, odoDelta, fromGps = gnss)
                return
            }

            // Dumb fallback: implied speed from successive coarse fixes.
            if (!gnss) {
                val previous = lastDistanceLocation
                if (previous == null) {
                    lastDistanceLocation = location
                    return
                }
                val dtSec = (location.time - previous.time) / 1000.0
                if (dtSec < MIN_FALLBACK_DT_SEC) return
                val distanceM = previous.distanceTo(location)
                if (distanceM < MIN_FALLBACK_MOVE_M) return
                val impliedMps = distanceM / dtSec
                if (impliedMps > MAX_IMPLIED_SPEED_MPS) {
                    lastDistanceLocation = location
                    return
                }
                lastDistanceLocation = location
                applySpeed(
                    (impliedMps * 3.6).toFloat(),
                    deltaMeters = 0f,
                    fromGps = false
                )
                return
            }

            if (odoDelta > 0f) emit(odoDelta)
        }

        val fusedCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach(::onLocation)
            }
        }

        val fusedRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, GPS_INTERVAL_MS)
            .setMinUpdateIntervalMillis(0L)
            .setMinUpdateDistanceMeters(0f)
            .setMaxUpdateDelayMillis(0L)
            .setWaitForAccurateLocation(false)
            .build()

        var fusedOk = false
        try {
            fusedClient.requestLocationUpdates(fusedRequest, fusedCallback, Looper.getMainLooper())
            fusedOk = true
        } catch (_: Exception) {
            fusedOk = false
        }

        val gpsListener = LocationListener { location -> onLocation(location) }
        var gpsOk = false
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val platformRequest = PlatformLocationRequest.Builder(GPS_INTERVAL_MS)
                    .setMinUpdateIntervalMillis(0L)
                    .setMinUpdateDistanceMeters(0f)
                    .setQuality(PlatformLocationRequest.QUALITY_HIGH_ACCURACY)
                    .build()
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    platformRequest,
                    ContextCompat.getMainExecutor(appContext),
                    gpsListener
                )
            } else {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    0L,
                    0f,
                    gpsListener,
                    Looper.getMainLooper()
                )
            }
            gpsOk = true
        } catch (_: Exception) {
            gpsOk = false
        }

        // Coarse cell/wifi stream for tunnels / indoor — only drives the dial when GNSS is stale.
        val networkListener = LocationListener { location -> onLocation(location) }
        var networkOk = false
        try {
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    NETWORK_INTERVAL_MS,
                    NETWORK_MIN_DISTANCE_M,
                    networkListener,
                    Looper.getMainLooper()
                )
                networkOk = true
            }
        } catch (_: Exception) {
            networkOk = false
        }

        if (!fusedOk && !gpsOk && !networkOk) {
            close()
            return@callbackFlow
        }

        val mainHandler = Handler(Looper.getMainLooper())
        val staleTicker = object : Runnable {
            override fun run() {
                refreshGpsAvailability()
                // If GNSS is gone and we're still showing motion but sensors say parked, zero out.
                if (!gpsAvailable && physicallyStill() && shownKmh >= STOP_KMH) {
                    shownKmh = 0f
                    emit()
                }
                mainHandler.postDelayed(this, GPS_STALE_CHECK_MS)
            }
        }
        mainHandler.postDelayed(staleTicker, GPS_STALE_CHECK_MS)

        val linear = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.values.size < 3) return
                val mag = sqrt(
                    event.values[0] * event.values[0] +
                        event.values[1] * event.values[1] +
                        event.values[2] * event.values[2]
                )
                val dynamic = if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    abs(mag - SensorManager.GRAVITY_EARTH)
                } else {
                    mag
                }
                motionEnergy = motionEnergy * 0.8f + dynamic * 0.2f
                if (motionEnergy < STILL_ENERGY) stillTicks++ else stillTicks = 0

                if (physicallyStill() && shownKmh in STOP_KMH..PARK_NOISE_CEILING_KMH) {
                    shownKmh = 0f
                    emit()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        linear?.let {
            sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_FASTEST)
        }

        trySend(SpeedSample(speedKmh = 0f, isMoving = false, gpsAvailable = false))

        awaitClose {
            mainHandler.removeCallbacks(staleTicker)
            if (fusedOk) fusedClient.removeLocationUpdates(fusedCallback)
            if (gpsOk) locationManager.removeUpdates(gpsListener)
            if (networkOk) locationManager.removeUpdates(networkListener)
            sensorManager.unregisterListener(sensorListener)
        }
    }

    private companion object {
        const val GPS_INTERVAL_MS = 50L
        const val NETWORK_INTERVAL_MS = 1_000L
        const val NETWORK_MIN_DISTANCE_M = 5f
        const val GPS_STALE_MS = 4_000L
        const val GPS_STALE_CHECK_MS = 1_000L
        const val GPS_QUALITY_ACCURACY_M = 30f
        const val MAX_GPS_ACCURACY_M = 75f
        const val MAX_FALLBACK_ACCURACY_M = 500f
        const val MAX_SPEED_KMH = 280f
        const val MAX_IMPLIED_SPEED_MPS = 70f
        const val MIN_ODO_MOVE_M = 5f
        const val MIN_FALLBACK_MOVE_M = 15f
        const val MIN_FALLBACK_DT_SEC = 1.0

        const val STOP_KMH = 0.8f
        const val MOVE_KMH = 0.8f
        const val PARK_NOISE_CEILING_KMH = 12f
        const val STILL_ENERGY = 0.4f
        const val STILL_TICKS = 8
        /** ~85% weight on newest GNSS sample. */
        const val GPS_EMA_ALPHA = 0.85f
        /** Heavier smoothing for coarse network deltas. */
        const val FALLBACK_EMA_ALPHA = 0.35f

        fun consumeDistanceDelta(
            location: Location,
            previous: Location?,
            maxAccuracyM: Float,
            onAnchor: (Location) -> Unit
        ): Float {
            if (location.provider == LocationManager.NETWORK_PROVIDER) return 0f
            if (location.hasAccuracy() && location.accuracy > maxAccuracyM) return 0f

            if (previous == null) {
                onAnchor(location)
                return 0f
            }

            val dtSec = (location.time - previous.time) / 1000.0
            if (dtSec <= 0.05) return 0f

            val distanceM = previous.distanceTo(location)
            if (distanceM < MIN_ODO_MOVE_M) return 0f

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
