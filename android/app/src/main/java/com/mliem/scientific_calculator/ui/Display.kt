package com.mliem.scientific_calculator.ui

import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ColorFilter
import com.mliem.scientific_calculator.R
import com.mliem.scientific_calculator.engine.AngleMode
import com.mliem.scientific_calculator.engine.CalculatorState
import com.mliem.scientific_calculator.engine.Key
import com.mliem.scientific_calculator.engine.Segment
import com.mliem.scientific_calculator.engine.spoken
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val BASE_SIZE = 46f
private const val MIN_SCALE = 0.3f
private const val DRAG_STEP_DP = 22

@Composable
fun Display(
    state: CalculatorState,
    onKey: (Key) -> Unit,
    onJump: (List<Segment>, Int) -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalCalcColors.current
    Column(modifier.padding(start = 20.dp, end = 12.dp, bottom = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val radians = state.angleMode == AngleMode.Rad
            BasicText(
                stringResource(if (radians) R.string.radians_short else R.string.degrees_short),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .widthIn(min = 48.dp)
                    .clickable { onKey(Key.DegRad) }
                    .semantics { contentDescription = "" }
                    .padding(vertical = 14.dp),
                style = TextStyle(color = colors.dim, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
            )
            if (state.hypActive) Badge(stringResource(R.string.badge_hyperbolic))
            if (state.stoMode) Badge(stringResource(R.string.badge_store))
            if (state.shiftActive) Badge(stringResource(R.string.badge_shift))
            Spacer(Modifier.weight(1f))
            IconAction(R.drawable.ic_history, stringResource(R.string.history), onHistory)
            IconAction(R.drawable.ic_settings, stringResource(R.string.settings), onSettings)
        }
        Expression(state, onKey, onJump, Modifier.weight(1f).fillMaxWidth())
        ResultLine(state.result)
    }
}

@Composable
private fun Badge(label: String) {
    val colors = LocalCalcColors.current
    BasicText(
        label,
        modifier = Modifier.padding(start = 8.dp).clip(CircleShape).background(colors.opKey).padding(horizontal = 10.dp, vertical = 4.dp),
        style = TextStyle(color = colors.opText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun IconAction(iconRes: Int, description: String, onClick: () -> Unit) {
    val colors = LocalCalcColors.current
    Box(
        Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onClick).semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Image(painterResource(iconRes), null, Modifier.size(22.dp), colorFilter = ColorFilter.tint(colors.dim))
    }
}

/**
 * The editable expression. Dragging steps the caret, which can enter and leave fractions, and the type
 * shrinks until stacked fractions fit the height. Height is linear in the type size, so one measurement
 * of the natural height gives the scale.
 */
@Composable
private fun Expression(state: CalculatorState, onKey: (Key) -> Unit, onJump: (List<Segment>, Int) -> Unit, modifier: Modifier) {
    val colors = LocalCalcColors.current
    val stepPx = with(LocalDensity.current) { DRAG_STEP_DP.dp.toPx() }
    val scroll = rememberScrollState()
    var scale by remember { mutableFloatStateOf(1f) }
    val empty = state.expression.isEmpty() && state.cursor.path.isEmpty() && state.cursor.insertAt == 0
    val description = stringResource(R.string.expression_description)
    val style = MathStyle(colors.text, colors.dim, colors.accent, state.cursor, onJump)

    BoxWithConstraints(
        modifier
            .semantics { contentDescription = description + " " + state.expression.spoken() }
            .pointerInput(stepPx) {
                var dx = 0f
                var dy = 0f
                detectDragGestures(onDragStart = { dx = 0f; dy = 0f }) { change, drag ->
                    change.consume()
                    dx += drag.x
                    dy += drag.y
                    if (abs(dx) >= stepPx && abs(dx) >= abs(dy)) {
                        onKey(if (dx > 0) Key.Right else Key.Left)
                        dx = 0f; dy = 0f
                    } else if (abs(dy) >= stepPx) {
                        onKey(if (dy > 0) Key.Down else Key.Up)
                        dx = 0f; dy = 0f
                    }
                }
            },
    ) {
        val availableHeight = constraints.maxHeight
        // Scrolling by touch is off: the drag above moves the caret, and the caret scrolls itself into view.
        Box(Modifier.fillMaxSize().horizontalScroll(scroll, enabled = false), contentAlignment = Alignment.BottomEnd) {
            Row(
                Modifier
                    .widthIn(min = this@BoxWithConstraints.maxWidth)
                    .padding(vertical = 4.dp)
                    .onSizeChanged { size ->
                        val height = size.height
                        val natural = height / scale
                        if (natural > 0f) {
                            val next = (availableHeight / natural).coerceIn(MIN_SCALE, 1f)
                            if (abs(next - scale) > 0.01f) scale = next
                        }
                    },
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (empty) {
                    BasicText(
                        stringResource(R.string.empty_expression),
                        style = TextStyle(color = colors.dim, fontSize = (BASE_SIZE * scale).sp, fontWeight = FontWeight.Light),
                    )
                } else {
                    MathNodes(state.expression, BASE_SIZE * scale, style)
                }
            }
        }
    }
}

@Composable
private fun ResultLine(result: String?) {
    val colors = LocalCalcColors.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1200)
            copied = false
        }
    }
    Box(Modifier.fillMaxWidth().height(44.dp), contentAlignment = Alignment.CenterEnd) {
        if (result != null) {
            val description = stringResource(R.string.result_description, result)
            BasicText(
                if (copied) stringResource(R.string.copied) else result,
                modifier = Modifier
                    .clickable {
                        scope.launch {
                            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("result", result)))
                            copied = true
                        }
                    }
                    .semantics { contentDescription = description }
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                style = TextStyle(
                    color = if (copied) colors.accent else colors.dim,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Light,
                ),
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}
