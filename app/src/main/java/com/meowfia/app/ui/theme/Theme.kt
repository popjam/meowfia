package com.meowfia.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = MeowfiaColors.Primary,
    onPrimary = MeowfiaColors.TextOnPrimary,
    secondary = MeowfiaColors.Secondary,
    onSecondary = MeowfiaColors.TextPrimary,
    tertiary = MeowfiaColors.Tertiary,
    background = MeowfiaColors.Background,
    onBackground = MeowfiaColors.TextPrimary,
    surface = MeowfiaColors.Surface,
    onSurface = MeowfiaColors.TextPrimary,
    surfaceVariant = MeowfiaColors.SurfaceElevated,
    onSurfaceVariant = MeowfiaColors.TextSecondary
)

@Composable
fun MeowfiaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = MeowfiaTypography,
        content = content
    )
}
