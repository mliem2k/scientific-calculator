package com.mliem.scientific_calculator.ui

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/** The one thing [KeyHaptics] needs from a view, so a test can record the calls without a device. */
fun interface HapticSink {
    fun perform(constant: Int, flags: Int): Boolean
}

/**
 * Key clicks through the view instead of Compose's [androidx.compose.ui.hapticfeedback.HapticFeedbackType],
 * whose virtual key can be a no-op on some devices. [HapticFeedbackConstants.KEYBOARD_TAP] is the click the
 * platform is written for, and ignoring the global setting lets the app's own haptics switch decide. Recent
 * platforms deprecate the ignore flag, so a newer device may still drop the tap while system touch feedback
 * is off; the flag is passed anyway because older versions honour it.
 * [HapticFeedbackConstants.CLOCK_TICK] is the fallback for devices that do not report a keyboard tap; it
 * exists since API 21, so the minSdk 26 build needs no version guard.
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

@Composable
fun rememberKeyHaptics(enabled: Boolean): KeyHaptics {
    val view = LocalView.current
    return remember(enabled, view) { KeyHaptics(enabled, ViewHapticSink(view)) }
}
