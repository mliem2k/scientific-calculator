package com.mliem.scientific_calculator.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mliem.scientific_calculator.engine.HistoryEntry
import kotlinx.coroutines.flow.first

class HistoryRepository(private val store: DataStore<Preferences>) {
    private val key = stringPreferencesKey("history")

    /** An unreadable or missing record loads as empty history rather than failing the launch. */
    suspend fun load(): List<HistoryEntry> =
        store.data.first()[key]?.let { runCatching { HistoryCodec.decode(it) }.getOrNull() } ?: emptyList()

    suspend fun save(entries: List<HistoryEntry>) {
        store.edit { if (entries.isEmpty()) it.remove(key) else it[key] = HistoryCodec.encode(entries) }
    }
}
