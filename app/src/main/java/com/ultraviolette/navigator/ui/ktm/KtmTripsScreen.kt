package com.ultraviolette.navigator.ui.ktm

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultraviolette.navigator.model.ClusterPlaceholders
import com.ultraviolette.navigator.model.SavedTrip
import com.ultraviolette.navigator.ui.theme.KtmMuted
import com.ultraviolette.navigator.ui.theme.KtmOrange
import com.ultraviolette.navigator.ui.theme.ktmRideModeLabel
import com.ultraviolette.navigator.ui.theme.rememberKtmChrome
import com.ultraviolette.navigator.ui.theme.rememberKtmUiFont

private data class KtmTripEntry(
    val title: String,
    val whenText: String,
    val distance: String,
    val avg: String,
    val duration: String,
    val rideMode: String,
    val isCurrent: Boolean
)

/**
 * KTM trips history — same pill-menu + detail shell as the control centre.
 */
@Composable
fun KtmTripsContent(
    data: ClusterPlaceholders,
    trips: List<SavedTrip>,
    onClose: () -> Unit,
    onEndRide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chrome = rememberKtmChrome(data)
    val uiFont = rememberKtmUiFont()
    val hasCurrentRide = data.tripKm != "0.0 km" && data.tripKm != "0 km"

    val entries = buildList {
        if (hasCurrentRide) {
            add(
                KtmTripEntry(
                    title = "This ride",
                    whenText = "In progress",
                    distance = data.tripKm,
                    avg = if (data.avgSpeedKmh == "———") "——" else data.avgSpeedKmh.trimStart('0').ifEmpty { "0" },
                    duration = data.tripTimeText,
                    rideMode = ktmRideModeLabel(data.rideMode),
                    isCurrent = true
                )
            )
        }
        trips.forEach { trip ->
            add(
                KtmTripEntry(
                    title = trip.dateText,
                    whenText = trip.timeRangeText,
                    distance = trip.distanceKmText,
                    avg = trip.avgSpeedText,
                    duration = trip.durationText,
                    rideMode = ktmRideModeLabel(trip.rideMode),
                    isCurrent = false
                )
            )
        }
    }

    var selectedIndex by remember(entries.size) { mutableIntStateOf(0) }
    val selected = entries.getOrNull(selectedIndex.coerceIn(0, (entries.size - 1).coerceAtLeast(0)))

    KtmSubmenuShell(
        menuTitles = if (entries.isEmpty()) listOf("No trips") else entries.map { it.title },
        selectedIndex = if (entries.isEmpty()) 0 else selectedIndex.coerceIn(0, entries.lastIndex),
        onSelect = { if (entries.isNotEmpty()) selectedIndex = it },
        onBack = onClose,
        speedText = ktmSpeedDigits(data.speedKmh),
        gearText = chrome.gear,
        modeLabel = chrome.modeLabel,
        bottomTrailing = if (hasCurrentRide) {
            {
                Text(
                    text = "END RIDE",
                    color = KtmOrange,
                    fontFamily = uiFont,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onEndRide
                        )
                        .padding(horizontal = 4.dp)
                )
            }
        } else null,
        modifier = modifier
    ) {
        if (selected == null) {
            KtmDetailColumn {
                KtmDetailTitle("TRIPS")
                Text(
                    text = "NO TRIPS YET",
                    color = Color.White,
                    fontFamily = uiFont,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Rides are saved when you stop for a while\nor tap End Ride.",
                    color = KtmMuted,
                    fontFamily = uiFont,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            KtmDetailColumn {
                KtmDetailTitle(if (selected.isCurrent) "THIS RIDE" else "TRIP")
                Text(
                    text = selected.title.uppercase(),
                    color = Color.White,
                    fontFamily = uiFont,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = selected.whenText,
                    color = KtmMuted,
                    fontFamily = uiFont,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                KtmDetailStat("DISTANCE", selected.distance)
                KtmDetailStat("AVG SPEED", "${selected.avg} km/h")
                KtmDetailStat("TIME", selected.duration)
                KtmDetailStat("MODE", selected.rideMode)
                Spacer(Modifier.height(12.dp))
                if (selected.isCurrent) {
                    KtmDetailCta("End ride →", onClick = onEndRide)
                }
            }
        }
    }
}
