package com.mliem.scientific_calculator.engine

/**
 * The history to show once the stored list arrives. [live] holds whatever the user did while the read was in
 * flight, so it wins over [loaded] on a duplicate id, and [cleared] means a clear in that window drops the
 * stored entries instead of bringing them back. The result is newest first and capped at [MAX_HISTORY].
 */
fun mergeHistory(
    loaded: List<HistoryEntry>,
    live: List<HistoryEntry>,
    cleared: Boolean = false,
): List<HistoryEntry> = (if (cleared) live else live + loaded)
    .distinctBy { it.id }
    .sortedByDescending { it.timestamp }
    .take(MAX_HISTORY)
