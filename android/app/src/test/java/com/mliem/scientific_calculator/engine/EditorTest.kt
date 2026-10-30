package com.mliem.scientific_calculator.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class EditorTest {
    private val start = Cursor()

    @Test fun digitsExtendTheNumberBeforeTheCaret() {
        val one = Editor.digit(emptyList(), start, '4')
        val two = Editor.digit(one.expression, one.cursor, '2')
        assertEquals(listOf<Node>(Num("42")), two.expression)
        assertEquals(1, two.cursor.insertAt)
    }

    @Test fun decimalPointIsInsertedOnce() {
        val a = Editor.decimalPoint(emptyList(), start)
        assertEquals(listOf<Node>(Num("0.")), a.expression)
        val b = Editor.decimalPoint(a.expression, a.cursor)
        assertEquals(a.expression, b.expression)
    }

    @Test fun editsLeaveTheOriginalTreeUntouched() {
        val before = listOf<Node>(Num("5"))
        val after = Editor.digit(before, Cursor(emptyList(), 1), '6')
        assertEquals(listOf<Node>(Num("5")), before)
        assertEquals(listOf<Node>(Num("56")), after.expression)
    }

    @Test fun fractionPutsTheCaretInTheNumerator() {
        val edit = Editor.fraction(emptyList(), start)
        assertEquals(listOf(Segment(0, Slot.Numerator)), edit.cursor.path)
    }

    @Test fun squareWrapsThePreviousNode() {
        val edit = Editor.square(listOf(Num("4")), Cursor(emptyList(), 1))
        assertEquals(listOf<Node>(Exponent(listOf(Num("4")), listOf(Num("2")))), edit.expression)
        assertEquals(Cursor(emptyList(), 1), edit.cursor)
    }

    @Test fun powerWithNothingBeforeItGetsAnEmptyBase() {
        val edit = Editor.power(emptyList(), start)
        assertEquals(listOf<Node>(Exponent(emptyList(), emptyList())), edit.expression)
        assertEquals(listOf(Segment(0, Slot.Exponent)), edit.cursor.path)
    }

    @Test fun editsInsideANestedSlotRebuildOnlyThePath() {
        val tree = listOf<Node>(Fraction(listOf(Num("1")), listOf(Num("2"))), Num("9"))
        val cursor = Cursor(listOf(Segment(0, Slot.Denominator)), 1)
        val edit = Editor.digit(tree, cursor, '5')
        assertEquals(Fraction(listOf(Num("1")), listOf(Num("25"))), edit.expression[0])
        assertSame(tree[1], edit.expression[1])
    }

    @Test fun deleteShrinksANumberThenRemovesIt() {
        val first = Editor.delete(listOf(Num("42")), Cursor(emptyList(), 1))
        assertEquals(listOf<Node>(Num("4")), first.expression)
        val second = Editor.delete(first.expression, first.cursor)
        assertEquals(emptyList<Node>(), second.expression)
        assertEquals(0, second.cursor.insertAt)
    }

    @Test fun deleteAtTheStartOfASlotStepsOutWithoutRemovingAnything() {
        val tree = listOf<Node>(Fraction(emptyList(), emptyList()))
        val edit = Editor.delete(tree, Cursor(listOf(Segment(0, Slot.Numerator)), 0))
        assertEquals(tree, edit.expression)
        assertEquals(Cursor(emptyList(), 0), edit.cursor)
    }

    @Test fun closeParenStepsOutAfterTheOwner() {
        val open = Editor.openParen(emptyList(), start)
        val closed = Editor.closeParen(open.expression, open.cursor)
        assertEquals(Cursor(emptyList(), 1), closed.cursor)
    }

    @Test fun timesTenPowerLeavesTheCaretInTheExponent() {
        val edit = Editor.timesTenPower(listOf(Num("3")), Cursor(emptyList(), 1))
        assertEquals(listOf(Segment(2, Slot.Exponent)), edit.cursor.path)
        assertEquals(Op(Operator.Times), edit.expression[1])
    }
}

class NavigatorTest {
    private val fraction = listOf<Node>(Fraction(listOf(Num("1")), listOf(Num("2"))), Num("3"))

    @Test fun rightWalksNumeratorThenDenominatorThenOut() {
        var cursor = Navigator.right(fraction, Cursor())
        assertEquals(Cursor(listOf(Segment(0, Slot.Numerator)), 0), cursor)
        cursor = Navigator.right(fraction, Navigator.right(fraction, cursor))
        assertEquals(Cursor(listOf(Segment(0, Slot.Denominator)), 0), cursor)
        cursor = Navigator.right(fraction, Navigator.right(fraction, cursor))
        assertEquals(Cursor(emptyList(), 1), cursor)
    }

    @Test fun leftReentersFromTheEnd() {
        val cursor = Navigator.left(fraction, Cursor(emptyList(), 1))
        assertEquals(Cursor(listOf(Segment(0, Slot.Denominator)), 1), cursor)
        val numerator = Navigator.left(fraction, Navigator.left(fraction, cursor))
        assertEquals(Cursor(listOf(Segment(0, Slot.Numerator)), 1), numerator)
    }

    @Test fun upAndDownKeepTheColumnWhereTheTargetIsShorter() {
        val tree = listOf<Node>(Fraction(listOf(Num("1")), listOf(Num("2"), Num("3"))))
        val down = Navigator.down(tree, Cursor(listOf(Segment(0, Slot.Numerator)), 1))
        assertEquals(Cursor(listOf(Segment(0, Slot.Denominator)), 1), down)
        val up = Navigator.up(tree, Cursor(listOf(Segment(0, Slot.Denominator)), 2))
        assertEquals(Cursor(listOf(Segment(0, Slot.Numerator)), 1), up)
    }

    @Test fun functionArgumentsAreReachable() {
        val tree = listOf<Node>(Func(FunctionName.Sin, listOf(Num("9"))))
        assertEquals(Cursor(listOf(Segment(0, Slot.Argument)), 0), Navigator.right(tree, Cursor()))
    }

    @Test fun anEmptyDegreeIsSkipped() {
        val tree = listOf<Node>(Radical(emptyList(), listOf(Num("4"))))
        assertEquals(Cursor(listOf(Segment(0, Slot.Radicand)), 0), Navigator.right(tree, Cursor()))
        val withDegree = listOf<Node>(Radical(listOf(Num("3")), listOf(Num("8"))))
        assertEquals(Cursor(listOf(Segment(0, Slot.Degree)), 0), Navigator.right(withDegree, Cursor()))
    }

    @Test fun movingAtTheEdgesOfTheExpressionDoesNothing() {
        assertEquals(Cursor(), Navigator.left(emptyList(), Cursor()))
        assertEquals(Cursor(), Navigator.right(emptyList(), Cursor()))
    }
}
