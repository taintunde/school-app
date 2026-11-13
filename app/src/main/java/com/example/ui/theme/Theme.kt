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
    primary = Green80,
    secondary = GreenGrey80,
    tertiary = Gold80,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = SchoolGreen,
    onSecondary = DarkBackground,
    onBackground = BackgroundLight,
    onSurface = BackgroundLight
)

private val LightColorScheme = lightColorScheme(
    primary = SchoolGreen,
    secondary = SecondaryBlue,
    tertiary = SchoolGold,
    background = BackgroundLight,
    surface = SurfaceLight,
    onPrimary = SurfaceLight,
    onSecondary = SurfaceLight,
    onBackground = DarkBackground,
    onSurface = DarkBackground
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep the school brand colors consistent by default
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
