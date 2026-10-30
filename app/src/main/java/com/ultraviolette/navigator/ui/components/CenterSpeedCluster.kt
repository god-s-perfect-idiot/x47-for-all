package com.ultraviolette.navigator.ui.components

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
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultraviolette.navigator.model.ClusterPlaceholders
import com.ultraviolette.navigator.ui.theme.ClusterDisplayFont
import com.ultraviolette.navigator.ui.theme.ClusterMuted
import com.ultraviolette.navigator.ui.theme.ClusterOrange
import com.ultraviolette.navigator.ui.theme.ClusterText
import com.ultraviolette.navigator.ui.theme.ClusterUiFont

@Composable
fun CenterSpeedCluster(
    data: ClusterPlaceholders,
    accent: Color,
    onControlCentreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                LeanAngleGauge(Modifier.fillMaxSize())
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
                    imageVector = Icons.Filled.WbSunny,
                    contentDescription = null,
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

        // Speed digits / "---" share one text style so idle dashes match font size.
        Text(
            text = data.speedKmh,
            color = ClusterText,
            fontFamily = ClusterDisplayFont,
            fontSize = SpeedDigitSize,
            fontWeight = FontWeight.Bold,
            letterSpacing = SpeedLetterSpacing,
            lineHeight = SpeedDigitSize,
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

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 22.dp)
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

private val SpeedDigitSize = 200.sp
private val SpeedLetterSpacing = 30.sp
private val SpeedDigitHalfHeight = 44.dp
private val SpeedUnitGap = 12.dp
private val SpeedUnitHalfHeight = 9.dp
