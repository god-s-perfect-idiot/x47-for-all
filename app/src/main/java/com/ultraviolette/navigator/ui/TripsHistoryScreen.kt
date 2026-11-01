package com.ultraviolette.navigator.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultraviolette.navigator.model.ClusterPlaceholders
import com.ultraviolette.navigator.model.RideMode
import com.ultraviolette.navigator.model.SavedTrip
import com.ultraviolette.navigator.ui.components.ChamferShape
import com.ultraviolette.navigator.ui.components.clusterTileEnter
import com.ultraviolette.navigator.ui.components.clusterTileEnterLinearDelayMs
import com.ultraviolette.navigator.ui.ktm.KtmTripsContent
import com.ultraviolette.navigator.ui.theme.ClusterUiFont
import com.ultraviolette.navigator.ui.theme.UltravioletteNavigatorTheme
import com.ultraviolette.navigator.ui.theme.clusterColors
import com.ultraviolette.navigator.ui.theme.isKtmTheme
import com.ultraviolette.navigator.ui.theme.rememberClusterAccent

/**
 * Saved-ride list opened from the Control Centre TRIPS tile.
 * Current ride sits on top; past rides scroll below newest-first.
 * KTM uses the shared pill-menu shell ([KtmTripsContent]).
 */
@Composable
fun TripsHistoryContent(
    data: ClusterPlaceholders,
    trips: List<SavedTrip>,
    onClose: () -> Unit,
    onEndRide: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isKtmTheme(data.brandTheme)) {
        KtmTripsContent(
            data = data,
            trips = trips,
            onClose = onClose,
            onEndRide = onEndRide,
            modifier = modifier
        )
        return
    }

    val colors = clusterColors()
    val accent = rememberClusterAccent(data)
    val hasCurrentRide = data.tripKm != "0.0 km" && data.tripKm != "0 km"
    val tripCount = trips.size + if (hasCurrentRide) 1 else 0
    val headerTileCount = if (hasCurrentRide) 3 else 2

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TripsBackTile(
                onClick = onClose,
                modifier = Modifier
                    .width(64.dp)
                    .fillMaxHeight()
                    .clusterTileEnter(clusterTileEnterLinearDelayMs(0))
            )
            TripsHeaderTile(
                count = tripCount,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clusterTileEnter(clusterTileEnterLinearDelayMs(1))
            )
            if (hasCurrentRide) {
                EndRideTile(
                    accent = accent.primary,
                    onClick = onEndRide,
                    modifier = Modifier
                        .width(110.dp)
                        .fillMaxHeight()
                        .clusterTileEnter(clusterTileEnterLinearDelayMs(2))
                )
            }
        }

        if (!hasCurrentRide && trips.isEmpty()) {
            EmptyTripsTile(
                modifier = Modifier
                    .fillMaxSize()
                    .clusterTileEnter(clusterTileEnterLinearDelayMs(headerTileCount))
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(bottom = 4.dp)
            ) {
                if (hasCurrentRide) {
                    item(key = "current") {
                        TripRowTile(
                            title = "THIS RIDE",
                            whenText = "In progress",
                            distance = data.tripKm,
                            avg = if (data.avgSpeedKmh == "———") "——" else data.avgSpeedKmh.trimStart('0').ifEmpty { "0" },
                            duration = data.tripTimeText,
                            rideMode = data.rideMode,
                            accent = accent.primary,
                            highlight = true,
                            modifier = Modifier.clusterTileEnter(
                                clusterTileEnterLinearDelayMs(headerTileCount)
                            )
                        )
                    }
                }
                itemsIndexed(trips, key = { _, trip -> trip.id }) { index, trip ->
                    val enterOrder = headerTileCount + (if (hasCurrentRide) 1 else 0) + index
                    TripRowTile(
                        title = trip.dateText.uppercase(),
                        whenText = trip.timeRangeText,
                        distance = trip.distanceKmText,
                        avg = trip.avgSpeedText,
                        duration = trip.durationText,
                        rideMode = trip.rideMode,
                        accent = accent.primary,
                        highlight = false,
                        modifier = Modifier.clusterTileEnter(
                            clusterTileEnterLinearDelayMs(enterOrder)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun TripsPanel(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val colors = clusterColors()
    val shape = remember { ChamferShape(10.dp) }
    val clickMod = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .clip(shape)
            .background(colors.background)
            .border(1.15.dp, colors.pillBorder, shape)
            .then(clickMod)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        content()
    }
}

@Composable
private fun TripsBackTile(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = clusterColors()
    TripsPanel(modifier = modifier, onClick = onClick) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            TripsBackChevron(color = colors.text, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun TripsBackChevron(color: Color, modifier: Modifier = Modifier) {
    val path = remember { PathParser().parsePathString(BackChevronPath).toPath() }
    Canvas(modifier) {
        val scale = size.minDimension / BackChevronViewBox
        withTransform({
            scale(scale, scale, pivot = Offset.Zero)
        }) {
            drawPath(path, color)
        }
    }
}

private const val BackChevronViewBox = 1200f
private const val BackChevronPath =
    "m210.632 599.989l178.735 178.735L810.633 1200l178.735-178.721l-421.267-421.29l421.267-421.266L810.645 0L389.378 421.267L210.655 599.989z"

@Composable
private fun TripsHeaderTile(count: Int, modifier: Modifier = Modifier) {
    val colors = clusterColors()
    TripsPanel(modifier = modifier) {
        Row(
            Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "TRIPS",
                    color = colors.text,
                    fontFamily = ClusterUiFont,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.0.sp
                )
                Text(
                    text = if (count == 0) "No rides yet" else "$count saved",
                    color = colors.muted,
                    fontFamily = ClusterUiFont,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun EndRideTile(
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TripsPanel(modifier = modifier, onClick = onClick) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "END RIDE",
                color = accent,
                fontFamily = ClusterUiFont,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EmptyTripsTile(modifier: Modifier = Modifier) {
    val colors = clusterColors()
    TripsPanel(modifier = modifier) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "NO TRIPS YET",
                    color = colors.text,
                    fontFamily = ClusterUiFont,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Rides are saved when you stop for a while\nor tap End Ride.",
                    color = colors.muted,
                    fontFamily = ClusterUiFont,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun TripRowTile(
    title: String,
    whenText: String,
    distance: String,
    avg: String,
    duration: String,
    rideMode: String,
    accent: Color,
    highlight: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = clusterColors()
    val shape = remember { ChamferShape(10.dp) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(shape)
            .background(if (highlight) accent.copy(alpha = 0.10f) else colors.background)
            .border(
                width = 1.15.dp,
                color = if (highlight) accent else colors.pillBorder,
                shape = shape
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            Modifier.weight(1.15f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                color = colors.text,
                fontFamily = ClusterUiFont,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp
            )
            Text(
                text = whenText,
                color = colors.muted,
                fontFamily = ClusterUiFont,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = rideMode,
                color = if (highlight) accent else colors.muted,
                fontFamily = ClusterUiFont,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
        }
        TripMetric(
            value = distance.removeSuffix(" km"),
            unit = "KM",
            label = "Distance",
            modifier = Modifier.weight(0.85f)
        )
        TripMetric(
            value = avg,
            unit = "KM/H",
            label = "Avg",
            modifier = Modifier.weight(0.75f)
        )
        TripMetric(
            value = duration,
            unit = "",
            label = "Time",
            modifier = Modifier.weight(0.70f)
        )
    }
}

@Composable
private fun TripMetric(
    value: String,
    unit: String,
    label: String,
    modifier: Modifier = Modifier
) {
    val colors = clusterColors()
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                color = colors.text,
                fontFamily = ClusterUiFont,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            if (unit.isNotEmpty()) {
                Spacer(Modifier.width(3.dp))
                Text(
                    text = unit,
                    color = colors.muted,
                    fontFamily = ClusterUiFont,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
        }
        Text(
            text = label,
            color = colors.muted,
            fontFamily = ClusterUiFont,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview(name = "Trips History", showBackground = true, widthDp = 854, heightDp = 400)
@Composable
private fun TripsHistoryPreview() {
    UltravioletteNavigatorTheme {
        TripsHistoryContent(
            data = ClusterPlaceholders(
                tripKm = "3.2 km",
                avgSpeedKmh = "028",
                tripTimeText = "0:12",
                rideMode = RideMode.COMBAT
            ),
            trips = listOf(
                SavedTrip(
                    startedAtEpochMs = System.currentTimeMillis() - 86_400_000L,
                    endedAtEpochMs = System.currentTimeMillis() - 86_000_000L,
                    distanceMeters = 12_400.0,
                    movingMs = 28 * 60_000L,
                    rideMode = RideMode.GLIDE,
                    efficiencyBars = 7,
                    disciplineBars = 5
                ),
                SavedTrip(
                    startedAtEpochMs = System.currentTimeMillis() - 172_800_000L,
                    endedAtEpochMs = System.currentTimeMillis() - 172_400_000L,
                    distanceMeters = 4_100.0,
                    movingMs = 14 * 60_000L,
                    rideMode = RideMode.BALLISTIC
                )
            ),
            onClose = {},
            onEndRide = {}
        )
    }
}
