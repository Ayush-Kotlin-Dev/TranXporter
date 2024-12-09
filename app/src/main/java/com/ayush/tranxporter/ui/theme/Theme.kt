package com.ayush.tranxporter.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    // Primary
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = PrimaryBlueLight,
    onPrimaryContainer = TextPrimary,

    // Secondary
    secondary = SecondaryBlue,
    onSecondary = Color.White,
    secondaryContainer = SecondaryBlueLight,
    onSecondaryContainer = TextPrimary,

    // Tertiary
    tertiary = WarningAmber,
    onTertiary = Color.White,
    tertiaryContainer = SuccessGreen,
    onTertiaryContainer = Color.White,

    // Background
    background = BackgroundWhite,
    onBackground = TextPrimary,

    // Surface
    surface = SurfaceLight,
    onSurface = TextPrimary,
    surfaceVariant = GreyBackground,
    onSurfaceVariant = TextSecondary,

    // Error
    error = ErrorRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFEBEE),
    onErrorContainer = ErrorRed,

    // Additional colors
    outline = TextDisabled,
    outlineVariant = TextDisabled.copy(alpha = 0.12f),
    scrim = TextPrimary.copy(alpha = 0.32f)
)

@Composable
fun TranXporterTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}