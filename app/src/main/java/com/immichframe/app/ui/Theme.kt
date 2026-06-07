package com.immichframe.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7CC0FF),
    onPrimary = Color(0xFF002C50),
    background = Color(0xFF0F1115),
    surface = Color(0xFF181B22),
    onBackground = Color(0xFFE6E6E6),
    onSurface = Color(0xFFE6E6E6),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1F6FB2),
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFF4F6FA),
)

@Composable
fun ImmichFrameTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else DarkColors
    MaterialTheme(colorScheme = colors, content = content)
}
