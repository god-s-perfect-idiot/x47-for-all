package com.ultraviolette.navigator.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultraviolette.navigator.model.ClusterPlaceholders
import com.ultraviolette.navigator.ui.theme.ClusterGreen
import com.ultraviolette.navigator.ui.theme.ClusterOrange
import com.ultraviolette.navigator.ui.theme.ClusterRed
import com.ultraviolette.navigator.ui.theme.ClusterUiFont
import com.ultraviolette.navigator.ui.theme.TopBarSoCFill
import com.ultraviolette.navigator.ui.theme.clusterColors
import com.ultraviolette.navigator.ui.theme.rememberClusterAccent

/** Bottom width of the light center status notch. */
private val CenterBottomWidth: Dp = 196.dp
/** Bottom width of each gray side notch hugging the center. */
private val SideBottomWidth: Dp = 118.dp
private val TopBarHeight: Dp = 50.dp

/**
 * Geometric top status strip matching UV cluster refs:
 * light center notch + gray left/right notches (same 45° inverted combat slant).
 */
@Composable
fun TopStatusBar(
    data: ClusterPlaceholders,
    modifier: Modifier = Modifier
) {
    val colors = clusterColors()
    val accent = rememberClusterAccent(data)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(TopBarHeight)
    ) {
        TopBarGeometry(
            batteryPercent = data.batteryPercent,
            modifier = Modifier.fillMaxSize()
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Left gray notch — chevron + phone badge, flush to the center notch
            Row(
                modifier = Modifier
                    .width(SideBottomWidth + TopBarHeight)
                    .fillMaxHeight()
                    .padding(start = 10.dp, end = 18.dp, top = 4.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End)
            ) {
                RegenChevron(
                    color = accent.primary,
                    modifier = Modifier.size(24.dp)
                )
                ModeBadge(
                    isMoving = data.isMoving,
                    phoneTempHigh = data.phoneTempHigh
                )
            }

            // Center light notch — time / SoC / signal
            Row(
                modifier = Modifier
                    .width(CenterBottomWidth + TopBarHeight)
                    .fillMaxHeight()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = data.timeText,
                    color = colors.text,
                    fontFamily = ClusterUiFont,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.width(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (data.isCharging) {
                        ChargingBolt(
                            color = colors.text,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    PercentValue(
                        value = data.batteryPercent,
                        color = colors.text,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                CellularSignal(
                    bars = data.signalBars,
                    modifier = Modifier.size(15.dp, 13.dp)
                )
                Spacer(Modifier.width(5.dp))
                WifiGlyph(
                    connected = data.wifiOn,
                    bars = data.wifiBars,
                    modifier = Modifier.size(14.dp)
                )
                if (data.bluetoothOn) {
                    Spacer(Modifier.width(5.dp))
                    BluetoothGlyph(Modifier.size(13.dp))
                }
            }

            // Right gray notch — GNSS lock glyph
            Row(
                modifier = Modifier
                    .width(SideBottomWidth + TopBarHeight)
                    .fillMaxHeight()
                    .padding(start = 18.dp, end = 10.dp, top = 4.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                GpsSignalGlyph(
                    available = data.gpsAvailable,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun TopBarGeometry(
    batteryPercent: Int,
    modifier: Modifier = Modifier
) {
    val colors = clusterColors()
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // Same 45° as combat (run/rise = 1), inverted: wide top, clipped bottom.
        val slantRun = h
        val centerBottomW = CenterBottomWidth.toPx()
        val sideBottomW = SideBottomWidth.toPx()

        val bottomLeft = (w - centerBottomW) / 2f
        val bottomRight = bottomLeft + centerBottomW
        val topLeft = bottomLeft - slantRun
        val topRight = bottomRight + slantRun

        // Left gray notch — same slant, flush against the center flank
        val leftBottomOuter = bottomLeft - sideBottomW
        val leftTopOuter = topLeft - sideBottomW
        val leftWing = Path().apply {
            moveTo(leftTopOuter, 0f)
            lineTo(topLeft, 0f)
            lineTo(bottomLeft, h)
            lineTo(leftBottomOuter, h)
            close()
        }
        drawPath(leftWing, colors.panel)

        // Right gray notch — mirrored
        val rightBottomOuter = bottomRight + sideBottomW
        val rightTopOuter = topRight + sideBottomW
        val rightWing = Path().apply {
            moveTo(topRight, 0f)
            lineTo(rightTopOuter, 0f)
            lineTo(rightBottomOuter, h)
            lineTo(bottomRight, h)
            close()
        }
        drawPath(rightWing, colors.panel)

        // Center light notch
        val center = Path().apply {
            moveTo(topLeft, 0f)
            lineTo(topRight, 0f)
            lineTo(bottomRight, h)
            lineTo(bottomLeft, h)
            close()
        }
        drawPath(center, colors.topBarCenter)
        drawPath(center, colors.topBarCenterStroke, style = Stroke(1.15.dp.toPx()))

        // SoC bar — same 45° inward slant as center notch (wide top, clipped bottom)
        val barH = 5.5.dp.toPx()
        val barBottomPad = 6.dp.toPx()
        val barSidePad = 2.dp.toPx()
        val barTop = h - barH - barBottomPad
        val barLeft = bottomLeft + barSidePad
        val barRight = bottomRight - barSidePad
        val barWidth = (barRight - barLeft).coerceAtLeast(1f)
        // run/rise = 1 → parallel to notch flanks
        val cut = barH

        fun socPath(fillW: Float) = Path().apply {
            val fw = fillW.coerceIn(cut * 2f, barWidth)
            moveTo(barLeft, barTop)
            lineTo(barLeft + fw, barTop)
            lineTo(barLeft + fw - cut, barTop + barH)
            lineTo(barLeft + cut, barTop + barH)
            close()
        }
        drawPath(socPath(barWidth), colors.topBarBarTrack)
        val fill = (batteryPercent.coerceIn(0, 100) / 100f) * barWidth
        if (fill > cut * 2) {
            drawPath(socPath(fill), TopBarSoCFill)
        }
    }
}

@Composable
private fun ChargingBolt(
    color: Color,
    modifier: Modifier = Modifier
) {
    val path = remember { PathParser().parsePathString(ChargingBoltPath).toPath() }
    Canvas(modifier) {
        val scale = size.minDimension / ChargingBoltViewBox
        withTransform({
            scale(scale, scale, pivot = Offset.Zero)
        }) {
            drawPath(path, color)
        }
    }
}

private const val ChargingBoltViewBox = 24f
private const val ChargingBoltPath =
    "M17.168 8H13l.806-4.835A1 1 0 0 0 12.819 2H7.667a1 1 0 0 0-.986.835l-1.667 10A1 1 0 0 0 6 14h4v8l8.01-12.459A1 1 0 0 0 17.168 8"

/**
 * Phone badge in the left top-bar notch — always the phone glyph.
 * Off (parked): gray fill + white glyph.
 * Moving + cool phone: green fill + white glyph.
 * Moving + hot phone: red fill + white glyph.
 */
@Composable
private fun ModeBadge(
    isMoving: Boolean,
    phoneTempHigh: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = clusterColors()
    val fill = when {
        !isMoving -> colors.modeBadgeFill
        phoneTempHigh -> ClusterRed
        else -> ClusterGreen
    }
    Box(
        modifier = modifier.size(22.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val r = 3.dp.toPx()
            drawRoundRect(
                color = fill,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r)
            )
        }
        PhoneGlyph(
            color = Color.White,
            modifier = Modifier.size(11.dp)
        )
    }
}

@Composable
private fun PhoneGlyph(
    color: Color,
    modifier: Modifier = Modifier
) {
    val path = remember { PathParser().parsePathString(PhoneGlyphPath).toPath() }
    Canvas(modifier) {
        val scale = size.minDimension / PhoneGlyphViewBox
        withTransform({
            scale(scale, scale, pivot = Offset.Zero)
        }) {
            drawPath(path, color)
        }
    }
}

/** Smartphone glyph — 16 viewBox (home-button phone silhouette). */
private const val PhoneGlyphViewBox = 16f
private const val PhoneGlyphPath =
    "M3 2a2 2 0 0 1 2-2h6a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2zm6 11a1 1 0 1 0-2 0a1 1 0 0 0 2 0"

@Composable
private fun RegenChevron(
    color: Color,
    modifier: Modifier = Modifier
) {
    val path = remember { PathParser().parsePathString(RegenChevronPath).toPath() }
    Canvas(modifier) {
        val scale = size.minDimension / RegenChevronViewBox
        withTransform({
            scale(scale, scale, pivot = Offset.Zero)
        }) {
            drawPath(path, color)
        }
    }
}

/** Phosphor navigation-arrow — 256 viewBox (upward chevron mark). */
private const val RegenChevronViewBox = 256f
private const val RegenChevronPath =
    "M236.2 218.31A15.88 15.88 0 0 1 224 224a16.2 16.2 0 0 1-5.37-.92l-79.95-27a4 4 0 0 1-2.72-3.79V120a8 8 0 0 0-8.53-8a8.19 8.19 0 0 0-7.47 8.26v72a4 4 0 0 1-2.72 3.79l-79.95 27a16 16 0 0 1-19.26-22.92l95.97-168a16 16 0 0 1 27.89 0L237.9 200.1a15.89 15.89 0 0 1-1.7 18.21"

@Composable
private fun CellularSignal(bars: Int, modifier: Modifier = Modifier) {
    val colors = clusterColors()
    val lit = bars.coerceIn(0, CellularBarCount)
    val activeColor = when {
        lit <= 0 -> ClusterOrange
        lit <= 2 -> ClusterOrange
        else -> colors.text
    }
    Canvas(modifier) {
        val scaleX = size.width / CellularViewBox
        val scaleY = size.height / CellularViewBox
        withTransform({
            scale(scaleX, scaleY, pivot = Offset.Zero)
        }) {
            CellularBarPaths.forEachIndexed { index, path ->
                val on = index < lit
                drawPath(
                    path,
                    color = when {
                        lit <= 0 -> colors.signalInactive
                        on -> activeColor
                        else -> colors.signalInactive
                    }
                )
            }
        }
    }
}

@Composable
private fun WifiGlyph(
    connected: Boolean,
    bars: Int,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = connected,
        transitionSpec = {
            val spec = tween<Float>(WifiGlyphMorphMs, easing = FastOutSlowInEasing)
            (
                fadeIn(spec) + scaleIn(initialScale = 0.86f, animationSpec = spec)
                ) togetherWith (
                fadeOut(spec) + scaleOut(targetScale = 1.08f, animationSpec = spec)
                ) using SizeTransform(clip = false)
        },
        label = "wifiGlyph",
        modifier = modifier
    ) { isConnected ->
        val colors = clusterColors()
        if (isConnected) {
            val lit = bars.coerceIn(0, WifiArcCount)
            val strong = lit >= 2
            Canvas(Modifier.fillMaxSize()) {
                val scale = size.minDimension / WifiViewBox
                withTransform({
                    scale(scale, scale, pivot = Offset.Zero)
                }) {
                    // Arcs ordered outer → mid → core. Core always lit when connected.
                    WifiConnectedArcPaths.forEachIndexed { index, path ->
                        val needed = WifiArcCount - index
                        val on = lit >= needed || index == WifiArcCount - 1
                        drawPath(
                            path,
                            color = when {
                                !on -> colors.signalInactive
                                strong -> colors.text
                                else -> ClusterOrange
                            }
                        )
                    }
                }
            }
        } else {
            val paths = remember { WifiDisconnectedPaths }
            Canvas(Modifier.fillMaxSize()) {
                val scale = size.minDimension / WifiViewBox
                withTransform({
                    scale(scale, scale, pivot = Offset.Zero)
                }) {
                    paths.forEach { drawPath(it, ClusterOrange) }
                }
            }
        }
    }
}

/** Five rising bars — Ionicons “cellular” 512 viewBox. */
private const val CellularViewBox = 512f
private const val CellularBarCount = 5

private val CellularBarPaths: List<Path> by lazy {
    listOf(
        "M22.67 394.9v89.3h60.84v-89.3z",
        "M124 343.8v140.4h60.9V343.8z",
        "M225.4 273.6v210.6h61V273.6z",
        "M327 168.2v316h60.8v-316z",
        "M428.4 27.8v456.4h60.9V27.8z"
    ).map { PathParser().parsePathString(it).toPath() }
}

private const val WifiViewBox = 56f
private const val WifiArcCount = 3
private const val WifiGlyphMorphMs = 280

/** Connected Wi‑Fi arcs: outer, mid, core (56 viewBox). */
private val WifiConnectedArcPaths: List<Path> by lazy {
    listOf(
        "M5.465 25.035c.445.445 1.101.422 1.523-.023c5.532-5.883 12.844-8.977 21.024-8.977c8.226 0 15.562 3.117 21.07 9c.398.399 1.032.399 1.453-.047l3.118-3.117c.374-.398.374-.914.07-1.289c-5.297-6.516-15.258-11.32-25.711-11.32S7.598 14.066 2.3 20.582c-.328.375-.305.89.07 1.29Z",
        "M14.84 34.434c.469.492 1.078.445 1.523-.07c2.719-3.024 7.125-5.204 11.649-5.157c4.57-.047 8.953 2.203 11.695 5.227c.445.468 1.031.468 1.477-.024l3.492-3.445c.375-.375.422-.867.07-1.266c-3.398-4.195-9.703-7.289-16.734-7.289s-13.336 3.117-16.735 7.29c-.351.398-.304.866.07 1.265Z",
        "M28.012 46.738c.492 0 .937-.258 1.804-1.101l5.485-5.274c.351-.328.422-.843.117-1.242c-1.477-1.898-4.242-3.539-7.406-3.539c-3.235 0-6.047 1.711-7.5 3.68c-.211.328-.14.773.21 1.101l5.485 5.274c.867.843 1.313 1.101 1.805 1.101Z"
    ).map { PathParser().parsePathString(it).toPath() }
}

/** Disconnected Wi‑Fi (slash) — 56 viewBox, single compound path. */
private val WifiDisconnectedPaths: List<Path> by lazy {
    listOf(
        "M47.77 49.973a1.81 1.81 0 0 0 2.554 0c.68-.727.703-1.852 0-2.555L8.277 5.371c-.703-.703-1.851-.703-2.578 0c-.68.68-.68 1.875 0 2.555Zm1.312-20.344c.398.398 1.032.398 1.476-.047l3.095-3.117c.374-.399.374-.914.07-1.29c-5.414-6.421-15.258-11.32-25.711-11.32c-2.274 0-4.524.235-6.703.68l6.187 6.164c8.274-.164 15.797 3.024 21.586 8.93m-43.617-.047c.445.445 1.101.422 1.523-.023c2.813-3 6.117-5.25 9.75-6.75l-4.968-4.97c-3.797 1.946-7.079 4.5-9.47 7.337c-.327.375-.304.89.071 1.289Zm9.375 9.422c.469.469 1.078.445 1.523-.07c2.696-2.977 7.008-5.063 11.39-5.11l-5.952-5.93c-4.383 1.172-8.133 3.563-10.524 6.399c-.351.398-.304.89.07 1.266Zm28.64-2.297l1.196-1.148c.375-.375.422-.868.07-1.266c-2.25-2.695-5.742-4.945-9.82-6.14ZM28.035 51.332c.492 0 .938-.258 1.805-1.102l5.484-5.273c.352-.328.422-.844.117-1.242c-1.476-1.899-4.242-3.54-7.406-3.54c-3.234 0-6.047 1.712-7.5 3.68c-.21.329-.14.774.211 1.102l5.484 5.273c.868.844 1.313 1.102 1.805 1.102"
    ).map { PathParser().parsePathString(it).toPath() }
}

@Composable
private fun BluetoothGlyph(modifier: Modifier = Modifier) {
    val colors = clusterColors()
    val paths = remember { BluetoothStrokePaths }
    Canvas(modifier) {
        val scale = size.minDimension / BluetoothViewBox
        val stroke = Stroke(
            width = 1.5f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
        withTransform({
            scale(scale, scale, pivot = Offset.Zero)
        }) {
            paths.forEach { drawPath(it, colors.text, style = stroke) }
        }
    }
}

private const val BluetoothViewBox = 16f

/** Connected Bluetooth rune — 16 viewBox, stroke 1.5. */
private val BluetoothStrokePaths: List<Path> by lazy {
    listOf(
        "m3.75 11.25 8.5-6.25-4.5-3.25v12.5l4.5-3.25-8.5-6.25",
        "m1.75 8h1.5m9.5 0h1.5"
    ).map { PathParser().parsePathString(it).toPath() }
}

@Composable
private fun GpsSignalGlyph(
    available: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = available,
        transitionSpec = {
            val spec = tween<Float>(GpsGlyphMorphMs, easing = FastOutSlowInEasing)
            (
                fadeIn(spec) + scaleIn(initialScale = 0.86f, animationSpec = spec)
                ) togetherWith (
                fadeOut(spec) + scaleOut(targetScale = 1.08f, animationSpec = spec)
                ) using SizeTransform(clip = false)
        },
        label = "gpsSignalGlyph",
        modifier = modifier
    ) { hasGps ->
        val colors = clusterColors()
        val color = if (hasGps) colors.text else ClusterOrange
        val paths = remember(hasGps) {
            if (hasGps) GpsAvailablePaths else GpsUnavailablePaths
        }
        Canvas(Modifier.fillMaxSize()) {
            val scale = size.minDimension / GpsViewBox
            // Stroke is in viewBox units; the transform scales it with the glyph.
            val stroke = Stroke(
                width = 1.5f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
            withTransform({
                scale(scale, scale, pivot = Offset.Zero)
            }) {
                paths.forEach { drawPath(it, color, style = stroke) }
            }
        }
    }
}

private const val GpsViewBox = 24f
private const val GpsGlyphMorphMs = 280

private val GpsPinPath =
    "M4.853 19.147c3.196 3.196 8.06 3.707 11.789 1.533c.887-.517 1.33-.776 1.357-1.302s-.471-.89-1.468-1.618c-1.848-1.35-3.667-3-5.48-4.812C9.24 11.136 7.59 9.317 6.24 7.47c-.728-.997-1.092-1.495-1.618-1.468s-.785.47-1.302 1.357c-2.174 3.73-1.663 8.593 1.533 11.79Z"

private val GpsInnerArcPath = "M9 10.03A3.515 3.515 0 0 1 13.97 15"

private val GpsAvailablePaths: List<Path> by lazy {
    listOf(
        GpsInnerArcPath,
        GpsPinPath,
        "M15.557 2c1.68.172 3.224.84 4.412 2.026c1.192 1.19 1.86 2.737 2.031 4.42",
        "M18.502 9a4.66 4.66 0 0 0-1.242-2.27A4.67 4.67 0 0 0 15 5.495"
    ).map { PathParser().parsePathString(it).toPath() }
}

private val GpsUnavailablePaths: List<Path> by lazy {
    listOf(
        "m22 8l-3-3m0 0l-3-3m3 3l-3 3m3-3l3-3",
        GpsInnerArcPath,
        GpsPinPath
    ).map { PathParser().parsePathString(it).toPath() }
}
