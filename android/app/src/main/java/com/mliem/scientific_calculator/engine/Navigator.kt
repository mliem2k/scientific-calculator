package com.mliem.scientific_calculator.engine

/** Caret movement. Left and right walk every slot in reading order, so the caret reaches all of them. */
object Navigator {
    private val up = mapOf(Slot.Denominator to Slot.Numerator, Slot.Exponent to Slot.Base)
    private val down = up.entries.associate { (lower, upper) -> upper to lower }

    private fun Node.firstVisibleSlot() = slots.firstOrNull { !isHidden(it) }

    private fun Node.lastVisibleSlot() = slots.lastOrNull { !isHidden(it) }

    private fun Node.slotBefore(slot: Slot) = slots.take(slots.indexOf(slot)).lastOrNull { !isHidden(it) }

    private fun Node.slotAfter(slot: Slot) = slots.drop(slots.indexOf(slot) + 1).firstOrNull { !isHidden(it) }

    fun right(expr: List<Node>, cursor: Cursor): Cursor {
        val list = expr.listAt(cursor.path)
        if (cursor.insertAt < list.size) {
            val first = list[cursor.insertAt].firstVisibleSlot()
                ?: return cursor.copy(insertAt = cursor.insertAt + 1)
            return Cursor(cursor.path + Segment(cursor.insertAt, first), 0)
        }
        val seg = cursor.path.lastOrNull() ?: return cursor
        val parentPath = cursor.path.dropLast(1)
        val owner = expr.listAt(parentPath)[seg.nodeIndex]
        val next = owner.slotAfter(seg.slot) ?: return Cursor(parentPath, seg.nodeIndex + 1)
        return Cursor(parentPath + Segment(seg.nodeIndex, next), 0)
    }

    fun left(expr: List<Node>, cursor: Cursor): Cursor {
        if (cursor.insertAt > 0) {
            val previous = expr.listAt(cursor.path)[cursor.insertAt - 1]
            val last = previous.lastVisibleSlot() ?: return cursor.copy(insertAt = cursor.insertAt - 1)
            return Cursor(cursor.path + Segment(cursor.insertAt - 1, last), checkNotNull(previous.slot(last)).size)
        }
        val seg = cursor.path.lastOrNull() ?: return cursor
        val parentPath = cursor.path.dropLast(1)
        val owner = expr.listAt(parentPath)[seg.nodeIndex]
        val before = owner.slotBefore(seg.slot) ?: return Cursor(parentPath, seg.nodeIndex)
        return Cursor(parentPath + Segment(seg.nodeIndex, before), checkNotNull(owner.slot(before)).size)
    }

    fun up(expr: List<Node>, cursor: Cursor) = jump(expr, cursor, up)

    fun down(expr: List<Node>, cursor: Cursor) = jump(expr, cursor, down)

    private fun jump(expr: List<Node>, cursor: Cursor, targets: Map<Slot, Slot>): Cursor {
        val seg = cursor.path.lastOrNull() ?: return cursor
        val target = targets[seg.slot] ?: return cursor
        val parentPath = cursor.path.dropLast(1)
        val owner = expr.listAt(parentPath)[seg.nodeIndex]
        val size = checkNotNull(owner.slot(target)).size
        return Cursor(parentPath + Segment(seg.nodeIndex, target), minOf(cursor.insertAt, size))
    }
}
