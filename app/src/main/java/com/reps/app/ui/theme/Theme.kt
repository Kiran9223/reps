package com.reps.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val RepsDarkColorScheme = darkColorScheme(
    primary = RepsPrimary,
    onPrimary = RepsOnPrimary,
    primaryContainer = RepsPrimaryContainer,
    onPrimaryContainer = RepsPrimary,
    secondary = RepsSecondary,
    onSecondary = RepsOnSecondary,
    secondaryContainer = RepsSecondaryContainer,
    onSecondaryContainer = RepsSecondary,
    background = RepsBackground,
    onBackground = RepsOnBackground,
    surface = RepsSurface,
    onSurface = RepsOnSurface,
    surfaceVariant = RepsSurfaceVariant,
    onSurfaceVariant = RepsOnSurfaceVariant,
    outline = RepsOutline,
    error = RepsError,
    errorContainer = RepsErrorContainer,
)

@Composable
fun RepsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RepsDarkColorScheme,
        typography = RepsTypography,
        content = content
    )
}
