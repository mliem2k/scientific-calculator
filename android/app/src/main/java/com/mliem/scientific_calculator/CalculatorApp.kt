package com.mliem.scientific_calculator

import android.app.Application
import com.mliem.android.update.Channel
import com.mliem.android.update.UpdateClient
import com.mliem.android.update.UpdateConfig
import com.mliem.android.update.UpdateMessages
import com.mliem.scientific_calculator.data.CurrencyRepository
import com.mliem.scientific_calculator.data.HistoryRepository
import com.mliem.scientific_calculator.data.InstallResultReceiver
import com.mliem.scientific_calculator.data.SettingsRepository
import com.mliem.scientific_calculator.data.dataStore

/** Holds the app wide dependencies; there are few enough that a DI framework would add more than it saves. */
class CalculatorApp : Application() {
    val settings by lazy { SettingsRepository(dataStore) }
    val history by lazy { HistoryRepository(dataStore) }
    val currency by lazy { CurrencyRepository(dataStore) }
    val updates by lazy {
        UpdateClient(
            context = this,
            config = UpdateConfig(
                repo = "mliem2k/scientific-calculator",
                installReceiver = InstallResultReceiver::class.java,
                channels = listOf(Channel.NIGHTLY),
                updatesEnabled = BuildConfig.BUILD_NUMBER > 0,
                messages = UpdateMessages(
                    permission = getString(R.string.install_allow),
                    checksum = getString(R.string.install_checksum),
                    digestMissing = getString(R.string.install_no_digest),
                    signature = getString(R.string.install_signature),
                    installFailed = getString(R.string.install_failed),
                    noApk = getString(R.string.install_no_apk),
                    insecure = getString(R.string.install_insecure),
                    differentApp = getString(R.string.install_different_app),
                    notNewer = getString(R.string.install_not_newer),
                    incomplete = getString(R.string.install_incomplete),
                ),
            ),
        )
    }
}
