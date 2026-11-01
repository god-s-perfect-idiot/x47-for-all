package com.ultraviolette.navigator.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultraviolette.navigator.model.ClusterPlaceholders
import com.ultraviolette.navigator.ui.theme.ClusterDisplayFont
import com.ultraviolette.navigator.ui.theme.ClusterUiFont
import com.ultraviolette.navigator.ui.theme.clusterColors

/** Outer inset for left rail start / right rail end (inner side stays 10.dp). */
internal val SideMetricsEdgePad = 16.dp
private val EdgePad = SideMetricsEdgePad
/** Soft radius on wing corners (matches MetricPanelShape). */
private val WingCorner = 4.dp
/** Thin grey stem under avg / time — narrower so the block reads as shrinking. */
private val PanelStemWidth = 56.dp
/** Bottom wing under EFF / SPD — taller grey band, runs into the combat notch. */
private val ShrinkWingHeight = 40.dp
/** Outer inset (screen short edge) for each EFF / SPD wing group. */
private val FooterWingOuterPad = 32.dp
/** Inner inset (toward combat notch) — tighter so groups sit a touch closer. */
private val FooterWingInnerPad = 16.dp
private val FooterBottomPad = 8.dp
private val FooterMarkHeight = 16.dp
private val FooterLabelSize = 15.sp
private val FooterStripeWidth = 120.dp
/** Even gap between label / value / sub / underline on each side stack. */
private val MetricStackGap = 6.dp
/** Shared avg + trip-time figure — one size, no clip (Klapt trim/clip was cutting digits). */
private val SideValueSize = 42.sp
/** Shared accent label size for avg and time. */
private val SideLabelSize = 16.sp
/** Inner pad inside the grey metric panel. */
private val PanelPadH = 12.dp
private val PanelPadV = 10.dp
/** Extra bottom inset so metrics clear the steeper stem-fold hypotenuse. */
private val PanelPadBottom = 28.dp

/**
 * Klapt demo metrics are unreliable — never use Trim.Both on display figures
 * (it was slicing tops of display digits). Keep includeFontPadding off only.
 */
private fun tightStyle(
    fontSize: TextUnit,
    fontWeight: FontWeight,
    fontFamily: FontFamily,
    letterSpacing: TextUnit = 0.sp,
    lineHeight: TextUnit = fontSize,
    trim: LineHeightStyle.Trim = LineHeightStyle.Trim.None
) = TextStyle(
    fontFamily = fontFamily,
    fontWeight = fontWeight,
    fontSize = fontSize,
    letterSpacing = letterSpacing,
    lineHeight = lineHeight,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = trim
    )
)

/** Big side value — shared style for avg (3 digits) and trip time. */
@Composable
private fun SideMetricValue(
    text: String,
    alignEnd: Boolean,
    fontSize: TextUnit = SideValueSize,
    letterSpacing: TextUnit = (-1.2).sp,
    modifier: Modifier = Modifier
) {
    val colors = clusterColors()
    Text(
        text = text,
        color = colors.text,
        textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
        softWrap = false,
        maxLines = 1,
        modifier = modifier,
        style = tightStyle(
            fontSize = fontSize,
            fontWeight = FontWeight.Medium,
            fontFamily = ClusterDisplayFont,
            letterSpacing = letterSpacing,
            lineHeight = fontSize
        )
    )
}

/** Left rail: odo + avg speed panel, with a thinner grey stem continuing below. */
@Composable
fun LeftMetricsColumn(
    data: ClusterPlaceholders,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val colors = clusterColors()
    Column(modifier.fillMaxHeight()) {
        MetricHeader(
            label = "odo",
            value = data.odometerKm,
            accent = accent,
            modifier = Modifier.padding(start = EdgePad, top = 2.dp, bottom = 8.dp)
        )

        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(MetricStackGap),
            modifier = Modifier
                .padding(end = 10.dp, top = 8.dp)
                .fillMaxWidth()
                .clip(MetricPanelShape(mirror = false, foldToWidth = PanelStemWidth))
                .background(colors.panel)
                .padding(
                    start = EdgePad,
                    end = PanelPadH,
                    top = PanelPadV,
                    bottom = PanelPadBottom
                )
        ) {
            Text(
                text = "avg speed",
                color = accent,
                style = tightStyle(
                    fontSize = SideLabelSize,
                    fontWeight = FontWeight.Medium,
                    fontFamily = ClusterUiFont
                )
            )
            SideMetricValue(
                text = data.avgSpeedKmh,
                alignEnd = false
            )
            Text(
                text = "km/h",
                color = colors.muted,
                style = tightStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = ClusterUiFont
                )
            )
            AccentUnderline(
                accent = accent,
                mirror = false
            )
        }

        // Thinner continuation under the panel — same grey, left-aligned shrink.
        // Offset up under the panel’s soft bottom corners so the join stays solid.
        Box(
            Modifier
                .padding(end = 10.dp)
                .offset(y = -WingCorner)
                .width(PanelStemWidth)
                .weight(1f)
                .background(
                    colors.panel,
                    RoundedCornerShape(bottomStart = WingCorner, bottomEnd = WingCorner)
                )
        )
    }
}

/** Right rail: ride + trip time panel, with a thinner grey stem continuing below. */
@Composable
fun RightMetricsColumn(
    data: ClusterPlaceholders,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val colors = clusterColors()
    Column(
        modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.End
    ) {
        MetricHeader(
            label = "ride",
            value = data.tripKm,
            accent = accent,
            alignEnd = true,
            modifier = Modifier.padding(end = EdgePad, top = 2.dp, bottom = 8.dp)
        )

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(MetricStackGap),
            modifier = Modifier
                .padding(start = 10.dp, top = 8.dp)
                .fillMaxWidth()
                .clip(MetricPanelShape(mirror = true, foldToWidth = PanelStemWidth))
                .background(colors.panel)
                .padding(
                    start = PanelPadH,
                    end = EdgePad,
                    top = PanelPadV,
                    bottom = PanelPadBottom
                )
        ) {
            Text(
                text = "time",
                color = accent,
                style = tightStyle(
                    fontSize = SideLabelSize,
                    fontWeight = FontWeight.Medium,
                    fontFamily = ClusterUiFont
                )
            )
            SideMetricValue(
                text = data.tripTimeText,
                alignEnd = true
            )
            Text(
                text = "h:m",
                color = colors.muted,
                style = tightStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = ClusterUiFont
                )
            )
            AccentUnderline(
                accent = accent,
                mirror = true
            )
        }

        // Thinner continuation under the panel — same grey, right-aligned shrink.
        // Offset up under the panel’s soft bottom corners so the join stays solid.
        Box(
            Modifier
                .padding(start = 10.dp)
                .offset(y = -WingCorner)
                .width(PanelStemWidth)
                .weight(1f)
                .background(
                    colors.panel,
                    RoundedCornerShape(bottomStart = WingCorner, bottomEnd = WingCorner)
                )
        )
    }
}

/**
 * Grey band under EFF: left edge → into the combat notch plateau.
 * Top aligns with notch top-left; slant runs down-right into the plateau.
 * Soft lower corners where the hill meets the combat flat / notch.
 */
@Composable
fun LeftShrinkWing(modifier: Modifier = Modifier) {
    val colors = clusterColors()
    Canvas(
        modifier
            .fillMaxWidth()
            .padding(bottom = CombatFlatHeight)
            .height(ShrinkWingHeight)
    ) {
        val cx = size.width / 2f
        val topHalf = CombatNotchTopHalf.toPx()
        val topRight = cx - topHalf
        val bottomRight = topRight + size.height // ~45° into the notch
        val r = WingCorner.toPx()
        val path = filletedPolygon(
            vertices = listOf(
                Offset(0f, 0f),
                Offset(topRight, 0f),
                Offset(bottomRight, size.height),
                Offset(0f, size.height)
            ),
            // Soften lower corners (flat + notch) and the top-inner; keep outer-top square.
            radii = floatArrayOf(0f, r, r, r)
        )
        drawPath(path, colors.panel)
    }
}

/**
 * Grey band under SPD: mirrored LeftShrinkWing into the combat notch plateau.
 * Top aligns with notch top-right; slant runs down-left into the plateau.
 * Soft lower corners where the hill meets the combat flat / notch.
 */
@Composable
fun RightShrinkWing(modifier: Modifier = Modifier) {
    val colors = clusterColors()
    Canvas(
        modifier
            .fillMaxWidth()
            .padding(bottom = CombatFlatHeight)
            .height(ShrinkWingHeight)
    ) {
        val cx = size.width / 2f
        val topHalf = CombatNotchTopHalf.toPx()
        val topLeft = cx + topHalf
        val bottomLeft = topLeft - size.height // ~45° into the notch
        val r = WingCorner.toPx()
        val path = filletedPolygon(
            vertices = listOf(
                Offset(size.width, 0f),
                Offset(size.width, size.height),
                Offset(bottomLeft, size.height),
                Offset(topLeft, 0f)
            ),
            // Soften lower corners (flat + notch) and the top-inner; keep outer-top square.
            radii = floatArrayOf(0f, r, r, r)
        )
        drawPath(path, colors.panel)
    }
}

/**
 * EFF / SPD sit in the light band above the combat flat, each group centered
 * in its wing between the short edge and the notch.
 */
@Composable
fun CombatWingFooters(
    data: ClusterPlaceholders,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val colors = clusterColors()
    Row(
        modifier
            .fillMaxWidth()
            .padding(bottom = CombatFlatHeight + FooterBottomPad),
        verticalAlignment = Alignment.Bottom
    ) {
        Row(
            Modifier
                .weight(1f)
                .padding(start = FooterWingOuterPad, end = FooterWingInnerPad),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            DiagonalStripes(
                modifier = Modifier
                    .width(FooterStripeWidth)
                    .height(FooterMarkHeight),
                filled = data.efficiencyBars,
                active = data.efficiencyActive,
                fillColor = accent,
                emptyColor = colors.stripe,
                offColor = colors.combat
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = data.efficiencyLabel,
                color = colors.muted,
                fontFamily = ClusterUiFont,
                fontSize = FooterLabelSize,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp
            )
        }

        Spacer(Modifier.width(CombatNotchBaseHalf * 2))

        Row(
            Modifier
                .weight(1f)
                .padding(start = FooterWingInnerPad, end = FooterWingOuterPad),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = data.disciplineLabel,
                color = colors.muted,
                fontFamily = ClusterUiFont,
                fontSize = FooterLabelSize,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp
            )
            Spacer(Modifier.width(12.dp))
            DiagonalStripes(
                modifier = Modifier
                    .width(FooterStripeWidth)
                    .height(FooterMarkHeight),
                filled = data.disciplineBars,
                active = data.disciplineActive,
                mirrorSlant = true,
                fillColor = accent,
                emptyColor = colors.stripe,
                offColor = colors.combat
            )
        }
    }
}

@Composable
private fun MetricHeader(
    label: String,
    value: String,
    accent: Color,
    alignEnd: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = clusterColors()
    Column(
        modifier,
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start
    ) {
        Text(
            text = label,
            color = accent,
            fontFamily = ClusterUiFont,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            color = colors.value,
            fontFamily = ClusterUiFont,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun AccentUnderline(
    accent: Color,
    mirror: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier
            .width(22.dp)
            .height(2.dp)
    ) {
        val path = Path().apply {
            if (!mirror) {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width - size.height, size.height)
                lineTo(0f, size.height)
            } else {
                moveTo(size.height, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
            }
            close()
        }
        drawPath(path, accent)
    }
}
