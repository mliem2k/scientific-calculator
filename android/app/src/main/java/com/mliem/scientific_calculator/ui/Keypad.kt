package com.mliem.scientific_calculator.ui

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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

enum class KeyRole { Digit, Func, Equals, Sci }

/**
 * A round glossy key. [active] fills it with the accent (a toggle that is on).
 */
@Composable
fun GlossyKey(
    label: String,
    role: KeyRole,
    description: String,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    toggleState: String? = null,
    glyph: (@Composable (Color) -> Unit)? = null,
    onClick: () -> Unit,
) {
    val colors = LocalCalcColors.current
    val brush: Brush
    val content: Color
    when {
        active -> { brush = Brush.verticalGradient(listOf(colors.accent, colors.accent)); content = colors.background }
        role == KeyRole.Digit -> { brush = Brush.verticalGradient(listOf(colors.digitTop, colors.digitBottom)); content = colors.text }
        role == KeyRole.Func -> { brush = Brush.verticalGradient(listOf(colors.funcTop, colors.funcBottom)); content = colors.text }
        role == KeyRole.Equals -> { brush = Brush.verticalGradient(listOf(colors.equalsTop, colors.equalsBottom)); content = colors.equalsText }
        else -> { brush = Brush.verticalGradient(listOf(colors.sciKey, colors.sciKey)); content = colors.sciText }
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.93f else 1f, label = "keyPress")

    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(brush)
            .border(1.dp, Brush.verticalGradient(listOf(colors.rim, Color.Transparent)), CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .semantics {
                contentDescription = description
                this.role = SemanticsRole.Button
                if (toggleState != null) stateDescription = toggleState
            },
        contentAlignment = Alignment.Center,
    ) {
        if (glyph != null) {
            glyph(content)
        } else {
            BasicText(
                label,
                style = TextStyle(
                    color = content,
                    fontSize = keyTextSize(label, role).sp,
                    fontWeight = when {
                        role == KeyRole.Sci -> FontWeight.Medium
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
}

private fun keyTextSize(label: String, role: KeyRole) = when {
    role == KeyRole.Sci -> if (label.length > 4) 15 else 19
    label == "AC" -> 22
    label == "%" -> 24
    label.length > 1 -> 30
    else -> 34
}

/** What a key turns into while shifted, hyperbolic or store is active. */
private class Variant(val key: Key, val label: String, @param:StringRes val name: Int)

private class KeyDef(
    val key: Key,
    val label: String,
    val role: KeyRole,
    @param:StringRes val name: Int? = null,
    val shifted: Variant? = null,
    val hyperbolic: Variant? = null,
    val store: Variant? = null,
)

private fun sci(key: Key, label: String, name: Int, shifted: Variant? = null, hyperbolic: Variant? = null, store: Variant? = null) =
    KeyDef(key, label, KeyRole.Sci, name, shifted, hyperbolic, store)

private fun digit(key: Key) = KeyDef(key, key.digit.toString(), KeyRole.Digit)

private val DIVIDE = KeyDef(Key.Divide, "÷", KeyRole.Func, R.string.key_divide)
private val TIMES = KeyDef(Key.Times, "×", KeyRole.Func, R.string.key_multiply, Variant(Key.Npr, "nPr", R.string.key_permutation))
private val MINUS = KeyDef(Key.Minus, "−", KeyRole.Func, R.string.key_minus)
private val PLUS = KeyDef(Key.Plus, "+", KeyRole.Func, R.string.key_plus)
private val ALL_CLEAR = KeyDef(Key.AllClear, "AC", KeyRole.Func, R.string.key_all_clear)
private val PERCENT = KeyDef(Key.Percent, "%", KeyRole.Func, R.string.key_percent)
private val DELETE = KeyDef(Key.Delete, "⌫", KeyRole.Func, R.string.key_delete)
private val DOUBLE_ZERO = KeyDef(Key.DoubleZero, "00", KeyRole.Digit, R.string.key_double_zero)
private val DOT = KeyDef(Key.Dot, ".", KeyRole.Digit, R.string.key_decimal_point)
private val EQUALS = KeyDef(Key.Equals, "=", KeyRole.Equals, R.string.key_equals, Variant(Key.FractionToggle, "S⇄D", R.string.key_equals_shift))

private val PAD = listOf(
    listOf(ALL_CLEAR, PERCENT, DELETE, DIVIDE.copyWith(Variant(Key.Ncr, "nCr", R.string.key_combination))),
    listOf(digit(Key.D7), digit(Key.D8), digit(Key.D9), TIMES),
    listOf(digit(Key.D4), digit(Key.D5), digit(Key.D6), MINUS),
    listOf(digit(Key.D1), digit(Key.D2), digit(Key.D3), PLUS),
    listOf(DOUBLE_ZERO, digit(Key.D0), DOT, EQUALS),
)

private fun KeyDef.copyWith(shifted: Variant) = KeyDef(key, label, role, name, shifted, hyperbolic, store)

/** The ColorOS scientific pad: five columns, with extra rows for the keys it does not have. */
private val SCIENTIFIC = listOf(
    listOf(
        sci(Key.Sin, "sin", R.string.key_sin, Variant(Key.Asin, "sin⁻¹", R.string.key_sin_shift), Variant(Key.Sinh, "sinh", R.string.key_sin_hyp)),
        sci(Key.Cos, "cos", R.string.key_cos, Variant(Key.Acos, "cos⁻¹", R.string.key_cos_shift), Variant(Key.Cosh, "cosh", R.string.key_cos_hyp)),
        sci(Key.Tan, "tan", R.string.key_tan, Variant(Key.Atan, "tan⁻¹", R.string.key_tan_shift), Variant(Key.Tanh, "tanh", R.string.key_tan_hyp)),
        sci(Key.ParenOpen, "(", R.string.key_paren_open),
        sci(Key.ParenClose, ")", R.string.key_paren_close),
    ),
    listOf(
        sci(Key.Log, "log", R.string.key_log, Variant(Key.Pow10, "10ˣ", R.string.key_pow10)),
        sci(Key.Ln, "ln", R.string.key_ln, Variant(Key.ExpFn, "eˣ", R.string.key_e_shift)),
        sci(Key.Pi, "π", R.string.key_pi),
        sci(Key.E, "e", R.string.key_e, Variant(Key.TimesTenPower, "×10ˣ", R.string.key_times_ten)),
        sci(Key.Shift, "shift", R.string.key_shift),
    ),
    listOf(
        sci(Key.Factorial, "!", R.string.key_factorial, Variant(Key.Ncr, "nCr", R.string.key_combination)),
        ALL_CLEAR, PERCENT, DELETE, DIVIDE,
    ),
    listOf(
        sci(Key.Power, "^", R.string.key_power, Variant(Key.NthRoot, "ⁿ√", R.string.key_power_shift)),
        digit(Key.D7), digit(Key.D8), digit(Key.D9), TIMES,
    ),
    listOf(
        sci(Key.Sqrt, "√", R.string.key_sqrt, Variant(Key.Cbrt, "³√", R.string.key_sqrt_shift)),
        digit(Key.D4), digit(Key.D5), digit(Key.D6), MINUS,
    ),
    listOf(
        sci(Key.Square, "x²", R.string.key_square, Variant(Key.Cube, "x³", R.string.key_square_shift)),
        digit(Key.D1), digit(Key.D2), digit(Key.D3), PLUS,
    ),
    listOf(
        sci(Key.Abs, "|x|", R.string.key_abs, Variant(Key.Hyp, "hyp", R.string.key_absolute_hyp)),
        DOUBLE_ZERO, digit(Key.D0), DOT, EQUALS,
    ),
    // After Sto, A or B stores the result in that variable; otherwise they insert it.
    listOf(
        sci(Key.Fraction, "", R.string.key_fraction, Variant(Key.Mixed, "", R.string.key_fraction_shift)),
        sci(Key.Ans, "Ans", R.string.key_answer, Variant(Key.MixedToggle, "a b/c", R.string.key_mixed_toggle)),
        sci(Key.Store, "Sto", R.string.key_store),
        sci(Key.MemA, "A", R.string.key_variable_a),
        sci(Key.MemB, "B", R.string.key_variable_b),
    ),
)

private class Resolved(val key: Key, val label: String, @param:StringRes val name: Int?)

private fun KeyDef.resolve(state: CalculatorState): Resolved {
    val variant = when {
        state.stoMode && store != null -> store
        state.shiftActive && shifted != null -> shifted
        state.hypActive && hyperbolic != null -> hyperbolic
        else -> null
    }
    return if (variant != null) Resolved(variant.key, variant.label, variant.name) else Resolved(key, label, name)
}

@Composable
private fun rememberKeyPress(state: CalculatorState, haptics: Boolean, speak: (String) -> Unit, onKey: (Key) -> Unit): (KeyDef, String) -> Unit {
    val haptic = LocalHapticFeedback.current
    return { def, spoken ->
        if (haptics) haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
        speak(spoken)
        onKey(def.resolve(state).key)
    }
}

@Composable
private fun CalcKey(def: KeyDef, state: CalculatorState, modifier: Modifier, onPress: (KeyDef, String) -> Unit) {
    val resolved = def.resolve(state)
    val description = resolved.name?.let { stringResource(it) } ?: resolved.label
    val lit = (def.key == Key.Shift && state.shiftActive) || (def.key == Key.Store && state.stoMode) ||
        (def.key == Key.Abs && state.hypActive)
    val toggle = def.key == Key.Shift || def.key == Key.Store
    val on = stringResource(R.string.state_on)
    val off = stringResource(R.string.state_off)
    GlossyKey(
        label = resolved.label,
        role = def.role,
        description = description,
        modifier = modifier,
        active = lit,
        toggleState = if (toggle) (if (lit) on else off) else null,
        glyph = if (def.key == Key.Fraction) { color -> FractionGlyph(mixed = state.shiftActive, color = color) } else null,
        onClick = { onPress(def, description) },
    )
}

/** Five rows of round keys, as wide as the columns allow and smaller when the height is short. */
@Composable
fun NumberPad(
    state: CalculatorState,
    haptics: Boolean,
    onKey: (Key) -> Unit,
    modifier: Modifier = Modifier,
    speak: (String) -> Unit = {},
) = KeyGrid(PAD, 4, 14.dp, state, haptics, onKey, modifier, speak)

/** The scientific layout: ColorOS rows plus a row for fractions, memory and absolute value. */
@Composable
fun ScientificPad(
    state: CalculatorState,
    haptics: Boolean,
    onKey: (Key) -> Unit,
    modifier: Modifier = Modifier,
    speak: (String) -> Unit = {},
) = KeyGrid(SCIENTIFIC, 5, 8.dp, state, haptics, onKey, modifier, speak)

@Composable
private fun KeyGrid(
    rows: List<List<KeyDef>>,
    columns: Int,
    gap: androidx.compose.ui.unit.Dp,
    state: CalculatorState,
    haptics: Boolean,
    onKey: (Key) -> Unit,
    modifier: Modifier,
    speak: (String) -> Unit,
) {
    val press = rememberKeyPress(state, haptics, speak, onKey)
    BoxWithConstraints(modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp)) {
        val diameter = minOf((maxWidth - gap * (columns - 1)) / columns, (maxHeight - gap * (rows.size - 1)) / rows.size)
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            rows.forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    row.forEach { def -> CalcKey(def, state, Modifier.size(diameter), press) }
                }
            }
        }
    }
}

/** Textbook fraction glyph: a box over a bar over a box, with a whole part box when [mixed]. */
@Composable
fun FractionGlyph(mixed: Boolean, color: Color) {
    // The canvas is exactly as wide as the drawing (a 12dp bar, plus the whole part box when mixed), so it centers.
    Canvas(Modifier.size(width = if (mixed) 23.dp else 12.dp, height = 22.dp)) {
        val stroke = Stroke(1.5.dp.toPx())
        val corner = CornerRadius(2.dp.toPx())
        val box = Size(9.dp.toPx(), 6.5.dp.toPx())
        val left = if (mixed) 11.dp.toPx() else 0f
        val barWidth = 12.dp.toPx()
        val boxLeft = left + (barWidth - box.width) / 2
        if (mixed) drawRoundRect(color, Offset(0f, 4.dp.toPx()), Size(8.dp.toPx(), 14.dp.toPx()), corner, stroke)
        drawRoundRect(color, Offset(boxLeft, 0.8.dp.toPx()), box, corner, stroke)
        drawLine(color, Offset(left, 11.dp.toPx()), Offset(left + barWidth, 11.dp.toPx()), 1.5.dp.toPx())
        drawRoundRect(color, Offset(boxLeft, 14.7.dp.toPx()), box, corner, stroke)
    }
}
