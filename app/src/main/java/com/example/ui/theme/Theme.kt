package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = Color(0xFF8CF3FF),
    secondary = ElectricViolet,
    onSecondary = Color.White,
    secondaryContainer = ElectricVioletDark,
    onSecondaryContainer = Color(0xFFEDE9FE),
    tertiary = EmeraldReady,
    onTertiary = Color(0xFF003918),
    tertiaryContainer = Color(0xFF005325),
    onTertiaryContainer = Color(0xFF6BFF9A),
    background = CyberObsidian,
    onBackground = TextPrimary,
    surface = CyberSurface,
    onSurface = TextPrimary,
    surfaceVariant = CyberSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = CyberCardBorder,
    error = RubyStop,
    onError = Color.White
)

@Composable
fun GameLingoTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
