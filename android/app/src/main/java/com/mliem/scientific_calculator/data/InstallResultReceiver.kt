package com.mliem.scientific_calculator.data

import android.content.Context
import com.mliem.android.update.InstallResultReceiver as CoreInstallResultReceiver
import com.mliem.android.update.UpdateClient
import com.mliem.scientific_calculator.CalculatorApp

/** Receives the system installer's result and hands it to the shared update client. */
class InstallResultReceiver : CoreInstallResultReceiver() {
    override fun updateClient(context: Context): UpdateClient =
        (context.applicationContext as CalculatorApp).updates
}
