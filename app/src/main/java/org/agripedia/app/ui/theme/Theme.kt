package org.agripedia.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.agripedia.app.data.prefs.ThemeMode

// Nature palette: plant green, dark green, natural beige, earth brown. Contrast checked for WCAG AA on text pairs.
private val Green40 = Color(0xFF2E6B3A)
private val Green20 = Color(0xFF0E3A18)
private val Green90 = Color(0xFFB7F0B9)
private val Green80 = Color(0xFF9CD49E)
private val Earth40 = Color(0xFF6B5B3E)
private val Earth90 = Color(0xFFF4DFBA)
private val Earth80 = Color(0xFFD8C39E)
private val Clay40 = Color(0xFF9A4A2B)
private val Clay90 = Color(0xFFFFDBCF)
private val Beige = Color(0xFFFBF8F1)
private val BeigeVariant = Color(0xFFEDE6D6)

private val LightColors = lightColorScheme(
    primary = Green40, onPrimary = Color.White, primaryContainer = Green90, onPrimaryContainer = Green20,
    secondary = Earth40, onSecondary = Color.White, secondaryContainer = Earth90, onSecondaryContainer = Color(0xFF241A04),
    tertiary = Clay40, onTertiary = Color.White, tertiaryContainer = Clay90, onTertiaryContainer = Color(0xFF3A0B00),
    background = Beige, onBackground = Color(0xFF1B1C18), surface = Beige, onSurface = Color(0xFF1B1C18),
    surfaceVariant = BeigeVariant, onSurfaceVariant = Color(0xFF45483F), outline = Color(0xFF75786E),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF6F2E8), surfaceContainer = Color(0xFFF1ECE0),
    surfaceContainerHigh = Color(0xFFEBE6DA), surfaceContainerHighest = Color(0xFFE5E0D4),
)

private val DarkColors = darkColorScheme(
    primary = Green80, onPrimary = Color(0xFF00390F), primaryContainer = Color(0xFF155223), onPrimaryContainer = Green90,
    secondary = Earth80, onSecondary = Color(0xFF3A2F14), secondaryContainer = Color(0xFF524428), onSecondaryContainer = Earth90,
    tertiary = Color(0xFFFFB59C), onTertiary = Color(0xFF5C1A04), tertiaryContainer = Color(0xFF7B3317), onTertiaryContainer = Clay90,
    background = Color(0xFF121612), onBackground = Color(0xFFE3E3DC), surface = Color(0xFF121612), onSurface = Color(0xFFE3E3DC),
    surfaceVariant = Color(0xFF43483F), onSurfaceVariant = Color(0xFFC4C8BC), outline = Color(0xFF8E9287),
    surfaceContainerLowest = Color(0xFF0D110D), surfaceContainerLow = Color(0xFF1A1E19), surfaceContainer = Color(0xFF1E221D),
    surfaceContainerHigh = Color(0xFF282C27), surfaceContainerHighest = Color(0xFF333731),
)

private val AppTypography = Typography().run {
    copy(
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = bodyLarge.copy(lineHeight = 24.sp),
        labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp),
    )
}

@Composable
fun AgriTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, typography = AppTypography, content = content)
}
