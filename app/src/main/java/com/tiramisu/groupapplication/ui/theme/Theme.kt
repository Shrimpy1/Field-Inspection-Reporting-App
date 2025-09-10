package com.tiramisu.groupapplication.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat


private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = OnPrimaryWhite,
    primaryContainer = PrimaryContainerLightBlue,
    onPrimaryContainer = OnPrimaryContainerDarkBlue,
    secondary = SecondaryDarkBlue,
    onSecondary = OnSecondaryWhite,
    secondaryContainer = SecondaryContainerLightGray,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryPurple,
    onTertiary = OnTertiaryWhite,
    tertiaryContainer = TertiaryContainerLightPurple,
    onTertiaryContainer = OnTertiaryContainerDark,
    error = ErrorRed,
    onError = OnErrorWhite,
    errorContainer = ErrorContainerLightRed,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundLight,
    onBackground = OnBackgroundDark,
    surface = SurfaceLight,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineGray
)

@Composable
fun GroupApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}