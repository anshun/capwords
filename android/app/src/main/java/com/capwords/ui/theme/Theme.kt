package com.capwords.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val CapLightColors = lightColorScheme(
    primary = CapPrimary,
    background = CapBackground,
    surface = CapSurface,
    onSurface = CapOnSurface,
    onBackground = CapOnSurface,
    outline = CapOutline,
)

@Composable
fun CapWordsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Phase 1 keeps a single light scheme that mirrors the source app's look.
    MaterialTheme(
        colorScheme = CapLightColors,
        typography = CapTypography,
        content = content,
    )
}
