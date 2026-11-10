package com.mliem.scientific_calculator.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

/** The one thing [KeyHaptics] needs from a device, so a test can record the calls without hardware. */
fun interface HapticSink {
    fun perform(constant: Int, flags: Int): Boolean
}

/**
 * Key clicks through the view first, then a real vibration. Compose's virtual key and
 * [View.performHapticFeedback] can be silently suppressed by the system touch-feedback setting,
 * which is what happens on some One UI builds. The vibrator is the fallback that still works
 * when that setting is off.
 */
class KeyHaptics(private val enabled: Boolean, private val sink: HapticSink) {
    /** Returns whether the device took a haptic; a disabled key makes no call at all. */
    @Suppress("DEPRECATION") // The flag is deprecated on recent SDKs but is still the way to override the setting.
    fun tap(): Boolean {
        if (!enabled) return false
        val flags = HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        if (sink.perform(HapticFeedbackConstants.KEYBOARD_TAP, flags)) return true
        return sink.perform(HapticFeedbackConstants.CLOCK_TICK, flags)
    }
}

class ViewHapticSink(private val view: View) : HapticSink {
    override fun perform(constant: Int, flags: Int): Boolean = view.performHapticFeedback(constant, flags)
}

/**
 * A real vibration first, then the view's haptic as a fallback for a device without a vibrator.
 * The app's own haptics switch is the authority, so this deliberately does not consult the
 * system touch-feedback setting.
 */
class DeviceHapticSink(
    private val vibrate: () -> Boolean,
    private val fallback: HapticSink,
) : HapticSink {
    override fun perform(constant: Int, flags: Int): Boolean =
        if (vibrate()) true else fallback.perform(constant, flags)
}

private fun systemVibrator(context: Context): Vibrator? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

private fun vibrateTick(vibrator: Vibrator?): Boolean {
    if (vibrator == null || !vibrator.hasVibrator()) return false
    // A short, strong tick. Predefined ticks are quieter on several Samsung builds.
    val effect = VibrationEffect.createOneShot(15L, VibrationEffect.DEFAULT_AMPLITUDE)
    return runCatching {
        vibrator.vibrate(effect)
        true
    }.getOrDefault(false)
}

@Composable
fun rememberKeyHaptics(enabled: Boolean): KeyHaptics {
    val view = LocalView.current
    val context = LocalContext.current.applicationContext
    return remember(enabled, view, context) {
        val vibrator = systemVibrator(context)
        KeyHaptics(
            enabled = enabled,
            sink = DeviceHapticSink(
                vibrate = { vibrateTick(vibrator) },
                fallback = ViewHapticSink(view),
            ),
        )
    }
}
