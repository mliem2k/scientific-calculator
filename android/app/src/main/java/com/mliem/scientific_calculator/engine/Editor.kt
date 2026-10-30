package com.mliem.scientific_calculator.engine

/** A new tree and where the caret ends up in it. */
data class Edit(val expression: List<Node>, val cursor: Cursor)

/** Structural edits at the caret. Each returns a new [Edit]; the inputs are never changed. */
object Editor {
    private fun List<Node>.withInserted(index: Int, vararg nodes: Node) =
        toMutableList().also { it.addAll(index, nodes.asList()) }

    private fun insert(expr: List<Node>, cursor: Cursor, node: Node) = Edit(
        expr.updateAt(cursor.path) { it.withInserted(cursor.insertAt, node) },
        cursor.copy(insertAt = cursor.insertAt + 1),
    )

    /** Inserts [node] and puts the caret at the start of its [slot]. */
    private fun insertAndEnter(expr: List<Node>, cursor: Cursor, node: Node, slot: Slot): Edit {
        val inserted = insert(expr, cursor, node)
        return Edit(inserted.expression, Cursor(cursor.path + Segment(cursor.insertAt, slot), 0))
    }

    private class Wrapped(val expression: List<Node>, val index: Int, val path: List<Segment>)

    /** Replaces the node before the caret (if any) by the node [make] builds around it. */
    private fun wrapPrevious(expr: List<Node>, cursor: Cursor, make: (List<Node>) -> Node): Wrapped {
        val hasPrevious = cursor.insertAt > 0
        val index = if (hasPrevious) cursor.insertAt - 1 else cursor.insertAt
        val updated = expr.updateAt(cursor.path) { list ->
            val previous = if (hasPrevious) listOf(list[index]) else emptyList()
            list.toMutableList().also {
                if (hasPrevious) it.removeAt(index)
                it.add(index, make(previous))
            }
        }
        return Wrapped(updated, index, cursor.path)
    }

    private fun Wrapped.caretAfter() = Edit(expression, Cursor(path, index + 1))

    private fun Wrapped.caretIn(slot: Slot) = Edit(expression, Cursor(path + Segment(index, slot), 0))

    fun digit(expr: List<Node>, cursor: Cursor, digit: Char): Edit {
        val previous = expr.listAt(cursor.path).getOrNull(cursor.insertAt - 1)
        if (previous is Num) return replaceNumber(expr, cursor, previous.value + digit)
        return insert(expr, cursor, Num(digit.toString()))
    }

    fun decimalPoint(expr: List<Node>, cursor: Cursor): Edit {
        val previous = expr.listAt(cursor.path).getOrNull(cursor.insertAt - 1)
        if (previous is Num) {
            return if ('.' in previous.value) Edit(expr, cursor) else replaceNumber(expr, cursor, previous.value + '.')
        }
        return insert(expr, cursor, Num("0."))
    }

    private fun replaceNumber(expr: List<Node>, cursor: Cursor, value: String) = Edit(
        expr.updateAt(cursor.path) { list -> list.toMutableList().also { it[cursor.insertAt - 1] = Num(value) } },
        cursor,
    )

    /** Inserts a number as one node, for example a pasted value. */
    fun numberLiteral(expr: List<Node>, cursor: Cursor, value: String) = insert(expr, cursor, Num(value))

    fun operator(expr: List<Node>, cursor: Cursor, op: Operator) = insert(expr, cursor, Op(op))

    fun constant(expr: List<Node>, cursor: Cursor, name: ConstantName) = insert(expr, cursor, Const(name))

    fun fraction(expr: List<Node>, cursor: Cursor) =
        insertAndEnter(expr, cursor, Fraction(emptyList(), emptyList()), Slot.Numerator)

    /** Turns the number before the caret into a whole part and opens an empty fraction after it. */
    fun mixed(expr: List<Node>, cursor: Cursor): Edit {
        val updated = expr.updateAt(cursor.path) {
            it.withInserted(cursor.insertAt, Num("0"), Fraction(emptyList(), emptyList()))
        }
        return Edit(updated, Cursor(cursor.path + Segment(cursor.insertAt + 1, Slot.Numerator), 0))
    }

    fun power(expr: List<Node>, cursor: Cursor) =
        wrapPrevious(expr, cursor) { Exponent(it, emptyList()) }.caretIn(Slot.Exponent)

    fun square(expr: List<Node>, cursor: Cursor) =
        wrapPrevious(expr, cursor) { Exponent(it, listOf(Num("2"))) }.caretAfter()

    fun cube(expr: List<Node>, cursor: Cursor) =
        wrapPrevious(expr, cursor) { Exponent(it, listOf(Num("3"))) }.caretAfter()

    /** Appends ×10 to the power of an empty exponent. */
    fun timesTenPower(expr: List<Node>, cursor: Cursor): Edit {
        val updated = expr.updateAt(cursor.path) {
            it.withInserted(cursor.insertAt, Op(Operator.Times), Exponent(listOf(Num("10")), emptyList()))
        }
        return Edit(updated, Cursor(cursor.path + Segment(cursor.insertAt + 1, Slot.Exponent), 0))
    }

    fun squareRoot(expr: List<Node>, cursor: Cursor) =
        insertAndEnter(expr, cursor, Radical(emptyList(), emptyList()), Slot.Radicand)

    fun cubeRoot(expr: List<Node>, cursor: Cursor) =
        insertAndEnter(expr, cursor, Radical(listOf(Num("3")), emptyList()), Slot.Radicand)

    fun nthRoot(expr: List<Node>, cursor: Cursor) =
        insertAndEnter(expr, cursor, Radical(emptyList(), emptyList()), Slot.Degree)

    fun function(expr: List<Node>, cursor: Cursor, name: FunctionName) =
        insertAndEnter(expr, cursor, Func(name, emptyList()), Slot.Argument)

    fun factorial(expr: List<Node>, cursor: Cursor) =
        wrapPrevious(expr, cursor) { Factorial(it) }.caretAfter()

    fun combinations(expr: List<Node>, cursor: Cursor) =
        wrapPrevious(expr, cursor) { Ncr(it, emptyList()) }.caretIn(Slot.R)

    fun permutations(expr: List<Node>, cursor: Cursor) =
        wrapPrevious(expr, cursor) { Npr(it, emptyList()) }.caretIn(Slot.R)

    fun openParen(expr: List<Node>, cursor: Cursor) =
        insertAndEnter(expr, cursor, Paren(emptyList()), Slot.Children)

    /** Steps out of the slot the caret is in, to just after the node that owns it. */
    fun closeParen(expr: List<Node>, cursor: Cursor): Edit {
        val last = cursor.path.lastOrNull() ?: return Edit(expr, cursor)
        return Edit(expr, Cursor(cursor.path.dropLast(1), last.nodeIndex + 1))
    }

    fun delete(expr: List<Node>, cursor: Cursor): Edit {
        if (cursor.insertAt == 0) {
            // At the start of a slot, backspace leaves the slot without removing anything.
            val last = cursor.path.lastOrNull() ?: return Edit(expr, cursor)
            return Edit(expr, Cursor(cursor.path.dropLast(1), last.nodeIndex))
        }
        val index = cursor.insertAt - 1
        val node = expr.listAt(cursor.path)[index]
        if (node is Num && node.value.length > 1) return replaceNumber(expr, cursor, node.value.dropLast(1))
        val updated = expr.updateAt(cursor.path) { list -> list.toMutableList().also { it.removeAt(index) } }
        return Edit(updated, cursor.copy(insertAt = index))
    }

    fun clear() = Edit(emptyList(), Cursor())
}
