package com.ultraviolette.navigator.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultraviolette.navigator.ui.theme.ClusterBlue
import com.ultraviolette.navigator.ui.theme.ClusterUiFont
import com.ultraviolette.navigator.ui.theme.clusterColors

/**
 * Full-width thin dark mode strip. Flat wings reach the short edges;
 * center trapezoid rises slightly for the ride-mode label.
 * EFF / SPD span the light wing band above the flat, stopping at the notch.
 * Accent color (green / blue / red) shows while moving; gray when parked.
 */
val CombatFlatHeight: Dp = 16.dp
val CombatNotchRise: Dp = 24.dp
val CombatBarHeight: Dp = CombatFlatHeight + CombatNotchRise
/** Half-width of the trapezoid base where wings meet the notch. */
val CombatNotchBaseHalf: Dp = 124.dp
/** Half-width of the raised notch plateau (top edge). */
val CombatNotchTopHalf: Dp = 100.dp

val CombatHangBelow: Dp = 0.dp
val CombatRiseAbove: Dp = CombatNotchRise

/** Ride-mode color morph duration (parked gray → moving accent). */
internal const val RideModeTransitionMs = 360

@Composable
fun CombatModeNotch(
    rideMode: String,
    isMoving: Boolean = false,
    accent: Color = ClusterBlue,
    modifier: Modifier = Modifier
) {
    val colors = clusterColors()
    val barColor by animateColorAsState(
        targetValue = if (isMoving) accent else colors.combat,
        animationSpec = tween(RideModeTransitionMs, easing = FastOutSlowInEasing),
        label = "combatBarColor"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(CombatBarHeight),
        contentAlignment = Alignment.TopCenter
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val flatY = CombatNotchRise.toPx()
            val cx = size.width / 2f
            val topHalf = CombatNotchTopHalf.toPx()
            val baseHalf = CombatNotchBaseHalf.toPx()
            val r = 2.dp.toPx()

            val path = Path().apply {
                // Same sharp trapezoid as before; r only softens the 4 corners.
                moveTo(0f, flatY)
                lineTo(cx - baseHalf - r, flatY)
                quadraticTo(cx - baseHalf, flatY, cx - baseHalf + r, flatY - r)
                lineTo(cx - topHalf - r, r)
                quadraticTo(cx - topHalf, 0f, cx - topHalf + r, 0f)
                lineTo(cx + topHalf - r, 0f)
                quadraticTo(cx + topHalf, 0f, cx + topHalf + r, r)
                lineTo(cx + baseHalf - r, flatY - r)
                quadraticTo(cx + baseHalf, flatY, cx + baseHalf + r, flatY)
                lineTo(size.width, flatY)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(path, barColor)
        }

        // Sit the label in the raised notch, not the lower flat wings.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(CombatNotchRise + CombatFlatHeight * 0.65f)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            ModeGlyph(Modifier.size(12.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = rideMode,
                color = colors.onBar,
                fontFamily = ClusterUiFont,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.8.sp
            )
        }
    }
}

/** Shuffle / transfer glyph next to ride mode (SVG viewBox 0 0 8 8). */
private const val ModeGlyphViewBox = 8f
private const val ModeGlyphPath =
    "M1 0v2h1V1h4v2H5l1.5 2.5L8 3H7V0zm.5 2.5L0 5h1v3h6V6H6v1H2V5h1z"

@Composable
private fun ModeGlyph(modifier: Modifier = Modifier) {
    val colors = clusterColors()
    val path = remember { PathParser().parsePathString(ModeGlyphPath).toPath() }
    Canvas(modifier = modifier) {
        val scale = size.minDimension / ModeGlyphViewBox
        withTransform({
            scale(scale, scale, pivot = Offset.Zero)
        }) {
            drawPath(path, colors.onBar)
        }
    }
}
