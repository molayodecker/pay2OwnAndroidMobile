package com.pay2own.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Navy,
    onPrimary = White,
    primaryContainer = Mist,
    onPrimaryContainer = Navy,
    secondary = Cyan,
    onSecondary = White,
    tertiary = Yellow,
    onTertiary = Navy,
    background = White,
    onBackground = Navy,
    surface = White,
    onSurface = Navy,
    surfaceVariant = Mist,
    onSurfaceVariant = NavyMuted,
    outline = Navy,
    error = Navy,
    onError = White,
    errorContainer = Mist,
    onErrorContainer = Navy,
)

private val DarkColors = darkColorScheme(
    primary = Cyan,
    onPrimary = Navy,
    primaryContainer = Color(0xFF1A4F72),
    onPrimaryContainer = White,
    secondary = Cyan,
    onSecondary = Navy,
    tertiary = Yellow,
    onTertiary = Navy,
    background = Navy,
    onBackground = White,
    surface = NavyRaised,
    onSurface = White,
    surfaceVariant = Color(0xFF24306E),
    onSurfaceVariant = Color(0xFFB9DFF3),
    outline = White,
    error = Yellow,
    onError = Navy,
    errorContainer = Color(0xFF24306E),
    onErrorContainer = Yellow,
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun Pay2OwnTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        shapes = AppShapes,
        content = content,
    )
}
