package com.ultraviolette.navigator.ui.theme

import androidx.compose.ui.graphics.Color
import com.ultraviolette.navigator.model.BrandTheme
import com.ultraviolette.navigator.model.RideMode

/**
 * Brand pack visuals + ride-mode accent remaps.
 * [tileBackground] / [tileForeground] drive the Themes picker full-bleed tiles.
 */
data class BrandPackColors(
    val primary: Color,
    val secondary: Color,
    val wash: Color,
    val onPrimary: Color,
    val tileBackground: Color,
    val tileForeground: Color,
    val tileMuted: Color,
    val glide: Color,
    val combat: Color,
    val ballistic: Color
)

val UvBrandColors = BrandPackColors(
    primary = Color(0xFFFFFFFF),
    secondary = Color(0xFF6A2CFF),
    wash = Color(0xFF1A1A1A),
    onPrimary = Color(0xFF000000),
    tileBackground = Color(0xFF000000),
    tileForeground = Color(0xFFFFFFFF),
    tileMuted = Color(0xFFB0B0B0),
    glide = ClusterGlide,
    combat = ClusterBlue,
    ballistic = ClusterBallistic
)

val KtmBrandColors = BrandPackColors(
    primary = Color(0xFFFF6600),
    secondary = Color(0xFF000000),
    wash = Color(0xFFFFE8D6),
    onPrimary = Color(0xFF000000),
    tileBackground = Color(0xFFFF6600),
    tileForeground = Color(0xFF000000),
    tileMuted = Color(0xFF3A1A00),
    glide = Color(0xFFB85A00),
    combat = Color(0xFFFF6600),
    ballistic = Color(0xFFE63E00)
)

val BmwBrandColors = BrandPackColors(
    primary = Color(0xFF00ADEE),
    secondary = Color(0xFF00188F),
    wash = Color(0xFFE8F7FC),
    onPrimary = Color(0xFFFFFFFF),
    tileBackground = Color(0xFFF4F6F8),
    tileForeground = Color(0xFF111111),
    tileMuted = Color(0xFF5A636C),
    glide = Color(0xFF4A90A4),
    combat = Color(0xFF00ADEE),
    ballistic = Color(0xFFE30613)
)

/** BMW Motorrad M-stripe triad drawn on the BMW theme tile. */
val BmwStripeCyan = Color(0xFF00ADEE)
val BmwStripeNavy = Color(0xFF00188F)
val BmwStripeRed = Color(0xFFE30613)

val ApriliaBrandColors = BrandPackColors(
    primary = Color(0xFFE30613),
    secondary = Color(0xFFFFFFFF),
    wash = Color(0xFFFFE5E7),
    onPrimary = Color(0xFFFFFFFF),
    tileBackground = Color(0xFFE30613),
    tileForeground = Color(0xFFFFFFFF),
    tileMuted = Color(0xFFFFC9CD),
    glide = Color(0xFF2D6B4F),
    combat = Color(0xFFE30613),
    ballistic = Color(0xFF8B0000)
)

fun brandPackColors(themeId: String): BrandPackColors = when (
    BrandTheme.canonicalize(themeId) ?: BrandTheme.DEFAULT
) {
    BrandTheme.KTM -> KtmBrandColors
    BrandTheme.BMW -> BmwBrandColors
    BrandTheme.APRILIA -> ApriliaBrandColors
    else -> UvBrandColors
}

fun brandRideAccent(themeId: String, rideMode: String): Color {
    val pack = brandPackColors(themeId)
    return when (RideMode.canonicalize(rideMode) ?: RideMode.COMBAT) {
        RideMode.GLIDE -> pack.glide
        RideMode.BALLISTIC -> pack.ballistic
        else -> pack.combat
    }
}
