package com.ayush.tranxporter.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    onPrimary = Color.White,
    primaryContainer = BlueLight,
    onPrimaryContainer = Color.White,
    secondary = AmberPrimary,
    onSecondary = Color.Black,
    secondaryContainer = AmberLight,
    onSecondaryContainer = Color.Black,
    background = GreyLight,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    error = RedError,
    onError = Color.White
)

@Composable
fun TranXporterTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}