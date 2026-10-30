package com.ultraviolette.navigator.ui.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultraviolette.navigator.model.ClusterPlaceholders
import com.ultraviolette.navigator.ui.theme.ClusterOrange
import com.ultraviolette.navigator.ui.theme.ClusterPanel
import com.ultraviolette.navigator.ui.theme.ClusterText
import com.ultraviolette.navigator.ui.theme.ClusterUiFont
import com.ultraviolette.navigator.ui.theme.rememberClusterAccent

private val TopBarCenter = Color(0xFFF2F3F5)
private val TopBarCenterStroke = Color(0xFFC5C9CF)
private val TopBarBarTrack = Color(0xFFB8BDC3)
private val TopBarSoCFill = Color(0xFF1A9A52)
private val ModeBadgeFill = Color(0xFF5A6068)

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
            // Left gray notch — R3 / chevron / M
            Row(
                modifier = Modifier
                    .width(SideBottomWidth + TopBarHeight)
                    .fillMaxHeight()
                    .padding(start = 10.dp, end = 18.dp, top = 4.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End)
            ) {
                Text(
                    text = data.regenLevel,
                    color = ClusterText,
                    fontFamily = ClusterUiFont,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                RegenChevron(
                    color = accent.primary,
                    filled = data.isBallistic,
                    modifier = Modifier.size(10.dp, 9.dp)
                )
                ModeBadgeM()
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
                    color = ClusterText,
                    fontFamily = ClusterUiFont,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.width(10.dp))
                PercentValue(
                    value = data.batteryPercent,
                    color = ClusterText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
                Spacer(Modifier.width(10.dp))
                CellularSignal(
                    bars = data.signalBars,
                    modifier = Modifier.size(14.dp, 11.dp)
                )
                if (data.wifiOn) {
                    Spacer(Modifier.width(5.dp))
                    WifiGlyph(Modifier.size(13.dp))
                }
            }

            // Right gray notch — TC / ABS (+ other telltales)
            Row(
                modifier = Modifier
                    .width(SideBottomWidth + TopBarHeight)
                    .fillMaxHeight()
                    .padding(start = 18.dp, end = 10.dp, top = 4.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.Start)
            ) {
                if (data.hillHoldOn) HillHoldIcon(Modifier.size(20.dp))
                if (data.tractionControlOn) TractionControlIcon(Modifier.size(20.dp))
                if (data.absOn) AbsIcon(Modifier.size(20.dp))
                if (data.tirePressureWarn) TpmsIcon(Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun TopBarGeometry(
    batteryPercent: Int,
    modifier: Modifier = Modifier
) {
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
        drawPath(leftWing, ClusterPanel)

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
        drawPath(rightWing, ClusterPanel)

        // Center light notch
        val center = Path().apply {
            moveTo(topLeft, 0f)
            lineTo(topRight, 0f)
            lineTo(bottomRight, h)
            lineTo(bottomLeft, h)
            close()
        }
        drawPath(center, TopBarCenter)
        drawPath(center, TopBarCenterStroke, style = Stroke(1.15.dp.toPx()))

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
        drawPath(socPath(barWidth), TopBarBarTrack)
        val fill = (batteryPercent.coerceIn(0, 100) / 100f) * barWidth
        if (fill > cut * 2) {
            drawPath(socPath(fill), TopBarSoCFill)
        }
    }
}

@Composable
private fun ModeBadgeM() {
    Box(
        modifier = Modifier.size(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val r = 2.2.dp.toPx()
            drawRoundRect(
                color = ModeBadgeFill,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r)
            )
        }
        Text(
            text = "M",
            color = Color.White,
            fontFamily = ClusterUiFont,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RegenChevron(
    color: Color,
    filled: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val path = Path().apply {
            moveTo(size.width / 2f, 0f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        if (filled) {
            drawPath(path, color)
        } else {
            drawPath(path, color, style = Stroke(1.8.dp.toPx()))
            // solid chevron look for combat
            drawPath(path, color)
        }
    }
}

@Composable
private fun CellularSignal(bars: Int, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val count = 4
        val gap = size.width * 0.14f
        val barW = (size.width - gap * (count - 1)) / count
        for (i in 0 until count) {
            val bh = size.height * ((i + 1) / count.toFloat())
            val left = i * (barW + gap)
            drawRect(
                color = if (i < bars) ClusterText else Color(0xFFC4C8CD),
                topLeft = Offset(left, size.height - bh),
                size = Size(barW, bh)
            )
        }
    }
}

@Composable
private fun WifiGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = 1.5.dp.toPx()
        drawArc(
            color = ClusterText,
            startAngle = 210f,
            sweepAngle = 120f,
            useCenter = false,
            topLeft = Offset(size.width * 0.08f, size.height * 0.05f),
            size = Size(size.width * 0.84f, size.height * 0.84f),
            style = Stroke(stroke, cap = StrokeCap.Round)
        )
        drawArc(
            color = ClusterText,
            startAngle = 220f,
            sweepAngle = 100f,
            useCenter = false,
            topLeft = Offset(size.width * 0.22f, size.height * 0.22f),
            size = Size(size.width * 0.56f, size.height * 0.56f),
            style = Stroke(stroke, cap = StrokeCap.Round)
        )
        drawCircle(
            ClusterText,
            radius = 1.3.dp.toPx(),
            center = Offset(size.width / 2f, size.height * 0.78f)
        )
    }
}

@Composable
private fun HillHoldIcon(modifier: Modifier = Modifier) {
    val blue = Color(0xFF2F9BFF)
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(blue, style = Stroke(1.5.dp.toPx()))
        }
        Text(
            text = "H",
            color = blue,
            fontFamily = ClusterUiFont,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TractionControlIcon(modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = ClusterOrange
            val stroke = 1.45.dp.toPx()
            drawCircle(c, style = Stroke(stroke))
            // TC-off diagonal slash
            drawLine(
                c,
                Offset(size.width * 0.18f, size.height * 0.82f),
                Offset(size.width * 0.82f, size.height * 0.18f),
                stroke * 1.2f,
                StrokeCap.Round
            )
        }
        Text(
            text = "TC",
            color = ClusterOrange,
            fontFamily = ClusterUiFont,
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AbsIcon(modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = ClusterOrange
            val stroke = 1.4.dp.toPx()
            drawCircle(c, style = Stroke(stroke))
            // Parenthesis arcs of the ABS telltale
            drawArc(
                color = c,
                startAngle = 110f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(size.width * 0.05f, size.height * 0.16f),
                size = Size(size.width * 0.30f, size.height * 0.68f),
                style = Stroke(stroke * 0.9f, cap = StrokeCap.Round)
            )
            drawArc(
                color = c,
                startAngle = -70f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(size.width * 0.65f, size.height * 0.16f),
                size = Size(size.width * 0.30f, size.height * 0.68f),
                style = Stroke(stroke * 0.9f, cap = StrokeCap.Round)
            )
        }
        Text(
            text = "ABS",
            color = ClusterOrange,
            fontFamily = ClusterUiFont,
            fontSize = 6.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TpmsIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val c = ClusterOrange
        val stroke = 1.55.dp.toPx()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val outer = size.minDimension * 0.44f
        val mid = size.minDimension * 0.30f
        val inner = size.minDimension * 0.16f

        // Tire sidewall / tread cross-section
        drawCircle(c, radius = outer, style = Stroke(stroke * 1.15f))
        drawCircle(c, radius = mid, style = Stroke(stroke * 0.85f))
        // Rim
        drawCircle(c, radius = inner, style = Stroke(stroke * 0.8f))

        // Small tread nubs on outer ring
        for (i in 0 until 8) {
            val a = Math.toRadians(i * 45.0 - 90.0)
            val x0 = cx + (outer * Math.cos(a)).toFloat()
            val y0 = cy + (outer * Math.sin(a)).toFloat()
            val x1 = cx + ((outer + stroke * 0.9f) * Math.cos(a)).toFloat()
            val y1 = cy + ((outer + stroke * 0.9f) * Math.sin(a)).toFloat()
            drawLine(c, Offset(x0, y0), Offset(x1, y1), stroke * 0.7f, StrokeCap.Round)
        }

        // Exclamation inside rim
        drawLine(
            c,
            Offset(cx, cy - size.height * 0.10f),
            Offset(cx, cy + size.height * 0.04f),
            stroke,
            StrokeCap.Round
        )
        drawCircle(c, radius = 1.15.dp.toPx(), center = Offset(cx, cy + size.height * 0.14f))
    }
}
