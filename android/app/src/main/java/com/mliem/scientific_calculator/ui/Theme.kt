package com.mliem.scientific_calculator.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.mliem.scientific_calculator.data.ThemeChoice

/** Flat, light surfaces with one green accent: the ColorOS calculator look. */
@Immutable
class CalcColors(
    val isDark: Boolean,
    val background: Color,
    /** Grouped rows in the history and settings screens. */
    val surface: Color,
    val line: Color,
    val text: Color,
    val dim: Color,
    val digitKey: Color,
    val digitText: Color,
    val sciKey: Color,
    val sciText: Color,
    val utilKey: Color,
    val utilText: Color,
    val opKey: Color,
    val opText: Color,
    val equalsKey: Color,
    val equalsText: Color,
    /** Caret, links and the active state. */
    val accent: Color,
)

val LightColors = CalcColors(
    isDark = false,
    background = Color(0xFFF5F5F5), surface = Color(0xFFFFFFFF), line = Color(0xFFE4E4E6),
    text = Color(0xFF111111), dim = Color(0xFF6F6F74),
    digitKey = Color(0xFFFFFFFF), digitText = Color(0xFF111111),
    sciKey = Color(0xFFEBEBED), sciText = Color(0xFF2B2B2D),
    utilKey = Color(0xFFE3E3E6), utilText = Color(0xFF111111),
    opKey = Color(0xFFDDF3E6), opText = Color(0xFF00693A),
    equalsKey = Color(0xFF00A85A), equalsText = Color(0xFFFFFFFF),
    accent = Color(0xFF00793F),
)

val DarkColors = CalcColors(
    isDark = true,
    background = Color(0xFF000000), surface = Color(0xFF1C1C1E), line = Color(0xFF2C2C2E),
    text = Color(0xFFF2F2F2), dim = Color(0xFF98989F),
    digitKey = Color(0xFF1F1F21), digitText = Color(0xFFFFFFFF),
    sciKey = Color(0xFF161617), sciText = Color(0xFFE6E6E8),
    utilKey = Color(0xFF2C2C2E), utilText = Color(0xFFFFFFFF),
    opKey = Color(0xFF12301F), opText = Color(0xFF4FE08C),
    equalsKey = Color(0xFF2FD27A), equalsText = Color(0xFF00210F),
    accent = Color(0xFF4FE08C),
)

val LocalCalcColors = staticCompositionLocalOf { LightColors }

@Composable
fun CalculatorTheme(choice: ThemeChoice, content: @Composable () -> Unit) {
    val dark = when (choice) {
        ThemeChoice.System -> isSystemInDarkTheme()
        ThemeChoice.Light -> false
        ThemeChoice.Dark -> true
    }
    val colors = if (dark) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // The app draws behind both bars, so only the icon tint and the 3-button scrim follow the theme.
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
            window.isNavigationBarContrastEnforced = false
        }
    }

    val scheme = if (dark) {
        darkColorScheme(primary = colors.accent, onPrimary = colors.equalsText, background = colors.background,
            surface = colors.surface, onSurface = colors.text, onSurfaceVariant = colors.dim, outline = colors.line)
    } else {
        lightColorScheme(primary = colors.accent, onPrimary = colors.equalsText, background = colors.background,
            surface = colors.surface, onSurface = colors.text, onSurfaceVariant = colors.dim, outline = colors.line)
    }
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(LocalCalcColors provides colors, content = content)
    }
}
