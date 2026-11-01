package com.ultraviolette.navigator.ui.ktm

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.ultraviolette.navigator.model.ClusterPlaceholders
import com.ultraviolette.navigator.ui.theme.KtmBarPanel
import com.ultraviolette.navigator.ui.theme.KtmCyan
import com.ultraviolette.navigator.ui.theme.KtmDivider
import com.ultraviolette.navigator.ui.theme.KtmMuted
import com.ultraviolette.navigator.ui.theme.KtmNeutralGreen
import com.ultraviolette.navigator.ui.theme.KtmOrange
import com.ultraviolette.navigator.ui.theme.clusterColors
import com.ultraviolette.navigator.ui.theme.ktmWallClock
import com.ultraviolette.navigator.ui.theme.rememberKtmChrome
import com.ultraviolette.navigator.ui.theme.rememberKtmDisplayFont
import com.ultraviolette.navigator.ui.theme.rememberKtmUiFont

private val BottomBarHeight = 56.dp

/**
 * KTM riding cluster — overlay composition matching the reference:
 * oversized left tach, center nav bowl, right gear/temp/speed, EFF bottom bar.
 */
@Composable
fun KtmClusterBody(
    data: ClusterPlaceholders,
    onControlCentreClick: () -> Unit,
    onRideModeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = clusterColors()
    val chrome = rememberKtmChrome(data)
    val uiFont = rememberKtmUiFont()
    val modeTap = remember { MutableInteractionSource() }
    val settingsTap = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Clock — top left over the tach (Spaceport lacks a real ':' glyph).
        Text(
            text = remember(data.timeText) { ktmWallClock(data.timeText) },
            color = Color.White,
            fontFamily = uiFont,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 18.dp, top = 12.dp)
                .zIndex(2f)
        )

        // Gear + battery — upper right stack.
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 22.dp, top = 12.dp)
                .zIndex(2f),
            horizontalAlignment = Alignment.End
        ) {
            KtmGearDigit(
                gear = chrome.gear,
                color = if (chrome.gear == "N") KtmNeutralGreen else Color.White
            )
            Spacer(Modifier.height(4.dp))
            KtmBatteryPill(
                percent = data.batteryPercent,
                charging = data.isCharging
            )
        }

        // Settings cogs — above EFF; tach draws after so its base rests on the bar.
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 14.dp, bottom = BottomBarHeight + 8.dp)
                .size(48.dp)
                .zIndex(1f)
                .clickable(
                    interactionSource = settingsTap,
                    indication = null,
                    onClick = onControlCentreClick
                ),
            contentAlignment = Alignment.Center
        ) {
            KtmSettingsGear(
                color = Color.White,
                modifier = Modifier.size(38.dp)
            )
        }

        // Tach: top inset matches gear column so the rail lines up with the gear cap.
        KtmTachometer(
            rpm = chrome.rpm,
            accent = chrome.orange,
            spdBars = chrome.spdBars,
            spdActive = chrome.spdActive,
            modifier = Modifier
                .fillMaxWidth(0.84f)
                .fillMaxHeight()
                .padding(start = 44.dp, top = 12.dp, bottom = BottomBarHeight)
                .align(Alignment.CenterStart)
                .zIndex(0f)
        )

        // Ride mode — centered on the horizontal axis above the bar.
        Text(
            text = chrome.modeLabel,
            color = Color.White,
            fontFamily = uiFont,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.4.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = BottomBarHeight + 14.dp)
                .zIndex(2f)
                .clickable(
                    interactionSource = modeTap,
                    indication = null,
                    onClick = onRideModeClick
                )
        )

        // Speed — bottom right above the bar.
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 22.dp, bottom = BottomBarHeight + 8.dp)
                .zIndex(2f)
        ) {
            KtmSpeedDigits(text = data.speedKmh)
            Text(
                text = "km/h",
                color = KtmMuted,
                fontFamily = uiFont,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 4.dp)
            )
        }

        KtmBottomTelemetryBar(
            data = data,
            effText = chrome.effText,
            effBars = chrome.effBars,
            effActive = chrome.effActive,
            accent = chrome.orange,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(BottomBarHeight)
                .zIndex(3f)
        )
    }
}

@Composable
private fun KtmSpeedDigits(text: String) {
    val displayFont = rememberKtmDisplayFont()
    val raw = text.filter { it.isDigit() }
    // Idle / no fix → 0 km/h (not em-dash placeholders).
    val show = if (raw.isEmpty() || text.contains('—')) {
        listOf('0')
    } else {
        raw.trimStart('0').ifEmpty { "0" }.takeLast(3).map { it }
    }
    Row(horizontalArrangement = Arrangement.End) {
        show.forEachIndexed { index, glyph ->
            AnimatedContent(
                targetState = glyph,
                transitionSpec = {
                    val spec = tween<Float>(140, easing = FastOutSlowInEasing)
                    (
                        fadeIn(spec) + scaleIn(initialScale = 0.82f, animationSpec = spec)
                        ) togetherWith (
                        fadeOut(spec) + scaleOut(targetScale = 1.08f, animationSpec = spec)
                        ) using SizeTransform(clip = false)
                },
                label = "ktmSpeed$index"
            ) { char ->
                Text(
                    text = char.toString(),
                    color = Color.White,
                    fontFamily = displayFont,
                    fontSize = 78.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 78.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(48.dp)
                )
            }
        }
    }
}

@Composable
private fun KtmGearDigit(gear: String, color: Color) {
    val displayFont = rememberKtmDisplayFont()
    AnimatedContent(
        targetState = gear,
        transitionSpec = {
            val spec = tween<Float>(180, easing = FastOutSlowInEasing)
            (
                fadeIn(spec) + scaleIn(initialScale = 0.7f, animationSpec = spec)
                ) togetherWith (
                fadeOut(spec) + scaleOut(targetScale = 1.15f, animationSpec = spec)
                ) using SizeTransform(clip = false)
        },
        label = "ktmGear"
    ) { value ->
        Text(
            text = value,
            color = color,
            fontFamily = displayFont,
            fontSize = 108.sp,
            fontWeight = FontWeight.Black,
            lineHeight = 108.sp
        )
    }
}

@Composable
private fun KtmBatteryPill(percent: Int, charging: Boolean) {
    val uiFont = rememberKtmUiFont()
    val pct = percent.coerceIn(0, 100)
    val fg = when {
        charging -> Color(0xFF1A9A52)
        pct <= 15 -> Color(0xFFFF6B6B)
        else -> KtmCyan
    }
    val bg = fg.copy(alpha = 0.18f)
    Row(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Canvas(Modifier.size(24.dp, 16.dp)) {
            val body = androidx.compose.ui.geometry.Size(size.width * 0.72f, size.height * 0.78f)
            val topLeft = Offset(size.width * 0.05f, size.height * 0.11f)
            drawRoundRect(
                color = fg,
                topLeft = topLeft,
                size = body,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.8.dp.toPx())
            )
            val fillW = body.width * (pct / 100f) * 0.82f
            if (fillW > 0.5f) {
                drawRoundRect(
                    color = fg,
                    topLeft = Offset(topLeft.x + body.width * 0.08f, topLeft.y + body.height * 0.18f),
                    size = androidx.compose.ui.geometry.Size(fillW, body.height * 0.64f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.2.dp.toPx(), 1.2.dp.toPx())
                )
            }
            drawRect(
                color = fg,
                topLeft = Offset(topLeft.x + body.width, size.height * 0.32f),
                size = androidx.compose.ui.geometry.Size(size.width * 0.12f, size.height * 0.36f)
            )
        }
        Text(
            text = if (charging) "$pct%+" else "$pct%",
            color = fg,
            fontFamily = uiFont,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/** Dual-cog mark — opens Control Centre from above the EFF cell. */
@Composable
private fun KtmSettingsGear(
    color: Color,
    modifier: Modifier = Modifier
) {
    val path = remember { PathParser().parsePathString(KtmSettingsCogsPath).toPath() }
    Canvas(modifier) {
        val scale = size.minDimension / KtmSettingsCogsViewBox
        withTransform({
            scale(scale, scale, pivot = Offset.Zero)
        }) {
            drawPath(path, color)
        }
    }
}

private const val KtmSettingsCogsViewBox = 20f
private const val KtmSettingsCogsPath =
    "M15.003 11q.221 0 .436.023c.2.022.355.178.397.373l.222 1.036a.5.5 0 0 0 .642.37l1.005-.325a.47.47 0 0 1 .522.156q.26.352.438.757a.465.465 0 0 1-.125.529l-.786.71a.5.5 0 0 0 0 .742l.786.71a.465.465 0 0 1 .125.528q-.18.405-.438.757a.47.47 0 0 1-.522.157l-1.005-.326a.5.5 0 0 0-.642.371l-.222 1.034a.47.47 0 0 1-.396.374a4 4 0 0 1-.874 0a.465.465 0 0 1-.396-.373l-.22-1.035a.5.5 0 0 0-.644-.37l-1.006.325a.46.46 0 0 1-.52-.157a4 4 0 0 1-.438-.758a.465.465 0 0 1 .125-.529l.785-.708a.5.5 0 0 0 0-.742l-.785-.71a.465.465 0 0 1-.125-.528q.178-.406.437-.758a.46.46 0 0 1 .52-.156l1.007.326a.5.5 0 0 0 .643-.371l.22-1.036a.465.465 0 0 1 .397-.373q.216-.023.437-.023m0 3a1 1 0 1 0 0 2a1 1 0 0 0 0-2M7.5 1q.455 0 .892.063l.288.045l.073.02a.5.5 0 0 1 .326.368l.395 1.844a.5.5 0 0 0 .642.37l1.792-.58l.073-.018a.5.5 0 0 1 .461.168a6.5 6.5 0 0 1 1.184 2.046a.5.5 0 0 1-.137.538l-1.4 1.265a.5.5 0 0 0 0 .742l1.4 1.264a.5.5 0 0 1 .137.538a6.5 6.5 0 0 1-1.184 2.046a.5.5 0 0 1-.534.15l-1.792-.58a.5.5 0 0 0-.642.371l-.395 1.843a.5.5 0 0 1-.4.388q-.57.107-1.179.109c-.405 0-.8-.04-1.182-.11a.5.5 0 0 1-.398-.387l-.394-1.843a.5.5 0 0 0-.642-.37l-1.793.58a.5.5 0 0 1-.534-.151a6.5 6.5 0 0 1-1.184-2.046a.5.5 0 0 1 .137-.538l1.4-1.264a.5.5 0 0 0 .072-.661l-.072-.081l-1.4-1.265a.5.5 0 0 1-.137-.538A6.5 6.5 0 0 1 2.556 3.28l.052-.052a.5.5 0 0 1 .482-.098l1.793.58a.5.5 0 0 0 .642-.37l.394-1.844l.021-.072a.5.5 0 0 1 .377-.316Q6.891 1.001 7.5 1m0 4.75a1.75 1.75 0 1 0 0 3.5a1.75 1.75 0 0 0 0-3.5"

/**
 * Brightness along the bar (0 = left edge, 1 = right). Peaks in the middle
 * so Speed / Trip read brightest, EFF / Odo stay quieter.
 */
private fun ktmBarHighlight(t: Float): Float {
    val d = ((t - 0.5f) * 2f).let { it * it }
    return (0.08f + 0.72f * (1f - d)).coerceIn(0.08f, 0.80f)
}

/** Horizontal slice of the continuous white wash for panel [index] of 4. */
private fun ktmBarPanelWash(index: Int): Brush {
    val left = index / 4f
    val right = (index + 1) / 4f
    return Brush.horizontalGradient(
        0f to Color.White.copy(alpha = ktmBarHighlight(left) * 0.22f),
        0.5f to Color.White.copy(alpha = ktmBarHighlight((left + right) * 0.5f) * 0.28f),
        1f to Color.White.copy(alpha = ktmBarHighlight(right) * 0.22f)
    )
}

/** Top-edge sheen for panel [index] — vertical falloff, horizontal intensity. */
private fun ktmBarPanelTopSheen(index: Int): Brush {
    val left = index / 4f
    val right = (index + 1) / 4f
    val mid = (left + right) * 0.5f
    val peak = ktmBarHighlight(mid)
    return Brush.verticalGradient(
        0.00f to Color.White.copy(alpha = peak * 0.95f),
        0.12f to Color.White.copy(alpha = peak * 0.55f),
        0.38f to Color.White.copy(alpha = peak * 0.18f),
        0.70f to Color.White.copy(alpha = peak * 0.04f),
        1.00f to Color.Transparent
    )
}

private fun ktmBarPanelTopLine(index: Int): Brush {
    val left = index / 4f
    val right = (index + 1) / 4f
    return Brush.horizontalGradient(
        0f to Color.White.copy(alpha = ktmBarHighlight(left)),
        0.5f to Color.White.copy(alpha = ktmBarHighlight((left + right) * 0.5f)),
        1f to Color.White.copy(alpha = ktmBarHighlight(right))
    )
}

@Composable
private fun KtmBottomTelemetryBar(
    data: ClusterPlaceholders,
    effText: String,
    effBars: Int,
    effActive: Boolean,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val uiFont = rememberKtmUiFont()
    val colors = clusterColors()

    // Gaps stay on the cluster face; chrome/gradients live only inside each panel.
    Row(
        modifier = modifier
            .background(colors.background)
            .padding(horizontal = 3.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        KtmBarSection(
            index = 0,
            modifier = Modifier.weight(1f)
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "EFF",
                    color = Color.White.copy(alpha = 0.72f),
                    fontFamily = uiFont,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                    textAlign = TextAlign.Center
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = effText,
                        color = Color.White,
                        fontFamily = uiFont,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    KtmEffPips(
                        bars = effBars,
                        active = effActive,
                        accent = accent,
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                    )
                }
            }
        }
        KtmTelemetryCell(
            label = "ø Speed 1",
            value = if (data.avgSpeedKmh == "———") "-- km/h" else "${data.avgSpeedKmh} km/h",
            index = 1,
            modifier = Modifier.weight(1f)
        )
        KtmTelemetryCell(
            label = "Trip 1",
            value = data.tripKm,
            index = 2,
            modifier = Modifier.weight(1f)
        )
        KtmTelemetryCell(
            label = "Odometer",
            value = data.odometerKm,
            index = 3,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun KtmBarSection(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val wash = remember(index) { ktmBarPanelWash(index) }
    val topSheen = remember(index) { ktmBarPanelTopSheen(index) }
    val topLine = remember(index) { ktmBarPanelTopLine(index) }
    val edgeAlpha = ktmBarHighlight((index + 0.5f) / 4f) * 0.35f

    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(KtmBarPanel)
            .background(wash)
            .border(width = 0.6.dp, color = Color.White.copy(alpha = edgeAlpha)),
        contentAlignment = Alignment.Center
    ) {
        // Soft white falloff from the top — primary look from the reference.
        Box(
            Modifier
                .fillMaxSize()
                .background(topSheen)
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .align(Alignment.TopCenter)
                .background(topLine)
        )
        content()
    }
}

@Composable
private fun KtmEffPips(
    bars: Int,
    active: Boolean,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val lit = bars.coerceIn(0, 10)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(10) { index ->
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        if (active && index < lit) accent else Color.White.copy(alpha = 0.18f)
                    )
            )
        }
    }
}

@Composable
private fun KtmTelemetryCell(
    label: String,
    value: String,
    index: Int,
    modifier: Modifier = Modifier
) {
    val uiFont = rememberKtmUiFont()
    KtmBarSection(index = index, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.72f),
                fontFamily = uiFont,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Text(
                text = value,
                color = Color.White,
                fontFamily = uiFont,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

