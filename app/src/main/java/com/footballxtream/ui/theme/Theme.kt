package com.footballxtream.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

private val DarkColors = darkColorScheme(
    primary = Color(0xFF39D353),
    onPrimary = Color(0xFF06210E),
    background = Color(0xFF0A0E12),
    onBackground = Color(0xFFE6EAEE),
    surface = Color(0xFF141B22),
    onSurface = Color(0xFFE6EAEE),
    surfaceVariant = Color(0xFF1E2730),
    onSurfaceVariant = Color(0xFFB9C2CC),
)

@Composable
fun FootballXtreamTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content,
    )
}
