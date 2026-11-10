package com.mliem.scientific_calculator.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemHapticsTest {
    @Test
    fun bothSwitchesMustBeOn() {
        assertTrue(systemHapticsEnabled(vibrateOn = true, hapticFeedback = true))
        assertFalse(systemHapticsEnabled(vibrateOn = false, hapticFeedback = true))
        assertFalse(systemHapticsEnabled(vibrateOn = true, hapticFeedback = false))
        assertFalse(systemHapticsEnabled(vibrateOn = false, hapticFeedback = false))
    }
}
