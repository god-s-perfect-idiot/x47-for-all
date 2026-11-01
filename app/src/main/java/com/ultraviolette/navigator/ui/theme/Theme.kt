package com.ultraviolette.navigator.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.ultraviolette.navigator.model.BrandTheme

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

private val KtmMaterialScheme = darkColorScheme(
    primary = KtmOrange,
    onPrimary = Color(0xFF000000),
    background = KtmClusterColors.background,
    onBackground = KtmClusterColors.text,
    surface = KtmClusterColors.pill,
    onSurface = KtmClusterColors.text
)

@Composable
fun UltravioletteNavigatorTheme(
    darkTheme: Boolean = false,
    brandTheme: String = BrandTheme.DEFAULT,
    content: @Composable () -> Unit
) {
    val ktm = isKtmTheme(brandTheme)
    val colors = when {
        ktm -> KtmClusterColors
        darkTheme -> DarkClusterColors
        else -> LightClusterColors
    }
    CompositionLocalProvider(LocalClusterColors provides colors) {
        MaterialTheme(
            colorScheme = when {
                ktm -> KtmMaterialScheme
                darkTheme -> DarkMaterialScheme
                else -> LightMaterialScheme
            },
            typography = ClusterTypography,
            content = content
        )
    }
}
