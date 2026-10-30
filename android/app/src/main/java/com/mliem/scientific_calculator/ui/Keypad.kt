package com.mliem.scientific_calculator.ui

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mliem.scientific_calculator.R
import com.mliem.scientific_calculator.engine.CalculatorState
import com.mliem.scientific_calculator.engine.Key

private enum class Role { Digit, Func, Equals, Sci }

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
        sci(Key.Ans, "Ans", R.string.key_answer),
        sci(Key.Store, "Store", R.string.key_store, Key.Paste, "Paste", R.string.key_store_shift),
    ),
    listOf(
        sci(Key.Sin, "sin", R.string.key_sin, Key.Asin, "sin⁻¹", R.string.key_sin_shift, Key.Sinh, "sinh", R.string.key_sin_hyp),
        sci(Key.Cos, "cos", R.string.key_cos, Key.Acos, "cos⁻¹", R.string.key_cos_shift, Key.Cosh, "cosh", R.string.key_cos_hyp),
        sci(Key.Tan, "tan", R.string.key_tan, Key.Atan, "tan⁻¹", R.string.key_tan_shift, Key.Tanh, "tanh", R.string.key_tan_hyp),
        sci(Key.Log, "log", R.string.key_log, Key.MixedToggle, "a b/c", R.string.key_log_shift),
        sci(Key.Ln, "ln", R.string.key_ln, Key.FractionToggle, "F⇄D", R.string.key_ln_shift),
        sci(Key.Abs, "|x|", R.string.key_abs),
    ),
    listOf(
        sci(Key.Sqrt, "√", R.string.key_sqrt, Key.Cbrt, "³√", R.string.key_sqrt_shift),
        sci(Key.Square, "x²", R.string.key_square, Key.MemX, "X", R.string.key_variable_x),
        sci(Key.Cube, "x³", R.string.key_cube, Key.MemB, "B", R.string.key_variable_b),
        sci(Key.Power, "xʸ", R.string.key_power, Key.NthRoot, "ⁿ√", R.string.key_power_shift),
        sci(Key.Factorial, "x!", R.string.key_factorial, Key.MemA, "A", R.string.key_variable_a),
        sci(Key.TimesTenPower, "×10ˣ", R.string.key_times_ten),
    ),
    listOf(
        sci(Key.ParenOpen, "(", R.string.key_paren_open),
        sci(Key.ParenClose, ")", R.string.key_paren_close, Key.MemY, "Y", R.string.key_variable_y),
        sci(Key.Fraction, "a/b", R.string.key_fraction, Key.Mixed, "mix", R.string.key_fraction_shift),
    ),
)

private fun digit(key: Key) = KeyDef(key, key.digit.toString(), Role.Digit)

private val PAD = listOf(
    listOf(
        KeyDef(Key.AllClear, "AC", Role.Func, R.string.key_all_clear),
        KeyDef(Key.Percent, "%", Role.Func, R.string.key_percent),
        KeyDef(Key.Delete, "⌫", Role.Func, R.string.key_delete),
        KeyDef(Key.Divide, "÷", Role.Func, R.string.key_divide, Key.Ncr, "nCr", R.string.key_combination),
    ),
    listOf(
        digit(Key.D7), digit(Key.D8), digit(Key.D9),
        KeyDef(Key.Times, "×", Role.Func, R.string.key_multiply, Key.Npr, "nPr", R.string.key_permutation),
    ),
    listOf(digit(Key.D4), digit(Key.D5), digit(Key.D6), KeyDef(Key.Minus, "−", Role.Func, R.string.key_minus)),
    listOf(digit(Key.D1), digit(Key.D2), digit(Key.D3), KeyDef(Key.Plus, "+", Role.Func, R.string.key_plus)),
    listOf(
        KeyDef(Key.DoubleZero, "00", Role.Digit, R.string.key_double_zero),
        digit(Key.D0),
        KeyDef(Key.Dot, ".", Role.Digit, R.string.key_decimal_point),
        KeyDef(Key.Equals, "=", Role.Equals, R.string.key_equals, Key.FractionToggle, "S⇄D", R.string.key_equals_shift),
    ),
)

@Composable
private fun rememberPress(state: CalculatorState, haptics: Boolean, onKey: (Key) -> Unit): (KeyDef) -> Unit {
    val haptic = LocalHapticFeedback.current
    return { def ->
        if (haptics) haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
        onKey(
            when {
                state.shiftActive -> def.shiftKey ?: def.key
                state.hypActive -> def.hypKey ?: def.key
                else -> def.key
            },
        )
    }
}

/** The scientific keys, shown above the pad while the display's toggle is on. */
@Composable
fun SciPanel(state: CalculatorState, haptics: Boolean, onKey: (Key) -> Unit, modifier: Modifier = Modifier) {
    val press = rememberPress(state, haptics, onKey)
    Column(modifier.padding(horizontal = 16.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SCIENTIFIC.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { def ->
                    val lit = (def.key == Key.Shift && state.shiftActive) ||
                        (def.key == Key.Hyp && state.hypActive) || (def.key == Key.Store && state.stoMode)
                    val weight = if (row.size < 6) 2f else 1f
                    CalcKey(def, state, lit, Modifier.weight(weight).heightIn(min = 40.dp), CircleShape) { press(def) }
                }
            }
        }
    }
}

/** Five rows of round keys. The circles are as wide as the columns allow and shrink when height is short. */
@Composable
fun NumberPad(state: CalculatorState, haptics: Boolean, onKey: (Key) -> Unit, modifier: Modifier = Modifier) {
    val press = rememberPress(state, haptics, onKey)
    val gap = 14.dp
    BoxWithConstraints(modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp)) {
        val diameter = minOf((maxWidth - gap * 3) / 4, (maxHeight - gap * 4) / 5)
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            PAD.forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    row.forEach { def -> CalcKey(def, state, false, Modifier.size(diameter), CircleShape) { press(def) } }
                }
            }
        }
    }
}

@Composable
private fun CalcKey(def: KeyDef, state: CalculatorState, lit: Boolean, modifier: Modifier, shape: Shape, onClick: () -> Unit) {
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
    val sci = def.role == Role.Sci
    val brush: Brush
    val content: Color
    when {
        lit -> { brush = Brush.verticalGradient(listOf(colors.accent, colors.accent)); content = colors.background }
        def.role == Role.Digit -> { brush = Brush.verticalGradient(listOf(colors.digitTop, colors.digitBottom)); content = colors.text }
        def.role == Role.Func -> { brush = Brush.verticalGradient(listOf(colors.funcTop, colors.funcBottom)); content = colors.text }
        def.role == Role.Equals -> { brush = Brush.verticalGradient(listOf(colors.equalsTop, colors.equalsBottom)); content = colors.equalsText }
        else -> { brush = Brush.verticalGradient(listOf(colors.sciKey, colors.sciKey)); content = colors.sciText }
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.93f else 1f, label = "keyPress")
    val toggle = def.key == Key.Shift || def.key == Key.Hyp || def.key == Key.Store
    val on = stringResource(R.string.state_on)
    val off = stringResource(R.string.state_off)

    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(brush)
            .border(1.dp, Brush.verticalGradient(listOf(colors.rim, Color.Transparent)), shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .semantics {
                contentDescription = description
                role = SemanticsRole.Button
                if (toggle) stateDescription = if (lit) on else off
            },
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            label,
            style = TextStyle(
                color = content,
                fontSize = (if (sci) sciSize(label) else padSize(label)).sp,
                fontWeight = when {
                    sci -> FontWeight.Medium
                    label == "AC" || label == "%" -> FontWeight.Bold
                    else -> FontWeight.Light
                },
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
            softWrap = false,
        )
    }
}

private fun sciSize(label: String) = if (label.length > 4) 13 else 16

private fun padSize(label: String) = when {
    label == "AC" -> 24
    label == "%" -> 26
    label.length > 1 -> 34
    else -> 38
}
