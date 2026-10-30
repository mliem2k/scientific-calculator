package com.mliem.scientific_calculator.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private var counter = 0
private val entries = EntryFactory(newId = { "id${counter++}" }, now = { 1_000L })

private fun CalculatorState.keys(vararg keys: Key, precision: Int = 0) =
    keys.fold(this) { state, key -> state.press(key, precision, entries) }

class CalculatorTest {
    private val fresh = CalculatorState()

    @Test fun digitsBuildANumberAndTheLiveResultFollows() {
        val state = fresh.keys(Key.D4, Key.D2)
        assertEquals(listOf<Node>(Num("42")), state.expression)
        assertEquals("42", state.result)
    }

    @Test fun equalsEvaluatesAndRecordsHistory() {
        val state = fresh.keys(Key.D2, Key.Plus, Key.D3, Key.Equals)
        assertEquals("5", state.result)
        assertEquals(1, state.history.size)
        assertEquals("5", state.history[0].result)
        assertEquals("5", state.lastResult)
    }

    @Test fun equalsOnAnIncompleteExpressionRecordsNothing() {
        val state = fresh.keys(Key.D2, Key.Plus, Key.Equals)
        assertTrue(state.history.isEmpty())
    }

    @Test fun equalsOnAMathErrorShowsItWithoutHistoryOrAnswer() {
        val state = fresh.keys(Key.D1, Key.Divide, Key.D0, Key.Equals)
        assertEquals(MATH_ERROR, state.result)
        assertTrue(state.history.isEmpty())
        assertNull(state.lastResult)
    }

    @Test fun historyKeepsTheNewestFiftyEntries() {
        var state = fresh
        repeat(55) { state = state.keys(Key.D1, Key.Equals, Key.AllClear) }
        assertEquals(MAX_HISTORY, state.history.size)
    }

    @Test fun shiftAndHypTogglePerKeyAndClearAfterOneKey() {
        val shifted = fresh.keys(Key.Shift)
        assertTrue(shifted.shiftActive)
        assertTrue(!shifted.keys(Key.Shift).shiftActive)
        assertTrue(shifted.keys(Key.Hyp).hypActive && !shifted.keys(Key.Hyp).shiftActive)
        assertTrue(!shifted.keys(Key.D1).shiftActive)
    }

    @Test fun angleModeToggleReevaluatesTheResult() {
        val state = fresh.keys(Key.Sin, Key.D9, Key.D0)
        assertEquals("1", state.result)
        assertEquals("0.8939966636", state.keys(Key.DegRad).result)
        assertEquals(AngleMode.Rad, state.keys(Key.DegRad).angleMode)
    }

    @Test fun storeThenAKeyWritesTheResultToMemory() {
        val state = fresh.keys(Key.D5, Key.Equals, Key.Store, Key.MemA)
        assertEquals(5.0, state.memory[ConstantName.A])
        assertTrue(!state.stoMode)
    }

    @Test fun aMemoryKeyWithoutStoreInsertsTheVariable() {
        assertEquals(listOf<Node>(Const(ConstantName.A)), fresh.keys(Key.MemA).expression)
    }

    @Test fun anyOtherKeyCancelsStore() {
        assertTrue(!fresh.keys(Key.Store, Key.D5).stoMode)
    }

    @Test fun answerKeyReadsTheLastResult() {
        val state = fresh.keys(Key.D5, Key.Equals, Key.AllClear, Key.Ans, Key.Plus, Key.D1)
        assertEquals("6", state.result)
    }

    @Test fun fractionToggleSwitchesTheResultBothWays() {
        val state = fresh.keys(Key.D1, Key.Divide, Key.D4)
        val asFraction = state.keys(Key.FractionToggle)
        assertEquals("1/4", asFraction.result)
        assertEquals("0.25", asFraction.keys(Key.FractionToggle).result)
    }

    @Test fun mixedToggleConvertsAnImproperFraction() {
        val state = fresh.keys(Key.D7, Key.Divide, Key.D3).keys(Key.FractionToggle)
        assertEquals("7/3", state.result)
        assertEquals("2 1/3", state.keys(Key.MixedToggle).result)
        assertEquals("7/3", state.keys(Key.MixedToggle).keys(Key.MixedToggle).result)
    }

    @Test fun fixedPrecisionAppliesToTheLiveResult() {
        val state = fresh.keys(Key.D1, Key.Divide, Key.D3, precision = 4)
        assertEquals("0.3333", state.result)
    }

    @Test fun cubeWrapsThePreviousNumber() {
        val state = fresh.keys(Key.D4, Key.Cube)
        assertTrue(state.expression[0] is Exponent)
        assertEquals("64", state.result)
    }

    @Test fun restoreLoadsAnEntryWithTheCaretAtTheEnd() {
        val done = fresh.keys(Key.D9, Key.Equals)
        val restored = fresh.restore(done.history[0])
        assertEquals("9", restored.result)
        assertEquals(Cursor(emptyList(), 1), restored.cursor)
    }

    @Test fun pasteInsertsANumberLiteral() {
        val state = fresh.pasteNumber("12.5")
        assertEquals(listOf<Node>(Num("12.5")), state.expression)
        assertEquals("12.5", state.result)
    }

    @Test fun deleteAndAllClear() {
        assertEquals(emptyList<Node>(), fresh.keys(Key.D5, Key.Delete).expression)
        assertEquals(emptyList<Node>(), fresh.keys(Key.D5, Key.AllClear).expression)
    }
}
