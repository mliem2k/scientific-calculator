package com.mliem.scientific_calculator

import android.os.Bundle
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mliem.scientific_calculator.engine.Key
import com.mliem.scientific_calculator.ui.CalculatorTheme
import com.mliem.scientific_calculator.ui.CalculatorViewModel
import com.mliem.scientific_calculator.ui.Display
import com.mliem.scientific_calculator.ui.HistoryScreen
import com.mliem.scientific_calculator.ui.Keypad
import com.mliem.scientific_calculator.ui.LocalCalcColors
import com.mliem.scientific_calculator.ui.Screen
import com.mliem.scientific_calculator.ui.SettingsScreen
import com.mliem.scientific_calculator.ui.UpdateDialog
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

    CalculatorTheme(settings.theme) {
        val colors = LocalCalcColors.current
        // The background fills the whole window, including behind the system bars; only content is inset.
        Column(Modifier.fillMaxSize().background(colors.background).windowInsetsPadding(WindowInsets.safeDrawing)) {
            if (!ready) return@Column
            BackHandler(enabled = screen != Screen.Calculator) { viewModel.open(Screen.Calculator) }
            AnimatedContent(
                targetState = screen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen",
            ) { current ->
                when (current) {
                    Screen.Calculator -> CalculatorScreen(viewModel, state, settings.hapticFeedback)
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
                        onCheckUpdates = { viewModel.checkForUpdates(manual = true) },
                    )
                }
            }
            prompt?.let { UpdateDialog(it, viewModel::dismissUpdatePrompt) }
        }
    }
}

@Composable
private fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    state: com.mliem.scientific_calculator.engine.CalculatorState,
    haptics: Boolean,
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val onKey: (Key) -> Unit = { key ->
        if (key == Key.Paste) {
            scope.launch {
                clipboard.getClipEntry()?.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text
                    ?.let { viewModel.paste(it.toString()) }
            }
        } else {
            viewModel.press(key)
        }
    }
    Column(Modifier.fillMaxSize()) {
        Display(
            state = state,
            onKey = onKey,
            onJump = viewModel::jumpTo,
            onHistory = { viewModel.open(Screen.History) },
            onSettings = { viewModel.open(Screen.Settings) },
            modifier = Modifier.weight(1f),
        )
        Keypad(state, haptics, onKey, Modifier.weight(1.75f))
    }
}
