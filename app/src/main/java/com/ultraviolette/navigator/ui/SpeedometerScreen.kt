package com.ultraviolette.navigator.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
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
import com.ultraviolette.navigator.ui.ktm.KtmClusterBody
import com.ultraviolette.navigator.ui.ktm.KtmControlCenterContent
import com.ultraviolette.navigator.ui.ktm.KtmPaneMotionMs
import com.ultraviolette.navigator.ui.theme.UltravioletteNavigatorTheme
import com.ultraviolette.navigator.ui.theme.isKtmTheme
import com.ultraviolette.navigator.ui.theme.rememberClusterAccent

/** Side rails hug the bezel; tight enough that avg / time grey boxes stay compact. */
private val SideRailWidth = 145.dp

private enum class ClusterPane { Speedometer, ControlCentre, Trips, Settings }

@Composable
fun SpeedometerScreen(
    data: ClusterPlaceholders = ClusterPlaceholders(),
    trips: List<SavedTrip> = emptyList(),
    onRideModeClick: () -> Unit = {},
    onRideModeSelect: (String) -> Unit = {},
    onEndRide: () -> Unit = {},
    onBikeNameChange: (String) -> Unit = {},
    onBrandThemeChange: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var pane by remember { mutableStateOf(ClusterPane.Speedometer) }
    var themesImmersive by remember { mutableStateOf(false) }
    val ktm = isKtmTheme(data.brandTheme)
    BackHandler(enabled = pane != ClusterPane.Speedometer) {
        pane = when (pane) {
            ClusterPane.Trips, ClusterPane.Settings -> ClusterPane.ControlCentre
            ClusterPane.ControlCentre -> ClusterPane.Speedometer
            ClusterPane.Speedometer -> ClusterPane.Speedometer
        }
    }

    ClusterFrame(
        data = data,
        // KTM draws its own clock / ABS chrome; UV keeps the geometric top strip.
        showTopBar = !themesImmersive && !ktm,
        modifier = modifier
    ) {
        AnimatedContent(
            targetState = pane,
            transitionSpec = {
                val opening = when {
                    initialState == ClusterPane.Speedometer -> true
                    targetState == ClusterPane.Speedometer -> false
                    initialState == ClusterPane.ControlCentre -> true
                    targetState == ClusterPane.ControlCentre -> false
                    else -> targetState.ordinal > initialState.ordinal
                }
                if (ktm) {
                    val spec = tween<Float>(KtmPaneMotionMs, easing = FastOutSlowInEasing)
                    val slide = tween<IntOffset>(KtmPaneMotionMs, easing = FastOutSlowInEasing)
                    // Horizontal punch — KTM TFT menus slide sideways.
                    if (opening) {
                        (
                            fadeIn(spec) + slideInHorizontally(animationSpec = slide) { it / 3 }
                            ) togetherWith (
                            fadeOut(spec) + slideOutHorizontally(animationSpec = slide) { -it / 5 }
                            )
                    } else {
                        (
                            fadeIn(spec) + slideInHorizontally(animationSpec = slide) { -it / 5 }
                            ) togetherWith (
                            fadeOut(spec) + slideOutHorizontally(animationSpec = slide) { it / 3 }
                            )
                    } using SizeTransform(clip = false)
                } else {
                    val spec = tween<Float>(280, easing = FastOutSlowInEasing)
                    val slide = tween<IntOffset>(280, easing = FastOutSlowInEasing)
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
                }
            },
            label = "clusterPane",
            modifier = Modifier.fillMaxSize()
        ) { current ->
            when (current) {
                ClusterPane.ControlCentre -> {
                    if (ktm) {
                        KtmControlCenterContent(
                            data = data,
                            onClose = { pane = ClusterPane.Speedometer },
                            onRideModeClick = onRideModeClick,
                            onRideModeSelect = onRideModeSelect,
                            onTripsClick = { pane = ClusterPane.Trips },
                            onSettingsClick = { pane = ClusterPane.Settings },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        ControlCenterContent(
                            data = data,
                            onClose = { pane = ClusterPane.Speedometer },
                            onRideModeClick = onRideModeClick,
                            onTripsClick = { pane = ClusterPane.Trips },
                            onSettingsClick = { pane = ClusterPane.Settings },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
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
                    onBrandThemeChange = onBrandThemeChange,
                    onThemesActiveChange = { themesImmersive = it },
                    modifier = Modifier.fillMaxSize()
                )
                ClusterPane.Speedometer -> {
                    if (ktm) {
                        KtmClusterBody(
                            data = data,
                            onControlCentreClick = { pane = ClusterPane.ControlCentre },
                            onRideModeClick = onRideModeClick,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        SpeedometerBody(
                            data = data,
                            onControlCentreClick = { pane = ClusterPane.ControlCentre },
                            onRideModeClick = onRideModeClick,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
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

@Preview(name = "KTM", showBackground = true, widthDp = 854, heightDp = 480)
@Composable
private fun KtmPreview() {
    UltravioletteNavigatorTheme(brandTheme = "KTM") {
        SpeedometerScreen(
            ClusterPlaceholders(
                brandTheme = "KTM",
                speedKmh = "050",
                isMoving = true,
                timeText = "20:23",
                tripKm = "32.1 km",
                avgSpeedKmh = "64",
                odometerKm = "1010 km",
                batteryPercent = 100
            )
        )
    }
}
