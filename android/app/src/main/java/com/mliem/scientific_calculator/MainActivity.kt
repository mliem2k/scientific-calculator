package com.mliem.scientific_calculator

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mliem.scientific_calculator.engine.CalculatorState
import com.mliem.scientific_calculator.engine.Key
import com.mliem.scientific_calculator.ui.CalculatorTheme
import com.mliem.scientific_calculator.ui.CalculatorViewModel
import com.mliem.scientific_calculator.ui.ConverterGrid
import com.mliem.scientific_calculator.ui.CurrencyScreen
import com.mliem.scientific_calculator.ui.Display
import com.mliem.scientific_calculator.ui.HistoryScreen
import com.mliem.scientific_calculator.ui.LocalCalcColors
import com.mliem.scientific_calculator.ui.NumberPad
import com.mliem.scientific_calculator.ui.NumberSystemScreen
import com.mliem.scientific_calculator.ui.Screen
import com.mliem.scientific_calculator.ui.ScientificPad
import com.mliem.scientific_calculator.ui.SettingsScreen
import com.mliem.scientific_calculator.ui.UnitConverterScreen
import com.mliem.scientific_calculator.ui.UpdateDialog
import com.mliem.scientific_calculator.ui.rememberSpeaker
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: CalculatorViewModel by viewModels { CalculatorViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { !viewModel.ready.value }
        enableEdgeToEdge()
        setContent { CalculatorRoot(viewModel) }
    }
}

@Composable
private fun CalculatorRoot(viewModel: CalculatorViewModel) {
    val ready by viewModel.ready.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val state by viewModel.calc.collectAsStateWithLifecycle()
    val screen by viewModel.screen.collectAsStateWithLifecycle()
    val prompt by viewModel.updatePrompt.collectAsStateWithLifecycle()
    val checking by viewModel.checking.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()
    val currencyRefreshing by viewModel.currencyRefreshing.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val talkBackOn = remember {
        (context.getSystemService(AccessibilityManager::class.java)).isTouchExplorationEnabled
    }
    val voiceOn = settings.voiceFeedback && !talkBackOn
    val speaker = rememberSpeaker(voiceOn)
    // The answer is read out after equals; keys are read as they are pressed.
    LaunchedEffect(state.lastResult) {
        if (voiceOn) state.lastResult?.let { speaker.speak(it) }
    }

    CalculatorTheme(settings.theme) {
        val colors = LocalCalcColors.current
        // The background fills the whole window, including behind the system bars; only content is inset.
        Column(Modifier.fillMaxSize().background(colors.background).windowInsetsPadding(WindowInsets.safeDrawing)) {
            if (!ready) return@Column
            BackHandler(enabled = screen.parent != null) { screen.parent?.let(viewModel::open) }
            AnimatedContent(
                targetState = screen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen",
            ) { current ->
                when (current) {
                    Screen.Calculator -> CalculatorScreen(viewModel, state, settings.hapticFeedback, voiceOn, speaker::speak)
                    Screen.History -> HistoryScreen(
                        history = state.history,
                        onBack = { viewModel.open(Screen.Calculator) },
                        onClear = viewModel::clearHistory,
                        onRestore = viewModel::restore,
                    )
                    Screen.Settings -> SettingsScreen(
                        settings = settings,
                        checking = checking,
                        onBack = { viewModel.open(Screen.Calculator) },
                        onTheme = viewModel::setTheme,
                        onAngle = viewModel::setDefaultAngleMode,
                        onPrecision = viewModel::setResultPrecision,
                        onHaptics = viewModel::setHapticFeedback,
                        onVoice = viewModel::setVoiceFeedback,
                        talkBackOn = talkBackOn,
                        onCheckUpdates = { viewModel.checkForUpdates(manual = true) },
                    )
                    Screen.Converters -> ConverterGrid(onBack = { viewModel.open(Screen.Calculator) }, onOpen = viewModel::open)
                    is Screen.UnitConverter -> UnitConverterScreen(current.category, settings.resultPrecision) { viewModel.open(Screen.Converters) }
                    Screen.NumberSystemConverter -> NumberSystemScreen { viewModel.open(Screen.Converters) }
                    Screen.CurrencyConverter -> CurrencyScreen(currency, currencyRefreshing, viewModel::refreshCurrency) { viewModel.open(Screen.Converters) }
                }
            }
            prompt?.let { UpdateDialog(it, viewModel::dismissUpdatePrompt) }
        }
    }
}

@Composable
private fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    state: CalculatorState,
    haptics: Boolean,
    voice: Boolean,
    speak: (String) -> Unit,
) {
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var scientific by rememberSaveable { mutableStateOf(false) }
    val speakKey: (String) -> Unit = { if (voice) speak(it) }
    val onPasteRequest: () -> Unit = {
        scope.launch {
            val text = clipboard.getClipEntry()?.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()
            if (text != null && !viewModel.paste(text)) {
                Toast.makeText(context, R.string.paste_invalid, Toast.LENGTH_SHORT).show()
            }
        }
    }
    val onKey: (Key) -> Unit = viewModel::press
    Column(Modifier.fillMaxSize()) {
        Display(
            state = state,
            scientific = scientific,
            onToggleScientific = { scientific = !scientific },
            onKey = onKey,
            onJump = viewModel::jumpTo,
            onPasteRequest = onPasteRequest,
            onFloating = {
                if (Settings.canDrawOverlays(context)) {
                    context.startService(Intent(context, FloatingCalculatorService::class.java))
                    (context as? Activity)?.moveTaskToBack(true)
                } else {
                    Toast.makeText(context, R.string.overlay_permission_needed, Toast.LENGTH_LONG).show()
                    context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
                }
            },
            onConverters = { viewModel.open(Screen.Converters) },
            onHistory = { viewModel.open(Screen.History) },
            onSettings = { viewModel.open(Screen.Settings) },
            modifier = Modifier.weight(if (scientific) 0.7f else 1f),
        )
        if (scientific) {
            ScientificPad(state, haptics, onKey, Modifier.weight(1.55f), speakKey)
        } else {
            NumberPad(state, haptics, onKey, Modifier.weight(1.25f), speakKey)
        }
    }
}
