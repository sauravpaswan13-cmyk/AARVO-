package com.aarvo.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = AarvoPrimary,
    onPrimary = AarvoOnPrimary,
    primaryContainer = AarvoPrimaryContainer,
    onPrimaryContainer = AarvoOnPrimaryContainer,
    secondary = AarvoSecondary,
    onSecondary = AarvoOnSecondary,
    secondaryContainer = AarvoSecondaryContainer,
    onSecondaryContainer = AarvoOnSecondaryContainer,
    background = AarvoBackground,
    onBackground = AarvoOnBackground,
    surface = AarvoSurface,
    onSurface = AarvoOnSurface,
    surfaceVariant = AarvoSurfaceVariant,
    onSurfaceVariant = AarvoOnSurfaceVariant,
    outline = AarvoOutline
)

private val DarkColors = darkColorScheme(
    primary = AarvoPrimaryDark,
    onPrimary = AarvoOnPrimaryDark,
    primaryContainer = AarvoPrimaryContainerDark,
    onPrimaryContainer = AarvoOnPrimaryContainerDark,
    secondary = AarvoSecondaryDark,
    onSecondary = AarvoOnSecondaryDark,
    secondaryContainer = AarvoSecondaryContainerDark,
    onSecondaryContainer = AarvoOnSecondaryContainerDark,
    background = AarvoBackgroundDark,
    onBackground = AarvoOnBackgroundDark,
    surface = AarvoSurfaceDark,
    onSurface = AarvoOnSurfaceDark,
    surfaceVariant = AarvoSurfaceVariantDark,
    onSurfaceVariant = AarvoOnSurfaceVariantDark,
    outline = AarvoOutlineDark
)

@Composable
fun AarvoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = (if (darkTheme) Color(0xFF26160C) else Color(0xFFFFE5CC)).toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AarvoTypography,
        content = content
    )
}
