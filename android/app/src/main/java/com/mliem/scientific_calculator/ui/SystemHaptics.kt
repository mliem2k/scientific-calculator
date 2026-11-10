package com.mliem.scientific_calculator.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** Both switches have to be on; `vibrate_on` is the master vibration setting. */
internal fun systemHapticsEnabled(vibrateOn: Boolean, hapticFeedback: Boolean): Boolean =
    vibrateOn && hapticFeedback

internal fun readSystemHaptics(context: Context): Boolean = runCatching {
    val resolver = context.contentResolver
    systemHapticsEnabled(
        vibrateOn = Settings.System.getInt(resolver, Settings.System.VIBRATE_ON, 1) == 1,
        hapticFeedback = Settings.System.getInt(resolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) == 1,
    )
}.getOrDefault(true)

/** Re-reads the system switches when the app comes back from Settings. */
@Composable
internal fun rememberSystemHapticsEnabled(): Boolean {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var enabled by remember(context) { mutableStateOf(readSystemHaptics(context)) }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) enabled = readSystemHaptics(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return enabled
}

internal fun openSystemVibrationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_SOUND_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    } else {
        context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
