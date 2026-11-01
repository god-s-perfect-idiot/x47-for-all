package com.ultraviolette.navigator.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ultraviolette.navigator.R

/** Dashford DEMO from fonts/dashford-font — single Regular cut; letters/digits only. */
val DashfordFont = FontFamily(
    Font(R.font.dashford_demo, FontWeight.Light),
    Font(R.font.dashford_demo, FontWeight.Normal),
    Font(R.font.dashford_demo, FontWeight.Medium),
    Font(R.font.dashford_demo, FontWeight.SemiBold),
    Font(R.font.dashford_demo, FontWeight.Bold)
)

/** Spaceport One — KTM theme wordmark (regular + italic). */
val SpaceportOneFont = FontFamily(
    Font(R.font.spaceport_one, FontWeight.Normal, FontStyle.Normal),
    Font(R.font.spaceport_one, FontWeight.Bold, FontStyle.Normal),
    Font(R.font.spaceport_one, FontWeight.Black, FontStyle.Normal),
    Font(R.font.spaceport_one_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.spaceport_one_italic, FontWeight.Bold, FontStyle.Italic),
    Font(R.font.spaceport_one_italic, FontWeight.Black, FontStyle.Italic)
)

/** BMW Motorrad Type Global — BMW theme wordmark. */
val BmwMotorradFont = FontFamily(
    Font(R.font.bmw_motorrad_type, FontWeight.Normal),
    Font(R.font.bmw_motorrad_type, FontWeight.Medium),
    Font(R.font.bmw_motorrad_type, FontWeight.SemiBold),
    Font(R.font.bmw_motorrad_type, FontWeight.Bold)
)

/** Arial Bold / Black — Aprilia theme wordmark. */
val ArialBrandFont = FontFamily(
    Font(R.font.arial_bold, FontWeight.Normal),
    Font(R.font.arial_bold, FontWeight.Medium),
    Font(R.font.arial_bold, FontWeight.SemiBold),
    Font(R.font.arial_bold, FontWeight.Bold),
    Font(R.font.arial_black, FontWeight.Black)
)

val ClusterUiFont = DashfordFont
val ClusterDisplayFont = DashfordFont

val ClusterTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = DashfordFont,
        fontWeight = FontWeight.Bold,
        fontSize = 84.sp,
        letterSpacing = 2.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = DashfordFont,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = DashfordFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = DashfordFont,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    ),
    labelSmall = TextStyle(
        fontFamily = DashfordFont,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.4.sp
    )
)
