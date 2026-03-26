package com.example.picker_photo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Single light scheme — no dynamic color, uses our design tokens ─────────
private val PickerColorScheme = lightColorScheme(
    primary                = Primary,
    onPrimary              = OnPrimary,
    primaryContainer       = PrimaryContainer,
    onPrimaryContainer     = OnPrimaryContainer,
    surface                = Surface,
    onSurface              = OnSurface,
    onSurfaceVariant       = OnSurfaceVariant,
    surfaceContainerLowest = SurfaceContainerLowest,
    surfaceContainerLow    = SurfaceContainerLow,
    surfaceContainerHigh   = SurfaceContainerHigh,
    outlineVariant         = OutlineVariant,
    background             = Surface,
    onBackground           = OnSurface,
)

@Composable
fun PickerphotoTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PickerColorScheme,
        typography  = Typography,
        content     = content
    )
}