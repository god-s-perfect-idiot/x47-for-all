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
import android.os.Looper
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
    /** Real-time GNSS Doppler speed in km/h (0 when parked / untrustworthy). */
    val speedKmh: Float,
    /** Meters advanced since the previous accepted fix (odo/trip). */
    val deltaMeters: Float = 0f,
    /** True when the dial should leave idle chrome. */
    val isMoving: Boolean = false
)

/**
 * Max-rate GNSS Doppler speed (Maps-style [Location.getSpeed]).
 *
 * Subscribes to **both** Play Services fused location and the raw GPS provider
 * so we get every fix the chip produces. Newest [Location.elapsedRealtimeNanos]
 * wins. IMU is a still-veto only — never used as velocity.
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
        var shownKmh = 0f
        var motionEnergy = 0f
        var stillTicks = 0

        fun physicallyStill(): Boolean = motionEnergy < STILL_ENERGY && stillTicks >= STILL_TICKS

        fun emit(deltaMeters: Float = 0f) {
            val moving = shownKmh >= MOVE_KMH
            trySend(
                SpeedSample(
                    speedKmh = if (moving) shownKmh else 0f,
                    deltaMeters = deltaMeters,
                    isMoving = moving
                )
            )
        }

        fun onGps(location: Location) {
            if (location.provider == LocationManager.NETWORK_PROVIDER) return

            // Drop stale / duplicate fixes from dual listeners.
            val nanos = location.elapsedRealtimeNanos
            if (nanos > 0L && nanos < lastFixNanos) return
            if (nanos > 0L) lastFixNanos = nanos

            if (location.hasAccuracy() && location.accuracy > MAX_ACCURACY_M) return

            val odoDelta = consumeDistanceDelta(location, lastDistanceLocation) { next ->
                lastDistanceLocation = next
            }

            if (!location.hasSpeed()) {
                if (odoDelta > 0f) emit(odoDelta)
                return
            }

            var kmh = (location.speed * 3.6f).coerceAtLeast(0f)
            if (kmh > MAX_SPEED_KMH) {
                if (odoDelta > 0f) emit(odoDelta)
                return
            }

            // Only kill readings deep inside uncertainty (Maps still shows low speeds).
            if (location.hasSpeedAccuracy()) {
                val sigmaKmh = location.speedAccuracyMetersPerSecond * 3.6f
                if (kmh > 0f && kmh < sigmaKmh * 0.5f) kmh = 0f
            }

            // Still + low GNSS → parked noise. High GNSS while "still" = cruise.
            if (physicallyStill() && kmh < PARK_NOISE_CEILING_KMH) {
                kmh = 0f
            }

            // Near pass-through — snap to stop, otherwise track hard.
            shownKmh = when {
                kmh < STOP_KMH -> 0f
                shownKmh < STOP_KMH -> kmh
                else -> shownKmh * (1f - EMA_ALPHA) + kmh * EMA_ALPHA
            }

            emit(odoDelta)
        }

        val fusedCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                // Every fix in the batch — not only lastLocation.
                result.locations.forEach(::onGps)
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

        // Raw GPS in parallel — often delivers faster than fused alone.
        val gpsListener = LocationListener { location -> onGps(location) }
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

        if (!fusedOk && !gpsOk) {
            close()
            return@callbackFlow
        }

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

        trySend(SpeedSample(speedKmh = 0f, isMoving = false))

        awaitClose {
            if (fusedOk) fusedClient.removeLocationUpdates(fusedCallback)
            if (gpsOk) locationManager.removeUpdates(gpsListener)
            sensorManager.unregisterListener(sensorListener)
        }
    }

    private companion object {
        /** Request as fast as the GNSS chip will go. */
        const val GPS_INTERVAL_MS = 50L
        const val MAX_ACCURACY_M = 75f
        const val MAX_SPEED_KMH = 280f
        const val MAX_IMPLIED_SPEED_MPS = 70f
        const val MIN_ODO_MOVE_M = 5f

        const val STOP_KMH = 0.8f
        const val MOVE_KMH = 0.8f
        const val PARK_NOISE_CEILING_KMH = 12f
        const val STILL_ENERGY = 0.4f
        const val STILL_TICKS = 8
        /** ~85% weight on newest sample — tracks hard, tiny calm only. */
        const val EMA_ALPHA = 0.85f

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
