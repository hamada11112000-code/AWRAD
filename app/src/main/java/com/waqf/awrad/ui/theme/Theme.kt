package com.waqf.awrad.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.waqf.awrad.R
import com.waqf.awrad.data.ThemeMode

val Amiri = FontFamily(
    Font(R.font.amiri_regular, FontWeight.Normal),
    Font(R.font.amiri_bold, FontWeight.Bold),
)

val Tajawal = FontFamily(
    Font(R.font.tajawal_regular, FontWeight.Normal),
    Font(R.font.tajawal_medium, FontWeight.Medium),
    Font(R.font.tajawal_bold, FontWeight.Bold),
    Font(R.font.tajawal_extrabold, FontWeight.ExtraBold),
)

/** Brand palette: deep Waqf green, manuscript paper and gold. */
object Brand {
    val Deep = Color(0xFF0B2A26)
    val Green = Color(0xFF0F3832)
    val Teal = Color(0xFF13574D)
    val TealBright = Color(0xFF1D7A6B)
    val Gold = Color(0xFFC9A45C)
    val GoldLight = Color(0xFFE6CB8E)
    val GoldInk = Color(0xFF8A6A2C)
    val Paper = Color(0xFFF6F1E6)
    val Cream = Color(0xFFFBF7EE)
}

@Immutable
data class AwradColors(
    val background: Color,
    val surface: Color,
    val card: Color,
    val cardBorder: Color,
    val ink: Color,
    val inkSoft: Color,
    val mute: Color,
    val accent: Color,
    val accentSoft: Color,
    val gold: Color,
    val goldInk: Color,
    val goldSoft: Color,
    val fadlBg: Color,
    val chipGreenBg: Color,
    val track: Color,
    val isDark: Boolean,
)

private val LightAwrad = AwradColors(
    background = Brand.Paper,
    surface = Color.White,
    card = Brand.Cream,
    cardBorder = Color(0x73C9A45C),
    ink = Color(0xFF1F2A27),
    inkSoft = Color(0xFF4A4A3F),
    mute = Color(0xFF6E7B76),
    accent = Brand.Teal,
    accentSoft = Color(0xFFDDEDE8),
    gold = Brand.Gold,
    goldInk = Brand.GoldInk,
    goldSoft = Color(0xFFEFE5CF),
    fadlBg = Color(0xFFF3ECDC),
    chipGreenBg = Color(0xFFDDEDE8),
    track = Color(0xFFEEE7D6),
    isDark = false,
)

private val DarkAwrad = AwradColors(
    background = Color(0xFF081F1C),
    surface = Color(0xFF10302B),
    card = Color(0xFF143A34),
    cardBorder = Color(0x59C9A45C),
    ink = Color(0xFFF1EAD9),
    inkSoft = Color(0xFFD7CFBE),
    mute = Color(0xFF93A9A2),
    accent = Color(0xFF5CC2AE),
    accentSoft = Color(0xFF1B4A42),
    gold = Brand.Gold,
    goldInk = Brand.GoldLight,
    goldSoft = Color(0xFF2E4436),
    fadlBg = Color(0xFF1B433C),
    chipGreenBg = Color(0xFF1B4A42),
    track = Color(0xFF214A43),
    isDark = true,
)

val LocalAwradColors = staticCompositionLocalOf { LightAwrad }

object AwradTheme {
    val colors: AwradColors
        @Composable get() = LocalAwradColors.current
}

private val base = TextStyle(fontFamily = Tajawal)

private val AppTypography = Typography(
    displaySmall = base.copy(fontFamily = Amiri, fontWeight = FontWeight.Bold, fontSize = 30.sp),
    headlineMedium = base.copy(fontFamily = Amiri, fontWeight = FontWeight.Bold, fontSize = 26.sp),
    headlineSmall = base.copy(fontFamily = Amiri, fontWeight = FontWeight.Bold, fontSize = 23.sp),
    titleLarge = base.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleMedium = base.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
    titleSmall = base.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
    bodyLarge = base.copy(fontSize = 16.sp, lineHeight = 26.sp),
    bodyMedium = base.copy(fontSize = 14.sp, lineHeight = 23.sp),
    bodySmall = base.copy(fontSize = 12.sp, lineHeight = 19.sp),
    labelLarge = base.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
    labelMedium = base.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
    labelSmall = base.copy(fontWeight = FontWeight.Medium, fontSize = 11.sp),
)

@Composable
fun AwradTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val c = if (dark) DarkAwrad else LightAwrad
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.accent, onPrimary = Brand.Deep, secondary = c.gold, onSecondary = Brand.Deep,
            background = c.background, onBackground = c.ink, surface = c.surface, onSurface = c.ink,
            surfaceVariant = c.card, onSurfaceVariant = c.mute, outline = c.cardBorder,
            surfaceContainerLow = c.surface, surfaceContainer = c.surface, surfaceContainerHigh = c.card,
        )
    } else {
        lightColorScheme(
            primary = c.accent, onPrimary = Color.White, secondary = c.gold, onSecondary = Brand.Deep,
            background = c.background, onBackground = c.ink, surface = c.surface, onSurface = c.ink,
            surfaceVariant = c.card, onSurfaceVariant = c.mute, outline = c.cardBorder,
            surfaceContainerLow = c.surface, surfaceContainer = c.surface, surfaceContainerHigh = c.card,
        )
    }
    CompositionLocalProvider(LocalAwradColors provides c) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
    }
}
