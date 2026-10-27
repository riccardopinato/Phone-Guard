package com.riccardopinato.phoneguard.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val OmniAccent = Color(0xFF18C98B)
val OmniAccentStrong = Color(0xFF00B878)
val OmniHero = Color(0xFF17212B)
val OmniHeroSoft = Color(0xFF22303C)
val OmniHeroText = Color(0xFFF5F8FA)
val OmniHeroMuted = Color(0xFF9EABB5)
val OmniWarning = Color(0xFFFFB84D)
val OmniDanger = Color(0xFFFF5D68)

private val LightScheme = lightColorScheme(
    primary = OmniAccentStrong,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDF8EE),
    onPrimaryContainer = Color(0xFF073D2D),
    secondary = Color(0xFF263640),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8EEF2),
    onSecondaryContainer = Color(0xFF1A2730),
    tertiary = Color(0xFF5578F6),
    background = Color(0xFFF4F6F8),
    onBackground = Color(0xFF151A1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF151A1E),
    surfaceVariant = Color(0xFFEEF2F4),
    onSurfaceVariant = Color(0xFF69747D),
    outline = Color(0xFFD7DEE3),
    error = OmniDanger,
    errorContainer = Color(0xFFFFE6E8),
    onErrorContainer = Color(0xFF6D1018)
)

private val DarkScheme = darkColorScheme(
    primary = OmniAccent,
    onPrimary = Color(0xFF002116),
    primaryContainer = Color(0xFF0C4936),
    onPrimaryContainer = Color(0xFFBFF7DF),
    secondary = Color(0xFFD9E3E9),
    onSecondary = Color(0xFF17242C),
    secondaryContainer = Color(0xFF26343E),
    onSecondaryContainer = Color(0xFFE7EEF2),
    tertiary = Color(0xFF9AAFFF),
    background = Color(0xFF0C1116),
    onBackground = Color(0xFFF2F5F7),
    surface = Color(0xFF131A20),
    onSurface = Color(0xFFF2F5F7),
    surfaceVariant = Color(0xFF1B242C),
    onSurfaceVariant = Color(0xFFA9B3BB),
    outline = Color(0xFF35424C),
    error = Color(0xFFFF7B84),
    errorContainer = Color(0xFF4E1F24),
    onErrorContainer = Color(0xFFFFDADB)
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 29.sp,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 25.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 21.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.2.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun PhoneGuardTheme(
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Dynamic color intentionally stays off: this app uses its own stable utility identity.
    @Suppress("UNUSED_VARIABLE")
    val ignoredDynamicColor = dynamicColor

    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
