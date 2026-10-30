package com.mliem.scientific_calculator.engine

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.floor

private const val SIGNIFICANT_DIGITS = 10
private const val LARGEST_PLAIN = 1e15

/** [precision] is decimal places; 0 means automatic (10 significant digits). */
fun formatNumber(n: Double, precision: Int = 0): String = when {
    precision > 0 && abs(n) < LARGEST_PLAIN ->
        plainString(BigDecimal(n).setScale(precision, RoundingMode.HALF_UP).toDouble())
    n == floor(n) && abs(n) < LARGEST_PLAIN -> plainString(n)
    else -> plainString(BigDecimal(n).round(MathContext(SIGNIFICANT_DIGITS, RoundingMode.HALF_UP)).toDouble())
}

/** The shortest digits that round trip, in plain notation unless the value is tiny or huge. */
private fun plainString(d: Double): String {
    if (d == 0.0) return "0"
    val decimal = BigDecimal(d.toString()).stripTrailingZeros()
    val exponent = decimal.precision() - decimal.scale() - 1
    if (exponent in -6..20) return decimal.toPlainString()
    val digits = decimal.unscaledValue().abs().toString()
    val mantissa = if (digits.length > 1) "${digits[0]}.${digits.substring(1)}" else digits
    val sign = if (d < 0) "-" else ""
    return "$sign${mantissa}e${if (exponent > 0) "+" else "-"}${abs(exponent)}"
}

private const val LARGEST_DENOMINATOR = 999
private const val FRACTION_TOLERANCE = 1e-9
private val FRACTION = Regex("^(-?\\d+)/(\\d+)$")
private val MIXED = Regex("^(-?\\d+) (\\d+)/(\\d+)$")

/** The simplest fraction equal to [decimal], or null for whole numbers and denominators over 999. */
fun toFraction(decimal: String): String? {
    val x = decimal.toDoubleOrNull() ?: return null
    if (!x.isFinite() || x == floor(x)) return null
    // Continued fraction convergents of |x|.
    var (h1, h0) = 1L to 0L
    var (k1, k0) = 0L to 1L
    var remainder = abs(x)
    repeat(32) {
        val a = floor(remainder).toLong()
        val h = a * h1 + h0
        val k = a * k1 + k0
        if (k > LARGEST_DENOMINATOR) return null
        h0 = h1; h1 = h; k0 = k1; k1 = k
        if (abs(abs(x) - h.toDouble() / k) <= FRACTION_TOLERANCE * maxOf(1.0, abs(x))) {
            if (k <= 1) return null
            return "${if (x < 0) "-" else ""}$h/$k"
        }
        remainder = 1 / (remainder - a)
    }
    return null
}

fun toMixed(fraction: String): String? {
    val (numerator, denominator) = FRACTION.matchEntire(fraction)?.destructured ?: return null
    val n = numerator.toLong()
    val d = denominator.toLong()
    if (d == 0L || abs(n) < d) return null
    val whole = n / d
    val rest = abs(n % d)
    return if (rest == 0L) whole.toString() else "$whole $rest/$d"
}

fun fromMixed(mixed: String): String? {
    val (w, n, d) = MIXED.matchEntire(mixed)?.destructured ?: return null
    val whole = w.toLong()
    val numerator = n.toLong()
    val denominator = d.toLong()
    val improper = if (w.startsWith("-")) whole * denominator - numerator else whole * denominator + numerator
    return "$improper/$denominator"
}
