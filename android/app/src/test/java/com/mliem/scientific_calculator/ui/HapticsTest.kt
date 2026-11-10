package com.mliem.scientific_calculator.ui

import android.view.HapticFeedbackConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Records every call and answers with [results] in order, so the fallback can be exercised. */
private class RecordingSink(vararg results: Boolean) : HapticSink {
    private val answers = results.toList()
    val calls = mutableListOf<Pair<Int, Int>>()

    override fun perform(constant: Int, flags: Int): Boolean {
        calls += constant to flags
        return answers.getOrElse(calls.lastIndex) { false }
    }
}

class HapticsTest {
    @Suppress("DEPRECATION") // The requested flag is deprecated on recent SDKs but is what the key must pass.
    private val keyboardTap = HapticFeedbackConstants.KEYBOARD_TAP to HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
    @Suppress("DEPRECATION")
    private val clockTick = HapticFeedbackConstants.CLOCK_TICK to HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING

    @Test fun disabledMakesNoCalls() {
        val sink = RecordingSink(true)
        assertFalse(KeyHaptics(enabled = false, sink = sink).tap())
        assertEquals(emptyList<Pair<Int, Int>>(), sink.calls)
    }

    @Test fun enabledUsesTheKeyboardTapWithTheGlobalSettingIgnored() {
        val sink = RecordingSink(true)
        assertTrue(KeyHaptics(enabled = true, sink = sink).tap())
        assertEquals(listOf(keyboardTap), sink.calls)
    }

    @Test fun aRefusedKeyboardTapFallsBackToTheClockTick() {
        val sink = RecordingSink(false, true)
        assertTrue(KeyHaptics(enabled = true, sink = sink).tap())
        assertEquals(listOf(keyboardTap, clockTick), sink.calls)
    }

    @Test fun noFallbackWhenTheKeyboardTapIsTaken() {
        val sink = RecordingSink(true, true)
        KeyHaptics(enabled = true, sink = sink).tap()
        assertEquals(listOf(keyboardTap), sink.calls)
    }

    @Test fun aRefusedFallbackReportsFailure() {
        val sink = RecordingSink(false, false)
        assertFalse(KeyHaptics(enabled = true, sink = sink).tap())
        assertEquals(listOf(keyboardTap, clockTick), sink.calls)
    }

    @Test fun disabledStopsBeforeTheFallbackToo() {
        val sink = RecordingSink(false, true)
        assertFalse(KeyHaptics(enabled = false, sink = sink).tap())
        assertEquals(emptyList<Pair<Int, Int>>(), sink.calls)
    }

    @Test fun aRealVibrationSkipsTheViewHaptic() {
        val view = RecordingSink(true)
        var vibrations = 0
        val sink = DeviceHapticSink(vibrate = { vibrations++; true }, fallback = view)
        assertTrue(KeyHaptics(enabled = true, sink = sink).tap())
        assertEquals(1, vibrations)
        assertEquals(emptyList<Pair<Int, Int>>(), view.calls)
    }

    @Test fun aMissingVibratorFallsBackToTheViewHaptic() {
        val view = RecordingSink(true)
        val sink = DeviceHapticSink(vibrate = { false }, fallback = view)
        assertTrue(KeyHaptics(enabled = true, sink = sink).tap())
        assertEquals(listOf(keyboardTap), view.calls)
    }

    @Test fun neitherAVibratorNorTheViewHapticSucceeds() {
        val view = RecordingSink(false, false)
        val sink = DeviceHapticSink(vibrate = { false }, fallback = view)
        assertFalse(KeyHaptics(enabled = true, sink = sink).tap())
        assertEquals(listOf(keyboardTap, clockTick), view.calls)
    }
}
