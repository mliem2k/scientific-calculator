package com.mliem.scientific_calculator.engine

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.math.tanh

enum class AngleMode { Deg, Rad }

/** The four store slots, keyed by the constant that reads them. */
data class Memory(val values: Map<ConstantName, Double> = emptyMap()) {
    operator fun get(name: ConstantName) = values[name]

    fun with(name: ConstantName, value: Double) = Memory(values + (name to value))
}

sealed interface Evaluation {
    data class Value(val number: Double) : Evaluation

    /** The expression is well formed but has no real result, for example 1 ÷ 0. */
    data object MathError : Evaluation

    /** The expression is empty or still has an open slot, so there is nothing to show yet. */
    data object Incomplete : Evaluation
}

fun evaluate(
    nodes: List<Node>,
    angleMode: AngleMode,
    answer: Double = 0.0,
    memory: Memory = Memory(),
): Evaluation {
    if (nodes.isEmpty()) return Evaluation.Incomplete
    return try {
        val value = Parser(nodes, angleMode, answer, memory).parseAll()
        if (value.isFinite()) Evaluation.Value(value) else Evaluation.MathError
    } catch (_: IncompleteExpression) {
        Evaluation.Incomplete
    }
}

private class IncompleteExpression : Exception()

/**
 * Recursive descent over the node list: + and − bind loosest, then × ÷ and implicit multiplication
 * (two adjacent operands), then a sign. Powers, fractions and roots are nodes, so they need no operator.
 */
private class Parser(
    private val nodes: List<Node>,
    private val angleMode: AngleMode,
    private val answer: Double,
    private val memory: Memory,
) {
    private var pos = 0

    fun parseAll(): Double {
        val value = sum()
        if (pos != nodes.size) throw IncompleteExpression()
        return value
    }

    private fun nested(list: List<Node>) = Parser(list, angleMode, answer, memory).parseAll()

    private fun peekOperator() = (nodes.getOrNull(pos) as? Op)?.op

    private fun sum(): Double {
        var value = product()
        while (true) {
            when (peekOperator()) {
                Operator.Plus -> { pos++; value += product() }
                Operator.Minus -> { pos++; value -= product() }
                else -> return value
            }
        }
    }

    private fun product(): Double {
        var value = signed()
        while (pos < nodes.size) {
            when (peekOperator()) {
                Operator.Times -> { pos++; value *= signed() }
                Operator.Divide -> { pos++; value /= signed() }
                null -> value *= signed()
                else -> return value
            }
        }
        return value
    }

    private fun signed(): Double = when (peekOperator()) {
        Operator.Minus -> { pos++; -signed() }
        Operator.Plus -> { pos++; signed() }
        else -> operand()
    }

    private fun operand(): Double = when (val node = nodes.getOrNull(pos++) ?: throw IncompleteExpression()) {
        is Num -> node.value.toDoubleOrNull() ?: throw IncompleteExpression()
        is Op -> throw IncompleteExpression()
        is Const -> constant(node.name)
        is Fraction -> nested(node.numerator) / nested(node.denominator)
        is Exponent -> nested(node.base).pow(nested(node.exponent))
        is Radical -> root(nested(node.radicand), if (node.degree.isEmpty()) 2.0 else nested(node.degree))
        is Func -> function(node.name, nested(node.argument))
        is Factorial -> factorial(nested(node.operand))
        is Percent -> nested(node.operand) / 100
        is Ncr -> combinations(nested(node.n), nested(node.r))
        is Npr -> permutations(nested(node.n), nested(node.r))
        is Paren -> nested(node.children)
    }

    private fun constant(name: ConstantName) = when (name) {
        ConstantName.Pi -> PI
        ConstantName.E -> Math.E
        ConstantName.Ans -> answer
        else -> memory[name] ?: 0.0
    }

    private fun toRadians(x: Double) = if (angleMode == AngleMode.Deg) x * PI / 180 else x

    private fun fromRadians(x: Double) = if (angleMode == AngleMode.Deg) x * 180 / PI else x

    /** Sine that is exactly zero at multiples of 180 degrees instead of 1.2e-16. */
    private fun sine(x: Double): Double =
        if (angleMode == AngleMode.Deg && x % 180.0 == 0.0) 0.0 else sin(toRadians(x))

    private fun cosine(x: Double): Double =
        if (angleMode == AngleMode.Deg && (x - 90.0) % 180.0 == 0.0) 0.0 else cos(toRadians(x))

    private fun function(name: FunctionName, x: Double): Double = when (name) {
        FunctionName.Sin -> sine(x)
        FunctionName.Cos -> cosine(x)
        FunctionName.Tan -> sine(x) / cosine(x)
        FunctionName.Asin -> fromRadians(asin(x))
        FunctionName.Acos -> fromRadians(acos(x))
        FunctionName.Atan -> fromRadians(atan(x))
        FunctionName.Sinh -> sinh(x)
        FunctionName.Cosh -> cosh(x)
        FunctionName.Tanh -> tanh(x)
        FunctionName.Log -> log10(x)
        FunctionName.Ln -> ln(x)
        FunctionName.Exp -> exp(x)
        FunctionName.Abs -> abs(x)
    }

    private fun root(x: Double, degree: Double): Double {
        val oddDegree = degree == floor(degree) && degree % 2.0 != 0.0
        return if (x < 0 && oddDegree) -((-x).pow(1 / degree)) else x.pow(1 / degree)
    }

    private fun isWhole(x: Double) = x == floor(x)

    private fun factorial(x: Double): Double = when {
        x < 0 && isWhole(x) -> Double.NaN
        isWhole(x) -> (2..x.toInt().coerceAtMost(171)).fold(1.0) { acc, i -> acc * i }
        else -> gamma(x + 1)
    }

    /** Lanczos approximation; the reflection formula covers arguments below one half. */
    private fun gamma(x: Double): Double {
        if (x < 0.5) return PI / (sin(PI * x) * gamma(1 - x))
        val g = 7.0
        val coefficients = doubleArrayOf(
            0.99999999999980993, 676.5203681218851, -1259.1392167224028, 771.32342877765313,
            -176.61502916214059, 12.507343278686905, -0.13857109526572012,
            9.9843695780195716e-6, 1.5056327351493116e-7,
        )
        val shifted = x - 1
        var sum = coefficients[0]
        for (i in 1 until coefficients.size) sum += coefficients[i] / (shifted + i)
        val t = shifted + g + 0.5
        return sqrt(2 * PI) * t.pow(shifted + 0.5) * exp(-t) * sum
    }

    private fun isCount(x: Double) = x >= 0 && isWhole(x)

    private fun permutations(n: Double, r: Double): Double {
        if (!isCount(n) || !isCount(r) || r > n) return Double.NaN
        return (0 until r.toInt()).fold(1.0) { acc, i -> acc * (n - i) }
    }

    private fun combinations(n: Double, r: Double): Double {
        if (!isCount(n) || !isCount(r) || r > n) return Double.NaN
        val k = minOf(r, n - r).toInt()
        return (0 until k).fold(1.0) { acc, i -> acc * (n - i) / (i + 1) }
    }
}
