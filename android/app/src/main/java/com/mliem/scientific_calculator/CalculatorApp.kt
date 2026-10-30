package com.mliem.scientific_calculator

import android.app.Application
import com.mliem.scientific_calculator.data.HistoryRepository
import com.mliem.scientific_calculator.data.SettingsRepository
import com.mliem.scientific_calculator.data.Updater
import com.mliem.scientific_calculator.data.dataStore

/** Holds the app wide dependencies; there are few enough that a DI framework would add more than it saves. */
class CalculatorApp : Application() {
    val settings by lazy { SettingsRepository(dataStore) }
    val history by lazy { HistoryRepository(dataStore) }
    val updater by lazy { Updater() }
}
