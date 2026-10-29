package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF26D092),
    onPrimary = Color(0xFF032216),
    primaryContainer = Color(0xFF0D4E36),
    onPrimaryContainer = Color(0xFFC8FBE4),
    secondary = SignalGold,
    onSecondary = Color(0xFF261900),
    secondaryContainer = Color(0xFF422D00),
    onSecondaryContainer = Color(0xFFFFDF9E),
    tertiary = CrimsonExpress,
    onTertiary = Color.White,
    background = Color(0xFF061611),
    onBackground = Color(0xFFE5F2EC),
    surface = Color(0xFF0C231B),
    onSurface = Color(0xFFE5F2EC),
    surfaceVariant = Color(0xFF143529),
    onSurfaceVariant = Color(0xFFB4CFC3),
    outline = Color(0xFF2B5747)
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldMintLight,
    onPrimaryContainer = EmeraldDeep,
    secondary = Color(0xFFD68B00),
    onSecondary = Color.White,
    secondaryContainer = SignalGoldSoft,
    onSecondaryContainer = Color(0xFF3A2400),
    tertiary = CrimsonExpress,
    onTertiary = Color.White,
    background = AlabasterSurface,
    onBackground = Color(0xFF0B221A),
    surface = Color.White,
    onSurface = Color(0xFF0B221A),
    surfaceVariant = Color(0xFFE6F0EC),
    onSurfaceVariant = Color(0xFF3E594E),
    outline = Color(0xFFBDD0C8)
)

@Composable
fun MyApplicationTheme(
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
