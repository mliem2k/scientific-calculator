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

/** Glossy round keys on a flat background with a pink equals key: the ColorOS calculator look. */
@Immutable
class CalcColors(
    val isDark: Boolean,
    val background: Color,
    /** Grouped rows in the history and settings screens. */
    val surface: Color,
    val line: Color,
    val text: Color,
    val dim: Color,
    /** Gradients run top to bottom; the rim is the light edge along the top of a key. */
    val digitTop: Color,
    val digitBottom: Color,
    val funcTop: Color,
    val funcBottom: Color,
    val equalsTop: Color,
    val equalsBottom: Color,
    val equalsText: Color,
    val rim: Color,
    /** Flat keys of the scientific panel. */
    val sciKey: Color,
    val sciText: Color,
    /** Caret, links and the active state. */
    val accent: Color,
)

val LightColors = CalcColors(
    isDark = false,
    background = Color(0xFFF2F2F5), surface = Color(0xFFFFFFFF), line = Color(0xFFE3E3E8),
    text = Color(0xFF111111), dim = Color(0xFF6F6F76),
    digitTop = Color(0xFFFFFFFF), digitBottom = Color(0xFFF1F1F5),
    funcTop = Color(0xFFE9E9EE), funcBottom = Color(0xFFDADAE1),
    equalsTop = Color(0xFFF06CE6), equalsBottom = Color(0xFFD42FBF), equalsText = Color(0xFFFFFFFF),
    rim = Color(0xCCFFFFFF),
    sciKey = Color(0xFFE6E6EB), sciText = Color(0xFF2B2B2D),
    accent = Color(0xFFB8239F),
)

val DarkColors = CalcColors(
    isDark = true,
    background = Color(0xFF18181A), surface = Color(0xFF242426), line = Color(0xFF343437),
    text = Color(0xFFF5F5F7), dim = Color(0xFF9A9AA0),
    digitTop = Color(0xFF48494C), digitBottom = Color(0xFF343538),
    funcTop = Color(0xFF36373A), funcBottom = Color(0xFF27282A),
    equalsTop = Color(0xFFF58BEE), equalsBottom = Color(0xFFE53DD0), equalsText = Color(0xFFFFFFFF),
    rim = Color(0x33FFFFFF),
    sciKey = Color(0xFF2A2B2E), sciText = Color(0xFFE8E8EA),
    accent = Color(0xFFF08AEA),
)

/**
 * Dark with a pure black background, so unlit pixels stay off on an OLED panel. Digit keys sit well above
 * the black, operator keys sink toward it, and the cards are only a step lighter than the background.
 */
val AmoledColors = CalcColors(
    isDark = true,
    background = Color(0xFF000000), surface = Color(0xFF121214), line = Color(0xFF26262A),
    text = Color(0xFFFFFFFF), dim = Color(0xFF8E8E95),
    digitTop = Color(0xFF34353A), digitBottom = Color(0xFF1F2023),
    funcTop = Color(0xFF1F2023), funcBottom = Color(0xFF111113),
    equalsTop = Color(0xFFF58BEE), equalsBottom = Color(0xFFE23FCD), equalsText = Color(0xFFFFFFFF),
    rim = Color(0x40FFFFFF),
    sciKey = Color(0xFF18181B), sciText = Color(0xFFEDEDF0),
    accent = Color(0xFFF58BEE),
)

/** The palette a choice resolves to; System follows [systemDark]. */
fun colorsFor(choice: ThemeChoice, systemDark: Boolean) = when (choice) {
    ThemeChoice.System -> if (systemDark) DarkColors else LightColors
    ThemeChoice.Light -> LightColors
    ThemeChoice.Dark -> DarkColors
    ThemeChoice.Amoled -> AmoledColors
}

val LocalCalcColors = staticCompositionLocalOf { LightColors }

@Composable
fun CalculatorTheme(choice: ThemeChoice, content: @Composable () -> Unit) {
    val colors = colorsFor(choice, isSystemInDarkTheme())
    val dark = colors.isDark

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
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
