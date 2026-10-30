package com.ultraviolette.navigator.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ultraviolette.navigator.battery.BatteryRepository
import com.ultraviolette.navigator.location.OdometerStore
import com.ultraviolette.navigator.location.SpeedRepository
import com.ultraviolette.navigator.model.ClusterPlaceholders
import com.ultraviolette.navigator.sensor.LeanAngleRepository
import com.ultraviolette.navigator.sensor.formatLeanAngleDigits
import com.ultraviolette.navigator.time.ClockRepository
import com.ultraviolette.navigator.weather.WeatherRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class ClusterViewModel(application: Application) : AndroidViewModel(application) {
    private val speedRepository = SpeedRepository(application)
    private val batteryRepository = BatteryRepository(application)
    private val leanAngleRepository = LeanAngleRepository(application)
    private val weatherRepository = WeatherRepository(application)
    private val clockRepository = ClockRepository()
    private val odometerStore = OdometerStore(application)
    private val idle = ClusterPlaceholders()

    /** Lifetime meters — loaded from disk, written back as we ride. */
    private var totalMeters: Double = odometerStore.readTotalMeters()
    /** Current trip meters — resets when the process starts a new ViewModel. */
    private var tripMeters: Double = 0.0

    private val _data = MutableStateFlow(
        idle.copy(
            odometerKm = formatDistanceKm(totalMeters),
            tripKm = formatDistanceKm(tripMeters)
        )
    )
    val data: StateFlow<ClusterPlaceholders> = _data.asStateFlow()

    private var speedJob: Job? = null
    private var batteryJob: Job? = null
    private var leanJob: Job? = null
    private var weatherJob: Job? = null
    private var clockJob: Job? = null
    private var isMoving = false

    init {
        startBatteryUpdates()
        startLeanUpdates()
        startClockUpdates()
    }

    fun onLocationPermissionResult(granted: Boolean) {
        if (granted) {
            startSpeedUpdates()
            startWeatherUpdates()
        } else {
            stopSpeedUpdates(resetToIdle = true)
            stopWeatherUpdates()
        }
    }

    fun startSpeedUpdates() {
        if (!speedRepository.hasLocationPermission()) return
        if (speedJob?.isActive == true) return

        speedJob = viewModelScope.launch {
            speedRepository.observeSpeed().collect { sample ->
                isMoving = if (isMoving) {
                    sample.speedKmh >= EXIT_MOVING_KMH
                } else {
                    sample.speedKmh >= ENTER_MOVING_KMH
                }

                if (sample.deltaMeters > 0f) {
                    tripMeters += sample.deltaMeters
                    totalMeters += sample.deltaMeters
                    odometerStore.writeTotalMeters(totalMeters)
                }

                _data.update { current ->
                    current.copy(
                        speedKmh = if (isMoving) {
                            formatSpeedDigits(sample.speedKmh.roundToInt())
                        } else {
                            "———"
                        },
                        isMoving = isMoving,
                        odometerKm = formatDistanceKm(totalMeters),
                        tripKm = formatDistanceKm(tripMeters)
                    )
                }
            }
        }
    }

    fun startBatteryUpdates() {
        if (batteryJob?.isActive == true) return

        batteryJob = viewModelScope.launch {
            batteryRepository.observeBattery().collect { sample ->
                _data.update { current ->
                    current.copy(batteryPercent = sample.percent)
                }
            }
        }
    }

    fun startLeanUpdates() {
        if (leanJob?.isActive == true) return

        leanJob = viewModelScope.launch {
            leanAngleRepository.observeLean().collect { sample ->
                _data.update { current ->
                    current.copy(
                        leanAngleDeg = formatLeanAngleDigits(sample.leanAngleDeg),
                        leanAngleSignedDeg = sample.leanAngleDeg
                    )
                }
            }
        }
    }

    fun startWeatherUpdates() {
        if (!weatherRepository.hasLocationPermission()) return
        if (weatherJob?.isActive == true) return

        weatherJob = viewModelScope.launch {
            weatherRepository.observeWeather().collect { sample ->
                _data.update { current ->
                    current.copy(
                        ambientTempC = sample.tempC.toString(),
                        weatherCondition = sample.condition,
                        weatherIsDay = sample.isDay
                    )
                }
            }
        }
    }

    fun startClockUpdates() {
        if (clockJob?.isActive == true) return

        clockJob = viewModelScope.launch {
            clockRepository.observeClock().collect { sample ->
                _data.update { current ->
                    current.copy(timeText = sample.timeText)
                }
            }
        }
    }

    private fun stopSpeedUpdates(resetToIdle: Boolean) {
        speedJob?.cancel()
        speedJob = null
        isMoving = false
        if (resetToIdle) {
            // Keep live SoC + clock + distance + weather + lean when GPS is denied — only clear speed/motion.
            _data.update { current ->
                idle.copy(
                    batteryPercent = current.batteryPercent,
                    timeText = current.timeText,
                    odometerKm = formatDistanceKm(totalMeters),
                    tripKm = formatDistanceKm(tripMeters),
                    ambientTempC = current.ambientTempC,
                    weatherCondition = current.weatherCondition,
                    weatherIsDay = current.weatherIsDay,
                    leanAngleDeg = current.leanAngleDeg,
                    leanAngleSignedDeg = current.leanAngleSignedDeg
                )
            }
        }
    }

    private fun stopWeatherUpdates() {
        weatherJob?.cancel()
        weatherJob = null
    }

    override fun onCleared() {
        stopSpeedUpdates(resetToIdle = false)
        stopWeatherUpdates()
        leanJob?.cancel()
        leanJob = null
        batteryJob?.cancel()
        batteryJob = null
        clockJob?.cancel()
        clockJob = null
        super.onCleared()
    }

    private companion object {
        const val ENTER_MOVING_KMH = 1.5f
        const val EXIT_MOVING_KMH = 0.7f

        fun formatSpeedDigits(kmh: Int): String =
            "%03d".format(kmh.coerceIn(0, 999))

        /** Cluster-style distance: one decimal under 10 km, whole km after. */
        fun formatDistanceKm(meters: Double): String {
            val km = (meters / 1000.0).coerceAtLeast(0.0)
            return if (km < 10.0) {
                "%.1f km".format(km)
            } else {
                "%d km".format(km.roundToInt())
            }
        }
    }
}
