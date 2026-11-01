package com.ultraviolette.navigator.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import com.ultraviolette.navigator.model.BrandTheme
import com.ultraviolette.navigator.model.ClusterPlaceholders
import com.ultraviolette.navigator.model.RideMode
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** KTM Ready-to-Race orange — primary fill on tach + menu selection. */
val KtmOrange = Color(0xFFFF6600)
val KtmOrangeDeep = Color(0xFFE63E00)
val KtmOrangeDim = Color(0xFFB85A00)
val KtmCyan = Color(0xFF00D4E8)
val KtmNeutralGreen = Color(0xFF00E676)
val KtmRedline = Color(0xFFFF1A1A)
val KtmPanel = Color(0xFF141414)
val KtmPanelLift = Color(0xFF1C1C1C)
/** Lighter telemetry strip under the cluster face. */
val KtmBarPanel = Color(0xFF2E2E2E)
val KtmDivider = Color(0xFF3A3A3A)
val KtmMuted = Color(0xFF8A8A8A)
val KtmGhost = Color(0xFF4A4A4A)

/** Always-dark KTM face — independent of ambient light inversion. */
val KtmClusterColors = ClusterColors(
    background = Color(0xFF000000),
    bezel = Color(0xFF000000),
    dark = Color(0xFF222222),
    darker = Color(0xFF141414),
    combat = Color(0xFF2A2A2A),
    muted = KtmMuted,
    text = Color(0xFFFFFFFF),
    value = Color(0xFFFFFFFF),
    panel = KtmPanel,
    panelDeep = KtmPanelLift,
    pill = Color(0xFF1A1A1A),
    pillBorder = KtmDivider,
    crosshair = KtmGhost,
    stripe = KtmDivider,
    cell = Color(0xFF222222),
    topBarCenter = Color(0xFF161616),
    topBarCenterStroke = KtmDivider,
    topBarBarTrack = Color(0xFF2A2A2A),
    modeBadgeFill = Color(0xFF2A2A2A),
    modeBadgeText = Color(0xFFFFFFFF),
    signalInactive = KtmGhost,
    batteryTrack = Color(0xFF2A2A2A),
    bottomModeTab = Color(0xFF121212),
    onBar = Color(0xFFFFFFFF),
    isDark = true
)

fun isKtmTheme(themeId: String): Boolean =
    BrandTheme.canonicalize(themeId) == BrandTheme.KTM

/** Map UV ride modes onto KTM TFT labels from the reference. */
fun ktmRideModeLabel(rideMode: String): String = when (
    RideMode.canonicalize(rideMode) ?: RideMode.COMBAT
) {
    RideMode.GLIDE -> "STREET"
    RideMode.BALLISTIC -> "TRACK"
    else -> "SUPERMOTO"
}

fun ktmRideAccent(rideMode: String): Color = when (
    RideMode.canonicalize(rideMode) ?: RideMode.COMBAT
) {
    RideMode.GLIDE -> KtmOrangeDim
    RideMode.BALLISTIC -> KtmOrangeDeep
    else -> KtmOrange
}

/**
 * Pseudo gear from road speed — phone has no gearbox sensor.
 * Parked → N (green); otherwise stepped 1–6.
 */
fun ktmGearFromSpeed(speedText: String, isMoving: Boolean): String {
    val speed = speedText.filter { it.isDigit() }.toIntOrNull() ?: 0
    if (!isMoving || speed <= 0) return "N"
    return when {
        speed <= 25 -> "1"
        speed <= 45 -> "2"
        speed <= 65 -> "3"
        speed <= 85 -> "4"
        speed <= 105 -> "5"
        else -> "6"
    }
}

/**
 * Map road speed onto a 0–10 tach sweep (reference shows ~6 at 50 km/h).
 */
fun ktmRpmFromSpeed(speedText: String): Float {
    val speed = speedText.filter { it.isDigit() }.toIntOrNull() ?: 0
    return (speed / 85f * 10f).coerceIn(0f, 10f)
}

/** Phone SoC → glanceable range proxy (Battery detail only — not on the cluster). */
fun ktmRangeKm(batteryPercent: Int): String {
    val km = (batteryPercent.coerceIn(0, 100) * 4.6f).toInt()
    return "$km km"
}

/** Score readout for EFF / SPD (0–10, or -- when idle). */
fun ktmScoreText(bars: Int, active: Boolean): String =
    if (active) bars.coerceIn(0, 10).toString() else "--"

@Composable
fun rememberKtmUiFont(): FontFamily = SpaceportOneFont

@Composable
fun rememberKtmDisplayFont(): FontFamily = SpaceportOneFont

/**
 * Spaceport One maps ':' to an empty .notdef composite — the clock reads as
 * "2023" without a separator. Paint ':' in Dashford, which has a real glyph.
 */
fun ktmPunctSafe(text: String): AnnotatedString = buildAnnotatedString {
    for (ch in text) {
        if (ch == ':') {
            withStyle(SpanStyle(fontFamily = DashfordFont)) { append(ch) }
        } else {
            append(ch)
        }
    }
}

private val KtmWallIn = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)
private val KtmWallOut = DateTimeFormatter.ofPattern("HH:mm", Locale.US)

/** KTM wall clock digits — 24h `20:23` like the Ready-to-Race TFT reference. */
fun ktmWallClockDigits(timeText: String): String =
    runCatching {
        LocalTime.parse(timeText.trim().uppercase(Locale.US), KtmWallIn).format(KtmWallOut)
    }.getOrElse {
        runCatching { LocalTime.now().format(KtmWallOut) }.getOrDefault(timeText)
    }

fun ktmWallClock(timeText: String): AnnotatedString =
    ktmPunctSafe(ktmWallClockDigits(timeText))

data class KtmChrome(
    val orange: Color,
    val cyan: Color,
    val neutral: Color,
    val redline: Color,
    val modeLabel: String,
    val gear: String,
    val rpm: Float,
    val effBars: Int,
    val effActive: Boolean,
    val effText: String,
    /** Speed-discipline / smoothness — drives the SPD progress bar outside the RPM arc. */
    val spdBars: Int,
    val spdActive: Boolean,
    val spdText: String
)

@Composable
fun rememberKtmChrome(data: ClusterPlaceholders): KtmChrome {
    return remember(
        data.rideMode,
        data.speedKmh,
        data.isMoving,
        data.efficiencyBars,
        data.efficiencyActive,
        data.disciplineBars,
        data.disciplineActive
    ) {
        KtmChrome(
            orange = ktmRideAccent(data.rideMode),
            cyan = KtmCyan,
            neutral = KtmNeutralGreen,
            redline = KtmRedline,
            modeLabel = ktmRideModeLabel(data.rideMode),
            gear = ktmGearFromSpeed(data.speedKmh, data.isMoving),
            rpm = ktmRpmFromSpeed(data.speedKmh),
            effBars = data.efficiencyBars.coerceIn(0, 10),
            effActive = data.efficiencyActive,
            effText = ktmScoreText(data.efficiencyBars, data.efficiencyActive),
            spdBars = data.disciplineBars.coerceIn(0, 10),
            spdActive = data.disciplineActive,
            spdText = ktmScoreText(data.disciplineBars, data.disciplineActive)
        )
    }
}
