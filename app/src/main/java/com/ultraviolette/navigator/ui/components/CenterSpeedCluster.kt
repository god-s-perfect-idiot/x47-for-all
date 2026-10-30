package com.ultraviolette.navigator.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultraviolette.navigator.model.ClusterPlaceholders
import com.ultraviolette.navigator.ui.theme.ClusterDisplayFont
import com.ultraviolette.navigator.ui.theme.ClusterMuted
import com.ultraviolette.navigator.ui.theme.ClusterOrange
import com.ultraviolette.navigator.ui.theme.ClusterText
import com.ultraviolette.navigator.ui.theme.ClusterUiFont
import com.ultraviolette.navigator.weather.WeatherCondition

@Composable
fun CenterSpeedCluster(
    data: ClusterPlaceholders,
    accent: Color,
    onControlCentreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val showIdleChrome = !data.isMoving

    Box(modifier = modifier.fillMaxSize()) {
        CrosshairFrame(Modifier.fillMaxSize())

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(start = 72.dp, end = 72.dp, top = 18.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier.size(width = 78.dp, height = 48.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                LeanAngleGauge(
                    leanAngleDeg = data.leanAngleSignedDeg,
                    modifier = Modifier.fillMaxSize()
                )
                DegreeValue(
                    number = data.leanAngleDeg,
                    color = ClusterText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.offset(y = (-1).dp)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Icon(
                    imageVector = weatherIcon(data.weatherCondition, data.weatherIsDay),
                    contentDescription = "Weather",
                    tint = ClusterOrange,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                DegreeValue(
                    number = data.ambientTempC,
                    color = ClusterText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    suffix = "C"
                )
            }
        }

        AnimatedSpeedDigits(
            text = data.speedKmh,
            modifier = Modifier.align(Alignment.Center)
        )

        SlashLabel(
            left = data.speedUnitLeft,
            right = data.speedUnitRight,
            color = ClusterMuted,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = SpeedDigitHalfHeight + SpeedUnitGap + SpeedUnitHalfHeight)
        )

        AnimatedVisibility(
            visible = showIdleChrome,
            enter = fadeIn(
                animationSpec = tween(RideModeTransitionMs, easing = FastOutSlowInEasing)
            ) + scaleIn(
                initialScale = 0.92f,
                animationSpec = tween(RideModeTransitionMs, easing = FastOutSlowInEasing)
            ),
            exit = fadeOut(
                animationSpec = tween(RideModeTransitionMs, easing = FastOutSlowInEasing)
            ) + scaleOut(
                targetScale = 0.92f,
                animationSpec = tween(RideModeTransitionMs, easing = FastOutSlowInEasing)
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 22.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(196.dp)
                    .height(30.dp)
                    .clickable(onClick = onControlCentreClick),
                contentAlignment = Alignment.Center
            ) {
                ControlCentreShape(color = accent, modifier = Modifier.fillMaxSize())
                Text(
                    text = "CONTROL CENTRE",
                    color = ClusterText,
                    fontFamily = ClusterUiFont,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }
        }
    }
}

/**
 * Three fixed slots. Idle `———` morphs into padded digits (`001`…);
 * each glyph animates independently when it changes.
 */
@Composable
private fun AnimatedSpeedDigits(
    text: String,
    modifier: Modifier = Modifier
) {
    val glyphs = text.padEnd(3, '—').take(3)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(SpeedDigitGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        glyphs.forEachIndexed { index, glyph ->
            key(index) {
                AnimatedContent(
                    targetState = glyph,
                    transitionSpec = {
                        // Crossfade + soft scale — glyphs morph in place, no slide.
                        val spec = tween<Float>(DigitMorphMs, easing = FastOutSlowInEasing)
                        (
                            fadeIn(spec) + scaleIn(initialScale = 0.88f, animationSpec = spec)
                            ) togetherWith (
                            fadeOut(spec) + scaleOut(targetScale = 1.06f, animationSpec = spec)
                            ) using SizeTransform(clip = false)
                    },
                    label = "speedDigit$index"
                ) { char ->
                    Text(
                        text = char.toString(),
                        color = ClusterText,
                        fontFamily = ClusterDisplayFont,
                        fontSize = SpeedDigitSize,
                        fontWeight = FontWeight.Bold,
                        lineHeight = SpeedDigitSize,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(SpeedDigitWidth)
                    )
                }
            }
        }
    }
}

private fun weatherIcon(condition: WeatherCondition, isDay: Boolean): ImageVector =
    when (condition) {
        WeatherCondition.CLEAR -> if (isDay) Icons.Filled.WbSunny else Icons.Filled.NightsStay
        WeatherCondition.PARTLY_CLOUDY -> Icons.Filled.WbCloudy
        WeatherCondition.CLOUDY -> Icons.Filled.Cloud
        WeatherCondition.FOG -> Icons.Filled.Cloud
        WeatherCondition.DRIZZLE -> Icons.Filled.Grain
        WeatherCondition.RAIN -> Icons.Filled.WaterDrop
        WeatherCondition.SNOW -> Icons.Filled.AcUnit
        WeatherCondition.THUNDER -> Icons.Filled.Thunderstorm
    }

private val SpeedDigitSize = 100.sp
private val SpeedDigitWidth = 64.dp
private val SpeedDigitGap = 32.dp
private val SpeedDigitHalfHeight = 44.dp
private val SpeedUnitGap = 12.dp
private val SpeedUnitHalfHeight = 9.dp
private const val DigitMorphMs = 280
