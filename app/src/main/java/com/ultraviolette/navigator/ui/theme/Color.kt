package com.ultraviolette.navigator.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Semantic cluster palette — light face by default, inverted greys in dark mode. */
data class ClusterColors(
    val background: Color,
    val bezel: Color,
    val dark: Color,
    val darker: Color,
    val combat: Color,
    val muted: Color,
    val text: Color,
    val value: Color,
    val panel: Color,
    val panelDeep: Color,
    val pill: Color,
    val pillBorder: Color,
    val crosshair: Color,
    val stripe: Color,
    val cell: Color,
    val topBarCenter: Color,
    val topBarCenterStroke: Color,
    val topBarBarTrack: Color,
    val modeBadgeFill: Color,
    val modeBadgeText: Color,
    val signalInactive: Color,
    val batteryTrack: Color,
    val bottomModeTab: Color,
    /** Glyphs drawn on the combat / accent bar (white↔black with mode). */
    val onBar: Color,
    val isDark: Boolean
)

private fun Color.invertRgb(): Color =
    Color(
        red = 1f - red,
        green = 1f - green,
        blue = 1f - blue,
        alpha = alpha
    )

private fun lightClusterColors(): ClusterColors = ClusterColors(
    background = Color(0xFFFFFFFF),
    bezel = Color(0xFF000000),
    dark = Color(0xFF2A2E33),
    darker = Color(0xFF1A1C1F),
    combat = Color(0xFF5A6068),
    muted = Color(0xFF7A8088),
    text = Color(0xFF1A1D22),
    value = Color(0xFF22262C),
    panel = Color(0xFFD8DCE2),
    panelDeep = Color(0xFFC4C9CF),
    pill = Color(0xFFF4F5F7),
    pillBorder = Color(0xFFD0D3D8),
    crosshair = Color(0xFFB0B4BB),
    stripe = Color(0xFFB8BCC2),
    cell = Color(0xFFC5C9CF),
    topBarCenter = Color(0xFFF2F3F5),
    topBarCenterStroke = Color(0xFFC5C9CF),
    topBarBarTrack = Color(0xFFB8BDC3),
    modeBadgeFill = Color(0xFF5A6068),
    modeBadgeText = Color(0xFFFFFFFF),
    signalInactive = Color(0xFFC4C8CD),
    batteryTrack = Color(0xFFCDD1D6),
    bottomModeTab = Color(0xFF23272C),
    onBar = Color(0xFFFFFFFF),
    isDark = false
)

private fun ClusterColors.inverted(): ClusterColors = ClusterColors(
    background = background.invertRgb(),
    // Edge letterbox + combat / mode chrome stay as-is — inversion washes them out.
    bezel = bezel,
    dark = dark.invertRgb(),
    darker = darker.invertRgb(),
    combat = combat,
    muted = muted.invertRgb(),
    text = text.invertRgb(),
    value = value.invertRgb(),
    panel = panel.invertRgb(),
    panelDeep = panelDeep.invertRgb(),
    pill = pill.invertRgb(),
    pillBorder = pillBorder.invertRgb(),
    crosshair = crosshair.invertRgb(),
    stripe = stripe.invertRgb(),
    cell = cell.invertRgb(),
    topBarCenter = topBarCenter.invertRgb(),
    topBarCenterStroke = topBarCenterStroke.invertRgb(),
    topBarBarTrack = topBarBarTrack.invertRgb(),
    modeBadgeFill = modeBadgeFill,
    modeBadgeText = modeBadgeText,
    signalInactive = signalInactive.invertRgb(),
    batteryTrack = batteryTrack.invertRgb(),
    bottomModeTab = bottomModeTab,
    onBar = onBar,
    isDark = true
)

val LightClusterColors: ClusterColors = lightClusterColors()
val DarkClusterColors: ClusterColors = LightClusterColors.inverted()

val LocalClusterColors = staticCompositionLocalOf { LightClusterColors }

@Composable
fun clusterColors(): ClusterColors = LocalClusterColors.current

// Light-mode aliases kept for design-facts probe + accents that never invert.
val ClusterBackground = Color(0xFFFFFFFF)
val ClusterBezel = Color(0xFF000000)
val ClusterDark = Color(0xFF2A2E33)
val ClusterDarker = Color(0xFF1A1C1F)
/** Ride-mode tab (COMBAT) — lighter than ClusterDark so it reads as a bar, not a black notch. */
val ClusterCombat = Color(0xFF5A6068)
val ClusterMuted = Color(0xFF7A8088)
val ClusterText = Color(0xFF1A1D22)
val ClusterValue = Color(0xFF22262C)

val ClusterBlue = Color(0xFF2F9BFF)
/** Soft status red (lean tick / hot phone badge). */
val ClusterRed = Color(0xFFE03131)
/** BALLISTIC ride-mode accent — darker than [ClusterRed]. */
val ClusterBallistic = Color(0xFFB01E1E)
/** Soft status green (phone badge / side battery). */
val ClusterGreen = Color(0xFF2FCB72)
/** GLIDE ride-mode accent — darker than [ClusterGreen]. */
val ClusterGlide = Color(0xFF0F6B38)
val ClusterOrange = Color(0xFFE89A3A)
/** SoC fill — stays green in both modes. */
val TopBarSoCFill = Color(0xFF1A9A52)

/** Side-metric inset fill — matches avg / time grey boxes on the cluster face. */
val ClusterPanel = Color(0xFFD8DCE2)
val ClusterPanelDeep = Color(0xFFC4C9CF)
val ClusterPill = Color(0xFFF4F5F7)
val ClusterPillBorder = Color(0xFFD0D3D8)
val ClusterCrosshair = Color(0xFFB0B4BB)
val ClusterStripe = Color(0xFFB8BCC2)
val ClusterCell = Color(0xFFC5C9CF)
