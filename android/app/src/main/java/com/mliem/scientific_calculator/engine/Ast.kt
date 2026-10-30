package com.mliem.scientific_calculator.engine

/** A named child list of a node, for example the top of a fraction. */
enum class Slot { Numerator, Denominator, Base, Exponent, Degree, Radicand, Argument, Operand, N, R, Children }

enum class Operator(val symbol: Char) { Plus('+'), Minus('-'), Times('×'), Divide('÷') }

enum class ConstantName { Pi, E, Ans, A, B, X, Y }

enum class FunctionName { Sin, Cos, Tan, Asin, Acos, Atan, Sinh, Cosh, Tanh, Log, Ln, Exp, Abs }

/**
 * The expression tree the editor builds and the evaluator reads. Every node is immutable, so an edit
 * returns a new tree and the previous one stays valid as a history entry.
 */
sealed interface Node {
    /** Slots in the order the caret visits them. */
    val slots: List<Slot> get() = emptyList()

    fun slot(slot: Slot): List<Node>? = null

    fun withSlot(slot: Slot, nodes: List<Node>): Node = this

    /** A slot the caret steps over because it is empty and optional. */
    fun isHidden(slot: Slot): Boolean = false
}

data class Num(val value: String) : Node

data class Op(val op: Operator) : Node

data class Const(val name: ConstantName) : Node

data class Fraction(val numerator: List<Node>, val denominator: List<Node>) : Node {
    override val slots = listOf(Slot.Numerator, Slot.Denominator)

    override fun slot(slot: Slot) = when (slot) {
        Slot.Numerator -> numerator
        Slot.Denominator -> denominator
        else -> null
    }

    override fun withSlot(slot: Slot, nodes: List<Node>) = when (slot) {
        Slot.Numerator -> copy(numerator = nodes)
        Slot.Denominator -> copy(denominator = nodes)
        else -> this
    }
}

data class Exponent(val base: List<Node>, val exponent: List<Node>) : Node {
    override val slots = listOf(Slot.Base, Slot.Exponent)

    override fun slot(slot: Slot) = when (slot) {
        Slot.Base -> base
        Slot.Exponent -> exponent
        else -> null
    }

    override fun withSlot(slot: Slot, nodes: List<Node>) = when (slot) {
        Slot.Base -> copy(base = nodes)
        Slot.Exponent -> copy(exponent = nodes)
        else -> this
    }
}

/** A square root when [degree] is empty, otherwise the nth root. */
data class Radical(val degree: List<Node>, val radicand: List<Node>) : Node {
    override val slots = listOf(Slot.Degree, Slot.Radicand)

    override fun slot(slot: Slot) = when (slot) {
        Slot.Degree -> degree
        Slot.Radicand -> radicand
        else -> null
    }

    override fun withSlot(slot: Slot, nodes: List<Node>) = when (slot) {
        Slot.Degree -> copy(degree = nodes)
        Slot.Radicand -> copy(radicand = nodes)
        else -> this
    }

    override fun isHidden(slot: Slot) = slot == Slot.Degree && degree.isEmpty()
}

data class Func(val name: FunctionName, val argument: List<Node>) : Node {
    override val slots = listOf(Slot.Argument)

    override fun slot(slot: Slot) = if (slot == Slot.Argument) argument else null

    override fun withSlot(slot: Slot, nodes: List<Node>) = if (slot == Slot.Argument) copy(argument = nodes) else this
}

data class Factorial(val operand: List<Node>) : Node {
    override val slots = listOf(Slot.Operand)

    override fun slot(slot: Slot) = if (slot == Slot.Operand) operand else null

    override fun withSlot(slot: Slot, nodes: List<Node>) = if (slot == Slot.Operand) copy(operand = nodes) else this
}

data class Ncr(val n: List<Node>, val r: List<Node>) : Node {
    override val slots = listOf(Slot.N, Slot.R)

    override fun slot(slot: Slot) = when (slot) {
        Slot.N -> n
        Slot.R -> r
        else -> null
    }

    override fun withSlot(slot: Slot, nodes: List<Node>) = when (slot) {
        Slot.N -> copy(n = nodes)
        Slot.R -> copy(r = nodes)
        else -> this
    }
}

data class Npr(val n: List<Node>, val r: List<Node>) : Node {
    override val slots = listOf(Slot.N, Slot.R)

    override fun slot(slot: Slot) = when (slot) {
        Slot.N -> n
        Slot.R -> r
        else -> null
    }

    override fun withSlot(slot: Slot, nodes: List<Node>) = when (slot) {
        Slot.N -> copy(n = nodes)
        Slot.R -> copy(r = nodes)
        else -> this
    }
}

data class Paren(val children: List<Node>) : Node {
    override val slots = listOf(Slot.Children)

    override fun slot(slot: Slot) = if (slot == Slot.Children) children else null

    override fun withSlot(slot: Slot, nodes: List<Node>) = if (slot == Slot.Children) copy(children = nodes) else this
}

data class Segment(val nodeIndex: Int, val slot: Slot)

/** The caret: the slot it sits in, as a path from the root, and its index among that slot's nodes. */
data class Cursor(val path: List<Segment> = emptyList(), val insertAt: Int = 0)

fun List<Node>.listAt(path: List<Segment>): List<Node> =
    path.fold(this) { list, seg -> checkNotNull(list[seg.nodeIndex].slot(seg.slot)) }

/** Applies [transform] to the list at [path] and rebuilds only the nodes on the way down. */
fun List<Node>.updateAt(path: List<Segment>, transform: (List<Node>) -> List<Node>): List<Node> {
    if (path.isEmpty()) return transform(this)
    val seg = path.first()
    val node = this[seg.nodeIndex]
    val child = checkNotNull(node.slot(seg.slot)).updateAt(path.drop(1), transform)
    return toMutableList().also { it[seg.nodeIndex] = node.withSlot(seg.slot, child) }
}
