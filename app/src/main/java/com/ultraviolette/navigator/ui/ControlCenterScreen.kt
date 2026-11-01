package com.ultraviolette.navigator.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultraviolette.navigator.R
import com.ultraviolette.navigator.model.ClusterPlaceholders
import com.ultraviolette.navigator.model.RideMode
import com.ultraviolette.navigator.ui.components.ChamferShape
import com.ultraviolette.navigator.ui.components.PercentValue
import com.ultraviolette.navigator.ui.theme.ClusterUiFont
import com.ultraviolette.navigator.ui.theme.TopBarSoCFill
import com.ultraviolette.navigator.ui.theme.UltravioletteNavigatorTheme
import com.ultraviolette.navigator.ui.theme.clusterColors
import com.ultraviolette.navigator.ui.theme.rememberClusterAccent
import com.ultraviolette.navigator.weather.WeatherCondition

/** Slide distance for each tile entering from the right. */
private val TileEnterSlide = 56.dp
private const val TileEnterDurationMs = 320
/** Stagger between tiles in the same visual row (L→R). */
private const val TileEnterStaggerMs = 55
/** Extra pause before the next visual row starts. */
private const val TileEnterRowGapMs = 70

/**
 * Reading-order enter indices (left→right, top→bottom):
 * row0: Back, Identity, Battery
 * row1: Trips, RideMode, Sensors
 * row2: EFF, SPD, Google Maps
 */
private fun tileEnterDelayMs(order: Int): Int {
    val row = when (order) {
        in 0..2 -> 0
        in 3..5 -> 1
        else -> 2
    }
    val col = when (order) {
        in 0..2 -> order
        in 3..5 -> order - 3
        else -> order - 6
    }
    return row * (3 * TileEnterStaggerMs + TileEnterRowGapMs) + col * TileEnterStaggerMs
}

/** Slide in from the right + fade, staggered by [order]. */
@Composable
private fun Modifier.controlTileEnter(order: Int): Modifier {
    val density = LocalDensity.current
    val slidePx = with(density) { TileEnterSlide.toPx() }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(order) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = TileEnterDurationMs,
                delayMillis = tileEnterDelayMs(order),
                easing = FastOutSlowInEasing
            )
        )
    }
    return this.graphicsLayer {
        translationX = (1f - progress.value) * slidePx
        alpha = progress.value
    }
}

/**
 * Control Centre body (below [com.ultraviolette.navigator.ui.components.TopStatusBar]).
 *
 * Keeps UV modular-tile intent from docs/control-center-2.jpg, but only tiles that
 * make sense on a phone navigator: brand, battery, trips, ride mode, sensors,
 * EFF/SPD, and Google Maps launch. Drops bike ECU blocks (ABS / TC / regen /
 * hill hold / TPMS / dashcam / radar).
 */
@Composable
fun ControlCenterContent(
    data: ClusterPlaceholders,
    onClose: () -> Unit,
    onRideModeClick: () -> Unit,
    onTripsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = clusterColors()
    val accent = rememberClusterAccent(data)
    val gap = 6.dp

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(gap)
    ) {
        // Top row — brand + identity + battery
        Row(
            Modifier
                .fillMaxWidth()
                .weight(0.30f),
            horizontalArrangement = Arrangement.spacedBy(gap)
        ) {
            BackTile(
                onClick = onClose,
                modifier = Modifier
                    .weight(0.16f)
                    .fillMaxHeight()
                    .controlTileEnter(0)
            )
            IdentityTile(
                onClick = onClose,
                modifier = Modifier
                    .weight(0.44f)
                    .fillMaxHeight()
                    .controlTileEnter(1)
            )
            BatteryTile(
                percent = data.batteryPercent,
                isCharging = data.isCharging,
                modifier = Modifier
                    .weight(0.40f)
                    .fillMaxHeight()
                    .controlTileEnter(2)
            )
        }

        // Lower band — trips / scores | ride mode | sensors / Google Maps
        Row(
            Modifier
                .fillMaxWidth()
                .weight(0.70f),
            horizontalArrangement = Arrangement.spacedBy(gap)
        ) {
            Column(
                Modifier
                    .weight(0.28f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(gap)
            ) {
                TripsTile(
                    tripKm = data.tripKm,
                    avgSpeed = data.avgSpeedKmh,
                    odometer = data.odometerKm,
                    onClick = onTripsClick,
                    modifier = Modifier
                        .weight(0.55f)
                        .fillMaxWidth()
                        .controlTileEnter(3)
                )
                Row(
                    Modifier
                        .weight(0.45f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap)
                ) {
                    ScoreTile(
                        label = data.efficiencyLabel,
                        bars = data.efficiencyBars,
                        active = data.efficiencyActive,
                        accent = accent.primary,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .controlTileEnter(6)
                    )
                    ScoreTile(
                        label = data.disciplineLabel,
                        bars = data.disciplineBars,
                        active = data.disciplineActive,
                        accent = accent.primary,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .controlTileEnter(7)
                    )
                }
            }

            RideModeTile(
                rideMode = data.rideMode,
                accent = accent.primary,
                onClick = onRideModeClick,
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight()
                    .controlTileEnter(4)
            )

            Column(
                Modifier
                    .weight(0.30f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(gap)
            ) {
                SensorsTile(
                    data = data,
                    modifier = Modifier
                        .weight(0.55f)
                        .fillMaxWidth()
                        .controlTileEnter(5)
                )
                GoogleMapsTile(
                    modifier = Modifier
                        .weight(0.45f)
                        .fillMaxWidth()
                        .controlTileEnter(8)
                )
            }
        }
    }
}

@Composable
private fun ControlTile(
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
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        content()
    }
}

@Composable
private fun TileTitle(text: String, modifier: Modifier = Modifier) {
    val colors = clusterColors()
    // Dashford is a single Regular cut — Bold maps to the same glyphs. Stroke
    // underlay adds the visual weight tile labels need (TRIPS / EFF / SPD / …).
    val style = TextStyle(
        color = colors.muted,
        fontFamily = ClusterUiFont,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.1.sp
    )
    Box(modifier) {
        Text(
            text = text,
            style = style.copy(
                drawStyle = Stroke(
                    width = 2.6f,
                    miter = 10f,
                    join = StrokeJoin.Round,
                    cap = StrokeCap.Round
                )
            ),
            maxLines = 1
        )
        Text(text = text, style = style, maxLines = 1)
    }
}

@Composable
private fun BackTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = clusterColors()
    ControlTile(modifier = modifier, onClick = onClick) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            BackChevron(
                color = colors.text,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/** 1200 viewBox back chevron — filled left-pointing wedge. */
@Composable
private fun BackChevron(
    color: Color,
    modifier: Modifier = Modifier
) {
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
private fun IdentityTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = clusterColors()
    ControlTile(modifier = modifier, onClick = onClick) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            // Dashford has one cut — stroke underlay fattens "MY X-47".
            val titleStyle = TextStyle(
                color = colors.text,
                fontFamily = ClusterUiFont,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp
            )
            Box {
                Text(
                    text = "MY X-47",
                    style = titleStyle.copy(
                        drawStyle = Stroke(
                            width = 3.8f,
                            miter = 10f,
                            join = StrokeJoin.Round,
                            cap = StrokeCap.Round
                        )
                    ),
                    maxLines = 1
                )
                Text(text = "MY X-47", style = titleStyle, maxLines = 1)
            }
            Text(
                text = "NAVIGATOR",
                color = colors.muted,
                fontFamily = ClusterUiFont,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.4.sp
            )
        }
    }
}

@Composable
private fun BatteryTile(
    percent: Int,
    isCharging: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = clusterColors()
    ControlTile(modifier = modifier) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TileTitle("BATTERY")
                if (isCharging) {
                    Text(
                        text = "CHARGING",
                        color = TopBarSoCFill,
                        fontFamily = ClusterUiFont,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }
            }
            PercentValue(
                value = percent,
                color = colors.text,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BatteryBar(percent = percent)
                Text(
                    text = "Phone SoC",
                    color = colors.muted,
                    fontFamily = ClusterUiFont,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun BatteryBar(percent: Int, modifier: Modifier = Modifier) {
    val colors = clusterColors()
    val shape = remember { ChamferShape(3.dp) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(shape)
            .background(colors.batteryTrack)
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth((percent.coerceIn(0, 100) / 100f))
                .clip(shape)
                .background(TopBarSoCFill)
        )
    }
}

@Composable
private fun TripsTile(
    tripKm: String,
    avgSpeed: String,
    odometer: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = clusterColors()
    ControlTile(modifier = modifier, onClick = onClick) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            TileTitle("TRIPS")
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TripStat(value = tripKm.removeSuffix(" km"), unit = "KM", label = "This Ride")
                TripStat(
                    value = if (avgSpeed == "———") "--" else avgSpeed,
                    unit = "KM/H",
                    label = "Avg. Speed"
                )
            }
            Text(
                text = "Odo  $odometer",
                color = colors.muted,
                fontFamily = ClusterUiFont,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun TripStat(value: String, unit: String, label: String) {
    val colors = clusterColors()
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                color = colors.text,
                fontFamily = ClusterUiFont,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = unit,
                color = colors.muted,
                fontFamily = ClusterUiFont,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 3.dp)
            )
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

@Composable
private fun ScoreTile(
    label: String,
    bars: Int,
    active: Boolean,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val colors = clusterColors()
    ControlTile(modifier = modifier) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            TileTitle(label)
            Text(
                text = if (active) bars.toString() else "--",
                color = colors.text,
                fontFamily = ClusterUiFont,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            ScorePips(bars = bars, active = active, accent = accent)
        }
    }
}

@Composable
private fun ScorePips(
    bars: Int,
    active: Boolean,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val colors = clusterColors()
    val lit = bars.coerceIn(0, 10)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(10) { index ->
            val on = active && index < lit
            Box(
                Modifier
                    .weight(1f)
                    .height(6.dp)
                    .background(if (on) accent else colors.stripe)
            )
        }
    }
}

@Composable
private fun RideModeTile(
    rideMode: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = clusterColors()
    val modes = listOf(RideMode.GLIDE, RideMode.COMBAT, RideMode.BALLISTIC)
    ControlTile(modifier = modifier, onClick = onClick) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TileTitle("RIDE MODE")
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                modes.forEach { mode ->
                    val selected = rideMode.equals(mode, ignoreCase = true)
                    RideModeRow(
                        label = mode,
                        selected = selected,
                        accent = accent,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }
            }
            Text(
                text = "Tap to cycle",
                color = colors.muted,
                fontFamily = ClusterUiFont,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun RideModeRow(
    label: String,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val colors = clusterColors()
    val shape = remember { ChamferShape(6.dp) }
    Row(
        modifier = modifier
            .clip(shape)
            .background(if (selected) accent.copy(alpha = 0.16f) else colors.pill)
            .border(
                width = 1.dp,
                color = if (selected) accent else colors.pillBorder,
                shape = shape
            )
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = if (selected) colors.text else colors.muted,
            fontFamily = ClusterUiFont,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.0.sp
        )
        Text(
            text = if (selected) "ON" else "OFF",
            color = if (selected) accent else colors.muted,
            fontFamily = ClusterUiFont,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SensorsTile(
    data: ClusterPlaceholders,
    modifier: Modifier = Modifier
) {
    ControlTile(modifier = modifier) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            TileTitle("SENSORS")
            SensorRow(
                label = "GPS",
                value = if (data.gpsAvailable) "LOCK" else "——"
            )
            SensorRow(label = "ALT", value = data.altitudeText)
            SensorRow(label = "LEAN", value = "${data.leanAngleDeg}°")
            SensorRow(
                label = "WX",
                value = weatherShort(data.weatherCondition, data.ambientTempC)
            )
        }
    }
}

@Composable
private fun SensorRow(label: String, value: String) {
    val colors = clusterColors()
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = colors.muted,
            fontFamily = ClusterUiFont,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp
        )
        Text(
            text = value,
            color = colors.text,
            fontFamily = ClusterUiFont,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End
        )
    }
}

private fun weatherShort(condition: WeatherCondition, tempC: String): String {
    val tag = when (condition) {
        WeatherCondition.CLEAR -> "CLR"
        WeatherCondition.PARTLY_CLOUDY -> "PCL"
        WeatherCondition.CLOUDY -> "CLD"
        WeatherCondition.FOG -> "FOG"
        WeatherCondition.DRIZZLE -> "DRZ"
        WeatherCondition.RAIN -> "RAIN"
        WeatherCondition.SNOW -> "SNOW"
        WeatherCondition.THUNDER -> "THDR"
    }
    return if (tempC == "--") tag else "$tempC° $tag"
}

@Composable
private fun GoogleMapsTile(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    ControlTile(
        modifier = modifier,
        onClick = { openGoogleMaps(context) }
    ) {
        Box(Modifier.fillMaxSize()) {
            TileTitle("GOOGLE MAPS", Modifier.align(Alignment.TopStart))
            Image(
                painter = painterResource(R.drawable.ic_google_maps),
                contentDescription = "Google Maps",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(48.dp)
            )
        }
    }
}

private fun openGoogleMaps(context: Context) {
    val mapsIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0")).apply {
        setPackage("com.google.android.apps.maps")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(mapsIntent)
    } catch (_: ActivityNotFoundException) {
        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://maps.google.com")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(webIntent)
    }
}

@Preview(name = "Control Centre", showBackground = true, widthDp = 854, heightDp = 400)
@Composable
private fun ControlCenterPreview() {
    UltravioletteNavigatorTheme {
        ControlCenterContent(
            data = ClusterPlaceholders(
                tripKm = "0.0 km",
                avgSpeedKmh = "009",
                odometerKm = "12 km",
                batteryPercent = 72,
                gpsAvailable = true,
                altitudeText = "912 m",
                leanAngleDeg = "4",
                ambientTempC = "28",
                efficiencyBars = 6,
                efficiencyActive = true,
                disciplineBars = 4,
                disciplineActive = true
            ),
            onClose = {},
            onRideModeClick = {}
        )
    }
}
