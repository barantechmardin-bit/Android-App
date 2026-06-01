package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryTeal,
    onPrimary = Color.Black,
    secondary = AccentOrange,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkContainer,
    outline = DarkBorder,
    onBackground = Color(0xFFECEFF1),
    onSurface = Color(0xFFECEFF1),
    onSurfaceVariant = Color(0xFFB0BEC5)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryTealDark,
    onPrimary = Color.White,
    secondary = AccentOrange,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightContainer,
    outline = LightBorder,
    onBackground = Color(0xFF263238),
    onSurface = Color(0xFF263238),
    onSurfaceVariant = Color(0xFF546E7A)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to dark mode as requested
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
