package com.claudetest.matchtiles.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/*
 * A daytime, storybook palette: a sunny sky behind white sticker cards, candy accents for
 * anything the player earns or loses, and one deep navy for every piece of text so even the
 * small labels clear 4.5:1 contrast on white. Buttons put navy on the bright fills for the
 * same reason - white on pastel would not be readable.
 */
val SkyTop = Color(0xFFBCE4FF)
val SkyMid = Color(0xFFDFF2FF)
val Cream = Color(0xFFFFF4DC)
val CardWhite = Color(0xFFFFFFFF)
val CardCream = Color(0xFFFFFAEC)

val Sunny = Color(0xFFFFC53D)
val Coral = Color(0xFFFF6B6B)
val Mint = Color(0xFF3DD9A8)
val Ocean = Color(0xFF3AA6F0)
val Grape = Color(0xFF9B5DE5)
val Leaf = Color(0xFF7BD389)

val Navy = Color(0xFF33406E)
val NavySoft = Color(0xFF5E6E9E)
val NavyFaint = Color(0xFFDCE5F6)

private val TileMatchColors = lightColorScheme(
    primary = Mint,
    onPrimary = Navy,
    secondary = Grape,
    onSecondary = Color.White,
    tertiary = Sunny,
    onTertiary = Navy,
    background = SkyMid,
    onBackground = Navy,
    surface = CardWhite,
    onSurface = Navy,
    surfaceVariant = CardCream,
    onSurfaceVariant = NavySoft,
    error = Coral,
    onError = Navy,
    outline = NavyFaint,
)

/** Fat, rounded weights: a kid's game should never whisper. */
private val TileMatchTypography = Typography(
    displaySmall = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
    titleLarge = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.ExtraBold),
    titleMedium = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold),
    bodyMedium = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium),
    labelLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp),
    labelSmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp),
)

@Composable
fun TileMatchTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TileMatchColors,
        typography = TileMatchTypography,
        content = content,
    )
}
