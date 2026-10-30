package com.ultraviolette.navigator.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultraviolette.navigator.ui.theme.ClusterUiFont

/**
 * Dashford DEMO omits punctuation (%, /, °, <, - …).
 * These helpers draw those marks instead of falling back to another face.
 */

private fun noPadStyle(
    fontSize: TextUnit,
    fontWeight: FontWeight,
    fontFamily: FontFamily
) = TextStyle(
    fontFamily = fontFamily,
    fontWeight = fontWeight,
    fontSize = fontSize,
    lineHeight = fontSize,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    // Demo metrics can clip — keep Trim.None on display/label figures.
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None
    )
)

@Composable
fun SlashLabel(
    left: String,
    right: String,
    color: Color,
    fontSize: TextUnit,
    fontWeight: FontWeight = FontWeight.Medium,
    fontFamily: FontFamily = ClusterUiFont,
    modifier: Modifier = Modifier
) {
    val style = noPadStyle(fontSize, fontWeight, fontFamily)
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(left, color = color, style = style)
        Spacer(Modifier.width(3.dp))
        Canvas(Modifier.size(width = 7.dp, height = (fontSize.value * 0.75f).dp)) {
            drawLine(
                color = color,
                start = Offset(size.width, 0f),
                end = Offset(0f, size.height),
                strokeWidth = 1.6.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        Spacer(Modifier.width(3.dp))
        Text(right, color = color, style = style)
    }
}

@Composable
fun PercentValue(
    value: Int,
    color: Color,
    fontSize: TextUnit,
    fontWeight: FontWeight = FontWeight.SemiBold,
    fontFamily: FontFamily = ClusterUiFont,
    modifier: Modifier = Modifier
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = value.toString(),
            color = color,
            fontFamily = fontFamily,
            fontSize = fontSize,
            fontWeight = fontWeight
        )
        Spacer(Modifier.width(2.dp))
        Canvas(Modifier.size((fontSize.value * 0.55f).dp)) {
            val stroke = 1.4.dp.toPx()
            drawCircle(
                color,
                radius = size.minDimension * 0.16f,
                center = Offset(size.width * 0.30f, size.height * 0.30f),
                style = Stroke(stroke)
            )
            drawCircle(
                color,
                radius = size.minDimension * 0.16f,
                center = Offset(size.width * 0.70f, size.height * 0.70f),
                style = Stroke(stroke)
            )
            drawLine(
                color,
                Offset(size.width * 0.78f, size.height * 0.18f),
                Offset(size.width * 0.22f, size.height * 0.82f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun DegreeValue(
    number: String,
    color: Color,
    fontSize: TextUnit,
    fontWeight: FontWeight = FontWeight.SemiBold,
    fontFamily: FontFamily = ClusterUiFont,
    suffix: String = "",
    modifier: Modifier = Modifier
) {
    Row(modifier, verticalAlignment = Alignment.Top) {
        Text(number, color = color, fontFamily = fontFamily, fontSize = fontSize, fontWeight = fontWeight)
        Spacer(Modifier.width(1.dp))
        Canvas(
            Modifier
                .size((fontSize.value * 0.28f).dp)
                .align(Alignment.Top)
        ) {
            drawCircle(
                color = color,
                radius = size.minDimension * 0.35f,
                center = Offset(size.width / 2f, size.height / 2f),
                style = Stroke(1.3.dp.toPx())
            )
        }
        if (suffix.isNotEmpty()) {
            Text(
                text = suffix,
                color = color,
                fontFamily = fontFamily,
                fontSize = fontSize,
                fontWeight = fontWeight
            )
        }
    }
}

@Composable
fun RecLessThan(
    value: String,
    color: Color,
    fontSize: TextUnit,
    fontWeight: FontWeight = FontWeight.Medium,
    fontFamily: FontFamily = ClusterUiFont,
    modifier: Modifier = Modifier
) {
    val style = noPadStyle(fontSize, fontWeight, fontFamily)
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text("Rec ", color = color, style = style)
        Canvas(Modifier.size(width = 8.dp, height = 10.dp)) {
            drawLine(color, Offset(size.width, 0f), Offset(0f, size.height / 2f), 1.6.dp.toPx(), StrokeCap.Round)
            drawLine(color, Offset(0f, size.height / 2f), Offset(size.width, size.height), 1.6.dp.toPx(), StrokeCap.Round)
        }
        Spacer(Modifier.width(4.dp))
        Text(value, color = color, style = style)
    }
}
