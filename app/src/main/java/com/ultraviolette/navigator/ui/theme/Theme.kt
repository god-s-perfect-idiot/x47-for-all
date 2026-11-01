package com.ultraviolette.navigator.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val LightMaterialScheme = lightColorScheme(
    primary = ClusterBlue,
    onPrimary = LightClusterColors.pill,
    background = LightClusterColors.background,
    onBackground = LightClusterColors.text,
    surface = LightClusterColors.pill,
    onSurface = LightClusterColors.text
)

private val DarkMaterialScheme = darkColorScheme(
    primary = ClusterBlue,
    onPrimary = DarkClusterColors.pill,
    background = DarkClusterColors.background,
    onBackground = DarkClusterColors.text,
    surface = DarkClusterColors.pill,
    onSurface = DarkClusterColors.text
)

@Composable
fun UltravioletteNavigatorTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkClusterColors else LightClusterColors
    CompositionLocalProvider(LocalClusterColors provides colors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkMaterialScheme else LightMaterialScheme,
            typography = ClusterTypography,
            content = content
        )
    }
}
