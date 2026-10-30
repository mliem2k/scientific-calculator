package com.mliem.scientific_calculator.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Units of each currency per one US dollar, as of [updatedAtMillis]. */
data class CurrencyRates(val perDollar: Map<String, Double>, val updatedAtMillis: Long) {
    val codes: List<String> get() = perDollar.keys.sorted()

    fun convert(amount: Double, from: String, to: String): Double? {
        val fromRate = perDollar[from] ?: return null
        val toRate = perDollar[to] ?: return null
        return amount / fromRate * toRate
    }
}

/** Live rates from open.er-api.com (free, no key), cached so the converter works offline. */
class CurrencyRepository(
    private val store: DataStore<Preferences>,
    private val fetch: suspend () -> String = ::fetchRates,
) {
    private val ratesKey = stringPreferencesKey("currency_rates")
    private val timeKey = longPreferencesKey("currency_rates_time")

    suspend fun cached(): CurrencyRates? {
        val prefs = store.data.first()
        val json = prefs[ratesKey] ?: return null
        return runCatching { parse(json, prefs[timeKey] ?: 0L) }.getOrNull()
    }

    /** Fetches fresh rates and stores them; throws when the network or the response is bad. */
    suspend fun refresh(now: Long = System.currentTimeMillis()): CurrencyRates {
        val json = fetch()
        val rates = parse(json, now)
        store.edit {
            it[ratesKey] = json
            it[timeKey] = now
        }
        return rates
    }

    companion object {
        private const val URL = "https://open.er-api.com/v6/latest/USD"

        internal fun parse(json: String, updatedAt: Long): CurrencyRates {
            val root = JSONObject(json)
            check(root.optString("result") == "success") { "Rates request failed" }
            val rates = root.getJSONObject("rates")
            val map = rates.keys().asSequence().associateWith { rates.getDouble(it) }.filterValues { it > 0 }
            check(map.isNotEmpty()) { "No rates" }
            return CurrencyRates(map, updatedAt)
        }

        private suspend fun fetchRates(): String = withContext(Dispatchers.IO) {
            val connection = URL(URL).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 15_000
                connection.readTimeout = 15_000
                check(connection.responseCode == 200) { "Rates server returned ${connection.responseCode}" }
                connection.inputStream.bufferedReader().use { it.readText() }
            } finally {
                connection.disconnect()
            }
        }
    }
}
