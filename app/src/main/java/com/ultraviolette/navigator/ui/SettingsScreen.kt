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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultraviolette.navigator.model.BikeNameStore
import com.ultraviolette.navigator.model.BrandTheme
import com.ultraviolette.navigator.model.ClusterPlaceholders
import com.ultraviolette.navigator.ui.components.BladeShape
import com.ultraviolette.navigator.ui.components.BladeSharpCorner
import com.ultraviolette.navigator.ui.components.ChamferShape
import com.ultraviolette.navigator.ui.components.clusterTileEnter
import com.ultraviolette.navigator.ui.components.clusterTileEnterLinearDelayMs
import com.ultraviolette.navigator.ui.ktm.KtmDetailColumn
import com.ultraviolette.navigator.ui.ktm.KtmDetailCta
import com.ultraviolette.navigator.ui.ktm.KtmDetailStat
import com.ultraviolette.navigator.ui.ktm.KtmDetailTitle
import com.ultraviolette.navigator.ui.ktm.KtmSubmenuShell
import com.ultraviolette.navigator.ui.ktm.ktmSpeedDigits
import com.ultraviolette.navigator.ui.theme.ArialBrandFont
import com.ultraviolette.navigator.ui.theme.BrandPackColors
import com.ultraviolette.navigator.ui.theme.BmwMotorradFont
import com.ultraviolette.navigator.ui.theme.BmwStripeCyan
import com.ultraviolette.navigator.ui.theme.BmwStripeNavy
import com.ultraviolette.navigator.ui.theme.BmwStripeRed
import com.ultraviolette.navigator.ui.theme.ClusterUiFont
import com.ultraviolette.navigator.ui.theme.DashfordFont
import com.ultraviolette.navigator.ui.theme.KtmMuted
import com.ultraviolette.navigator.ui.theme.KtmOrange
import com.ultraviolette.navigator.ui.theme.KtmPanel
import com.ultraviolette.navigator.ui.theme.SpaceportOneFont
import com.ultraviolette.navigator.ui.theme.UltravioletteNavigatorTheme
import com.ultraviolette.navigator.ui.theme.brandPackColors
import com.ultraviolette.navigator.ui.theme.clusterColors
import com.ultraviolette.navigator.ui.theme.isKtmTheme
import com.ultraviolette.navigator.ui.theme.rememberClusterAccent
import com.ultraviolette.navigator.ui.theme.rememberKtmChrome
import com.ultraviolette.navigator.ui.theme.rememberKtmUiFont
import androidx.compose.foundation.shape.RoundedCornerShape

private enum class SettingsPage { Menu, Themes, Vehicle }

/**
 * Settings hub opened from the Control Centre SETTINGS tile.
 * Hosts Themes (brand packs) and Vehicle (bike name editor).
 */
@Composable
fun SettingsContent(
    data: ClusterPlaceholders,
    onClose: () -> Unit,
    onBikeNameChange: (String) -> Unit,
    onBrandThemeChange: (String) -> Unit = {},
    onThemesActiveChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var page by remember { mutableStateOf(SettingsPage.Menu) }
    BackHandler(enabled = page != SettingsPage.Menu) {
        page = SettingsPage.Menu
    }
    LaunchedEffect(page) {
        onThemesActiveChange(page == SettingsPage.Themes)
    }
    DisposableEffect(Unit) {
        onDispose { onThemesActiveChange(false) }
    }

    AnimatedContent(
        targetState = page,
        transitionSpec = {
            val spec = tween<Float>(280, easing = FastOutSlowInEasing)
            val slide = tween<IntOffset>(280, easing = FastOutSlowInEasing)
            val opening = when {
                initialState == SettingsPage.Menu -> true
                targetState == SettingsPage.Menu -> false
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
        label = "settingsPage",
        modifier = modifier
    ) { current ->
        when (current) {
            SettingsPage.Menu -> SettingsMenuPage(
                data = data,
                onClose = onClose,
                onThemesClick = { page = SettingsPage.Themes },
                onVehicleClick = { page = SettingsPage.Vehicle },
                modifier = Modifier.fillMaxSize()
            )
            // Themes keeps the brand-tile face — not KTM chrome.
            SettingsPage.Themes -> ThemesPage(
                selectedTheme = data.brandTheme,
                onClose = { page = SettingsPage.Menu },
                onThemeSelect = onBrandThemeChange,
                modifier = Modifier.fillMaxSize()
            )
            SettingsPage.Vehicle -> VehiclePage(
                data = data,
                onClose = { page = SettingsPage.Menu },
                onBikeNameChange = onBikeNameChange,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun SettingsMenuPage(
    data: ClusterPlaceholders,
    onClose: () -> Unit,
    onThemesClick: () -> Unit,
    onVehicleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val brandTheme = data.brandTheme
    val ktm = isKtmTheme(brandTheme)
    val colors = clusterColors()
    val gap = 6.dp
    val themeLabel = BrandTheme.meta(brandTheme).label
    val themeMeta = BrandTheme.meta(brandTheme)

    if (ktm) {
        val chrome = rememberKtmChrome(data)
        val uiFont = rememberKtmUiFont()
        var selectedIndex by remember { mutableIntStateOf(0) }
        val titles = listOf("Themes", "Vehicle")

        KtmSubmenuShell(
            menuTitles = titles,
            selectedIndex = selectedIndex,
            onSelect = { selectedIndex = it },
            onBack = onClose,
            speedText = ktmSpeedDigits(data.speedKmh),
            gearText = chrome.gear,
            modeLabel = chrome.modeLabel,
            modifier = modifier
        ) {
            when (selectedIndex) {
                0 -> KtmDetailColumn {
                    KtmDetailTitle("THEMES")
                    Text(
                        text = themeMeta.label,
                        color = Color.White,
                        fontFamily = uiFont,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = themeMeta.tagline,
                        color = KtmOrange,
                        fontFamily = uiFont,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    KtmDetailStat("ACTIVE", themeLabel)
                    Spacer(Modifier.height(12.dp))
                    KtmDetailCta("Open theme picker →", onClick = onThemesClick)
                }
                else -> KtmDetailColumn {
                    KtmDetailTitle("VEHICLE")
                    Text(
                        text = data.bikeName,
                        color = Color.White,
                        fontFamily = uiFont,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Bike display name",
                        color = KtmMuted,
                        fontFamily = uiFont,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))
                    KtmDetailCta("Edit bike name →", onClick = onVehicleClick)
                }
            }
        }
        return
    }

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
            onClose = onClose,
            enterOrder = 0
        )
        Row(
            Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(gap)
        ) {
            SettingsNavTile(
                title = "THEMES",
                subtitle = themeLabel,
                onClick = onThemesClick,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clusterTileEnter(clusterTileEnterLinearDelayMs(2))
            )
            SettingsNavTile(
                title = "VEHICLE",
                subtitle = "Bike name",
                onClick = onVehicleClick,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clusterTileEnter(clusterTileEnterLinearDelayMs(3))
            )
        }
    }
}

@Composable
private fun ThemesPage(
    selectedTheme: String,
    onClose: () -> Unit,
    onThemeSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = clusterColors()
    val gap = 6.dp
    val selectedLabel = BrandTheme.meta(selectedTheme).label
    val catalog = BrandTheme.catalog
    // Sharp corner faces the grid center on each quadrant.
    val sharpCorners = listOf(
        BladeSharpCorner.BottomEnd,
        BladeSharpCorner.BottomStart,
        BladeSharpCorner.TopEnd,
        BladeSharpCorner.TopStart
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(gap)
    ) {
        SettingsHeaderRow(
            title = "THEMES",
            subtitle = selectedLabel,
            onClose = onClose,
            enterOrder = 0
        )
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(gap)
        ) {
            repeat(2) { row ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(gap)
                ) {
                    repeat(2) { col ->
                        val index = row * 2 + col
                        val meta = catalog[index]
                        BrandThemeTile(
                            meta = meta,
                            pack = brandPackColors(meta.id),
                            sharp = sharpCorners[index],
                            selected = meta.id.equals(selectedTheme, ignoreCase = true),
                            onClick = { onThemeSelect(meta.id) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clusterTileEnter(clusterTileEnterLinearDelayMs(2 + index))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BrandThemeTile(
    meta: BrandTheme.Meta,
    pack: BrandPackColors,
    sharp: BladeSharpCorner,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = remember(sharp) { BladeShape(cut = 22.dp, sharp = sharp) }
    val ring = when {
        selected && meta.id == BrandTheme.UV -> Color(0xFFFFFFFF)
        selected && meta.id == BrandTheme.KTM -> Color(0xFF000000)
        selected && meta.id == BrandTheme.BMW -> BmwStripeNavy
        selected -> Color(0xFFFFFFFF)
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(pack.tileBackground)
            .then(
                if (selected) Modifier.border(2.2.dp, ring, shape)
                else Modifier
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        when (meta.id) {
            BrandTheme.UV -> UvThemeTileContent(pack = pack, selected = selected)
            BrandTheme.KTM -> KtmThemeTileContent(pack = pack, selected = selected)
            BrandTheme.BMW -> BmwThemeTileContent(pack = pack, selected = selected)
            else -> ApriliaThemeTileContent(pack = pack, selected = selected)
        }
    }
}

@Composable
private fun UvThemeTileContent(pack: BrandPackColors, selected: Boolean) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = if (selected) "ACTIVE" else "THEME",
            color = pack.tileMuted,
            fontFamily = DashfordFont,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "UV",
                color = pack.tileForeground,
                fontFamily = DashfordFont,
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.0.sp,
                maxLines = 1
            )
            Text(
                text = "ULTRAVIOLETTE",
                color = pack.tileMuted,
                fontFamily = DashfordFont,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.4.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun KtmThemeTileContent(pack: BrandPackColors, selected: Boolean) {
    val markStyle = TextStyle(
        color = pack.tileForeground,
        fontFamily = SpaceportOneFont,
        fontSize = 56.sp,
        fontWeight = FontWeight.Black,
        fontStyle = FontStyle.Italic,
        letterSpacing = 1.2.sp
    )
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = if (selected) "ACTIVE" else "THEME",
            color = pack.tileMuted,
            fontFamily = SpaceportOneFont,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.6.sp
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            // Stroke underlay fattens Spaceport One for the KTM wordmark.
            Box {
                Text(
                    text = "KTM",
                    style = markStyle.copy(
                        drawStyle = Stroke(
                            width = 5.5f,
                            miter = 10f,
                            join = StrokeJoin.Round,
                            cap = StrokeCap.Round
                        )
                    ),
                    maxLines = 1
                )
                Text(text = "KTM", style = markStyle, maxLines = 1)
            }
            Text(
                text = "READY TO RACE",
                color = pack.tileMuted,
                fontFamily = SpaceportOneFont,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.8.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun BmwThemeTileContent(pack: BrandPackColors, selected: Boolean) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = if (selected) "ACTIVE" else "THEME",
                color = pack.tileMuted,
                fontFamily = BmwMotorradFont,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp
            )
            BmwMStripes(Modifier.height(36.dp).width(28.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            // B / M / W each take one Motorrad M stripe color.
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "B",
                    color = BmwStripeCyan,
                    fontFamily = BmwMotorradFont,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp,
                    maxLines = 1
                )
                Text(
                    text = "M",
                    color = BmwStripeNavy,
                    fontFamily = BmwMotorradFont,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp,
                    maxLines = 1
                )
                Text(
                    text = "W",
                    color = BmwStripeRed,
                    fontFamily = BmwMotorradFont,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp,
                    maxLines = 1
                )
            }
            Text(
                text = "MOTORRAD",
                color = pack.tileForeground,
                fontFamily = BmwMotorradFont,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.6.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ApriliaThemeTileContent(pack: BrandPackColors, selected: Boolean) {
    // Pin RACING to the bottom so the oversized wordmark can't clip it.
    Box(Modifier.fillMaxSize()) {
        Text(
            text = if (selected) "ACTIVE" else "THEME",
            color = pack.tileMuted,
            fontFamily = ArialBrandFont,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp,
            modifier = Modifier.align(Alignment.TopStart)
        )
        Column(
            Modifier.align(Alignment.BottomStart),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "aprilia",
                color = pack.tileForeground,
                fontFamily = ArialBrandFont,
                fontSize = 58.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1.0).sp,
                maxLines = 1
            )
            Text(
                text = "RACING",
                color = pack.tileMuted,
                fontFamily = ArialBrandFont,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.2.sp,
                maxLines = 1
            )
        }
    }
}

/** BMW Motorrad M-style tricolor stripes. */
@Composable
private fun BmwMStripes(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stripeW = size.width / 2.7f
        val skew = size.width * 0.34f
        val gap = size.width * 0.05f
        val colors = listOf(BmwStripeCyan, BmwStripeNavy, BmwStripeRed)
        colors.forEachIndexed { i, color ->
            val x0 = i * (stripeW + gap)
            drawPath(
                Path().apply {
                    moveTo(x0 + skew, 0f)
                    lineTo(x0 + skew + stripeW, 0f)
                    lineTo(x0 + stripeW, size.height)
                    lineTo(x0, size.height)
                    close()
                },
                color = color
            )
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
    val ktm = isKtmTheme(data.brandTheme)
    val colors = clusterColors()
    val clusterAccent = rememberClusterAccent(data).primary
    val accent = if (ktm) KtmOrange else clusterAccent
    val ktmFont = rememberKtmUiFont()
    val uiFont = if (ktm) ktmFont else ClusterUiFont
    val chamferField = remember { ChamferShape(8.dp) }
    val chamferSave = remember { ChamferShape(8.dp) }
    val roundField = RoundedCornerShape(6.dp)
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

    val body: @Composable () -> Unit = {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "BIKE NAME",
                color = if (ktm) KtmMuted else colors.muted,
                fontFamily = uiFont,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp
            )
            val fieldShape = if (ktm) roundField else chamferField
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(fieldShape)
                    .background(if (ktm) KtmPanel else colors.pill)
                    .border(
                        1.15.dp,
                        if (ktm) KtmOrange.copy(alpha = 0.45f) else colors.pillBorder,
                        fieldShape
                    )
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
                        fontFamily = uiFont,
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
                color = if (ktm) KtmMuted else colors.muted,
                fontFamily = uiFont,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.weight(1f))
            val saveEnabled = draft.trim().isNotEmpty() && draft.trim() != bikeName
            val saveShape = if (ktm) roundField else chamferSave
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(saveShape)
                    .background(
                        if (saveEnabled) accent.copy(alpha = if (ktm) 0.28f else 0.18f)
                        else if (ktm) KtmPanel else colors.pill
                    )
                    .border(
                        width = 1.15.dp,
                        color = if (saveEnabled) accent else if (ktm) KtmMuted.copy(alpha = 0.4f) else colors.pillBorder,
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
                    color = if (saveEnabled) accent else if (ktm) KtmMuted else colors.muted,
                    fontFamily = uiFont,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
            }
        }
    }

    if (ktm) {
        val chrome = rememberKtmChrome(data)
        KtmSubmenuShell(
            menuTitles = listOf("Bike name"),
            selectedIndex = 0,
            onSelect = {},
            onBack = onClose,
            speedText = ktmSpeedDigits(data.speedKmh),
            gearText = chrome.gear,
            modeLabel = chrome.modeLabel,
            modifier = modifier
        ) {
            body()
        }
        return
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
            onClose = onClose,
            enterOrder = 0
        )
        SettingsPanel(
            Modifier
                .fillMaxSize()
                .clusterTileEnter(clusterTileEnterLinearDelayMs(2))
        ) {
            body()
        }
    }
}

@Composable
private fun SettingsHeaderRow(
    title: String,
    subtitle: String,
    onClose: () -> Unit,
    enterOrder: Int = 0,
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
                .clusterTileEnter(clusterTileEnterLinearDelayMs(enterOrder))
        )
        SettingsPanel(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .clusterTileEnter(clusterTileEnterLinearDelayMs(enterOrder + 1))
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
            onBikeNameChange = {},
            onBrandThemeChange = {}
        )
    }
}
