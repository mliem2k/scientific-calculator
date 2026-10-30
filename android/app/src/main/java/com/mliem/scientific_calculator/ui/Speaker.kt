package com.mliem.scientific_calculator.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** Speaks key names and results. The engine starts when voice feedback is first switched on. */
class Speaker(context: Context) {
    private val appContext = context.applicationContext
    private var engine: TextToSpeech? = null
    private var ready = false

    fun start() {
        if (engine != null) return
        engine = TextToSpeech(appContext) { status -> ready = status == TextToSpeech.SUCCESS }
    }

    /** Interrupts whatever is being said, so a fast typist is not read back a queue of old keys. */
    fun speak(text: String) {
        if (ready) engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "calc")
    }

    fun stop() {
        engine?.shutdown()
        engine = null
        ready = false
    }
}

@Composable
fun rememberSpeaker(enabled: Boolean): Speaker {
    val context = LocalContext.current
    val speaker = remember { Speaker(context) }
    DisposableEffect(enabled) {
        if (enabled) speaker.start()
        onDispose { if (!enabled) speaker.stop() }
    }
    DisposableEffect(Unit) { onDispose { speaker.stop() } }
    return speaker
}
