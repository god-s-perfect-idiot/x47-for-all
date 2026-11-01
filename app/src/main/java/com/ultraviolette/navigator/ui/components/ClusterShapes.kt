package com.ultraviolette.navigator.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.ultraviolette.navigator.ui.theme.ClusterBlue
import com.ultraviolette.navigator.ui.theme.ClusterGreen
import com.ultraviolette.navigator.ui.theme.ClusterRed
import com.ultraviolette.navigator.ui.theme.clusterColors

/**
 * Four short diagonals framing the speed readout.
 * Bottom pair uses the same ~45° slant as [MetricPanelShape]'s stem-fold
 * hypotenuse (parallel on each side); top pair is that slant inverted on Y.
 * Segments hug the side-rail edge so they sit close to the panel folds.
 */
@Composable
fun CrosshairFrame(modifier: Modifier = Modifier) {
    val colors = clusterColors()
    Canvas(modifier = modifier.fillMaxSize()) {
        val stroke = 4.dp.toPx()
        val color = colors.crosshair
        val w = size.width
        val h = size.height

        // Short 45° ticks (MetricPanelShape fold ≈ 1:1).
        val run = 72.dp.toPx()
        val rise = run
        // Sit against the side rails — close to the panel hypotenuses.
        val insetX = 6.dp.toPx()
        // Pull away from the speed dashes — top higher, bottom lower.
        val midYTop = h * 0.22f
        val midYBot = h * 0.74f

        // Bottom-left / — outer near left rail, parallel to left hypotenuse.
        val blMid = Offset(insetX + run * 0.5f, midYBot)
        val blOuter = Offset(blMid.x - run * 0.5f, blMid.y + rise * 0.5f)
        val blInner = Offset(blMid.x + run * 0.5f, blMid.y - rise * 0.5f)
        // Bottom-right \ — mirror X
        val brMid = Offset(w - insetX - run * 0.5f, midYBot)
        val brOuter = Offset(brMid.x + run * 0.5f, brMid.y + rise * 0.5f)
        val brInner = Offset(brMid.x - run * 0.5f, brMid.y - rise * 0.5f)
        // Top-left \ — Y-invert of bottom-left
        val tlMid = Offset(insetX + run * 0.5f, midYTop)
        val tlOuter = Offset(tlMid.x - run * 0.5f, tlMid.y - rise * 0.5f)
        val tlInner = Offset(tlMid.x + run * 0.5f, tlMid.y + rise * 0.5f)
        // Top-right / — Y-invert of bottom-right
        val trMid = Offset(w - insetX - run * 0.5f, midYTop)
        val trOuter = Offset(trMid.x + run * 0.5f, trMid.y - rise * 0.5f)
        val trInner = Offset(trMid.x - run * 0.5f, trMid.y + rise * 0.5f)

        drawLine(color, tlOuter, tlInner, stroke, StrokeCap.Butt)
        drawLine(color, trOuter, trInner, stroke, StrokeCap.Butt)
        drawLine(color, blOuter, blInner, stroke, StrokeCap.Butt)
        drawLine(color, brOuter, brInner, stroke, StrokeCap.Butt)
    }
}

/**
 * Semi-circle lean gauge. [leanAngleDeg] is signed: 0 upright, + right, − left.
 * Needle sweeps within the blue/red tick band (~±45°).
 */
@Composable
fun LeanAngleGauge(
    leanAngleDeg: Float = 0f,
    modifier: Modifier = Modifier
) {
    val colors = clusterColors()
    Canvas(modifier = modifier) {
        val stroke = 2.6.dp.toPx()
        val radius = minOf(size.width / 2f, size.height) - stroke
        val cx = size.width / 2f
        val cy = size.height
        drawArc(
            color = colors.crosshair,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(cx - radius, cy - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        // Blue / red tick marks on the arc (ref-3)
        fun tick(deg: Double, color: Color) {
            val a = Math.toRadians(deg)
            val inner = radius * 0.78f
            val outer = radius * 0.98f
            drawLine(
                color = color,
                start = Offset(cx + (inner * Math.cos(a)).toFloat(), cy - (inner * Math.sin(a)).toFloat()),
                end = Offset(cx + (outer * Math.cos(a)).toFloat(), cy - (outer * Math.sin(a)).toFloat()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Butt
            )
        }
        tick(120.0, ClusterBlue)
        tick(60.0, ClusterRed)

        // Math angle: 90° = upright. Positive lean (right) swings toward 60°.
        val needleDeg = (90.0 - leanAngleDeg.toDouble().coerceIn(-45.0, 45.0))
        val needle = Math.toRadians(needleDeg)
        drawLine(
            color = colors.dark,
            start = Offset(
                cx + (radius * 0.30f * Math.cos(needle)).toFloat(),
                cy - (radius * 0.30f * Math.sin(needle)).toFloat()
            ),
            end = Offset(
                cx + (radius * 0.90f * Math.cos(needle)).toFloat(),
                cy - (radius * 0.90f * Math.sin(needle)).toFloat()
            ),
            strokeWidth = 2.2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

/** Battery SoC bar with chamfered ends (refs 1–3). */
@Composable
fun BatteryBar(percent: Int, modifier: Modifier = Modifier) {
    val colors = clusterColors()
    Canvas(modifier = modifier) {
        val cut = size.height * 0.9f
        fun barPath(width: Float) = Path().apply {
            moveTo(cut, 0f)
            lineTo(width - cut, 0f)
            lineTo(width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(barPath(size.width), colors.batteryTrack)
        val fill = (percent.coerceIn(0, 100) / 100f) * size.width
        if (fill > cut * 2) {
            drawPath(barPath(fill), ClusterGreen)
        }
    }
}

/** EFF footer: 10 thick diagonal ticks with open gaps (ref-2). */
@Composable
fun DiagonalStripes(
    modifier: Modifier = Modifier,
    color: Color? = null,
    count: Int = 10
) {
    val stripeColor = color ?: clusterColors().crosshair
    Canvas(modifier = modifier) {
        val stroke = 4.5.dp.toPx()
        val slant = size.height * 0.48f
        // End pad keeps first/last strokes fully inside after slant.
        val pad = stroke * 0.55f + slant * 0.35f
        val usable = (size.width - pad * 2f).coerceAtLeast(stroke)
        val spacing = if (count > 1) usable / (count - 1) else 0f
        for (i in 0 until count) {
            val x = pad + i * spacing
            drawLine(
                color = stripeColor,
                start = Offset(x - slant * 0.5f, 0f),
                end = Offset(x + slant * 0.5f, size.height),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Three regen cells (user R3 reference).
 * Rounded parallelograms (~30° lean) with two short twin-pillar cutouts.
 */
@Composable
fun RegenCells(
    filled: Int,
    modifier: Modifier = Modifier,
    fillColor: Color? = null,
    emptyColor: Color? = null
) {
    val colors = clusterColors()
    val resolvedFill = fillColor ?: colors.onBar
    val resolvedEmpty = emptyColor ?: colors.onBar.copy(alpha = 0.28f)
    Canvas(modifier = modifier) {
        val count = 3
        val h = size.height
        // Match screenshot aspect: cell ~2.05× tall, gap ~0.48× tall, ~30° lean.
        val cellW = h * 2.05f
        val gap = h * 0.48f
        val skewX = -0.56f // x' = x + skewX*y → left edge shifts left toward bottom
        val leanPad = kotlin.math.abs(skewX) * h
        val corner = CornerRadius(h * 0.22f, h * 0.22f)
        val slotW = (cellW * 0.12f).coerceAtLeast(1.6.dp.toPx())
        val slotH = h * 0.38f
        val slotTop = (h - slotH) / 2f
        val slotCenters = floatArrayOf(0.37f, 0.63f)
        val totalW = count * cellW + (count - 1) * gap
        val originX = leanPad + ((size.width - leanPad - totalW) / 2f).coerceAtLeast(0f)
        val skewMatrix = Matrix().apply { values[Matrix.SkewX] = skewX }

        for (i in 0 until count) {
            val left = originX + i * (cellW + gap)
            val color = if (i < filled) resolvedFill else resolvedEmpty
            // Upright round-rect + twin slots, then skew into a parallelogram.
            withTransform({
                translate(left = left, top = 0f)
                transform(skewMatrix)
            }) {
                val path = Path().apply {
                    fillType = PathFillType.EvenOdd
                    addRoundRect(RoundRect(0f, 0f, cellW, h, corner))
                    for (t in slotCenters) {
                        val sx = cellW * t - slotW / 2f
                        addRoundRect(
                            RoundRect(
                                left = sx,
                                top = slotTop,
                                right = sx + slotW,
                                bottom = slotTop + slotH,
                                cornerRadius = CornerRadius(slotW * 0.35f, slotW * 0.35f)
                            )
                        )
                    }
                }
                drawPath(path, color)
            }
        }
    }
}

/**
 * Control Centre — same geometry as the top status notch (wide top → narrow
 * bottom, 45° flanks), in accent blue: diamond-cut top corners, gradient fill,
 * and two short darker tabs on the lower flanks.
 */
@Composable
fun ControlCentreShape(
    color: Color,
    modifier: Modifier = Modifier
) {
    val darkFace = clusterColors().isDark
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        // Same 45° as top notch / COMBAT↔EFF (run/rise = 1).
        val slant = h
        val tabOut = 7.dp.toPx()
        // Short tabs centered on each flank — not edge-to-edge.
        val tabH = h * 0.38f
        val tabTopY = (h - tabH) / 2f
        val tabBotY = tabTopY + tabH
        val diamond = 4.5.dp.toPx().coerceAtMost(h * 0.30f)

        // Top-notch orientation: wide top, clipped bottom. Side pad = tab room.
        val topLeft = tabOut
        val topRight = w - tabOut
        val bottomLeft = topLeft + slant
        val bottomRight = topRight - slant

        fun flankLeft(y: Float) = topLeft + (bottomLeft - topLeft) * (y / h)
        fun flankRight(y: Float) = topRight + (bottomRight - topRight) * (y / h)

        val tabColor = if (darkFace) {
            // Lift tabs toward accent so they read on a dark face.
            Color(
                red = (color.red * 0.72f + 0.18f).coerceIn(0f, 1f),
                green = (color.green * 0.72f + 0.18f).coerceIn(0f, 1f),
                blue = (color.blue * 0.78f + 0.20f).coerceIn(0f, 1f),
                alpha = 1f
            )
        } else {
            Color(
                red = color.red * 0.48f,
                green = color.green * 0.48f,
                blue = color.blue * 0.52f,
                alpha = 1f
            )
        }
        // Light face: pale → accent (mix toward white). Dark face: deep → accent (mix toward black).
        fun tone(c: Color, amount: Float) = if (darkFace) {
            Color(
                red = (c.red * (1f - amount)).coerceIn(0f, 1f),
                green = (c.green * (1f - amount)).coerceIn(0f, 1f),
                blue = (c.blue * (1f - amount)).coerceIn(0f, 1f),
                alpha = 1f
            )
        } else {
            Color(
                red = (c.red + (1f - c.red) * amount).coerceIn(0f, 1f),
                green = (c.green + (1f - c.green) * amount).coerceIn(0f, 1f),
                blue = (c.blue + (1f - c.blue) * amount).coerceIn(0f, 1f),
                alpha = 1f
            )
        }
        val topFill = tone(color, 0.88f)
        val midFill = tone(color, 0.42f)
        val bottomFill = tone(color, 0.32f)
        val strokeColor = if (darkFace) {
            Color(
                red = (color.red * 0.85f + 0.12f).coerceIn(0f, 1f),
                green = (color.green * 0.85f + 0.12f).coerceIn(0f, 1f),
                blue = (color.blue * 0.90f + 0.14f).coerceIn(0f, 1f),
                alpha = 1f
            )
        } else {
            Color(
                red = color.red * 0.55f,
                green = color.green * 0.55f,
                blue = color.blue * 0.62f,
                alpha = 1f
            )
        }

        // Side tabs — short parallelograms centered on each flank.
        val leftTab = Path().apply {
            moveTo(flankLeft(tabTopY) - tabOut, tabTopY)
            lineTo(flankLeft(tabTopY), tabTopY)
            lineTo(flankLeft(tabBotY), tabBotY)
            lineTo(flankLeft(tabBotY) - tabOut, tabBotY)
            close()
        }
        val rightTab = Path().apply {
            moveTo(flankRight(tabTopY), tabTopY)
            lineTo(flankRight(tabTopY) + tabOut, tabTopY)
            lineTo(flankRight(tabBotY) + tabOut, tabBotY)
            lineTo(flankRight(tabBotY), tabBotY)
            close()
        }
        drawPath(leftTab, tabColor)
        drawPath(rightTab, tabColor)

        // Main body: wide top, diamond corners, 45° inward flanks.
        val main = Path().apply {
            moveTo(topLeft + diamond, 0f)
            lineTo(topRight - diamond, 0f)
            lineTo(topRight, diamond)
            lineTo(bottomRight, h)
            lineTo(bottomLeft, h)
            lineTo(topLeft, diamond)
            close()
        }
        drawPath(
            main,
            Brush.verticalGradient(
                colors = listOf(topFill, midFill, bottomFill),
                startY = 0f,
                endY = h
            )
        )
        drawPath(main, strokeColor, style = Stroke(width = 1.3.dp.toPx()))
    }
}

/**
 * Side-metric panel: sharp outer-top corner, lightly rounded everywhere else,
 * right-triangle fold on the inner-bottom into [foldToWidth] (the stem below).
 * [foldHeight] is the vertical leg — keep it short so metrics sit above the cut.
 */
class MetricPanelShape(
    private val mirror: Boolean = false,
    /** Soft radius on every corner except the outer-top (which stays sharp). */
    private val corner: Dp = 4.dp,
    /** Width of the thinner stem the bottom-inner edge folds into. */
    private val foldToWidth: Dp = 56.dp,
    /** Vertical rise of the fold; larger = steeper, longer hypotenuse. */
    private val foldHeight: Dp = 78.dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val r = with(density) { corner.toPx() }
        val stem = with(density) { foldToWidth.toPx() }
            .coerceIn(0f, size.width)
        // Right angle at the inner-bottom corner; hypotenuse lands on the stem edge.
        val cutV = with(density) { foldHeight.toPx() }
            .coerceAtMost(size.height)
        // Vertices clockwise. Index 0 = outer-top (sharp); all others soft.
        val vertices = if (!mirror) {
            listOf(
                Offset(0f, 0f),
                Offset(size.width, 0f),
                Offset(size.width, size.height - cutV),
                Offset(stem, size.height),
                Offset(0f, size.height)
            )
        } else {
            listOf(
                Offset(size.width, 0f),
                Offset(size.width, size.height),
                Offset(size.width - stem, size.height),
                Offset(0f, size.height - cutV),
                Offset(0f, 0f)
            )
        }
        val radii = FloatArray(vertices.size) { i -> if (i == 0) 0f else r }
        return Outline.Generic(filletedPolygon(vertices, radii))
    }
}

/** Builds a closed path through [vertices], filleting each corner by [radii]. */
internal fun filletedPolygon(vertices: List<Offset>, radii: FloatArray): Path {
    val path = Path()
    val n = vertices.size
    if (n < 3) return path

    fun pointOnEdge(from: Offset, to: Offset, dist: Float): Offset {
        val dx = to.x - from.x
        val dy = to.y - from.y
        val len = kotlin.math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
        if (len < 1e-4f) return from
        val t = (dist / len).coerceIn(0f, 1f)
        return Offset(from.x + dx * t, from.y + dy * t)
    }

    for (i in 0 until n) {
        val prev = vertices[(i - 1 + n) % n]
        val curr = vertices[i]
        val next = vertices[(i + 1) % n]
        val lenPrev = kotlin.math.hypot(
            (curr.x - prev.x).toDouble(),
            (curr.y - prev.y).toDouble()
        ).toFloat()
        val lenNext = kotlin.math.hypot(
            (next.x - curr.x).toDouble(),
            (next.y - curr.y).toDouble()
        ).toFloat()
        val maxR = minOf(lenPrev, lenNext) * 0.45f
        val rr = radii[i].coerceIn(0f, maxR)

        if (rr <= 0f) {
            if (i == 0) path.moveTo(curr.x, curr.y) else path.lineTo(curr.x, curr.y)
        } else {
            val p1 = pointOnEdge(curr, prev, rr)
            val p2 = pointOnEdge(curr, next, rr)
            if (i == 0) path.moveTo(p1.x, p1.y) else path.lineTo(p1.x, p1.y)
            path.quadraticTo(curr.x, curr.y, p2.x, p2.y)
        }
    }
    path.close()
    return path
}

@Composable
fun BottomModeBarBackground(modifier: Modifier = Modifier) {
    val colors = clusterColors()
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(colors.dark)
        // Raised center trapezoid tab
        val centerW = size.width * 0.30f
        val left = (size.width - centerW) / 2f
        val flare = size.height * 0.55f
        val tab = Path().apply {
            moveTo(left + flare, 0f)
            lineTo(left + centerW - flare, 0f)
            lineTo(left + centerW + flare * 0.2f, size.height)
            lineTo(left - flare * 0.2f, size.height)
            close()
        }
        drawPath(tab, colors.bottomModeTab)
    }
}

@Composable
fun HexagonIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = size.minDimension / 2f - 1.dp.toPx()
        val path = Path()
        for (i in 0 until 6) {
            val a = Math.toRadians(60.0 * i - 30.0)
            val x = cx + (r * Math.cos(a)).toFloat()
            val y = cy + (r * Math.sin(a)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path, color, style = Stroke(width = 1.8.dp.toPx()))
        // inner gear-like hex
        val ir = r * 0.45f
        val inner = Path()
        for (i in 0 until 6) {
            val a = Math.toRadians(60.0 * i - 30.0)
            val x = cx + (ir * Math.cos(a)).toFloat()
            val y = cy + (ir * Math.sin(a)).toFloat()
            if (i == 0) inner.moveTo(x, y) else inner.lineTo(x, y)
        }
        inner.close()
        drawPath(inner, color)
    }
}
