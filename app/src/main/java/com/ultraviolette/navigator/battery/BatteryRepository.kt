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
    val isCharging: Boolean = false
)

/**
 * System battery SoC stream for the cluster top-bar percent + green fill.
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
            trySend(BatterySample(percent = percent, isCharging = isCharging))
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
}
