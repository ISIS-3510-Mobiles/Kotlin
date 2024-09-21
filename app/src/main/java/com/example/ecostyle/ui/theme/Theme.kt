package com.example.ecostyle.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.*

private val LightColorPalette = lightColors(
    primary = DarkGreen,
    primaryVariant = LimeGreen,
    secondary = LimeGreen,
    background = White,
    surface = LightGray,
    onPrimary = White,
    onSecondary = White,
    onBackground = White,
    onSurface = DarkGreen
)

@Composable
fun EcoStyleTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = LightColorPalette,
        typography = EcoStyleTypography,
        shapes = MaterialTheme.shapes,
        content = content
    )
}