package com.aarvo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = AarvoPrimary,
    onPrimary = AarvoOnPrimary,
    primaryContainer = AarvoPrimaryContainer,
    onPrimaryContainer = AarvoOnPrimaryContainer,
    secondary = AarvoSecondary,
    background = AarvoBackground,
    surface = AarvoSurface
)

private val DarkColors = darkColorScheme(
    primary = AarvoPrimaryDark,
    onPrimary = AarvoOnPrimaryDark,
    primaryContainer = AarvoPrimaryContainerDark,
    onPrimaryContainer = AarvoOnPrimaryContainerDark,
    secondary = AarvoSecondaryDark
)

@Composable
fun AarvoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AarvoTypography,
        content = content
    )
}
