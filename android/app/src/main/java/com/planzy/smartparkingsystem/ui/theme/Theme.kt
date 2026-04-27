package com.planzy.smartparkingsystem.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary          = Blue600,
    onPrimary        = Color.White,
    primaryContainer = Blue50,
    onPrimaryContainer = Blue600,
    surface          = Color.White,
    surfaceVariant   = Neutral100,
    background       = Neutral100,
    onBackground     = Neutral900,
    onSurface        = Neutral900,
)

private val DarkColors = darkColorScheme(
    primary          = Blue400,
    onPrimary        = Color(0xFF003087),
    primaryContainer = Color(0xFF00419A),
    onPrimaryContainer = Color(0xFFD6E3FF),
    surface          = Color(0xFF1C1B1F),
    surfaceVariant   = Color(0xFF44474F),
    background       = Color(0xFF1C1B1F),
    onBackground     = Color(0xFFE6E1E5),
    onSurface        = Color(0xFFE6E1E5),
)

@Composable
fun SmartParkingSystemTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content     = content,
    )
}