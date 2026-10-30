package com.mliem.scientific_calculator.engine

/**
 * Reads pasted text such as "12.5 + (3 × 4)%" into nodes. Only digits, the four operators, percent and
 * parentheses are accepted; anything else, or unbalanced parentheses, gives null.
 */
fun parseExpression(text: String): List<Node>? {
    val root = mutableListOf<Node>()
    val stack = ArrayDeque<MutableList<Node>>().also { it.addLast(root) }
    var i = 0
    val input = text.trim()
    if (input.isEmpty()) return null
    while (i < input.length) {
        val c = input[i]
        val current = stack.last()
        when {
            c.isWhitespace() || c == ',' -> i++
            c.isDigit() || c == '.' -> {
                val start = i
                while (i < input.length && (input[i].isDigit() || input[i] == '.')) i++
                val literal = input.substring(start, i)
                if (literal.count { it == '.' } > 1 || literal == ".") return null
                current.add(Num(if (literal.startsWith('.')) "0$literal" else literal))
            }
            c == '+' -> { current.add(Op(Operator.Plus)); i++ }
            c == '-' || c == '−' -> { current.add(Op(Operator.Minus)); i++ }
            c == '*' || c == '×' || c == 'x' || c == 'X' -> { current.add(Op(Operator.Times)); i++ }
            c == '/' || c == '÷' -> { current.add(Op(Operator.Divide)); i++ }
            c == '(' -> { stack.addLast(mutableListOf()); i++ }
            c == ')' -> {
                if (stack.size == 1) return null
                val inner = stack.removeLast()
                stack.last().add(Paren(inner))
                i++
            }
            c == '%' -> {
                if (current.isEmpty() || current.last() is Op) return null
                current.add(Percent(listOf(current.removeLast())))
                i++
            }
            else -> return null
        }
    }
    return if (stack.size == 1) root else null
}
