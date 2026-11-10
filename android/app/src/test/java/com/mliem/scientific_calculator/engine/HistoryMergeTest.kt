package com.mliem.scientific_calculator.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private fun entry(id: String, time: Long, result: String = id) = HistoryEntry(id, emptyList(), result, time)

class HistoryMergeTest {
    @Test fun mergesNewestFirst() {
        val loaded = listOf(entry("oldest", 1), entry("newest", 3))
        val live = listOf(entry("middle", 2))
        assertEquals(listOf("newest", "middle", "oldest"), mergeHistory(loaded, live).map { it.id })
    }

    @Test fun aDuplicateIdKeepsTheLiveEntry() {
        val loaded = listOf(entry("a", 1), entry("b", 2, result = "stored"))
        val live = listOf(entry("b", 2, result = "live"), entry("c", 3))
        val merged = mergeHistory(loaded, live)
        assertEquals(listOf("c", "b", "a"), merged.map { it.id })
        assertEquals("live", merged[1].result)
    }

    @Test fun theLiveCopyWinsEvenWhenTheStoredCopyHasANewerTime() {
        val loaded = listOf(entry("a", 5, result = "stored"))
        val live = listOf(entry("a", 4, result = "live"))
        assertEquals(listOf(entry("a", 4, result = "live")), mergeHistory(loaded, live))
    }

    @Test fun anEmptyStoredListLeavesTheLiveListAsIs() {
        val live = listOf(entry("b", 2), entry("a", 1))
        assertEquals(live, mergeHistory(emptyList(), live))
    }

    @Test fun aClearWhileHistoryLoadsDropsTheStoredEntries() {
        val loaded = listOf(entry("a", 1), entry("b", 2))
        assertEquals(emptyList<HistoryEntry>(), mergeHistory(loaded, emptyList(), cleared = true))
    }

    @Test fun aClearWhileHistoryLoadsKeepsLaterPresses() {
        val loaded = listOf(entry("a", 1), entry("b", 2))
        val live = listOf(entry("new", 3))
        assertEquals(listOf("new"), mergeHistory(loaded, live, cleared = true).map { it.id })
    }

    @Test fun aPressWhileHistoryLoadsIsPreserved() {
        val loaded = listOf(entry("a", 1), entry("b", 2))
        val live = listOf(entry("pressed", 3))
        assertEquals(listOf("pressed", "b", "a"), mergeHistory(loaded, live).map { it.id })
    }

    @Test fun fiftyLoadedPlusNewPressesKeepsTheNewestFifty() {
        val loaded = (1L..MAX_HISTORY.toLong()).map { entry("stored$it", it) }
        val live = listOf(entry("pressed1", 51), entry("pressed2", 52), entry("pressed3", 53))
        val merged = mergeHistory(loaded, live)
        assertEquals(MAX_HISTORY, merged.size)
        assertEquals(listOf("pressed3", "pressed2", "pressed1", "stored50", "stored49"), merged.take(5).map { it.id })
        assertEquals("stored4", merged.last().id)
        assertTrue(merged.none { it.id in setOf("stored1", "stored2", "stored3") })
    }

    @Test fun capsMoreThanFiftyStoredEntries() {
        val loaded = (1L..55L).map { entry("stored$it", it) }
        val merged = mergeHistory(loaded, emptyList())
        assertEquals(MAX_HISTORY, merged.size)
        assertEquals("stored55", merged.first().id)
        assertEquals("stored6", merged.last().id)
    }
}
