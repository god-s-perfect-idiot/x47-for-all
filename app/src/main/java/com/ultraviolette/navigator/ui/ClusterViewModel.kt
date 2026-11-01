package com.ultraviolette.navigator.ui

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ultraviolette.navigator.battery.BatteryRepository
import com.ultraviolette.navigator.connectivity.ConnectivityRepository
import com.ultraviolette.navigator.location.OdometerStore
import com.ultraviolette.navigator.location.SpeedRepository
import com.ultraviolette.navigator.location.TripStore
import com.ultraviolette.navigator.model.BikeNameStore
import com.ultraviolette.navigator.model.BrandTheme
import com.ultraviolette.navigator.model.BrandThemeStore
import com.ultraviolette.navigator.model.ClusterPlaceholders
import com.ultraviolette.navigator.model.RideMode
import com.ultraviolette.navigator.model.RideModeStore
import com.ultraviolette.navigator.model.SavedTrip
import com.ultraviolette.navigator.sensor.AmbientLightRepository
import com.ultraviolette.navigator.sensor.LeanAngleRepository
import com.ultraviolette.navigator.sensor.RidingSmoothnessRepository
import com.ultraviolette.navigator.sensor.SpeedDisciplineRepository
import com.ultraviolette.navigator.sensor.formatLeanAngleDigits
import com.ultraviolette.navigator.time.ClockRepository
import com.ultraviolette.navigator.weather.WeatherRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class ClusterViewModel(application: Application) : AndroidViewModel(application) {
    private val speedRepository = SpeedRepository(application)
    private val batteryRepository = BatteryRepository(application)
    private val leanAngleRepository = LeanAngleRepository(application)
    private val ambientLightRepository = AmbientLightRepository(application)
    private val ridingSmoothnessRepository = RidingSmoothnessRepository(application)
    private val speedDisciplineRepository = SpeedDisciplineRepository()
    private val connectivityRepository = ConnectivityRepository(application)
    private val weatherRepository = WeatherRepository(application)
    private val clockRepository = ClockRepository()
    private val odometerStore = OdometerStore(application)
    private val rideModeStore = RideModeStore(application)
    private val bikeNameStore = BikeNameStore(application)
    private val brandThemeStore = BrandThemeStore(application)
    private val tripStore = TripStore(application)
    private val idle = ClusterPlaceholders()

    /** Lifetime meters — loaded from disk, written back as we ride. */
    private var totalMeters: Double = odometerStore.readTotalMeters()
    /** Last chosen ride theme — loaded from disk so reopen keeps accent. */
    private val savedRideMode: String = rideModeStore.readRideMode()
    /** Last chosen bike display name — loaded from disk so reopen keeps identity. */
    private val savedBikeName: String = bikeNameStore.readBikeName()
    /** Last chosen brand pack — loaded from disk so reopen keeps UV/KTM/BMW/Aprilia. */
    private val savedBrandTheme: String = brandThemeStore.readBrandTheme()
    /** Current trip meters — continues until the ride is finalized into history. */
    private var tripMeters: Double = 0.0
    /** Moving-only trip duration (paused while speed is 0). */
    private var movingMsAccumulated: Long = 0L
    /** ElapsedRealtime when the current moving segment began; null while stopped. */
    private var movingSegmentStartMs: Long? = null
    /** Wall-clock start of the open ride; null until the first movement. */
    private var tripStartedAtEpochMs: Long? = null
    /** Last time we flushed the in-progress draft to disk. */
    private var lastDraftWriteElapsedMs: Long = 0L

    private val _data = MutableStateFlow(
        idle.copy(
            odometerKm = formatDistanceKm(totalMeters),
            tripKm = formatDistanceKm(tripMeters),
            rideMode = savedRideMode,
            bikeName = savedBikeName,
            brandTheme = savedBrandTheme
        )
    )
    val data: StateFlow<ClusterPlaceholders> = _data.asStateFlow()

    private val _trips = MutableStateFlow(tripStore.readTrips())
    val trips: StateFlow<List<SavedTrip>> = _trips.asStateFlow()

    private var speedJob: Job? = null
    private var batteryJob: Job? = null
    private var leanJob: Job? = null
    private var ambientLightJob: Job? = null
    private var smoothnessJob: Job? = null
    private var disciplineJob: Job? = null
    private var connectivityJob: Job? = null
    private var weatherJob: Job? = null
    private var clockJob: Job? = null
    private var tripTickerJob: Job? = null
    private var idleFinalizeJob: Job? = null
    private var isMoving = false

    init {
        recoverDraftIfNeeded()
        startBatteryUpdates()
        startLeanUpdates()
        startAmbientLightUpdates()
        startSmoothnessUpdates()
        startDisciplineUpdates()
        startConnectivityUpdates()
        startClockUpdates()
    }

    /** Seal the current ride into history and start a fresh trip counter. */
    fun endCurrentTrip() {
        cancelIdleFinalize()
        if (isMoving) pauseMovingClock()
        stopTripTicker()
        finalizeOpenTrip()
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

    /** Re-bind telephony signal after READ_PHONE_STATE / READ_BASIC_PHONE_STATE. */
    fun onPhoneStatePermissionResult(granted: Boolean) {
        if (granted) {
            // Restart so TelephonyCallback can attach with the new grant.
            stopConnectivityUpdates()
            startConnectivityUpdates()
        }
    }

    /** Re-bind Bluetooth after BLUETOOTH_CONNECT (API 31+). */
    fun onBluetoothPermissionResult(granted: Boolean) {
        if (granted) {
            stopConnectivityUpdates()
            startConnectivityUpdates()
        }
    }

    /** Bottom mode bar tap — GLIDE → COMBAT → BALLISTIC → GLIDE. */
    fun cycleRideMode() {
        _data.update { current ->
            val next = RideMode.next(current.rideMode)
            rideModeStore.writeRideMode(next)
            current.copy(rideMode = next)
        }
    }

    /** Set a specific ride mode (KTM control-centre mode pills). */
    fun setRideMode(mode: String) {
        val canonical = RideMode.canonicalize(mode) ?: return
        _data.update { current ->
            if (current.rideMode.equals(canonical, ignoreCase = true)) current
            else {
                rideModeStore.writeRideMode(canonical)
                current.copy(rideMode = canonical)
            }
        }
    }

    /** Control Centre → Settings → Vehicle — rename the identity tile bike. */
    fun setBikeName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        bikeNameStore.writeBikeName(trimmed)
        _data.update { current -> current.copy(bikeName = trimmed) }
    }

    /** Control Centre → Settings → Themes — switch brand accent pack. */
    fun setBrandTheme(theme: String) {
        val canonical = BrandTheme.canonicalize(theme) ?: return
        brandThemeStore.writeBrandTheme(canonical)
        _data.update { current -> current.copy(brandTheme = canonical) }
    }

    fun startSpeedUpdates() {
        if (!speedRepository.hasLocationPermission()) return
        if (speedJob?.isActive == true) return

        speedJob = viewModelScope.launch {
            speedRepository.observeSpeed().collect { sample ->
                // Repository owns the stationary gate — do not second-guess with a low threshold.
                val wasMoving = isMoving
                isMoving = sample.isMoving
                ridingSmoothnessRepository.updateSpeed(sample.speedKmh, sample.isMoving)
                speedDisciplineRepository.updateSpeed(sample.speedKmh, sample.isMoving)

                if (sample.isMoving && sample.deltaMeters > 0f) {
                    ensureTripStarted()
                    tripMeters += sample.deltaMeters
                    totalMeters += sample.deltaMeters
                    odometerStore.writeTotalMeters(totalMeters)
                }

                if (sample.isMoving && !wasMoving) {
                    ensureTripStarted()
                    cancelIdleFinalize()
                    movingSegmentStartMs = SystemClock.elapsedRealtime()
                    startTripTicker()
                } else if (!sample.isMoving && wasMoving) {
                    pauseMovingClock()
                    stopTripTicker()
                    persistDraft(force = true)
                    scheduleIdleFinalize()
                }

                publishTripMetrics(
                    speedKmh = if (sample.isMoving) {
                        formatSpeedDigits(sample.speedKmh.roundToInt())
                    } else {
                        "———"
                    },
                    isMoving = sample.isMoving,
                    gpsAvailable = sample.gpsAvailable,
                    altitudeText = formatAltitude(sample.altitudeM)
                )
                persistDraft(force = false)
            }
        }
    }

    /** Mark wall-clock start the first time this ride sees motion. */
    private fun ensureTripStarted() {
        if (tripStartedAtEpochMs == null) {
            tripStartedAtEpochMs = System.currentTimeMillis()
        }
    }

    /**
     * Resume an in-progress ride after process death. Clean exits finalize via
     * [finalizeOpenTrip] and clear the draft, so a leftover draft means the
     * previous process never sealed the ride.
     */
    private fun recoverDraftIfNeeded() {
        val draft = tripStore.readDraft() ?: return
        tripStartedAtEpochMs = draft.startedAtEpochMs
        tripMeters = draft.distanceMeters.coerceAtLeast(0.0)
        movingMsAccumulated = draft.movingMs.coerceAtLeast(0L)
        movingSegmentStartMs = null
        _data.update { current ->
            current.copy(
                tripKm = formatDistanceKm(tripMeters),
                avgSpeedKmh = formatAvgSpeed(tripMeters, movingMsAccumulated),
                tripTimeText = formatTripTime(movingMsAccumulated)
            )
        }
    }

    private fun persistDraft(force: Boolean) {
        val started = tripStartedAtEpochMs ?: return
        if (tripMeters < 1.0) return
        val now = SystemClock.elapsedRealtime()
        if (!force && now - lastDraftWriteElapsedMs < DRAFT_WRITE_INTERVAL_MS) return
        lastDraftWriteElapsedMs = now
        val current = _data.value
        tripStore.writeDraft(
            TripStore.TripDraft(
                startedAtEpochMs = started,
                distanceMeters = tripMeters,
                movingMs = movingElapsedMs(),
                rideMode = current.rideMode,
                efficiencyBars = current.efficiencyBars,
                disciplineBars = current.disciplineBars
            )
        )
    }

    /** After a long stop, seal the ride so the next movement starts a new trip. */
    private fun scheduleIdleFinalize() {
        cancelIdleFinalize()
        if (tripStartedAtEpochMs == null) return
        if (tripMeters < SavedTrip.MIN_SAVE_METERS) return
        idleFinalizeJob = viewModelScope.launch {
            delay(IDLE_FINALIZE_MS)
            if (!isMoving) {
                finalizeOpenTrip()
            }
        }
    }

    private fun cancelIdleFinalize() {
        idleFinalizeJob?.cancel()
        idleFinalizeJob = null
    }

    /** Seal the open ride into history when it has enough distance. */
    private fun finalizeOpenTrip() {
        cancelIdleFinalize()
        if (isMoving) pauseMovingClock()
        val started = tripStartedAtEpochMs
        val meters = tripMeters
        val movingMs = movingElapsedMs()
        tripStore.clearDraft()
        tripStartedAtEpochMs = null
        tripMeters = 0.0
        movingMsAccumulated = 0L
        movingSegmentStartMs = null
        if (started != null && meters >= SavedTrip.MIN_SAVE_METERS) {
            val current = _data.value
            appendTrip(
                SavedTrip(
                    startedAtEpochMs = started,
                    endedAtEpochMs = System.currentTimeMillis(),
                    distanceMeters = meters,
                    movingMs = movingMs,
                    rideMode = current.rideMode,
                    efficiencyBars = current.efficiencyBars,
                    disciplineBars = current.disciplineBars
                )
            )
        }
        publishTripMetrics()
    }

    private fun appendTrip(trip: SavedTrip) {
        val next = (listOf(trip) + _trips.value)
            .sortedByDescending { it.endedAtEpochMs }
            .distinctBy { it.id }
        _trips.value = next
        tripStore.writeTrips(next)
    }

    /** Advancing avg + trip clock only while speed > 0. */
    private fun startTripTicker() {
        if (tripTickerJob?.isActive == true) return
        tripTickerJob = viewModelScope.launch {
            while (isActive && movingSegmentStartMs != null) {
                publishTripMetrics()
                delay(1_000L)
            }
        }
    }

    private fun stopTripTicker() {
        tripTickerJob?.cancel()
        tripTickerJob = null
    }

    private fun pauseMovingClock() {
        val started = movingSegmentStartMs ?: return
        movingMsAccumulated += (SystemClock.elapsedRealtime() - started).coerceAtLeast(0L)
        movingSegmentStartMs = null
    }

    private fun movingElapsedMs(): Long {
        val open = movingSegmentStartMs ?: return movingMsAccumulated
        return movingMsAccumulated + (SystemClock.elapsedRealtime() - open).coerceAtLeast(0L)
    }

    private fun publishTripMetrics(
        speedKmh: String? = null,
        isMoving: Boolean? = null,
        gpsAvailable: Boolean? = null,
        altitudeText: String? = null
    ) {
        val elapsedMs = movingElapsedMs()
        _data.update { current ->
            current.copy(
                speedKmh = speedKmh ?: current.speedKmh,
                isMoving = isMoving ?: current.isMoving,
                gpsAvailable = gpsAvailable ?: current.gpsAvailable,
                altitudeText = altitudeText ?: current.altitudeText,
                odometerKm = formatDistanceKm(totalMeters),
                tripKm = formatDistanceKm(tripMeters),
                avgSpeedKmh = formatAvgSpeed(tripMeters, elapsedMs),
                tripTimeText = formatTripTime(elapsedMs)
            )
        }
    }

    fun startBatteryUpdates() {
        if (batteryJob?.isActive == true) return

        batteryJob = viewModelScope.launch {
            batteryRepository.observeBattery().collect { sample ->
                _data.update { current ->
                    current.copy(
                        batteryPercent = sample.percent,
                        isCharging = sample.isCharging,
                        phoneTempHigh = sample.isHot
                    )
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

    fun startAmbientLightUpdates() {
        if (ambientLightJob?.isActive == true) return

        ambientLightJob = viewModelScope.launch {
            ambientLightRepository.observeAmbientLight().collect { sample ->
                _data.update { current ->
                    if (current.isDarkMode == sample.isDark) current
                    else current.copy(isDarkMode = sample.isDark)
                }
            }
        }
    }

    fun startSmoothnessUpdates() {
        if (smoothnessJob?.isActive == true) return

        smoothnessJob = viewModelScope.launch {
            ridingSmoothnessRepository.observeSmoothness().collect { sample ->
                _data.update { current ->
                    if (current.efficiencyBars == sample.bars &&
                        current.efficiencyActive == sample.confident
                    ) {
                        current
                    } else {
                        current.copy(
                            efficiencyBars = sample.bars,
                            efficiencyActive = sample.confident
                        )
                    }
                }
            }
        }
    }

    fun startDisciplineUpdates() {
        if (disciplineJob?.isActive == true) return

        disciplineJob = viewModelScope.launch {
            speedDisciplineRepository.observeDiscipline().collect { sample ->
                _data.update { current ->
                    if (current.disciplineBars == sample.bars &&
                        current.disciplineActive == sample.confident
                    ) {
                        current
                    } else {
                        current.copy(
                            disciplineBars = sample.bars,
                            disciplineActive = sample.confident
                        )
                    }
                }
            }
        }
    }

    fun startConnectivityUpdates() {
        if (connectivityJob?.isActive == true) return

        connectivityJob = viewModelScope.launch {
            connectivityRepository.observeConnectivity().collect { sample ->
                _data.update { current ->
                    current.copy(
                        signalBars = sample.cellularBars,
                        wifiOn = sample.wifiConnected,
                        wifiBars = sample.wifiBars,
                        bluetoothOn = sample.bluetoothConnected
                    )
                }
            }
        }
    }

    private fun stopConnectivityUpdates() {
        connectivityJob?.cancel()
        connectivityJob = null
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
                        weatherIsDay = sample.isDay,
                        windSpeedText = formatWindSpeed(sample.windSpeedKmh)
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
        if (isMoving) pauseMovingClock()
        isMoving = false
        stopTripTicker()
        if (resetToIdle) {
            val elapsedMs = movingElapsedMs()
            // Keep live SoC + clock + distance + trip stats + weather + lean +
            // connectivity — clear speed/GPS.
            _data.update { current ->
                idle.copy(
                    batteryPercent = current.batteryPercent,
                    isCharging = current.isCharging,
                    phoneTempHigh = current.phoneTempHigh,
                    timeText = current.timeText,
                    odometerKm = formatDistanceKm(totalMeters),
                    tripKm = formatDistanceKm(tripMeters),
                    avgSpeedKmh = formatAvgSpeed(tripMeters, elapsedMs),
                    tripTimeText = formatTripTime(elapsedMs),
                    ambientTempC = current.ambientTempC,
                    weatherCondition = current.weatherCondition,
                    weatherIsDay = current.weatherIsDay,
                    windSpeedText = current.windSpeedText,
                    altitudeText = "--",
                    leanAngleDeg = current.leanAngleDeg,
                    leanAngleSignedDeg = current.leanAngleSignedDeg,
                    isDarkMode = current.isDarkMode,
                    signalBars = current.signalBars,
                    wifiOn = current.wifiOn,
                    wifiBars = current.wifiBars,
                    bluetoothOn = current.bluetoothOn,
                    efficiencyBars = current.efficiencyBars,
                    efficiencyActive = current.efficiencyActive,
                    disciplineBars = current.disciplineBars,
                    disciplineActive = current.disciplineActive,
                    rideMode = current.rideMode,
                    bikeName = current.bikeName,
                    brandTheme = current.brandTheme,
                    gpsAvailable = false
                )
            }
            ridingSmoothnessRepository.updateSpeed(0f, isMoving = false)
            speedDisciplineRepository.updateSpeed(0f, isMoving = false)
        }
    }

    private fun stopWeatherUpdates() {
        weatherJob?.cancel()
        weatherJob = null
    }

    override fun onCleared() {
        stopSpeedUpdates(resetToIdle = false)
        tripTickerJob?.cancel()
        tripTickerJob = null
        finalizeOpenTrip()
        stopWeatherUpdates()
        leanJob?.cancel()
        leanJob = null
        ambientLightJob?.cancel()
        ambientLightJob = null
        smoothnessJob?.cancel()
        smoothnessJob = null
        disciplineJob?.cancel()
        disciplineJob = null
        stopConnectivityUpdates()
        batteryJob?.cancel()
        batteryJob = null
        clockJob?.cancel()
        clockJob = null
        super.onCleared()
    }

    private companion object {
        /** Throttle draft disk writes while actively riding. */
        const val DRAFT_WRITE_INTERVAL_MS = 15_000L
        /** Seal a stopped ride after this long so the next movement is a new trip. */
        const val IDLE_FINALIZE_MS = 15 * 60 * 1000L

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

        fun formatAvgSpeed(meters: Double, movingMs: Long): String {
            if (movingMs < 1_000L || meters <= 0.0) return "———"
            val hours = movingMs / 3_600_000.0
            val kmh = ((meters / 1000.0) / hours).roundToInt().coerceIn(0, 999)
            return "%03d".format(kmh)
        }

        /** Moving time only, hours:minutes. */
        fun formatTripTime(movingMs: Long): String {
            val totalMin = (movingMs / 60_000L).coerceAtLeast(0L)
            val hours = totalMin / 60L
            val minutes = totalMin % 60L
            return "%d:%02d".format(hours, minutes)
        }

        fun formatAltitude(meters: Int?): String =
            if (meters == null) "--" else "%d m".format(meters)

        fun formatWindSpeed(kmh: Int): String =
            "%d km/h".format(kmh.coerceIn(0, 200))
    }
}
