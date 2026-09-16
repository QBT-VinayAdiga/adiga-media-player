package com.mplayerx.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// AMOLED-first: every large surface is true black so pixels stay off;
// one saturated accent carries contrast. Cards/thumbnails sit on
// near-black containers instead of Material's default dark gray.
private val Amoled = darkColorScheme(
    primary = Color(0xFFFFAB40),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF3A2503),
    onPrimaryContainer = Color(0xFFFFD9A3),
    secondary = Color(0xFF7DD3FC),
    onSecondary = Color.Black,
    background = Color.Black,
    onBackground = Color(0xFFF5F5F5),
    surface = Color.Black,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1A1A1C),
    onSurfaceVariant = Color(0xFFB3B3B3),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0E0E10),
    surfaceContainer = Color(0xFF141416),
    surfaceContainerHigh = Color(0xFF1C1C1F),
    surfaceContainerHighest = Color(0xFF242428),
    outline = Color(0xFF2E2E33),
    outlineVariant = Color(0xFF1F1F23),
    scrim = Color.Black,
    error = Color(0xFFFF6E6E),
)

@Composable
fun MPlayerXTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Amoled, content = content)
}
