package com.ultraviolette.navigator.ui

import android.os.Build
import android.view.RoundedCorner
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import com.ultraviolette.navigator.model.ClusterPlaceholders
import com.ultraviolette.navigator.ui.components.CenterSpeedCluster
import com.ultraviolette.navigator.ui.components.CombatBarHeight
import com.ultraviolette.navigator.ui.components.CombatFlatHeight
import com.ultraviolette.navigator.ui.components.CombatModeNotch
import com.ultraviolette.navigator.ui.components.CombatWingFooters
import com.ultraviolette.navigator.ui.components.LeftMetricsColumn
import com.ultraviolette.navigator.ui.components.LeftShrinkWing
import com.ultraviolette.navigator.ui.components.RightMetricsColumn
import com.ultraviolette.navigator.ui.components.RightShrinkWing
import com.ultraviolette.navigator.ui.components.TopStatusBar
import com.ultraviolette.navigator.ui.theme.ClusterBackground
import com.ultraviolette.navigator.ui.theme.ClusterBezel
import com.ultraviolette.navigator.ui.theme.UltravioletteNavigatorTheme
import com.ultraviolette.navigator.ui.theme.rememberClusterAccent
import kotlin.math.max

/** Minimum black clearance so UI clears phone rounded corners on short edges. */
private val MinEdgeBezel = 28.dp
/** Side rails hug the bezel; tight enough that Wh/km / range grey boxes stay compact. */
private val SideRailWidth = 145.dp

@Composable
fun SpeedometerScreen(
    data: ClusterPlaceholders = ClusterPlaceholders(),
    onControlCentreClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val view = LocalView.current
    val cutout = WindowInsets.displayCutout
    val cutoutLeft = with(density) { cutout.getLeft(this, layoutDirection).toDp() }
    val cutoutRight = with(density) { cutout.getRight(this, layoutDirection).toDp() }

    // Re-read when view size/insets change (including sensorLandscape flips).
    val rounded = remember(view, view.width, view.height) {
        edgeRoundedCornerPx(view)
    }
    val leftRound = with(density) { rounded.left.toDp() }
    val rightRound = with(density) { rounded.right.toDp() }

    // Always pad both short edges. Cutout alone vanishes on the non-notch side after rotate;
    // rounded-corner + min bezel keep coverage stable in both landscape directions.
    val leftBezel = maxOf(MinEdgeBezel, cutoutLeft, leftRound)
    val rightBezel = maxOf(MinEdgeBezel, cutoutRight, rightRound)
    val accent = rememberClusterAccent(data)

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(ClusterBezel)
    ) {
        EdgeBezel(leftBezel)

        Column(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(ClusterBackground)
        ) {
            TopStatusBar(data = data)

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Row(Modifier.fillMaxSize()) {
                    // Stems continue through EFF / R3 wings; stop above the dark combat flat.
                    LeftMetricsColumn(
                        data = data,
                        accent = accent.label,
                        modifier = Modifier
                            .width(SideRailWidth)
                            .fillMaxHeight()
                            .padding(bottom = CombatFlatHeight)
                    )

                    CenterSpeedCluster(
                        data = data,
                        accent = accent.primary,
                        onControlCentreClick = onControlCentreClick,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(bottom = CombatBarHeight)
                    )

                    RightMetricsColumn(
                        data = data,
                        accent = accent.label,
                        modifier = Modifier
                            .width(SideRailWidth)
                            .fillMaxHeight()
                            .padding(bottom = CombatFlatHeight)
                    )
                }

                LeftShrinkWing(
                    modifier = Modifier.align(Alignment.BottomStart)
                )
                RightShrinkWing(
                    modifier = Modifier.align(Alignment.BottomEnd)
                )

                CombatModeNotch(
                    rideMode = data.rideMode,
                    isMoving = data.isMoving,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )

                CombatWingFooters(
                    data = data,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        EdgeBezel(rightBezel)
    }
}

@Composable
private fun EdgeBezel(width: Dp) {
    Spacer(
        Modifier
            .width(width)
            .fillMaxHeight()
            .background(ClusterBezel)
    )
}

private data class EdgeRounds(val left: Int, val right: Int)

private fun edgeRoundedCornerPx(view: android.view.View): EdgeRounds {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        return EdgeRounds(0, 0)
    }
    val insets = ViewCompat.getRootWindowInsets(view)?.toWindowInsets() ?: return EdgeRounds(0, 0)
    val left = max(
        insets.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)?.radius ?: 0,
        insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT)?.radius ?: 0
    )
    val right = max(
        insets.getRoundedCorner(RoundedCorner.POSITION_TOP_RIGHT)?.radius ?: 0,
        insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_RIGHT)?.radius ?: 0
    )
    return EdgeRounds(left, right)
}

@Preview(name = "Combat", showBackground = true, widthDp = 854, heightDp = 480)
@Composable
private fun CombatPreview() {
    UltravioletteNavigatorTheme {
        SpeedometerScreen(ClusterPlaceholders())
    }
}
