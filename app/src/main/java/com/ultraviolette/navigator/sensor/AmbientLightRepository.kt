package com.ultraviolette.navigator.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class AmbientLightSample(
    /** True when ambient lux is low enough for the dark cluster palette. */
    val isDark: Boolean,
    /** Latest lux reading, or null when the sensor is unavailable. */
    val lux: Float? = null
)

/**
 * Ambient light → cluster day/night palette.
 *
 * Uses [Sensor.TYPE_LIGHT] with hysteresis so the UI does not flicker at the
 * threshold (dark below [DARK_BELOW_LUX], light above [LIGHT_ABOVE_LUX]).
 */
class AmbientLightRepository(context: Context) {
    private val appContext = context.applicationContext
    private val sensorManager =
        appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    fun observeAmbientLight(): Flow<AmbientLightSample> = callbackFlow {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        if (sensor == null) {
            trySend(AmbientLightSample(isDark = false, lux = null))
            close()
            return@callbackFlow
        }

        var isDark = false
        var hasEmitted = false

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type != Sensor.TYPE_LIGHT) return
                if (event.values.isEmpty()) return
                val lux = event.values[0]
                val next = when {
                    lux < DARK_BELOW_LUX -> true
                    lux > LIGHT_ABOVE_LUX -> false
                    else -> isDark
                }
                if (next != isDark || !hasEmitted) {
                    isDark = next
                    hasEmitted = true
                    trySend(AmbientLightSample(isDark = isDark, lux = lux))
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        val registered = sensorManager.registerListener(
            listener,
            sensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        if (!registered) {
            trySend(AmbientLightSample(isDark = false, lux = null))
            close()
            return@callbackFlow
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }

    private companion object {
        /** Enter dark mode under this lux (dim indoor / night). */
        const val DARK_BELOW_LUX = 20f
        /** Leave dark mode above this lux (clear hysteresis band). */
        const val LIGHT_ABOVE_LUX = 80f
    }
}
