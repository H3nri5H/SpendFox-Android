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
    primary = Color(0xFFF2762E),
    onPrimary = Color(0xFF241B17),
    secondary = Color(0xFF6F5E55),
    tertiary = Color(0xFF356B9B),
    background = Color(0xFFFFF8F2),
    onBackground = Color(0xFF241B17),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF241B17),
    surfaceVariant = Color(0xFFF7ECE4),
    onSurfaceVariant = Color(0xFF6F5E55),
    outlineVariant = Color(0xFFE7D6CB),
    error = Color(0xFFB94242)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF8A3D),
    onPrimary = Color(0xFF151210),
    secondary = Color(0xFFC9B7AC),
    tertiary = Color(0xFF7EAFE3),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFF4EB),
    surface = Color(0xFF111111),
    onSurface = Color(0xFFFFF4EB),
    surfaceVariant = Color(0xFF1B1714),
    onSurfaceVariant = Color(0xFFC9B7AC),
    outlineVariant = Color(0xFF493A33),
    error = Color(0xFFFF7A74)
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
