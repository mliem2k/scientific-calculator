package com.mliem.scientific_calculator.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateFormatTest {
    @Test
    fun formatsATagAsADate() {
        assertEquals("Nightly, Jun 16, 2026", formatTag("nightly-20260616-1781592056"))
        assertEquals("weird", formatTag("weird"))
    }

    @Test
    fun formatsABuildNumberAsADate() {
        assertEquals("Build 2026-06-16", versionLabel(1781592056L))
        assertEquals("Dev build", versionLabel(0L))
    }
}
