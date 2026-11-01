package com.ultraviolette.navigator.ui.ktm

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultraviolette.navigator.model.BrandTheme
import com.ultraviolette.navigator.model.ClusterPlaceholders
import com.ultraviolette.navigator.model.RideMode
import com.ultraviolette.navigator.ui.theme.KtmMuted
import com.ultraviolette.navigator.ui.theme.KtmOrange
import com.ultraviolette.navigator.ui.theme.KtmPanel
import com.ultraviolette.navigator.ui.theme.TopBarSoCFill
import com.ultraviolette.navigator.ui.theme.ktmRangeKm
import com.ultraviolette.navigator.ui.theme.ktmWallClockDigits
import com.ultraviolette.navigator.ui.theme.rememberKtmChrome
import com.ultraviolette.navigator.ui.theme.rememberKtmUiFont
import com.ultraviolette.navigator.weather.WeatherCondition

private enum class KtmMenuItem(val title: String) {
    About("About"),
    Battery("Battery"),
    RideMode("Ride Mode"),
    Connectivity("Connectivity"),
    Scores("Scores"),
    Trips("Trips"),
    Maps("Maps"),
    Settings("Settings")
}

/**
 * KTM control centre — scrollable orange-pill menu + right detail pane
 * + bottom back / speed / gear strip.
 */
@Composable
fun KtmControlCenterContent(
    data: ClusterPlaceholders,
    onClose: () -> Unit,
    onRideModeClick: () -> Unit,
    onRideModeSelect: (String) -> Unit = {},
    onTripsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val chrome = rememberKtmChrome(data)
    val context = LocalContext.current
    var selectedIndex by remember { mutableIntStateOf(0) }
    val items = KtmMenuItem.entries
    val selected = items[selectedIndex.coerceIn(0, items.lastIndex)]

    KtmSubmenuShell(
        menuTitles = items.map { it.title },
        selectedIndex = selectedIndex,
        onSelect = { selectedIndex = it },
        onBack = onClose,
        speedText = ktmSpeedDigits(data.speedKmh),
        gearText = chrome.gear,
        modeLabel = chrome.modeLabel,
        modifier = modifier
    ) {
        KtmDetailPane(
            data = data,
            selected = selected,
            chromeOrange = chrome.orange,
            onRideModeSelect = onRideModeSelect,
            onTripsClick = onTripsClick,
            onSettingsClick = onSettingsClick,
            onMapsClick = { openGoogleMaps(context) }
        )
    }
}

@Composable
private fun KtmDetailPane(
    data: ClusterPlaceholders,
    selected: KtmMenuItem,
    chromeOrange: Color,
    onRideModeSelect: (String) -> Unit,
    onTripsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onMapsClick: () -> Unit
) {
    val uiFont = rememberKtmUiFont()
    val chrome = rememberKtmChrome(data)

    when (selected) {
        KtmMenuItem.About -> {
            KtmDetailColumn {
                KtmDetailTitle("ABOUT")
                Text(
                    text = data.bikeName,
                    color = Color.White,
                    fontFamily = uiFont,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "NAVIGATOR",
                    color = KtmMuted,
                    fontFamily = uiFont,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.6.sp
                )
                Spacer(Modifier.height(4.dp))
                KtmDetailStat("THEME", BrandTheme.meta(data.brandTheme).label)
                KtmDetailStat("MODE", chrome.modeLabel)
                KtmDetailStat("ODO", data.odometerKm)
                KtmDetailStat(
                    "CLOCK",
                    remember(data.timeText) { ktmWallClockDigits(data.timeText) }
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = BrandTheme.meta(data.brandTheme).tagline,
                    color = chromeOrange,
                    fontFamily = uiFont,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.4.sp
                )
            }
        }

        KtmMenuItem.Battery -> {
            KtmDetailColumn {
                KtmDetailTitle("BATTERY")
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${data.batteryPercent}",
                        color = Color.White,
                        fontFamily = uiFont,
                        fontSize = 68.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "%",
                        color = KtmMuted,
                        fontFamily = uiFont,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Spacer(Modifier.weight(1f))
                    if (data.isCharging) {
                        Text(
                            text = "CHARGING",
                            color = TopBarSoCFill,
                            fontFamily = uiFont,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.0.sp,
                            modifier = Modifier.padding(bottom = 14.dp)
                        )
                    }
                }
                KtmBatteryBar(percent = data.batteryPercent)
                KtmDetailStat("RANGE PROXY", ktmRangeKm(data.batteryPercent))
                KtmDetailStat("PHONE TEMP", if (data.phoneTempHigh) "HOT" else "OK")
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Phone SoC · not bike pack",
                    color = KtmMuted,
                    fontFamily = uiFont,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        KtmMenuItem.RideMode -> {
            val rideScroll = rememberScrollState()
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rideScroll)
            ) {
                KtmDetailTitle("RIDE MODE")
                Spacer(Modifier.height(10.dp))
                KtmTachPreview(
                    rpm = chrome.rpm,
                    accent = chromeOrange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(148.dp)
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(RideMode.GLIDE, RideMode.COMBAT, RideMode.BALLISTIC).forEach { mode ->
                        val label = when (mode) {
                            RideMode.GLIDE -> "STREET"
                            RideMode.BALLISTIC -> "TRACK"
                            else -> "SUPERMOTO"
                        }
                        val on = data.rideMode.equals(mode, ignoreCase = true)
                        Box(
                            Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (on) chromeOrange else KtmPanel)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onRideModeSelect(mode) }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = Color.White,
                                fontFamily = uiFont,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp
                            )
                        }
                    }
                }
            }
        }

        KtmMenuItem.Connectivity -> {
            KtmDetailColumn {
                KtmDetailTitle("CONNECTIVITY")
                KtmDetailStat("GPS", if (data.gpsAvailable) "LOCK" else "——")
                KtmDetailStat("WIFI", if (data.wifiOn) "ON · ${data.wifiBars}/3" else "OFF")
                KtmDetailStat("BT", if (data.bluetoothOn) "LINKED" else "OFF")
                KtmDetailStat("SIGNAL", "${data.signalBars}/5")
                KtmDetailStat("ALT", data.altitudeText)
                KtmDetailStat("LEAN", "${data.leanAngleDeg}°")
                KtmDetailStat("WX", weatherShort(data.weatherCondition, data.ambientTempC))
                KtmDetailStat("WIND", data.windSpeedText)
            }
        }

        KtmMenuItem.Scores -> {
            KtmDetailColumn {
                KtmDetailTitle("SCORES")
                KtmScoreBlock(
                    label = data.efficiencyLabel,
                    bars = data.efficiencyBars,
                    active = data.efficiencyActive,
                    accent = chromeOrange
                )
                KtmScoreBlock(
                    label = data.disciplineLabel,
                    bars = data.disciplineBars,
                    active = data.disciplineActive,
                    accent = chromeOrange
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Live while moving · parked shows --",
                    color = KtmMuted,
                    fontFamily = uiFont,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        KtmMenuItem.Trips -> {
            KtmDetailColumn {
                KtmDetailTitle("TRIPS")
                KtmDetailStat("THIS RIDE", data.tripKm)
                KtmDetailStat("AVG SPEED", data.avgSpeedKmh)
                KtmDetailStat("TIME", data.tripTimeText)
                KtmDetailStat("ODO", data.odometerKm)
                Spacer(Modifier.height(12.dp))
                KtmDetailCta("Open trip history →", onClick = onTripsClick)
            }
        }

        KtmMenuItem.Maps -> {
            val mapsScroll = rememberScrollState()
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(mapsScroll),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "GOOGLE MAPS",
                    color = Color.White,
                    fontFamily = uiFont,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Launch navigation",
                    color = KtmMuted,
                    fontFamily = uiFont,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))
                KtmDetailCta("Open Maps →", onClick = onMapsClick)
            }
        }

        KtmMenuItem.Settings -> {
            KtmDetailColumn {
                KtmDetailTitle("SETTINGS")
                Text(
                    text = data.bikeName,
                    color = Color.White,
                    fontFamily = uiFont,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Themes · bike name",
                    color = KtmMuted,
                    fontFamily = uiFont,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                KtmDetailCta("Open settings →", onClick = onSettingsClick)
            }
        }
    }
}

@Composable
private fun KtmBatteryBar(percent: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(16.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(KtmPanel)
    ) {
        Box(
            Modifier
                .fillMaxWidth((percent.coerceIn(0, 100) / 100f))
                .height(16.dp)
                .background(TopBarSoCFill)
        )
    }
}

@Composable
private fun KtmScoreBlock(
    label: String,
    bars: Int,
    active: Boolean,
    accent: Color
) {
    val uiFont = rememberKtmUiFont()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = label,
                color = KtmMuted,
                fontFamily = uiFont,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.0.sp
            )
            Text(
                text = if (active) bars.toString() else "--",
                color = Color.White,
                fontFamily = uiFont,
                fontSize = 34.sp,
                fontWeight = FontWeight.Black
            )
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            val lit = bars.coerceIn(0, 10)
            repeat(10) { index ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(8.dp)
                        .background(if (active && index < lit) accent else KtmPanel)
                )
            }
        }
    }
}

internal fun ktmSpeedDigits(speedKmh: String): String =
    speedKmh.filter { it.isDigit() }.ifEmpty { "0" }.trimStart('0').ifEmpty { "0" }

private fun weatherShort(condition: WeatherCondition, tempC: String): String {
    val tag = when (condition) {
        WeatherCondition.CLEAR -> "CLR"
        WeatherCondition.PARTLY_CLOUDY -> "PCL"
        WeatherCondition.CLOUDY -> "CLD"
        WeatherCondition.FOG -> "FOG"
        WeatherCondition.DRIZZLE -> "DRZ"
        WeatherCondition.RAIN -> "RAIN"
        WeatherCondition.SNOW -> "SNOW"
        WeatherCondition.THUNDER -> "THDR"
    }
    return if (tempC == "--") tag else "$tempC° $tag"
}

private fun openGoogleMaps(context: Context) {
    val mapsIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0")).apply {
        setPackage("com.google.android.apps.maps")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(mapsIntent)
    } catch (_: ActivityNotFoundException) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
}
