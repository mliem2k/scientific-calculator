package com.mliem.scientific_calculator.engine

/** A plain text reading of the expression for screen readers, for example "(1) over (4)". */
fun List<Node>.spoken(): String = joinToString(" ") { it.spoken() }

private fun Node.spoken(): String = when (this) {
    is Num -> value
    is Op -> when (op) {
        Operator.Plus -> "plus"
        Operator.Minus -> "minus"
        Operator.Times -> "times"
        Operator.Divide -> "divided by"
    }
    is Const -> if (name == ConstantName.Pi) "pi" else name.name.lowercase()
    is Fraction -> "(${numerator.spoken()}) over (${denominator.spoken()})"
    is Exponent -> "${base.spoken()} to the power of ${exponent.spoken()}"
    is Radical -> if (degree.isEmpty()) "square root of ${radicand.spoken()}" else "root ${degree.spoken()} of ${radicand.spoken()}"
    is Func -> "${name.name.lowercase()} of ${argument.spoken()}"
    is Factorial -> "${operand.spoken()} factorial"
    is Ncr -> "${n.spoken()} choose ${r.spoken()}"
    is Npr -> "${n.spoken()} permute ${r.spoken()}"
    is Paren -> "(${children.spoken()})"
}
