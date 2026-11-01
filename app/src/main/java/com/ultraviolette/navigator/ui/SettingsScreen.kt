package com.ultraviolette.navigator.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultraviolette.navigator.model.BikeNameStore
import com.ultraviolette.navigator.model.ClusterPlaceholders
import com.ultraviolette.navigator.ui.components.ChamferShape
import com.ultraviolette.navigator.ui.theme.ClusterUiFont
import com.ultraviolette.navigator.ui.theme.UltravioletteNavigatorTheme
import com.ultraviolette.navigator.ui.theme.clusterColors
import com.ultraviolette.navigator.ui.theme.rememberClusterAccent

private enum class SettingsPage { Menu, Themes, Vehicle }

/**
 * Settings hub opened from the Control Centre SETTINGS tile.
 * Hosts Themes (placeholder) and Vehicle (bike name editor).
 */
@Composable
fun SettingsContent(
    data: ClusterPlaceholders,
    onClose: () -> Unit,
    onBikeNameChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var page by remember { mutableStateOf(SettingsPage.Menu) }
    BackHandler(enabled = page != SettingsPage.Menu) {
        page = SettingsPage.Menu
    }

    when (page) {
        SettingsPage.Menu -> SettingsMenuPage(
            onClose = onClose,
            onThemesClick = { page = SettingsPage.Themes },
            onVehicleClick = { page = SettingsPage.Vehicle },
            modifier = modifier
        )
        SettingsPage.Themes -> ThemesPage(
            onClose = { page = SettingsPage.Menu },
            modifier = modifier
        )
        SettingsPage.Vehicle -> VehiclePage(
            data = data,
            onClose = { page = SettingsPage.Menu },
            onBikeNameChange = onBikeNameChange,
            modifier = modifier
        )
    }
}

@Composable
private fun SettingsMenuPage(
    onClose: () -> Unit,
    onThemesClick: () -> Unit,
    onVehicleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = clusterColors()
    val gap = 6.dp

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(gap)
    ) {
        SettingsHeaderRow(
            title = "SETTINGS",
            subtitle = "Themes & vehicle",
            onClose = onClose
        )
        Row(
            Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(gap)
        ) {
            SettingsNavTile(
                title = "THEMES",
                subtitle = "Coming soon",
                onClick = onThemesClick,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            SettingsNavTile(
                title = "VEHICLE",
                subtitle = "Bike name",
                onClick = onVehicleClick,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
private fun ThemesPage(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = clusterColors()
    val gap = 6.dp

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(gap)
    ) {
        SettingsHeaderRow(
            title = "THEMES",
            subtitle = "Accent packs",
            onClose = onClose
        )
        SettingsPanel(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                // Left empty intentionally — themes land later.
            }
        }
    }
}

@Composable
private fun VehiclePage(
    data: ClusterPlaceholders,
    onClose: () -> Unit,
    onBikeNameChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = clusterColors()
    val accent = rememberClusterAccent(data).primary
    val bikeName = data.bikeName
    val gap = 6.dp
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var draft by remember(bikeName) { mutableStateOf(bikeName) }

    fun commit() {
        val trimmed = draft.trim()
        if (trimmed.isNotEmpty() && trimmed != bikeName) {
            onBikeNameChange(trimmed)
        } else if (trimmed.isEmpty()) {
            draft = bikeName
        }
        focusManager.clearFocus()
        keyboard?.hide()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(gap)
    ) {
        SettingsHeaderRow(
            title = "VEHICLE",
            subtitle = "Identity",
            onClose = onClose
        )
        SettingsPanel(Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "BIKE NAME",
                    color = colors.muted,
                    fontFamily = ClusterUiFont,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp
                )
                val fieldShape = remember { ChamferShape(8.dp) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(fieldShape)
                        .background(colors.pill)
                        .border(1.15.dp, colors.pillBorder, fieldShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { focusRequester.requestFocus() }
                        )
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = draft,
                        onValueChange = { next ->
                            if (next.length <= 18) draft = next.uppercase()
                        },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = colors.text,
                            fontFamily = ClusterUiFont,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        ),
                        cursorBrush = SolidColor(accent),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { commit() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                }
                Text(
                    text = "Shown on the Control Centre navigator tile.",
                    color = colors.muted,
                    fontFamily = ClusterUiFont,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.weight(1f))
                val saveEnabled = draft.trim().isNotEmpty() && draft.trim() != bikeName
                val saveShape = remember { ChamferShape(8.dp) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(saveShape)
                        .background(
                            if (saveEnabled) accent.copy(alpha = 0.18f) else colors.pill
                        )
                        .border(
                            width = 1.15.dp,
                            color = if (saveEnabled) accent else colors.pillBorder,
                            shape = saveShape
                        )
                        .clickable(
                            enabled = saveEnabled,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { commit() }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "SAVE",
                        color = if (saveEnabled) accent else colors.muted,
                        fontFamily = ClusterUiFont,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsHeaderRow(
    title: String,
    subtitle: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(56.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsBackTile(
            onClick = onClose,
            modifier = Modifier
                .width(64.dp)
                .fillMaxHeight()
        )
        SettingsPanel(
            Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                Text(
                    text = title,
                    color = clusterColors().text,
                    fontFamily = ClusterUiFont,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.0.sp
                )
                Text(
                    text = subtitle,
                    color = clusterColors().muted,
                    fontFamily = ClusterUiFont,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun SettingsNavTile(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = clusterColors()
    SettingsPanel(modifier = modifier, onClick = onClick) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                color = colors.muted,
                fontFamily = ClusterUiFont,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Text(
                text = title,
                color = colors.text,
                fontFamily = ClusterUiFont,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.0.sp
            )
            Text(
                text = subtitle,
                color = colors.muted,
                fontFamily = ClusterUiFont,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SettingsPanel(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val colors = clusterColors()
    val shape = remember { ChamferShape(10.dp) }
    val clickMod = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .clip(shape)
            .background(colors.background)
            .border(1.15.dp, colors.pillBorder, shape)
            .then(clickMod)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        content()
    }
}

@Composable
private fun SettingsBackTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = clusterColors()
    SettingsPanel(modifier = modifier, onClick = onClick) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            SettingsBackChevron(
                color = colors.text,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun SettingsBackChevron(
    color: Color,
    modifier: Modifier = Modifier
) {
    val path = remember { PathParser().parsePathString(BackChevronPath).toPath() }
    Canvas(modifier) {
        val scale = size.minDimension / BackChevronViewBox
        withTransform({
            scale(scale, scale, pivot = Offset.Zero)
        }) {
            drawPath(path, color)
        }
    }
}

private const val BackChevronViewBox = 1200f
private const val BackChevronPath =
    "m210.632 599.989l178.735 178.735L810.633 1200l178.735-178.721l-421.267-421.29l421.267-421.266L810.645 0L389.378 421.267L210.655 599.989z"

@Preview(name = "Settings", showBackground = true, widthDp = 854, heightDp = 400)
@Composable
private fun SettingsPreview() {
    UltravioletteNavigatorTheme {
        SettingsContent(
            data = ClusterPlaceholders(bikeName = BikeNameStore.DEFAULT_BIKE_NAME),
            onClose = {},
            onBikeNameChange = {}
        )
    }
}
