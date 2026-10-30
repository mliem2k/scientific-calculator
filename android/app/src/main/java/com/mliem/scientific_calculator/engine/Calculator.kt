package com.mliem.scientific_calculator.engine

import java.util.UUID

enum class ResultMode { Decimal, Fraction }

data class HistoryEntry(val id: String, val expression: List<Node>, val result: String, val timestamp: Long)

/** Every key the calculator reacts to, whether it is on the keypad, behind shift or hyp, or the caret pad. */
enum class Key(val digit: Char? = null) {
    D0('0'), D1('1'), D2('2'), D3('3'), D4('4'), D5('5'), D6('6'), D7('7'), D8('8'), D9('9'),
    Dot, Plus, Minus, Times, Divide, Fraction, Mixed, Power, Square, Cube, Sqrt, Cbrt, NthRoot,
    Factorial, Percent, DoubleZero, Ncr, Npr, ParenOpen, ParenClose, Pi, E, Ans, TimesTenPower,
    Sin, Cos, Tan, Asin, Acos, Atan, Sinh, Cosh, Tanh, Log, Ln, Pow10, ExpFn, Abs,
    Delete, AllClear, Left, Right, Up, Down, Equals,
    Shift, Hyp, DegRad, Store, MemA, MemB, MemX, MemY,
    FractionToggle, MixedToggle,
}

data class CalculatorState(
    val expression: List<Node> = emptyList(),
    val cursor: Cursor = Cursor(),
    val result: String? = null,
    val resultMode: ResultMode = ResultMode.Decimal,
    val angleMode: AngleMode = AngleMode.Deg,
    val shiftActive: Boolean = false,
    val hypActive: Boolean = false,
    val stoMode: Boolean = false,
    val memory: Memory = Memory(),
    /** The last shown answer, which the Ans key reads. */
    val lastResult: String? = null,
    val history: List<HistoryEntry> = emptyList(),
)

const val MAX_HISTORY = 50
const val DIVIDE_BY_ZERO = "Can't divide by 0"
const val NOT_A_NUMBER = "Not a number"
const val INFINITE_VALUE = "Infinite value"

/** What the result line shows for a failed calculation. */
fun MathErrorReason.text() = when (this) {
    MathErrorReason.DivideByZero -> DIVIDE_BY_ZERO
    MathErrorReason.NotANumber -> NOT_A_NUMBER
    MathErrorReason.Infinite -> INFINITE_VALUE
}

private val functionKeys = mapOf(
    Key.Sin to FunctionName.Sin, Key.Cos to FunctionName.Cos, Key.Tan to FunctionName.Tan,
    Key.Asin to FunctionName.Asin, Key.Acos to FunctionName.Acos, Key.Atan to FunctionName.Atan,
    Key.Sinh to FunctionName.Sinh, Key.Cosh to FunctionName.Cosh, Key.Tanh to FunctionName.Tanh,
    Key.Log to FunctionName.Log, Key.Ln to FunctionName.Ln, Key.Pow10 to FunctionName.Pow10, Key.ExpFn to FunctionName.Exp, Key.Abs to FunctionName.Abs,
)

private val memoryKeys = mapOf(
    Key.MemA to ConstantName.A, Key.MemB to ConstantName.B, Key.MemX to ConstantName.X, Key.MemY to ConstantName.Y,
)

/** Where the next history entry gets its id and time, replaceable in tests. */
class EntryFactory(
    val newId: () -> String = { UUID.randomUUID().toString() },
    val now: () -> Long = System::currentTimeMillis,
)

fun CalculatorState.press(key: Key, precision: Int = 0, entries: EntryFactory = EntryFactory()): CalculatorState {
    val settled = copy(shiftActive = false, hypActive = false, stoMode = false)
    return when (key) {
        Key.Shift -> copy(shiftActive = !shiftActive, hypActive = false)
        Key.Hyp -> copy(hypActive = !hypActive, shiftActive = false)
        Key.DegRad -> copy(
            angleMode = if (angleMode == AngleMode.Deg) AngleMode.Rad else AngleMode.Deg,
            shiftActive = false,
        ).evaluated(expression, cursor, precision)
        Key.Store -> copy(stoMode = true, shiftActive = false, hypActive = false)
        Key.MemA, Key.MemB, Key.MemX, Key.MemY -> pressMemory(checkNotNull(memoryKeys[key]), precision)
        Key.FractionToggle -> toggleFraction(settled)
        Key.MixedToggle -> toggleMixed(settled)
        Key.DoubleZero -> press(Key.D0, precision, entries).press(Key.D0, precision, entries)
        Key.Left -> copy(cursor = Navigator.left(expression, cursor))
        Key.Right -> copy(cursor = Navigator.right(expression, cursor))
        Key.Up -> copy(cursor = Navigator.up(expression, cursor))
        Key.Down -> copy(cursor = Navigator.down(expression, cursor))
        Key.Equals -> settled.commit(precision, entries)
        else -> {
            val edit = edit(key) ?: return settled
            settled.evaluated(edit.expression, edit.cursor, precision)
        }
    }
}

/** Inserts pasted nodes at the caret. */
fun CalculatorState.pasteNodes(nodes: List<Node>, precision: Int = 0): CalculatorState {
    val updated = expression.updateAt(cursor.path) { list ->
        list.toMutableList().also { it.addAll(cursor.insertAt, nodes) }
    }
    return copy(shiftActive = false, hypActive = false, stoMode = false)
        .evaluated(updated, cursor.copy(insertAt = cursor.insertAt + nodes.size), precision)
}

/** Loads a history entry back into the editor with the caret at its end. */
fun CalculatorState.restore(entry: HistoryEntry) = copy(
    expression = entry.expression,
    cursor = Cursor(emptyList(), entry.expression.size),
    result = entry.result,
    resultMode = ResultMode.Decimal,
    shiftActive = false,
    hypActive = false,
    stoMode = false,
)

private fun CalculatorState.edit(key: Key): Edit? {
    key.digit?.let { return Editor.digit(expression, cursor, it) }
    functionKeys[key]?.let { return Editor.function(expression, cursor, it) }
    return when (key) {
        Key.Dot -> Editor.decimalPoint(expression, cursor)
        Key.Plus -> Editor.operator(expression, cursor, Operator.Plus)
        Key.Minus -> Editor.operator(expression, cursor, Operator.Minus)
        Key.Times -> Editor.operator(expression, cursor, Operator.Times)
        Key.Divide -> Editor.operator(expression, cursor, Operator.Divide)
        Key.Fraction -> Editor.fraction(expression, cursor)
        Key.Mixed -> Editor.mixed(expression, cursor)
        Key.Power -> Editor.power(expression, cursor)
        Key.Square -> Editor.square(expression, cursor)
        Key.Cube -> Editor.cube(expression, cursor)
        Key.Sqrt -> Editor.squareRoot(expression, cursor)
        Key.Cbrt -> Editor.cubeRoot(expression, cursor)
        Key.NthRoot -> Editor.nthRoot(expression, cursor)
        Key.Factorial -> Editor.factorial(expression, cursor)
        Key.Percent -> Editor.percent(expression, cursor)
        Key.Ncr -> Editor.combinations(expression, cursor)
        Key.Npr -> Editor.permutations(expression, cursor)
        Key.ParenOpen -> Editor.openParen(expression, cursor)
        Key.ParenClose -> Editor.closeParen(expression, cursor)
        Key.Pi -> Editor.constant(expression, cursor, ConstantName.Pi)
        Key.E -> Editor.constant(expression, cursor, ConstantName.E)
        Key.Ans -> Editor.constant(expression, cursor, ConstantName.Ans)
        Key.TimesTenPower -> Editor.timesTenPower(expression, cursor)
        Key.Delete -> Editor.delete(expression, cursor)
        Key.AllClear -> Editor.clear()
        else -> null
    }
}

/** The live result of [expression], shown under the expression while typing. */
private fun CalculatorState.evaluated(expression: List<Node>, cursor: Cursor, precision: Int): CalculatorState {
    val shown = when (val outcome = evaluate(expression, angleMode, answer(), memory)) {
        is Evaluation.Value -> formatNumber(outcome.number, precision)
        is Evaluation.MathError -> outcome.reason.text()
        Evaluation.Incomplete -> null
    }
    return copy(expression = expression, cursor = cursor, result = shown, resultMode = ResultMode.Decimal)
}

private fun CalculatorState.answer() = lastResult?.toDoubleOrNull() ?: 0.0

private fun CalculatorState.commit(precision: Int, entries: EntryFactory): CalculatorState {
    val outcome = evaluate(expression, angleMode, answer(), memory)
    if (outcome is Evaluation.MathError) return copy(result = outcome.reason.text(), resultMode = ResultMode.Decimal)
    if (outcome !is Evaluation.Value) return this
    val text = formatNumber(outcome.number, precision)
    val entry = HistoryEntry(entries.newId(), expression, text, entries.now())
    return copy(
        history = (listOf(entry) + history).take(MAX_HISTORY),
        result = text,
        resultMode = ResultMode.Decimal,
        lastResult = text,
    )
}

private fun CalculatorState.pressMemory(name: ConstantName, precision: Int): CalculatorState {
    val settled = copy(shiftActive = false, hypActive = false, stoMode = false)
    if (stoMode) {
        val value = result?.toDoubleOrNull() ?: return settled
        return settled.copy(memory = memory.with(name, value))
    }
    val edit = Editor.constant(expression, cursor, name)
    return settled.evaluated(edit.expression, edit.cursor, precision)
}

private fun CalculatorState.toggleFraction(settled: CalculatorState): CalculatorState {
    val shown = result ?: return settled
    if (resultMode == ResultMode.Fraction) return settled.copy(result = lastResult, resultMode = ResultMode.Decimal)
    val fraction = toFraction(shown) ?: return settled
    return settled.copy(lastResult = shown, result = fraction, resultMode = ResultMode.Fraction)
}

private fun CalculatorState.toggleMixed(settled: CalculatorState): CalculatorState {
    val shown = result ?: return settled
    val converted = toMixed(shown) ?: fromMixed(shown) ?: return settled
    return settled.copy(result = converted, resultMode = ResultMode.Fraction)
}
