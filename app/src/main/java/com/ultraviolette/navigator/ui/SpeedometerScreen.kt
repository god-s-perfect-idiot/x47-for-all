package com.ultraviolette.navigator.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ultraviolette.navigator.model.ClusterPlaceholders
import com.ultraviolette.navigator.model.SavedTrip
import com.ultraviolette.navigator.ui.components.CenterSpeedCluster
import com.ultraviolette.navigator.ui.components.ClusterFrame
import com.ultraviolette.navigator.ui.components.CombatBarHeight
import com.ultraviolette.navigator.ui.components.CombatFlatHeight
import com.ultraviolette.navigator.ui.components.CombatModeNotch
import com.ultraviolette.navigator.ui.components.CombatWingFooters
import com.ultraviolette.navigator.ui.components.LeftMetricsColumn
import com.ultraviolette.navigator.ui.components.LeftShrinkWing
import com.ultraviolette.navigator.ui.components.RightMetricsColumn
import com.ultraviolette.navigator.ui.components.RightShrinkWing
import com.ultraviolette.navigator.ui.theme.UltravioletteNavigatorTheme
import com.ultraviolette.navigator.ui.theme.rememberClusterAccent

/** Side rails hug the bezel; tight enough that avg / time grey boxes stay compact. */
private val SideRailWidth = 145.dp

private enum class ClusterPane { Speedometer, ControlCentre, Trips, Settings }

@Composable
fun SpeedometerScreen(
    data: ClusterPlaceholders = ClusterPlaceholders(),
    trips: List<SavedTrip> = emptyList(),
    onRideModeClick: () -> Unit = {},
    onEndRide: () -> Unit = {},
    onBikeNameChange: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var pane by remember { mutableStateOf(ClusterPane.Speedometer) }
    BackHandler(enabled = pane != ClusterPane.Speedometer) {
        pane = when (pane) {
            ClusterPane.Trips, ClusterPane.Settings -> ClusterPane.ControlCentre
            ClusterPane.ControlCentre -> ClusterPane.Speedometer
            ClusterPane.Speedometer -> ClusterPane.Speedometer
        }
    }

    ClusterFrame(
        data = data,
        modifier = modifier
    ) {
        AnimatedContent(
            targetState = pane,
            transitionSpec = {
                val spec = tween<Float>(280, easing = FastOutSlowInEasing)
                val slide = tween<IntOffset>(280, easing = FastOutSlowInEasing)
                // Speedometer is root; everything else is a forward drill-in from Control Centre.
                val opening = when {
                    initialState == ClusterPane.Speedometer -> true
                    targetState == ClusterPane.Speedometer -> false
                    initialState == ClusterPane.ControlCentre -> true
                    targetState == ClusterPane.ControlCentre -> false
                    else -> targetState.ordinal > initialState.ordinal
                }
                if (opening) {
                    (
                        fadeIn(spec) + slideInVertically(animationSpec = slide) { it }
                        ) togetherWith (
                        fadeOut(spec) + slideOutVertically(animationSpec = slide) { -it / 6 }
                        )
                } else {
                    (
                        fadeIn(spec) + slideInVertically(animationSpec = slide) { -it / 6 }
                        ) togetherWith (
                        fadeOut(spec) + slideOutVertically(animationSpec = slide) { it }
                        )
                } using SizeTransform(clip = false)
            },
            label = "clusterPane",
            modifier = Modifier.fillMaxSize()
        ) { current ->
            when (current) {
                ClusterPane.ControlCentre -> ControlCenterContent(
                    data = data,
                    onClose = { pane = ClusterPane.Speedometer },
                    onRideModeClick = onRideModeClick,
                    onTripsClick = { pane = ClusterPane.Trips },
                    onSettingsClick = { pane = ClusterPane.Settings },
                    modifier = Modifier.fillMaxSize()
                )
                ClusterPane.Trips -> TripsHistoryContent(
                    data = data,
                    trips = trips,
                    onClose = { pane = ClusterPane.ControlCentre },
                    onEndRide = onEndRide,
                    modifier = Modifier.fillMaxSize()
                )
                ClusterPane.Settings -> SettingsContent(
                    data = data,
                    onClose = { pane = ClusterPane.ControlCentre },
                    onBikeNameChange = onBikeNameChange,
                    modifier = Modifier.fillMaxSize()
                )
                ClusterPane.Speedometer -> SpeedometerBody(
                    data = data,
                    onControlCentreClick = { pane = ClusterPane.ControlCentre },
                    onRideModeClick = onRideModeClick,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun SpeedometerBody(
    data: ClusterPlaceholders,
    onControlCentreClick: () -> Unit,
    onRideModeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = rememberClusterAccent(data)

    Box(modifier = modifier) {
        Row(Modifier.fillMaxSize()) {
            // Stems continue through EFF / SPD wings; stop above the dark combat flat.
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
            accent = accent.primary,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        CombatWingFooters(
            data = data,
            accent = accent.primary,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Above wing footers so the full bottom strip receives taps.
        val modeTapSource = remember { MutableInteractionSource() }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(CombatBarHeight)
                .clickable(
                    interactionSource = modeTapSource,
                    indication = null,
                    onClick = onRideModeClick
                )
        )
    }
}

@Preview(name = "Combat", showBackground = true, widthDp = 854, heightDp = 480)
@Composable
private fun CombatPreview() {
    UltravioletteNavigatorTheme {
        SpeedometerScreen(ClusterPlaceholders())
    }
}
