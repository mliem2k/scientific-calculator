package com.mliem.scientific_calculator.ui

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role as SemanticsRole
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mliem.scientific_calculator.R
import com.mliem.scientific_calculator.engine.CalculatorState
import com.mliem.scientific_calculator.engine.Key

private enum class Role { Digit, Util, Op, Equals, Sci }

/** One key and what it turns into under shift or hyp. */
private class KeyDef(
    val key: Key,
    val label: String,
    val role: Role,
    @param:StringRes val name: Int? = null,
    val shiftKey: Key? = null,
    val shiftLabel: String? = null,
    @param:StringRes val shiftName: Int? = null,
    val hypKey: Key? = null,
    val hypLabel: String? = null,
    @param:StringRes val hypName: Int? = null,
    val wide: Boolean = false,
)

private fun sci(key: Key, label: String, name: Int, shiftKey: Key? = null, shiftLabel: String? = null, shiftName: Int? = null,
                hypKey: Key? = null, hypLabel: String? = null, hypName: Int? = null) =
    KeyDef(key, label, Role.Sci, name, shiftKey, shiftLabel, shiftName, hypKey, hypLabel, hypName)

private val SCIENTIFIC = listOf(
    listOf(
        sci(Key.Shift, "shift", R.string.key_shift),
        sci(Key.Hyp, "hyp", R.string.key_hyperbolic),
        sci(Key.Pi, "π", R.string.key_pi),
        sci(Key.E, "e", R.string.key_e, Key.ExpFn, "eˣ", R.string.key_e_shift),
    ),
    listOf(
        sci(Key.Ans, "Ans", R.string.key_answer),
        sci(Key.Store, "Store", R.string.key_store, Key.Paste, "Paste", R.string.key_store_shift),
        sci(Key.Power, "xʸ", R.string.key_power, Key.NthRoot, "ⁿ√", R.string.key_power_shift),
        sci(Key.TimesTenPower, "×10ˣ", R.string.key_times_ten),
    ),
    listOf(
        sci(Key.Sin, "sin", R.string.key_sin, Key.Asin, "sin⁻¹", R.string.key_sin_shift, Key.Sinh, "sinh", R.string.key_sin_hyp),
        sci(Key.Cos, "cos", R.string.key_cos, Key.Acos, "cos⁻¹", R.string.key_cos_shift, Key.Cosh, "cosh", R.string.key_cos_hyp),
        sci(Key.Tan, "tan", R.string.key_tan, Key.Atan, "tan⁻¹", R.string.key_tan_shift, Key.Tanh, "tanh", R.string.key_tan_hyp),
        sci(Key.Abs, "|x|", R.string.key_abs),
    ),
    listOf(
        sci(Key.Log, "log", R.string.key_log, Key.MixedToggle, "a b/c", R.string.key_log_shift),
        sci(Key.Ln, "ln", R.string.key_ln, Key.FractionToggle, "F⇄D", R.string.key_ln_shift),
        sci(Key.Sqrt, "√", R.string.key_sqrt, Key.Cbrt, "³√", R.string.key_sqrt_shift),
        sci(Key.Factorial, "x!", R.string.key_factorial, Key.MemA, "A", R.string.key_variable_a),
    ),
    listOf(
        sci(Key.Square, "x²", R.string.key_square, Key.MemX, "X", R.string.key_variable_x),
        sci(Key.Cube, "x³", R.string.key_cube, Key.MemB, "B", R.string.key_variable_b),
        sci(Key.ParenOpen, "(", R.string.key_paren_open),
        sci(Key.ParenClose, ")", R.string.key_paren_close, Key.MemY, "Y", R.string.key_variable_y),
    ),
)

private fun digit(key: Key) = KeyDef(key, key.digit.toString(), Role.Digit)

private val PAD = listOf(
    listOf(
        KeyDef(Key.AllClear, "AC", Role.Util, R.string.key_all_clear),
        KeyDef(Key.Delete, "⌫", Role.Util, R.string.key_delete),
        KeyDef(Key.Fraction, "a/b", Role.Util, R.string.key_fraction, Key.Mixed, "mix", R.string.key_fraction_shift),
        KeyDef(Key.Divide, "÷", Role.Op, R.string.key_divide, Key.Ncr, "nCr", R.string.key_combination),
    ),
    listOf(
        digit(Key.D7), digit(Key.D8), digit(Key.D9),
        KeyDef(Key.Times, "×", Role.Op, R.string.key_multiply, Key.Npr, "nPr", R.string.key_permutation),
    ),
    listOf(digit(Key.D4), digit(Key.D5), digit(Key.D6), KeyDef(Key.Minus, "−", Role.Op, R.string.key_minus)),
    listOf(digit(Key.D1), digit(Key.D2), digit(Key.D3), KeyDef(Key.Plus, "+", Role.Op, R.string.key_plus)),
    listOf(
        KeyDef(Key.D0, "0", Role.Digit, wide = true),
        KeyDef(Key.Dot, ".", Role.Digit, R.string.key_decimal_point),
        KeyDef(Key.Equals, "=", Role.Equals, R.string.key_equals, Key.FractionToggle, "S⇄D", R.string.key_equals_shift),
    ),
)

private val Gap = 8.dp
private val SciGap = 6.dp

@Composable
fun Keypad(state: CalculatorState, haptics: Boolean, onKey: (Key) -> Unit, modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    fun press(def: KeyDef) {
        if (haptics) haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
        val key = when {
            state.shiftActive -> def.shiftKey ?: def.key
            state.hypActive -> def.hypKey ?: def.key
            else -> def.key
        }
        onKey(key)
    }

    BoxWithConstraints(modifier.padding(horizontal = 12.dp)) {
        val cell = (maxWidth - Gap * 3) / 4
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(SciGap)) {
                SCIENTIFIC.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(SciGap)) {
                        row.forEach { def ->
                            val lit = (def.key == Key.Shift && state.shiftActive) ||
                                (def.key == Key.Hyp && state.hypActive) || (def.key == Key.Store && state.stoMode)
                            CalcKey(def, state, lit, Modifier.weight(1f).heightIn(min = 40.dp), compact = true) { press(def) }
                        }
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Gap)) {
                PAD.forEach { row ->
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(Gap)) {
                        row.forEach { def ->
                            val width: Dp = if (def.wide) cell * 2 + Gap else cell
                            CalcKey(def, state, false, Modifier.width(width).fillMaxHeight(), compact = false) { press(def) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalcKey(def: KeyDef, state: CalculatorState, lit: Boolean, modifier: Modifier, compact: Boolean, onClick: () -> Unit) {
    val colors = LocalCalcColors.current
    val label = when {
        state.shiftActive && def.shiftLabel != null -> def.shiftLabel
        state.hypActive && def.hypLabel != null -> def.hypLabel
        else -> def.label
    }
    val nameRes = when {
        state.shiftActive && def.shiftName != null -> def.shiftName
        state.hypActive && def.hypName != null -> def.hypName
        else -> def.name
    }
    val description = if (nameRes != null) stringResource(nameRes) else label
    val (container, content) = when {
        lit -> colors.text to colors.background
        def.role == Role.Digit -> colors.digitKey to colors.digitText
        def.role == Role.Util -> colors.utilKey to colors.utilText
        def.role == Role.Op -> colors.opKey to colors.opText
        def.role == Role.Equals -> colors.equalsKey to colors.equalsText
        else -> colors.sciKey to colors.sciText
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, label = "keyPress")
    val toggle = def.key == Key.Shift || def.key == Key.Hyp || def.key == Key.Store
    val on = stringResource(R.string.state_on)
    val off = stringResource(R.string.state_off)

    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(if (compact) 12.dp else 22.dp))
            .background(container)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .semantics {
                contentDescription = description
                role = SemanticsRole.Button
                if (toggle) stateDescription = if (lit) on else off
            },
        contentAlignment = if (def.wide) Alignment.CenterStart else Alignment.Center,
    ) {
        if (def.key == Key.Fraction) {
            FractionGlyph(mixed = state.shiftActive, color = content)
        } else {
            BasicText(
                label,
                modifier = if (def.wide) Modifier.padding(start = 30.dp) else Modifier,
                style = TextStyle(
                    color = content,
                    fontSize = (if (compact) compactSize(label) else padSize(label)).sp,
                    fontWeight = if (compact) FontWeight.Medium else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

private fun compactSize(label: String) = when {
    label.length > 7 -> 14
    label.length > 5 -> 16
    else -> 18
}

private fun padSize(label: String) = when {
    label.length > 2 -> 20
    label.length > 1 -> 26
    else -> 32
}

/** Textbook fraction glyph: a box over a bar over a box, with a whole part box when [mixed]. */
@Composable
private fun FractionGlyph(mixed: Boolean, color: Color) {
    Canvas(Modifier.size(width = if (mixed) 38.dp else 26.dp, height = 30.dp)) {
        val stroke = Stroke(1.8.dp.toPx())
        val corner = CornerRadius(2.dp.toPx())
        val box = Size(12.dp.toPx(), 9.dp.toPx())
        val left = if (mixed) 14.dp.toPx() else 0f
        val centerX = left + 8.dp.toPx()
        if (mixed) drawRoundRect(color, Offset(0f, 7.dp.toPx()), Size(10.dp.toPx(), 16.dp.toPx()), corner, stroke)
        drawRoundRect(color, Offset(centerX - box.width / 2, 1.dp.toPx()), box, corner, stroke)
        drawLine(color, Offset(left, 15.dp.toPx()), Offset(left + 16.dp.toPx(), 15.dp.toPx()), 1.8.dp.toPx())
        drawRoundRect(color, Offset(centerX - box.width / 2, 20.dp.toPx()), box, corner, stroke)
    }
}
