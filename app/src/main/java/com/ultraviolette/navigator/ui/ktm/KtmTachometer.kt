package com.ultraviolette.navigator.ui.ktm

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultraviolette.navigator.ui.theme.KtmOrange
import com.ultraviolette.navigator.ui.theme.KtmRedline
import com.ultraviolette.navigator.ui.theme.rememberKtmUiFont
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * KTM bar tach: thick continuous arc up the left that flattens into a
 * straight horizontal rail along the top (Ready-to-Race TFT).
 *
 * Android angles: 0° = 3 o'clock, clockwise. Bottom-left ≈ 162°, top = 270°.
 * Geometry pins the arc base to the bottom of the bounds (just above EFF).
 * The rail tip is cut on a forward slant — top edge longer than bottom (10k end).
 */
private const val ArcStartDeg = 162f
private const val ArcSweepToTopDeg = 108f // 162 → 270 (top)
/** How much further the top edge runs past the bottom at the 10k tip. */
private const val TipSlantFraction = 1.55f // × stroke width

@Composable
fun KtmTachometer(
    rpm: Float,
    accent: Color = KtmOrange,
    spdBars: Int,
    spdActive: Boolean,
    modifier: Modifier = Modifier
) {
    val uiFont = rememberKtmUiFont()
    val density = LocalDensity.current
    val animatedRpm by animateFloatAsState(
        targetValue = rpm.coerceIn(0f, 10f),
        animationSpec = tween(KtmTachSweepMs, easing = FastOutSlowInEasing),
        label = "ktmRpm"
    )
    val animatedSpd by animateFloatAsState(
        targetValue = if (spdActive) spdBars.coerceIn(0, 10) / 10f else 0f,
        animationSpec = tween(KtmTachSweepMs, easing = FastOutSlowInEasing),
        label = "ktmSpd"
    )

    BoxWithConstraints(modifier = modifier) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val stroke = with(density) { 78.dp.toPx() }
        val tipSlant = stroke * TipSlantFraction
        // Drop the rail so its top edge sits on the gear-digit cap line
        // (parent already applies the gear column's top inset).
        val topClearance = with(density) { 4.dp.toPx() }
        val startSin = sin(Math.toRadians(ArcStartDeg.toDouble())).toFloat()
        val apexPad = stroke / 2f + topClearance
        // Lift the arc base off the EFF bar / above the settings cogs.
        val basePad = with(density) { 52.dp.toPx() }
        // Vertical span between raised base and lowered top.
        val ovalRadius = ((h - apexPad - basePad) / (1f + startSin)).coerceAtLeast(40f)
        val diameter = ovalRadius * 2f + stroke
        val top = apexPad - stroke / 2f // == topClearance
        // Room for the SPD progress bar outside the tach stroke; horizontal
        // rail still runs nearly full width.
        val left = diameter * 0.10f
        // Butt of the stroke; slant tip extends beyond this toward the right.
        val railEndX = (w * 0.99f - tipSlant).coerceAtLeast(w * 0.78f)

        val tachPath = remember(w, h, diameter, left, top, railEndX, stroke) {
            buildTachPath(left, top, diameter, stroke, railEndX)
        }
        val measure = remember(tachPath) {
            PathMeasure().also { it.setPath(tachPath, false) }
        }
        val pathLen = measure.length
        val apexY = remember(tachPath, pathLen) {
            measure.getPosition(pathLen).y
        }
        val tipPath = remember(railEndX, apexY, stroke, tipSlant) {
            buildTachSlantTip(railEndX, apexY, stroke, tipSlant)
        }

        // Equal path spacing through 10 — sits at the butt, not out in the tip,
        // so 9→10 matches 8→9 (the old tip nudge pulled 10 too far right).
        val labelPoints = remember(pathLen) {
            (1..10).map { n ->
                val dist = pathLen * (n / 10f)
                n to measure.getPosition(dist.coerceIn(0f, pathLen))
            }
        }

        Canvas(Modifier.fillMaxSize()) {
            val trackColor = Color(0xFF2A2A2A)
            val trackStyle = Stroke(
                width = stroke,
                cap = StrokeCap.Butt,
                join = StrokeJoin.Round
            )

            // Full inactive track — continuous arc → straight rail + slant tip.
            drawPath(tachPath, trackColor, style = trackStyle)
            drawPath(tipPath, trackColor)

            // Orange RPM fill along the same centreline; tip lights at redline.
            val fillFrac = (animatedRpm / 10f).coerceIn(0f, 1f)
            if (fillFrac > 0.01f) {
                val fillPath = Path()
                measure.getSegment(0f, pathLen * fillFrac, fillPath, startWithMoveTo = true)
                drawPath(fillPath, accent, style = trackStyle)
            }
            if (fillFrac >= 0.985f) {
                drawPath(tipPath, accent)
            }

            // Three rings, outside → in: SPD rail, trail, tacho body.
            val tachOuter = stroke * 0.5f
            val barStroke = 11.dp.toPx()
            val trailStroke = 2.25.dp.toPx()
            val ringGap = 3.dp.toPx()
            val trailOutward = tachOuter + ringGap + trailStroke * 0.5f
            val spdOutward = trailOutward + trailStroke * 0.5f + ringGap + barStroke * 0.5f
            val spdGray = Color(0xFF8A8A8A)
            val spdWhite = Color.White
            // Trail keeps constant width; a short rake is capped on the butt.
            val trailButtX = railEndX + tipSlant + 8.dp.toPx()
            val trailSlant = trailStroke * 1.8f
            val trailApexY = top + tachOuter - trailOutward
            val trailStyle = Stroke(
                width = trailStroke,
                cap = StrokeCap.Butt,
                join = StrokeJoin.Miter
            )
            val barStyle = Stroke(width = barStroke, cap = StrokeCap.Butt, join = StrokeJoin.Miter)
            val spdEndFrac = 0.40f // ~4 on the 0–10 tach scale

            // Trail — between SPD and tacho; gray through 10, orange to a small slant.
            val trailToTen = buildOffsetTachPath(
                left = left,
                top = top,
                diameter = diameter,
                stroke = stroke,
                railEndX = railEndX,
                outward = trailOutward
            )
            val trailToButt = buildOffsetTachPath(
                left = left,
                top = top,
                diameter = diameter,
                stroke = stroke,
                railEndX = trailButtX,
                outward = trailOutward
            )
            val tenMeasure = PathMeasure().also { it.setPath(trailToTen, false) }
            val trailMeasure = PathMeasure().also { it.setPath(trailToButt, false) }
            val tenDist = tenMeasure.length.coerceAtMost(trailMeasure.length)
            val preTen = Path()
            trailMeasure.getSegment(0f, tenDist, preTen, startWithMoveTo = true)
            drawPath(preTen, spdGray, style = trailStyle)
            if (trailMeasure.length > tenDist + 1f) {
                val postTen = Path()
                trailMeasure.getSegment(tenDist, trailMeasure.length, postTen, startWithMoveTo = true)
                drawPath(postTen, accent, style = trailStyle)
            }
            drawPath(
                buildTrailSlantCap(trailButtX, trailApexY, trailStroke, trailSlant),
                accent
            )

            // SPD — outermost ring; gray track + white fill, capped near 4k.
            val spdFrac = animatedSpd.coerceIn(0f, 1f)
            val fullRail = buildOffsetTachPath(
                left = left,
                top = top,
                diameter = diameter,
                stroke = stroke,
                railEndX = railEndX + tipSlant * 0.55f,
                outward = spdOutward
            )
            val fullMeasure = PathMeasure().also { it.setPath(fullRail, false) }
            val spdLen = fullMeasure.length * spdEndFrac
            val rail = Path()
            fullMeasure.getSegment(0f, spdLen, rail, startWithMoveTo = true)
            val railMeasure = PathMeasure().also { it.setPath(rail, false) }
            val railLen = railMeasure.length
            drawPath(rail, spdGray, style = barStyle)
            if (spdFrac > 0.01f) {
                val white = Path()
                railMeasure.getSegment(0f, railLen * spdFrac, white, startWithMoveTo = true)
                drawPath(white, spdWhite, style = barStyle)
            }
        }

        labelPoints.forEach { (n, pos) ->
            val color = when {
                n == 10 -> KtmRedline
                n > animatedRpm + 0.35f -> Color.White.copy(alpha = 0.40f)
                else -> Color.White
            }
            Text(
                text = n.toString(),
                color = color,
                fontFamily = uiFont,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (pos.x - with(density) { 8.dp.toPx() }).roundToInt(),
                            (pos.y - with(density) { 9.dp.toPx() }).roundToInt()
                        )
                    }
                    .size(20.dp)
            )
        }
    }
}

/**
 * Arc from lower-left up to the apex, then a straight horizontal rail to [railEndX].
 * Centreline of the stroke (not the outer edge). Slant tip is drawn separately.
 */
private fun buildTachPath(
    left: Float,
    top: Float,
    diameter: Float,
    stroke: Float,
    railEndX: Float
): Path {
    val inset = stroke / 2f
    val oval = Rect(
        left = left + inset,
        top = top + inset,
        right = left + diameter - inset,
        bottom = top + diameter - inset
    )
    val apexX = oval.center.x
    val apexY = oval.top
    return Path().apply {
        moveTo(
            oval.center.x + (oval.width / 2f) *
                cos(Math.toRadians(ArcStartDeg.toDouble())).toFloat(),
            oval.center.y + (oval.height / 2f) *
                sin(Math.toRadians(ArcStartDeg.toDouble())).toFloat()
        )
        arcTo(
            rect = oval,
            startAngleDegrees = ArcStartDeg,
            sweepAngleDegrees = ArcSweepToTopDeg,
            forceMoveTo = false
        )
        lineTo(railEndX.coerceAtLeast(apexX + 8f), apexY)
    }
}

/**
 * Forward-rake tip at 10k: top edge extends [slant] past the bottom edge,
 * so the rail ends on a diagonal rather than a vertical butt.
 */
private fun buildTachSlantTip(
    railEndX: Float,
    apexY: Float,
    stroke: Float,
    slant: Float
): Path {
    val half = stroke / 2f
    val overlap = 1.5f // bury the seam into the stroked butt
    return Path().apply {
        moveTo(railEndX - overlap, apexY - half)
        lineTo(railEndX + slant, apexY - half)
        lineTo(railEndX - overlap, apexY + half)
        close()
    }
}

/**
 * Small constant-width rake on the trail butt — top edge runs [slant] past
 * the bottom so the cut is a short diagonal, not a fat triangle.
 */
private fun buildTrailSlantCap(
    buttX: Float,
    centerY: Float,
    stroke: Float,
    slant: Float
): Path {
    val half = stroke / 2f
    val overlap = 1f
    return Path().apply {
        moveTo(buttX - overlap, centerY - half)
        lineTo(buttX + slant, centerY - half)
        lineTo(buttX, centerY + half)
        lineTo(buttX - overlap, centerY + half)
        close()
    }
}

/** Same silhouette, pushed outward for the SPD progress bar. */
private fun buildOffsetTachPath(
    left: Float,
    top: Float,
    diameter: Float,
    stroke: Float,
    railEndX: Float,
    outward: Float
): Path {
    val inset = stroke / 2f - outward
    val oval = Rect(
        left = left + inset,
        top = top + inset,
        right = left + diameter - inset,
        bottom = top + diameter - inset
    )
    val apexY = oval.top
    return Path().apply {
        moveTo(
            oval.center.x + (oval.width / 2f) *
                cos(Math.toRadians(ArcStartDeg.toDouble())).toFloat(),
            oval.center.y + (oval.height / 2f) *
                sin(Math.toRadians(ArcStartDeg.toDouble())).toFloat()
        )
        arcTo(
            rect = oval,
            startAngleDegrees = ArcStartDeg,
            sweepAngleDegrees = ArcSweepToTopDeg,
            forceMoveTo = false
        )
        lineTo(railEndX.coerceAtLeast(oval.center.x + 8f), apexY)
    }
}

/** Compact tach for the KTM control-centre preview pane. */
@Composable
fun KtmTachPreview(
    rpm: Float,
    accent: Color = KtmOrange,
    modifier: Modifier = Modifier
) {
    val animatedRpm by animateFloatAsState(
        targetValue = rpm.coerceIn(0f, 10f),
        animationSpec = tween(KtmTachSweepMs, easing = FastOutSlowInEasing),
        label = "ktmPreviewRpm"
    )
    Canvas(modifier = modifier) {
        val stroke = size.minDimension * 0.22f
        val tipSlant = stroke * TipSlantFraction
        val pad = size.minDimension * 0.04f
        val diam = size.minDimension * 1.15f
        val left = -diam * 0.15f
        val top = pad
        val railEndX = size.width * 0.95f - tipSlant
        val path = buildTachPath(
            left = left,
            top = top,
            diameter = diam,
            stroke = stroke,
            railEndX = railEndX
        )
        val measure = PathMeasure().also { it.setPath(path, false) }
        val apexY = measure.getPosition(measure.length).y
        val tip = buildTachSlantTip(railEndX, apexY, stroke, tipSlant)
        val style = Stroke(width = stroke, cap = StrokeCap.Butt, join = StrokeJoin.Round)
        val trackColor = Color(0xFF2A2A2A)
        drawPath(path, trackColor, style = style)
        drawPath(tip, trackColor)
        val fillFrac = animatedRpm / 10f
        if (fillFrac > 0.01f) {
            val fill = Path()
            measure.getSegment(0f, measure.length * fillFrac, fill, startWithMoveTo = true)
            drawPath(fill, accent, style = style)
        }
        if (fillFrac >= 0.985f) {
            drawPath(tip, accent)
        }
    }
}
