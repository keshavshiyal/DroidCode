package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.example.settings.AppSettings

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkBackground,
    secondary = DarkSecondary,
    onSecondary = DarkBackground,
    tertiary = DarkTertiary,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkEditorCanvas,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
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
    error = StatusError
)

@Composable
fun DroidCodeTheme(
    themeMode: AppSettings.ThemeMode = AppSettings.ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        AppSettings.ThemeMode.DARK -> true
        AppSettings.ThemeMode.LIGHT -> false
        AppSettings.ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
