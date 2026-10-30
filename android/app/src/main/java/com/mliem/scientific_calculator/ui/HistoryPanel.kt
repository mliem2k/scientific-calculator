package com.mliem.scientific_calculator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mliem.scientific_calculator.R
import com.mliem.scientific_calculator.engine.CalculatorState
import com.mliem.scientific_calculator.engine.HistoryEntry
import com.mliem.scientific_calculator.engine.Key
import com.mliem.scientific_calculator.engine.Segment
import com.mliem.scientific_calculator.engine.spoken
import java.text.DateFormat
import java.util.Date

/** The operator keys kept beside the list, so a picked result can be carried on into a new calculation. */
private val SIDE_KEYS = listOf(
    Triple(Key.Delete, "⌫", R.string.key_delete),
    Triple(Key.Divide, "÷", R.string.key_divide),
    Triple(Key.Times, "×", R.string.key_multiply),
    Triple(Key.Minus, "−", R.string.key_minus),
    Triple(Key.Plus, "+", R.string.key_plus),
)

/**
 * History as the ColorOS calculator shows it: the live display stays on top, and the keypad area becomes a
 * list of cards next to a slim column of operator keys. Tapping a card inserts its result.
 */
@Composable
fun HistoryPanel(
    state: CalculatorState,
    haptics: Boolean,
    onKey: (Key) -> Unit,
    onJump: (List<Segment>, Int) -> Unit,
    onInsert: (HistoryEntry) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = LocalCalcColors.current
    val haptic = LocalHapticFeedback.current
    var confirmClear by remember { mutableStateOf(false) }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            containerColor = colors.surface,
            title = { Text(stringResource(R.string.delete_all_history_title), color = colors.text) },
            text = { Text(stringResource(R.string.delete_all_history_body), color = colors.dim) },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.cancel), color = colors.dim) } },
            confirmButton = { TextButton(onClick = { confirmClear = false; onClear() }) { Text(stringResource(R.string.delete_all), color = colors.accent) } },
        )
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconAction(R.drawable.ic_back, stringResource(R.string.back), onBack)
            BasicText(
                stringResource(R.string.history),
                Modifier.padding(start = 4.dp),
                style = TextStyle(color = colors.text, fontSize = 22.sp, fontWeight = FontWeight.Medium),
            )
            Spacer(Modifier.weight(1f))
            if (state.history.isNotEmpty()) IconAction(R.drawable.ic_delete, stringResource(R.string.delete_history)) { confirmClear = true }
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Expression(state, onKey, onJump, {}, Modifier.weight(1f).fillMaxWidth().padding(start = 8.dp))
            ResultLine(state.result)
        }
        Row(Modifier.weight(1.25f).fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
            Box(Modifier.weight(1f).fillMaxHeight()) {
                if (state.history.isEmpty()) {
                    BasicText(
                        stringResource(R.string.no_history),
                        Modifier.fillMaxWidth().padding(top = 32.dp),
                        style = TextStyle(color = colors.dim, fontSize = 16.sp, textAlign = TextAlign.Center),
                    )
                } else {
                    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(state.history, key = { it.id }) { entry -> HistoryCard(entry) { onInsert(entry) } }
                    }
                }
            }
            Column(
                Modifier.padding(start = 12.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                SIDE_KEYS.forEach { (key, label, name) ->
                    GlossyKey(label, KeyRole.Func, stringResource(name), Modifier.size(58.dp)) {
                        if (haptics) haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                        onKey(key)
                    }
                }
                GlossyKey("=", KeyRole.Equals, stringResource(R.string.key_equals), Modifier.size(58.dp)) {
                    if (haptics) haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                    onKey(Key.Equals)
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(entry: HistoryEntry, onClick: () -> Unit) {
    val colors = LocalCalcColors.current
    val style = remember(colors) { MathStyle(colors.dim, colors.dim, colors.accent) }
    val description = stringResource(R.string.restore_description, entry.expression.spoken(), entry.result)
    val date = remember(entry.timestamp) { DateFormat.getDateInstance(DateFormat.SHORT).format(Date(entry.timestamp)) }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Box(Modifier.fillMaxWidth()) { MathNodes(entry.expression, 16f, style) }
        BasicText(
            entry.result,
            Modifier.padding(top = 2.dp),
            style = TextStyle(color = colors.text, fontSize = 26.sp, fontWeight = FontWeight.Medium),
            maxLines = 1,
        )
        BasicText(date, Modifier.padding(top = 4.dp), style = TextStyle(color = colors.dim.copy(alpha = 0.7f), fontSize = 12.sp))
    }
}
