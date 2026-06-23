package com.example.isplayer.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = PrimaryBlueContainer,
    onPrimaryContainer = Color(0xFF09233E),
    secondary = SecondarySlate,
    onSecondary = Color.White,
    secondaryContainer = SecondarySlateContainer,
    onSecondaryContainer = Color(0xFF111C27),
    tertiary = TagOrange,
    onTertiary = Color(0xFF402400),
    tertiaryContainer = TagOrangeContainer,
    onTertiaryContainer = Color(0xFF2F1A00),
    background = LightBackground,
    onBackground = TextPrimary,
    surface = LightSurface,
    surfaceContainer = LightSurfaceElevated,
    surfaceContainerHigh = Color(0xFFF0F4F7),
    surfaceContainerHighest = Color(0xFFE7EEF4),
    onSurface = TextPrimary,
    surfaceVariant = SearchBarBackground,
    onSurfaceVariant = TextSecondary,
    outline = DividerColor,
    outlineVariant = Color(0xFFE4EAF0),
    error = ErrorRed,
    onError = Color.White
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

@Composable
fun IsPlayerTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Use WindowCompat to set status bar color instead of deprecated property
            @Suppress("DEPRECATION")
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
