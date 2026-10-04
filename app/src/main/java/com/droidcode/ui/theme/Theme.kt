package com.droidcode.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.droidcode.settings.AppSettings

// NOTE: Strictly avoid trademarked or copyrighted theme names. Use generic, descriptive themes.
private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkBackground,
    secondary = DarkSecondary,
    onSecondary = DarkBackground,
    tertiary = DarkTertiary,
    background = Color(0xFF1E1E1E),
    onBackground = Color(0xFFCCCCCC),
    surface = Color(0xFF252526),
    onSurface = Color(0xFFCCCCCC),
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color(0xFF969696),
    outline = Color(0xFF333333),
    outlineVariant = Color(0xFF3E3E42),
    error = StatusError
)

private val OneDarkColorScheme = darkColorScheme(
    primary = Color(0xFF61AFEF),
    onPrimary = Color(0xFF282C34),
    secondary = Color(0xFF98C379),
    onSecondary = Color(0xFF282C34),
    tertiary = Color(0xFFC678DD),
    background = Color(0xFF282C34),
    onBackground = Color(0xFFABB2BF),
    surface = Color(0xFF21252B),
    onSurface = Color(0xFFABB2BF),
    surfaceVariant = Color(0xFF282C34),
    onSurfaceVariant = Color(0xFF5C6370),
    outline = Color(0xFF3B4048),
    outlineVariant = Color(0xFF4B5263),
    error = StatusError
)

private val DraculaColorScheme = darkColorScheme(
    primary = Color(0xFFFF79C6),
    onPrimary = Color(0xFF282A36),
    secondary = Color(0xFF50FA7B),
    onSecondary = Color(0xFF282A36),
    tertiary = Color(0xFFBD93F9),
    background = Color(0xFF282A36),
    onBackground = Color(0xFFF8F8F2),
    surface = Color(0xFF21222C),
    onSurface = Color(0xFFF8F8F2),
    surfaceVariant = Color(0xFF282A36),
    onSurfaceVariant = Color(0xFF6272A4),
    outline = Color(0xFF44475A),
    outlineVariant = Color(0xFF6272A4),
    error = StatusError
)

private val MonokaiColorScheme = darkColorScheme(
    primary = Color(0xFFF92672),
    onPrimary = Color(0xFF272822),
    secondary = Color(0xFFA6E22E),
    onSecondary = Color(0xFF272822),
    tertiary = Color(0xFF66D9EF),
    background = Color(0xFF272822),
    onBackground = Color(0xFFF8F8F2),
    surface = Color(0xFF1E1F1C),
    onSurface = Color(0xFFF8F8F2),
    surfaceVariant = Color(0xFF272822),
    onSurfaceVariant = Color(0xFF75715E),
    outline = Color(0xFF3E3D32),
    outlineVariant = Color(0xFF49483E),
    error = StatusError
)

private val SolarizedDarkColorScheme = darkColorScheme(
    primary = Color(0xFF268BD2),
    onPrimary = Color(0xFF002B36),
    secondary = Color(0xFF2AA198),
    onSecondary = Color(0xFF002B36),
    tertiary = Color(0xFF859900),
    background = Color(0xFF002B36),
    onBackground = Color(0xFF839496),
    surface = Color(0xFF073642),
    onSurface = Color(0xFF93A1A1),
    surfaceVariant = Color(0xFF002B36),
    onSurfaceVariant = Color(0xFF586E75),
    outline = Color(0xFF0F4A58),
    outlineVariant = Color(0xFF586E75),
    error = StatusError
)

private val NordColorScheme = darkColorScheme(
    primary = Color(0xFF88C0D0),
    onPrimary = Color(0xFF2E3440),
    secondary = Color(0xFFA3BE8C),
    onSecondary = Color(0xFF2E3440),
    tertiary = Color(0xFF81A1C1),
    background = Color(0xFF2E3440),
    onBackground = Color(0xFFECEFF4),
    surface = Color(0xFF242933),
    onSurface = Color(0xFFD8DEE9),
    surfaceVariant = Color(0xFF2E3440),
    onSurfaceVariant = Color(0xFF616E88),
    outline = Color(0xFF3B4252),
    outlineVariant = Color(0xFF4C566A),
    error = StatusError
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightSurface,
    secondary = LightSecondary,
    onSecondary = LightSurface,
    tertiary = LightTertiary,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightEditorCanvas,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    outlineVariant = Color(0xFFCBD5E1),
    error = StatusError
)

private val SolarizedLightColorScheme = lightColorScheme(
    primary = Color(0xFF268BD2),
    onPrimary = Color(0xFFFDF6E3),
    secondary = Color(0xFF2AA198),
    onSecondary = Color(0xFFFDF6E3),
    tertiary = Color(0xFF859900),
    background = Color(0xFFFDF6E3),
    onBackground = Color(0xFF657B83),
    surface = Color(0xFFEEE8D5),
    onSurface = Color(0xFF586E75),
    surfaceVariant = Color(0xFFFDF6E3),
    onSurfaceVariant = Color(0xFF93A1A1),
    outline = Color(0xFFE0D6BC),
    outlineVariant = Color(0xFF93A1A1),
    error = StatusError
)

@Composable
fun DroidCodeTheme(
    themeMode: AppSettings.ThemeMode = AppSettings.ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val colorScheme = when (themeMode) {
        AppSettings.ThemeMode.DARK -> DarkColorScheme
        AppSettings.ThemeMode.ONE_DARK -> OneDarkColorScheme
        AppSettings.ThemeMode.DRACULA -> DraculaColorScheme
        AppSettings.ThemeMode.MONOKAI -> MonokaiColorScheme
        AppSettings.ThemeMode.SOLARIZED_DARK -> SolarizedDarkColorScheme
        AppSettings.ThemeMode.NORD -> NordColorScheme
        AppSettings.ThemeMode.LIGHT -> LightColorScheme
        AppSettings.ThemeMode.SOLARIZED_LIGHT -> SolarizedLightColorScheme
        AppSettings.ThemeMode.SYSTEM -> if (isSystemDark) DarkColorScheme else LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
