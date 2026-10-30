package com.mliem.scientific_calculator.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mliem.scientific_calculator.CalculatorApp
import com.mliem.scientific_calculator.data.Settings
import com.mliem.scientific_calculator.data.ThemeChoice
import com.mliem.scientific_calculator.data.UpdateInfo
import com.mliem.scientific_calculator.engine.AngleMode
import com.mliem.scientific_calculator.engine.CalculatorState
import com.mliem.scientific_calculator.engine.Cursor
import com.mliem.scientific_calculator.engine.HistoryEntry
import com.mliem.scientific_calculator.engine.Key
import com.mliem.scientific_calculator.engine.Segment
import com.mliem.scientific_calculator.engine.UnitCategory
import com.mliem.scientific_calculator.engine.parseExpression
import com.mliem.scientific_calculator.engine.pasteNodes
import com.mliem.scientific_calculator.engine.press
import com.mliem.scientific_calculator.engine.restore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface Screen {
    /** The screen back leads to, or null at the root. */
    val parent: Screen?

    data object Calculator : Screen { override val parent: Screen? = null }
    data object History : Screen { override val parent: Screen = Calculator }
    data object Settings : Screen { override val parent: Screen = Calculator }
    data object Converters : Screen { override val parent: Screen = Calculator }
    data class UnitConverter(val category: UnitCategory) : Screen { override val parent: Screen = Converters }
    data object NumberSystemConverter : Screen { override val parent: Screen = Converters }
    data object CurrencyConverter : Screen { override val parent: Screen = Converters }
}

sealed interface UpdatePrompt {
    data class Available(val info: UpdateInfo) : UpdatePrompt
    data object UpToDate : UpdatePrompt
    data object Failed : UpdatePrompt
}

class CalculatorViewModel(app: CalculatorApp) : ViewModel() {
    private val settingsRepository = app.settings
    private val historyRepository = app.history
    private val updater = app.updater
    private val currencyRepository = app.currency

    private val _calc = MutableStateFlow(CalculatorState())
    val calc: StateFlow<CalculatorState> = _calc.asStateFlow()

    private val _settings = MutableStateFlow(Settings())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    /** False until saved settings and history are read, which keeps the splash screen up. */
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _screen = MutableStateFlow<Screen>(Screen.Calculator)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    private val _updatePrompt = MutableStateFlow<UpdatePrompt?>(null)
    val updatePrompt: StateFlow<UpdatePrompt?> = _updatePrompt.asStateFlow()

    private val _checking = MutableStateFlow(false)
    val checking: StateFlow<Boolean> = _checking.asStateFlow()

    init {
        viewModelScope.launch {
            val saved = settingsRepository.settings.first()
            _settings.value = saved
            _calc.value = CalculatorState(angleMode = saved.defaultAngleMode, history = historyRepository.load())
            _ready.value = true
            launch { settingsRepository.settings.collect { _settings.value = it } }
            checkForUpdates(manual = false)
        }
    }

    fun press(key: Key) {
        val before = _calc.value
        val after = before.press(key, _settings.value.resultPrecision)
        _calc.value = after
        if (after.history != before.history) saveHistory(after.history)
    }

    fun jumpTo(path: List<Segment>, insertAt: Int) = _calc.update { it.copy(cursor = Cursor(path, insertAt)) }

    /** Inserts clipboard text when it is numbers and math symbols; returns false for anything else. */
    fun paste(text: String): Boolean {
        val nodes = parseExpression(text) ?: return false
        _calc.update { it.pasteNodes(nodes, _settings.value.resultPrecision) }
        return true
    }

    fun restore(entry: HistoryEntry) {
        _calc.update { it.restore(entry) }
        _screen.value = Screen.Calculator
    }

    fun clearHistory() {
        _calc.update { it.copy(history = emptyList()) }
        saveHistory(emptyList())
    }

    fun open(screen: Screen) {
        _screen.value = screen
        if (screen == Screen.CurrencyConverter) loadCurrency()
    }

    private val _currency = MutableStateFlow<CurrencyUiState>(CurrencyUiState.Loading)
    val currency: StateFlow<CurrencyUiState> = _currency.asStateFlow()

    private val _currencyRefreshing = MutableStateFlow(false)
    val currencyRefreshing: StateFlow<Boolean> = _currencyRefreshing.asStateFlow()

    /** Shows cached rates straight away, then refreshes them when they are over an hour old or missing. */
    private fun loadCurrency() {
        viewModelScope.launch {
            val cached = currencyRepository.cached()
            if (cached != null) _currency.value = CurrencyUiState.Ready(cached)
            val stale = cached == null || System.currentTimeMillis() - cached.updatedAtMillis > CURRENCY_MAX_AGE_MILLIS
            if (stale) refreshCurrency()
        }
    }

    fun refreshCurrency() {
        if (_currencyRefreshing.value) return
        viewModelScope.launch {
            _currencyRefreshing.value = true
            runCatching { currencyRepository.refresh() }
                .onSuccess { _currency.value = CurrencyUiState.Ready(it) }
                .onFailure {
                    val current = _currency.value
                    _currency.value = if (current is CurrencyUiState.Ready) current.copy(failed = true) else CurrencyUiState.Unavailable
                }
            _currencyRefreshing.value = false
        }
    }

    fun setTheme(theme: ThemeChoice) = viewModelScope.launch { settingsRepository.setTheme(theme) }

    fun setDefaultAngleMode(mode: AngleMode) = viewModelScope.launch { settingsRepository.setDefaultAngleMode(mode) }

    fun setResultPrecision(places: Int) = viewModelScope.launch { settingsRepository.setResultPrecision(places) }

    fun setHapticFeedback(on: Boolean) = viewModelScope.launch { settingsRepository.setHapticFeedback(on) }

    fun setVoiceFeedback(on: Boolean) = viewModelScope.launch { settingsRepository.setVoiceFeedback(on) }

    fun dismissUpdatePrompt() {
        _updatePrompt.value = null
    }

    /** A launch check only speaks up when there is an update; a manual one always answers. */
    fun checkForUpdates(manual: Boolean) {
        if (_checking.value || !updater.enabled) return
        viewModelScope.launch {
            _checking.value = true
            runCatching { updater.check() }
                .onSuccess { found ->
                    if (found != null) _updatePrompt.value = UpdatePrompt.Available(found)
                    else if (manual) _updatePrompt.value = UpdatePrompt.UpToDate
                }
                .onFailure { if (manual) _updatePrompt.value = UpdatePrompt.Failed }
            _checking.value = false
        }
    }

    private fun saveHistory(entries: List<HistoryEntry>) {
        viewModelScope.launch { historyRepository.save(entries) }
    }

    companion object {
        private const val CURRENCY_MAX_AGE_MILLIS = 60 * 60 * 1000L

        val Factory = viewModelFactory {
            initializer { CalculatorViewModel(this[APPLICATION_KEY] as CalculatorApp) }
        }
    }
}
