package com.mplayerx.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Scheme = darkColorScheme(
    primary = Color(0xFF8AB4FF),
    onPrimary = Color(0xFF00315B),
    background = Color.Black,
    surface = Color(0xFF111114),
    onBackground = Color.White,
    onSurface = Color.White,
)

@Composable
fun MPlayerXTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
