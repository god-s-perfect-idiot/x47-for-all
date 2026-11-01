package com.ultraviolette.navigator.ui.ktm

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultraviolette.navigator.ui.theme.KtmDivider
import com.ultraviolette.navigator.ui.theme.KtmMuted
import com.ultraviolette.navigator.ui.theme.KtmNeutralGreen
import com.ultraviolette.navigator.ui.theme.KtmOrange
import com.ultraviolette.navigator.ui.theme.KtmPanel
import com.ultraviolette.navigator.ui.theme.KtmPanelLift
import com.ultraviolette.navigator.ui.theme.clusterColors
import com.ultraviolette.navigator.ui.theme.ktmPunctSafe
import com.ultraviolette.navigator.ui.theme.rememberKtmDisplayFont
import com.ultraviolette.navigator.ui.theme.rememberKtmUiFont

/** Fully-rounded orange selection pill used across KTM menus. */
val KtmMenuPillShape = RoundedCornerShape(20.dp)

@Composable
fun KtmPageBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = clusterColors()
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        content()
    }
}

/**
 * Shared KTM submenu shell — scrollable left pill list + right detail pane
 * + bottom back / status strip. Matches the control-centre layout.
 */
@Composable
fun KtmSubmenuShell(
    menuTitles: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    speedText: String? = null,
    gearText: String? = null,
    modeLabel: String? = null,
    bottomTrailing: (@Composable () -> Unit)? = null,
    detail: @Composable () -> Unit
) {
    val colors = clusterColors()
    val uiFont = rememberKtmUiFont()
    val displayFont = rememberKtmDisplayFont()
    val menuScroll = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {

        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 14.dp, top = 10.dp, bottom = 8.dp)
            ) {
                Column(
                    Modifier
                        .width(230.dp)
                        .fillMaxHeight()
                        .verticalScroll(menuScroll),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    menuTitles.forEachIndexed { index, title ->
                        KtmMenuPill(
                            title = title,
                            selected = index == selectedIndex,
                            onClick = { onSelect(index) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .ktmMenuEnter(ktmMenuEnterDelayMs(index))
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(6.dp))
                        .background(KtmPanelLift)
                        .padding(14.dp)
                        .ktmPreviewEnter()
                ) {
                    detail()
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .height(62.dp)
                    .background(KtmPanel)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                KtmPageBackButton(
                    onClick = onBack,
                    modifier = Modifier
                        .width(48.dp)
                        .height(40.dp)
                )
                Spacer(Modifier.weight(1f))
                if (bottomTrailing != null) {
                    bottomTrailing()
                    Spacer(Modifier.width(12.dp))
                }
                if (modeLabel != null) {
                    Text(
                        text = modeLabel,
                        color = KtmOrange,
                        fontFamily = uiFont,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.padding(end = 14.dp)
                    )
                }
                if (speedText != null) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = speedText,
                                color = Color.White,
                                fontFamily = displayFont,
                                fontSize = 44.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "km/h",
                                color = KtmMuted,
                                fontFamily = uiFont,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 7.dp)
                            )
                        }
                        if (gearText != null) {
                            Text(
                                text = gearText,
                                color = if (gearText == "N") KtmNeutralGreen else Color.White,
                                fontFamily = displayFont,
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KtmMenuPill(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiFont = rememberKtmUiFont()
    Row(
        modifier = modifier
            .height(40.dp)
            .clip(KtmMenuPillShape)
            .background(if (selected) KtmOrange else Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selected) {
            Canvas(Modifier.size(11.dp)) {
                val path = Path().apply {
                    moveTo(size.width * 0.5f, 0f)
                    lineTo(size.width, size.height * 0.5f)
                    lineTo(size.width * 0.5f, size.height)
                    lineTo(0f, size.height * 0.5f)
                    close()
                }
                drawPath(path, Color.White)
            }
            Spacer(Modifier.width(8.dp))
        } else {
            Spacer(Modifier.width(19.dp))
        }
        Text(
            text = title,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.88f),
            fontFamily = uiFont,
            fontSize = 18.sp,
            fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
fun KtmDetailTitle(text: String) {
    val uiFont = rememberKtmUiFont()
    Text(
        text = text,
        color = KtmMuted,
        fontFamily = uiFont,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.4.sp
    )
}

@Composable
fun KtmDetailStat(label: String, value: String) {
    val uiFont = rememberKtmUiFont()
    val valueText = remember(value) { ktmPunctSafe(value) }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = KtmMuted,
            fontFamily = uiFont,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = valueText,
            color = Color.White,
            fontFamily = uiFont,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun KtmDetailCta(text: String, onClick: () -> Unit) {
    val uiFont = rememberKtmUiFont()
    Text(
        text = text,
        color = KtmOrange,
        fontFamily = uiFont,
        fontSize = 16.sp,
        fontWeight = FontWeight.Black,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    )
}

@Composable
fun KtmDetailColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val scroll = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
fun KtmPageBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(KtmPanel)
            .border(1.dp, KtmDivider, RoundedCornerShape(6.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(22.dp)) {
            val path = PathParser().parsePathString(BackChevronPath).toPath()
            val scale = size.minDimension / BackChevronViewBox
            withTransform({
                scale(scale, scale, pivot = Offset.Zero)
            }) {
                drawPath(path, Color.White)
            }
        }
    }
}

private const val BackChevronViewBox = 1200f
private const val BackChevronPath =
    "m210.632 599.989l178.735 178.735L810.633 1200l178.735-178.721l-421.267-421.29l421.267-421.266L810.645 0L389.378 421.267L210.655 599.989z"
