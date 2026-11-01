package com.ultraviolette.navigator.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class BatterySample(
    /** Device charge level 0–100. */
    val percent: Int,
    /** True while plugged in / actively charging (or full on charger). */
    val isCharging: Boolean = false,
    /**
     * Battery pack temperature in °C ([BatteryManager.EXTRA_TEMPERATURE] / 10).
     * Used as the phone-temp proxy for the top-bar mode badge.
     */
    val temperatureC: Float = 0f,
    /** True when [temperatureC] is at or above [HOT_TEMP_C]. */
    val isHot: Boolean = false
)

/**
 * System battery SoC + temperature stream for the cluster top bar.
 */
class BatteryRepository(context: Context) {
    private val appContext = context.applicationContext

    fun observeBattery(): Flow<BatterySample> = callbackFlow {
        fun emitFrom(intent: Intent?) {
            if (intent == null) return
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level < 0 || scale <= 0) return
            val percent = ((level * 100f) / scale).toInt().coerceIn(0, 100)
            val status = intent.getIntExtra(
                BatteryManager.EXTRA_STATUS,
                BatteryManager.BATTERY_STATUS_UNKNOWN
            )
            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
            val isCharging = plugged != 0 ||
                status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
            // EXTRA_TEMPERATURE is tenths of a degree Celsius.
            val temperatureC =
                intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
                    .takeIf { it != Int.MIN_VALUE }
                    ?.div(10f)
                    ?: 0f
            trySend(
                BatterySample(
                    percent = percent,
                    isCharging = isCharging,
                    temperatureC = temperatureC,
                    isHot = temperatureC >= HOT_TEMP_C
                )
            )
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                emitFrom(intent)
            }
        }

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        // Sticky register also delivers the current level immediately.
        val sticky = ContextCompat.registerReceiver(
            appContext,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        emitFrom(sticky)

        awaitClose {
            appContext.unregisterReceiver(receiver)
        }
    }

    private companion object {
        /** Battery pack °C at which the top-bar phone badge flips to red while moving. */
        const val HOT_TEMP_C = 45f
    }
}
