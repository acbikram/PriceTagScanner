package com.pricetag.scanner.presentation.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val AppColorScheme = darkColorScheme(
    primary          = PrimaryTeal,
    onPrimary        = TextOnPrimary,
    primaryContainer = BgSurface,
    secondary        = PrimaryGreen,
    onSecondary      = TextOnPrimary,
    background       = BgDark,
    onBackground     = TextPrimary,
    surface          = BgSurface,
    onSurface        = TextPrimary,
    surfaceVariant   = BgInput,
    onSurfaceVariant = TextSecondary,
    outline          = BorderColor,
    error            = ErrorRed,
    onError          = TextPrimary,
)

@Composable
fun PriceTagScannerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography  = AppTypography,
        content     = content,
    )
}
