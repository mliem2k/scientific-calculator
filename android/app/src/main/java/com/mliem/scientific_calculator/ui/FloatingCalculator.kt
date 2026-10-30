package com.mliem.scientific_calculator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mliem.scientific_calculator.R
import com.mliem.scientific_calculator.data.Settings
import com.mliem.scientific_calculator.engine.CalculatorState
import com.mliem.scientific_calculator.engine.Cursor
import com.mliem.scientific_calculator.engine.press

/**
 * The calculator in a small window that floats over other apps. It keeps its own scratch calculation and
 * does not write to the history, which belongs to the full screen app.
 */
@Composable
fun FloatingCalculator(
    settings: Settings,
    onClose: () -> Unit,
    onFullScreen: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onTransparency: (Boolean) -> Unit,
) {
    CalculatorTheme(settings.theme) {
        val colors = LocalCalcColors.current
        var state by remember { mutableStateOf(CalculatorState(angleMode = settings.defaultAngleMode)) }
        var translucent by remember { mutableStateOf(false) }
        Column(
            Modifier
                .width(300.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(colors.background)
                .border(1.dp, colors.line, RoundedCornerShape(32.dp)),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            onDrag(drag.x, drag.y)
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HeaderButton("✕", stringResource(R.string.close_calculator), onClose)
                Spacer(Modifier.weight(1f))
                HeaderButton(if (translucent) "◑" else "◐", stringResource(R.string.floating_transparency)) {
                    translucent = !translucent
                    onTransparency(translucent)
                }
                IconAction(R.drawable.ic_fullscreen, stringResource(R.string.full_screen), onFullScreen)
            }
            Column(Modifier.padding(horizontal = 8.dp)) {
                Expression(
                    state = state,
                    onKey = { state = state.press(it, settings.resultPrecision) },
                    onJump = { path, index -> state = state.copy(cursor = Cursor(path, index)) },
                    onPasteRequest = {},
                    modifier = Modifier.fillMaxWidth().height(96.dp),
                )
                ResultLine(state.result)
            }
            NumberPad(
                state = state,
                haptics = settings.hapticFeedback,
                onKey = { state = state.press(it, settings.resultPrecision) },
                modifier = Modifier.fillMaxWidth().height(300.dp),
            )
        }
    }
}

@Composable
private fun HeaderButton(label: String, description: String, onClick: () -> Unit) {
    val colors = LocalCalcColors.current
    Box(
        Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClick).semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        BasicText(label, style = TextStyle(color = colors.dim, fontSize = 18.sp, fontWeight = FontWeight.Medium))
    }
}
