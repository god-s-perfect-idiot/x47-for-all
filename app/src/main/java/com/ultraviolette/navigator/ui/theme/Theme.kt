package com.ultraviolette.navigator.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ClusterColorScheme = lightColorScheme(
    primary = ClusterBlue,
    onPrimary = ClusterPill,
    background = ClusterBackground,
    onBackground = ClusterText,
    surface = ClusterPill,
    onSurface = ClusterText
)

@Composable
fun UltravioletteNavigatorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ClusterColorScheme,
        typography = ClusterTypography,
        content = content
    )
}
