package com.ultraviolette.navigator.time

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ClockSample(
    /** Cluster top-bar clock, e.g. "06:30 pm". */
    val timeText: String
)

/**
 * Device wall-clock stream for the cluster top-bar time.
 * Emits immediately, then once per minute at the next minute boundary.
 */
class ClockRepository {
    fun observeClock(): Flow<ClockSample> = flow {
        while (true) {
            emit(ClockSample(timeText = formatNow()))
            val now = LocalTime.now()
            val msToNextMinute =
                ((60 - now.second) * 1000L) - (now.nano / 1_000_000L)
            delay(msToNextMinute.coerceAtLeast(1L))
        }
    }

    private companion object {
        val formatter: DateTimeFormatter =
            DateTimeFormatter.ofPattern("hh:mm a", Locale.US)

        fun formatNow(): String =
            LocalTime.now().format(formatter).lowercase(Locale.US)
    }
}
