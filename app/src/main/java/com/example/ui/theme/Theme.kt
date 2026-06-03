package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkElegantColorScheme = darkColorScheme(
    primary = Color(0xFFA67CFF),     // Light purple for key interactions
    secondary = Color(0xFF6B4EE6),   // Stronger purple 
    tertiary = Color(0xFFFF7CE9),    // Pinkish accent
    background = Color(0xFF0F0C1B),  // Very dark purple-black
    surface = Color(0xFF1B162B),     // Slightly lighter for cards
    surfaceVariant = Color(0xFF27213C),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFFE5E0F2),
    onSurface = Color(0xFFE5E0F2),
    onSurfaceVariant = Color(0xFFC7C2D6)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = DarkElegantColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
