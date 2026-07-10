package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = UgandaGold,
    onPrimary = Color.White,
    primaryContainer = UgandaGoldLight,
    onPrimaryContainer = TextWhite,
    secondary = SavannahGreen,
    onSecondary = Color.White,
    tertiary = UgandaGoldDark,
    background = CarbonBg,
    onBackground = TextWhite,
    surface = CarbonCard,
    onSurface = TextWhite,
    surfaceVariant = CarbonCardPressed,
    onSurfaceVariant = TextGray,
    outline = BorderSlate,
    error = CrimsonA
)

private val LightColorScheme = lightColorScheme(
    primary = UgandaGold,
    onPrimary = Color.White,
    primaryContainer = UgandaGoldLight,
    onPrimaryContainer = TextWhite,
    secondary = SavannahGreen,
    onSecondary = Color.White,
    tertiary = UgandaGoldDark,
    background = CarbonBg,
    onBackground = TextWhite,
    surface = CarbonCard,
    onSurface = TextWhite,
    surfaceVariant = CarbonCardPressed,
    onSurfaceVariant = TextGray,
    outline = BorderSlate,
    error = CrimsonA
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark theme by default for the premium "Cosmic/Obsidian Slate" styling
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
