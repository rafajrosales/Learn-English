package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = DarkPolishPrimary,
    primaryContainer = DarkPolishPrimaryContainer,
    onPrimary = DarkPolishOnPrimary,
    onPrimaryContainer = DarkPolishOnPrimaryContainer,
    secondary = DarkPolishPrimary,
    background = DarkPolishBg,
    surface = DarkPolishSurface,
    surfaceVariant = DarkPolishSurfaceVariant,
    onBackground = DarkPolishTextPrimary,
    onSurface = DarkPolishTextPrimary,
    onSurfaceVariant = DarkPolishTextSecondary,
    outline = DarkPolishOutline
)

private val LightColorScheme = lightColorScheme(
    primary = PolishPrimary,
    primaryContainer = PolishPrimaryContainer,
    onPrimary = PolishOnPrimary,
    onPrimaryContainer = PolishOnPrimaryContainer,
    secondary = PolishSecondary,
    secondaryContainer = PolishSecondaryContainer,
    onSecondaryContainer = PolishOnSecondaryContainer,
    background = PolishBg,
    surface = PolishBg,
    surfaceVariant = PolishSurfaceVariant,
    onBackground = PolishTextPrimary,
    onSurface = PolishTextPrimary,
    onSurfaceVariant = PolishTextSecondary,
    outline = PolishOutline
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Set to false to enforce our custom Cyber/Space brand identity
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
