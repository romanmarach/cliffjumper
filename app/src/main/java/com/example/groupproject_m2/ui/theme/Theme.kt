package com.example.groupproject_m2.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Green80,
    onPrimary = Green40,
    primaryContainer = Color(0xFF1E4623),
    onPrimaryContainer = Color(0xFFC8F2C9),
    secondary = GreenGrey80,
    onSecondary = Color(0xFF223224),
    secondaryContainer = Color(0xFF324A35),
    onSecondaryContainer = Color(0xFFD3E9D0),
    tertiary = Mint80,
    onTertiary = Color(0xFF1F3A20),
    tertiaryContainer = Color(0xFF2D5A31),
    onTertiaryContainer = Color(0xFFDDF5DD),
    background = Color(0xFF0F1510),
    onBackground = Color(0xFFE6F1E5),
    surface = Color(0xFF121912),
    onSurface = Color(0xFFE6F1E5)
)

private val LightColorScheme = lightColorScheme(
    primary = Green40,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8E6C9),
    onPrimaryContainer = Color(0xFF163A19),
    secondary = GreenGrey40,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCEDC8),
    onSecondaryContainer = Color(0xFF243A27),
    tertiary = Mint40,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE8F5E9),
    onTertiaryContainer = Color(0xFF1E3D22),
    background = Color(0xFFF1F8E9),
    onBackground = Color(0xFF1A2B1B),
    surface = Color(0xFFECF7E7),
    onSurface = Color(0xFF1A2B1B)

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun GroupProject_m2Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
