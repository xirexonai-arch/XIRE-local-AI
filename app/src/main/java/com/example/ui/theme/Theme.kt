package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.data.model.ThemeMode

data class XireExtendedColors(
    val sidebar: Color,
    val card: Color,
    val border: Color,
    val muted: Color,
    val danger: Color
)

val LocalXireColors = staticCompositionLocalOf {
    XireExtendedColors(
        sidebar = LightSidebar,
        card = LightCard,
        border = LightBorder,
        muted = LightMuted,
        danger = LightDanger
    )
}

private val XireDarkColorScheme = darkColorScheme(
    primary = DarkAccent,
    onPrimary = DarkOnAccent,
    secondary = XireBronze,
    onSecondary = DarkOnAccent,
    background = DarkBg,
    onBackground = DarkText,
    surface = DarkCard,
    onSurface = DarkText,
    surfaceVariant = DarkSidebar,
    onSurfaceVariant = DarkMuted,
    outline = DarkBorder,
    outlineVariant = DarkBorder,
    error = DarkDanger,
    onError = Color.White
)

private val XireLightColorScheme = lightColorScheme(
    primary = LightAccent,
    onPrimary = LightOnAccent,
    secondary = XireBronze,
    onSecondary = LightOnAccent,
    background = LightBg,
    onBackground = LightText,
    surface = LightCard,
    onSurface = LightText,
    surfaceVariant = LightSidebar,
    onSurfaceVariant = LightMuted,
    outline = LightBorder,
    outlineVariant = LightBorder,
    error = LightDanger,
    onError = Color.White
)

private val XireShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun XireTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDark) XireDarkColorScheme else XireLightColorScheme
    val extendedColors = if (isDark) {
        XireExtendedColors(
            sidebar = DarkSidebar,
            card = DarkCard,
            border = DarkBorder,
            muted = DarkMuted,
            danger = DarkDanger
        )
    } else {
        XireExtendedColors(
            sidebar = LightSidebar,
            card = LightCard,
            border = LightBorder,
            muted = LightMuted,
            danger = LightDanger
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !isDark
                controller.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    CompositionLocalProvider(LocalXireColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = XireTypography,
            shapes = XireShapes,
            content = content
        )
    }
}
