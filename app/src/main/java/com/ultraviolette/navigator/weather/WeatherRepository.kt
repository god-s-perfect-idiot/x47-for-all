package com.ultraviolette.navigator.weather

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume
import kotlin.math.roundToInt

enum class WeatherCondition {
    CLEAR,
    PARTLY_CLOUDY,
    CLOUDY,
    FOG,
    DRIZZLE,
    RAIN,
    SNOW,
    THUNDER
}

data class WeatherSample(
    /** Ambient temperature in °C, rounded. */
    val tempC: Int,
    val condition: WeatherCondition,
    /** True when local solar day — picks sun vs moon for clear skies. */
    val isDay: Boolean,
    /** 10 m wind speed in km/h, rounded. */
    val windSpeedKmh: Int
)

/**
 * Live outdoor weather for the cluster temp + icon.
 *
 * Uses GNSS for lat/lon, then Open-Meteo current conditions (no API key).
 * Refreshes on a slow interval — weather doesn't need GNSS cadence.
 */
class WeatherRepository(context: Context) {
    private val appContext = context.applicationContext
    private val fusedClient = LocationServices.getFusedLocationProviderClient(appContext)
    private val locationManager =
        appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

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

    fun observeWeather(): Flow<WeatherSample> = callbackFlow {
        if (!hasLocationPermission()) {
            close()
            return@callbackFlow
        }

        val job = launch {
            while (isActive) {
                val location = currentLocation()
                if (location != null) {
                    val sample = fetchWeather(location.latitude, location.longitude)
                    if (sample != null) trySend(sample)
                }
                delay(REFRESH_INTERVAL_MS)
            }
        }

        awaitClose { job.cancel() }
    }

    @SuppressLint("MissingPermission")
    private suspend fun currentLocation(): Location? {
        if (!hasLocationPermission()) return null

        val fused = fusedCurrentOrLast()
        if (fused != null) return fused

        // Provider last-known fallback when Play Services has nothing yet.
        return listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        ).mapNotNull { provider ->
            try {
                locationManager.getLastKnownLocation(provider)
            } catch (_: SecurityException) {
                null
            } catch (_: IllegalArgumentException) {
                null
            }
        }.maxByOrNull { it.time }
    }

    @SuppressLint("MissingPermission")
    private suspend fun fusedCurrentOrLast(): Location? =
        suspendCancellableCoroutine { cont ->
            val token = CancellationTokenSource()
            cont.invokeOnCancellation { token.cancel() }

            try {
                fusedClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    token.token
                ).addOnCompleteListener { task ->
                    if (!cont.isActive) return@addOnCompleteListener
                    val current = task.result
                    if (current != null) {
                        cont.resume(current)
                        return@addOnCompleteListener
                    }
                    fusedClient.lastLocation
                        .addOnCompleteListener { lastTask ->
                            if (cont.isActive) cont.resume(lastTask.result)
                        }
                }
            } catch (_: Exception) {
                if (cont.isActive) cont.resume(null)
            }
        }

    private suspend fun fetchWeather(latitude: Double, longitude: Double): WeatherSample? =
        withContext(Dispatchers.IO) {
            val url = URL(
                "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=${"%.4f".format(latitude)}" +
                    "&longitude=${"%.4f".format(longitude)}" +
                    "&current=temperature_2m,weather_code,is_day,wind_speed_10m" +
                    "&wind_speed_unit=kmh" +
                    "&timezone=auto"
            )
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 8_000
                requestMethod = "GET"
            }
            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                parseCurrent(body)
            } catch (_: Exception) {
                null
            } finally {
                connection.disconnect()
            }
        }

    private companion object {
        /** Weather changes slowly; avoid hammering the API on a ride. */
        const val REFRESH_INTERVAL_MS = 10 * 60 * 1000L

        fun parseCurrent(body: String): WeatherSample? {
            val current = JSONObject(body).optJSONObject("current") ?: return null
            if (!current.has("temperature_2m") || !current.has("weather_code")) return null
            val tempC = current.getDouble("temperature_2m").roundToInt()
            val code = current.getInt("weather_code")
            val isDay = current.optInt("is_day", 1) == 1
            val wind = if (current.has("wind_speed_10m")) {
                current.getDouble("wind_speed_10m").roundToInt().coerceIn(0, 200)
            } else {
                0
            }
            return WeatherSample(
                tempC = tempC.coerceIn(-40, 55),
                condition = conditionFor(code),
                isDay = isDay,
                windSpeedKmh = wind
            )
        }

        /** WMO weather interpretation codes used by Open-Meteo. */
        fun conditionFor(code: Int): WeatherCondition = when (code) {
            0 -> WeatherCondition.CLEAR
            1, 2 -> WeatherCondition.PARTLY_CLOUDY
            3 -> WeatherCondition.CLOUDY
            45, 48 -> WeatherCondition.FOG
            in 51..57 -> WeatherCondition.DRIZZLE
            in 61..67, in 80..82 -> WeatherCondition.RAIN
            in 71..77, in 85..86 -> WeatherCondition.SNOW
            in 95..99 -> WeatherCondition.THUNDER
            else -> WeatherCondition.CLOUDY
        }
    }
}
