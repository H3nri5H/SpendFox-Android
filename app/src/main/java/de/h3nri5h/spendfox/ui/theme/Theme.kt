package de.h3nri5h.spendfox.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class SpendFoxThemeMode(val label: String) {
    System("System"),
    Light("Hell"),
    Dark("Dunkel")
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF2F6F63),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF7A5A42),
    tertiary = Color(0xFFE47737),
    background = Color(0xFFF4F7F2),
    onBackground = Color(0xFF17211D),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF17211D),
    surfaceVariant = Color(0xFFE7EFE8),
    onSurfaceVariant = Color(0xFF53605A),
    outlineVariant = Color(0xFFD2DDD4),
    error = Color(0xFFB2443D)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8BC7B8),
    onPrimary = Color(0xFF0B1F1A),
    secondary = Color(0xFFD1B79F),
    tertiary = Color(0xFFFFA56B),
    background = Color(0xFF09110F),
    onBackground = Color(0xFFEAF1EC),
    surface = Color(0xFF101916),
    onSurface = Color(0xFFEAF1EC),
    surfaceVariant = Color(0xFF1C2A25),
    onSurfaceVariant = Color(0xFFC5D1CB),
    outlineVariant = Color(0xFF34463F),
    error = Color(0xFFFF8C82)
)

private val SpendFoxShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun SpendFoxTheme(themeMode: SpendFoxThemeMode = SpendFoxThemeMode.System, content: @Composable () -> Unit) {
    val useDarkTheme = when (themeMode) {
        SpendFoxThemeMode.System -> isSystemInDarkTheme()
        SpendFoxThemeMode.Light -> false
        SpendFoxThemeMode.Dark -> true
    }
    MaterialTheme(
        colorScheme = if (useDarkTheme) DarkColors else LightColors,
        shapes = SpendFoxShapes,
        content = content
    )
}
