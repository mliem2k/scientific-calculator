package com.mliem.scientific_calculator.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mliem.scientific_calculator.engine.AngleMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeChoice { System, Light, Dark }

data class Settings(
    val theme: ThemeChoice = ThemeChoice.System,
    val defaultAngleMode: AngleMode = AngleMode.Deg,
    /** Decimal places, 0 means automatic. */
    val resultPrecision: Int = 0,
    val hapticFeedback: Boolean = true,
)

val PRECISION_CHOICES = listOf(0, 4, 6, 8, 10)

internal val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "calculator")

class SettingsRepository(private val store: DataStore<Preferences>) {
    private val theme = stringPreferencesKey("theme")
    private val angle = stringPreferencesKey("angle")
    private val precision = intPreferencesKey("precision")
    private val haptics = booleanPreferencesKey("haptics")

    val settings: Flow<Settings> = store.data.map { prefs ->
        Settings(
            theme = prefs[theme].toEnumOr(ThemeChoice.System),
            defaultAngleMode = prefs[angle].toEnumOr(AngleMode.Deg),
            resultPrecision = (prefs[precision] ?: 0).takeIf { it in PRECISION_CHOICES } ?: 0,
            hapticFeedback = prefs[haptics] ?: true,
        )
    }

    suspend fun setTheme(value: ThemeChoice) = store.edit { it[theme] = value.name }

    suspend fun setDefaultAngleMode(value: AngleMode) = store.edit { it[angle] = value.name }

    suspend fun setResultPrecision(value: Int) = store.edit { it[precision] = value }

    suspend fun setHapticFeedback(value: Boolean) = store.edit { it[haptics] = value }
}

private inline fun <reified E : Enum<E>> String?.toEnumOr(default: E): E =
    enumValues<E>().firstOrNull { it.name == this } ?: default
