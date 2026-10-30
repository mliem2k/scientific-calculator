package com.mliem.scientific_calculator.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mliem.scientific_calculator.R
import com.mliem.scientific_calculator.data.CurrencyRates
import com.mliem.scientific_calculator.engine.ConversionUnit
import com.mliem.scientific_calculator.engine.NumberSystem
import com.mliem.scientific_calculator.engine.NumberSystems
import com.mliem.scientific_calculator.engine.UnitCategory
import com.mliem.scientific_calculator.engine.Units
import com.mliem.scientific_calculator.engine.formatNumber
import java.text.DateFormat
import java.util.Currency
import java.util.Date

private const val MAX_INPUT = 15

@StringRes
private fun UnitCategory.title() = when (this) {
    UnitCategory.Length -> R.string.category_length
    UnitCategory.Area -> R.string.category_area
    UnitCategory.Volume -> R.string.category_volume
    UnitCategory.Weight -> R.string.category_weight
    UnitCategory.Temperature -> R.string.category_temperature
    UnitCategory.Speed -> R.string.category_speed
    UnitCategory.Pressure -> R.string.category_pressure
    UnitCategory.Power -> R.string.category_power
}

@DrawableRes
private fun UnitCategory.icon() = when (this) {
    UnitCategory.Length -> R.drawable.ic_cat_length
    UnitCategory.Area -> R.drawable.ic_cat_area
    UnitCategory.Volume -> R.drawable.ic_cat_volume
    UnitCategory.Weight -> R.drawable.ic_cat_weight
    UnitCategory.Temperature -> R.drawable.ic_cat_temperature
    UnitCategory.Speed -> R.drawable.ic_cat_speed
    UnitCategory.Pressure -> R.drawable.ic_cat_pressure
    UnitCategory.Power -> R.drawable.ic_cat_power
}

@Composable
fun ConverterGrid(onBack: () -> Unit, onOpen: (Screen) -> Unit) {
    val colors = LocalCalcColors.current
    ScreenShell(stringResource(R.string.unit_converter_title), onBack) {
        LazyVerticalGrid(
            GridCells.Fixed(3),
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val tiles: List<Triple<Screen, Int, @Composable () -> Unit>> = buildList {
                UnitCategory.entries.forEach { category ->
                    add(Triple(Screen.UnitConverter(category), category.title()) {
                        Image(painterResource(category.icon()), null, Modifier.size(32.dp), colorFilter = ColorFilter.tint(colors.text))
                    })
                }
                add(Triple(Screen.NumberSystemConverter, R.string.category_number_system) {
                    BasicText("01", style = TextStyle(color = colors.text, fontSize = 22.sp, fontWeight = FontWeight.Bold))
                })
                add(Triple(Screen.CurrencyConverter, R.string.category_currency) {
                    BasicText("$", style = TextStyle(color = colors.text, fontSize = 26.sp, fontWeight = FontWeight.Medium))
                })
            }
            gridItems(tiles) { (screen, title, icon) ->
                val name = stringResource(title)
                Column(
                    Modifier.fillMaxWidth().heightIn(min = 120.dp).clip(RoundedCornerShape(24.dp)).background(colors.surface)
                        .clickable { onOpen(screen) }.semantics { contentDescription = name }.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    icon()
                    Spacer(Modifier.size(10.dp))
                    BasicText(name, style = TextStyle(color = colors.text, fontSize = 14.sp))
                }
            }
        }
    }
}

/** One side of a conversion: a tappable unit name above a large value. */
@Composable
private fun ConversionCard(label: String, value: String, selected: Boolean, onSelect: () -> Unit, onPickUnit: () -> Unit) {
    val colors = LocalCalcColors.current
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(colors.surface).clickable(onClick = onSelect).padding(20.dp),
    ) {
        Row(
            Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onPickUnit).heightIn(min = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(label, style = TextStyle(color = colors.dim, fontSize = 16.sp, fontWeight = FontWeight.Medium))
            BasicText("  ›", style = TextStyle(color = colors.dim, fontSize = 18.sp))
        }
        BasicText(
            value.ifEmpty { "0" },
            modifier = Modifier.padding(top = 6.dp),
            style = TextStyle(color = if (selected) colors.text else colors.dim, fontSize = 36.sp, fontWeight = FontWeight.Light),
            maxLines = 1,
        )
    }
}

private sealed interface PadKeyKind {
    data class Char(val c: kotlin.Char, val enabled: Boolean = true) : PadKeyKind
    data object Clear : PadKeyKind
    data object Back : PadKeyKind
    data object Sign : PadKeyKind
    data object Blank : PadKeyKind
}

@Composable
private fun ConversionPad(rows: List<List<PadKeyKind>>, onChar: (Char) -> Unit, onClear: () -> Unit, onBack: () -> Unit, onSign: () -> Unit, modifier: Modifier) {
    val gap = 12.dp
    BoxWithConstraints(modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp)) {
        val diameter = minOf((maxWidth - gap * 3) / 4, (maxHeight - gap * (rows.size - 1)) / rows.size)
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            rows.forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    row.forEach { kind ->
                        val keyModifier = Modifier.size(diameter)
                        when (kind) {
                            PadKeyKind.Blank -> Spacer(keyModifier)
                            PadKeyKind.Clear -> GlossyKey("AC", KeyRole.Func, stringResource(R.string.key_all_clear), keyModifier, onClick = onClear)
                            PadKeyKind.Back -> GlossyKey("⌫", KeyRole.Func, stringResource(R.string.key_delete), keyModifier, onClick = onBack)
                            PadKeyKind.Sign -> GlossyKey("±", KeyRole.Func, stringResource(R.string.sign_toggle), keyModifier, onClick = onSign)
                            is PadKeyKind.Char -> {
                                val label = if (kind.c == 'Z') "00" else kind.c.toString()
                                GlossyKey(
                                    label, KeyRole.Digit, if (kind.c == '.') stringResource(R.string.key_decimal_point) else label,
                                    keyModifier.alpha(if (kind.enabled) 1f else 0.35f),
                                ) { if (kind.enabled) onChar(kind.c) }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun String.append(c: Char, replaceStart: Boolean): String {
    val base = if (replaceStart) "" else this
    return when {
        c == 'Z' -> if (base.isEmpty() || base == "0" || base == "-0") (if (base.startsWith("-")) "-0" else "0") else base.append('0', false).append('0', false)
        base.length >= MAX_INPUT -> base
        c == '.' -> if ('.' in base) base else if (base.isEmpty() || base == "-") "${base}0." else "$base."
        base == "0" -> c.toString()
        base == "-0" -> "-$c"
        else -> base + c
    }
}

private fun String.toInput(): Double? = if (isEmpty() || this == "-" ) 0.0 else toDoubleOrNull()

private fun unitLabel(unit: ConversionUnit) = "${unit.name} (${unit.symbol})"

@Composable
private fun <T> PickerDialog(title: String, items: List<T>, label: (T) -> String, onPick: (T) -> Unit, onDismiss: () -> Unit, searchable: Boolean = false) {
    val colors = LocalCalcColors.current
    var query by remember { mutableStateOf("") }
    val shown = if (query.isBlank()) items else items.filter { label(it).contains(query.trim(), ignoreCase = true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text(title, color = colors.text) },
        text = {
            Column {
                if (searchable) {
                    BasicTextField(
                        query, { query = it },
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(colors.sciKey).padding(12.dp),
                        textStyle = TextStyle(color = colors.text, fontSize = 16.sp),
                        cursorBrush = SolidColor(colors.accent),
                        singleLine = true,
                        decorationBox = { inner ->
                            if (query.isEmpty()) BasicText(stringResource(R.string.search_currency), style = TextStyle(color = colors.dim, fontSize = 16.sp))
                            inner()
                        },
                    )
                    Spacer(Modifier.size(8.dp))
                }
                if (shown.isEmpty()) {
                    BasicText(stringResource(R.string.no_results), Modifier.padding(16.dp), style = TextStyle(color = colors.dim, fontSize = 16.sp))
                }
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(shown) { item ->
                        BasicText(
                            label(item),
                            Modifier.fillMaxWidth().clickable { onPick(item) }.padding(vertical = 14.dp, horizontal = 4.dp),
                            style = TextStyle(color = colors.text, fontSize = 16.sp),
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = colors.accent) } },
    )
}

@Composable
fun UnitConverterScreen(category: UnitCategory, precision: Int, onBack: () -> Unit) {
    val units = checkNotNull(Units.byCategory[category])
    val defaults = remember(category) { Units.defaults(category) }
    var topId by rememberSaveable(category) { mutableStateOf(defaults.first.id) }
    var bottomId by rememberSaveable(category) { mutableStateOf(defaults.second.id) }
    var input by rememberSaveable(category) { mutableStateOf("1") }
    var typed by rememberSaveable(category) { mutableStateOf(false) }
    var topActive by rememberSaveable(category) { mutableStateOf(true) }
    var picking by rememberSaveable(category) { mutableStateOf<Boolean?>(null) }
    val top = units.first { it.id == topId }
    val bottom = units.first { it.id == bottomId }
    val active = if (topActive) top else bottom
    val other = if (topActive) bottom else top
    val shownOther = formatNumber(Units.convert(category, input.toInput() ?: 0.0, active, other), precision)

    fun select(top: Boolean) {
        if (top == topActive) return
        // The other card's value becomes what gets edited.
        input = shownOther
        topActive = top
        typed = false
    }
    val onChar: (Char) -> Unit = { c -> input = input.append(c, replaceStart = !typed); typed = true }
    val pad = buildList {
        add(listOf(PadKeyKind.Char('7'), PadKeyKind.Char('8'), PadKeyKind.Char('9'), PadKeyKind.Clear))
        add(listOf(PadKeyKind.Char('4'), PadKeyKind.Char('5'), PadKeyKind.Char('6'), PadKeyKind.Back))
        add(listOf(PadKeyKind.Char('1'), PadKeyKind.Char('2'), PadKeyKind.Char('3'), if (category == UnitCategory.Temperature) PadKeyKind.Sign else PadKeyKind.Blank))
        add(listOf(PadKeyKind.Char('Z'), PadKeyKind.Char('0'), PadKeyKind.Char('.'), PadKeyKind.Blank))
    }
    ScreenShell(stringResource(R.string.conversion_title, stringResource(category.title())), onBack, titleSize = 22) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ConversionCard(unitLabel(top), if (topActive) input else shownOther, topActive, { select(true) }, { picking = true })
                ConversionCard(unitLabel(bottom), if (!topActive) input else shownOther, !topActive, { select(false) }, { picking = false })
            }
            Spacer(Modifier.weight(1f))
            ConversionPad(
                pad, onChar,
                onClear = { input = "0"; typed = true },
                onBack = { input = input.dropLast(1).ifEmpty { "0" }; typed = true },
                onSign = { input = if (input.startsWith("-")) input.drop(1) else "-$input"; typed = true },
                modifier = Modifier.weight(1.4f),
            )
        }
    }
    picking?.let { forTop ->
        PickerDialog(stringResource(R.string.select_unit), units, ::unitLabel, onPick = {
            if (forTop) topId = it.id else bottomId = it.id
            picking = null
        }, onDismiss = { picking = null })
    }
}

@Composable
fun NumberSystemScreen(onBack: () -> Unit) {
    val systems = NumberSystem.entries
    var topSystem by rememberSaveable { mutableStateOf(NumberSystem.Binary) }
    var bottomSystem by rememberSaveable { mutableStateOf(NumberSystem.Decimal) }
    var input by rememberSaveable { mutableStateOf("1") }
    var typed by rememberSaveable { mutableStateOf(false) }
    var topActive by rememberSaveable { mutableStateOf(true) }
    var picking by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val active = if (topActive) topSystem else bottomSystem
    val other = if (topActive) bottomSystem else topSystem
    val shownOther = NumberSystems.convert(input, active, other).orEmpty()
    val allowed = NumberSystems.digitsFor(active)

    fun select(top: Boolean) {
        if (top == topActive) return
        input = shownOther.ifEmpty { "0" }
        topActive = top
        typed = false
    }
    fun pick(digit: Char) = PadKeyKind.Char(digit, enabled = digit in allowed)
    val pad = listOf(
        listOf(pick('7'), pick('8'), pick('9'), PadKeyKind.Clear),
        listOf(pick('4'), pick('5'), pick('6'), PadKeyKind.Back),
        listOf(pick('1'), pick('2'), pick('3'), pick('F')),
        listOf(PadKeyKind.Char('Z', enabled = true), pick('0'), PadKeyKind.Char('.', enabled = false), pick('E')),
        listOf(pick('A'), pick('B'), pick('C'), pick('D')),
    )
    ScreenShell(stringResource(R.string.conversion_title, stringResource(R.string.category_number_system)), onBack, titleSize = 22) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ConversionCard("${topSystem.label} ${topSystem.abbreviation}", if (topActive) input else shownOther, topActive, { select(true) }, { picking = true })
                ConversionCard("${bottomSystem.label} ${bottomSystem.abbreviation}", if (!topActive) input else shownOther, !topActive, { select(false) }, { picking = false })
            }
            Spacer(Modifier.weight(1f))
            ConversionPad(
                pad,
                onChar = { c ->
                    if (c != '.') {
                        val next = if (c == 'Z') "00" else c.toString()
                        input = if (!typed || input == "0") next.trimStart('0').ifEmpty { "0" } else (input + next).take(MAX_INPUT)
                        typed = true
                    }
                },
                onClear = { input = "0"; typed = true },
                onBack = { input = input.dropLast(1).ifEmpty { "0" }; typed = true },
                onSign = {},
                modifier = Modifier.weight(1.6f),
            )
        }
    }
    picking?.let { forTop ->
        PickerDialog(stringResource(R.string.select_unit), systems, { "${it.label} ${it.abbreviation}" }, onPick = {
            val current = if (forTop) topSystem else bottomSystem
            // Keep what is being edited readable in its new base.
            if (forTop == topActive) input = NumberSystems.convert(input, current, it) ?: "0"
            if (forTop) topSystem = it else bottomSystem = it
            picking = null
        }, onDismiss = { picking = null })
    }
}

sealed interface CurrencyUiState {
    data object Loading : CurrencyUiState
    data class Ready(val rates: CurrencyRates, val failed: Boolean = false) : CurrencyUiState
    data object Unavailable : CurrencyUiState
}

@Composable
fun CurrencyScreen(state: CurrencyUiState, refreshing: Boolean, onRefresh: () -> Unit, onBack: () -> Unit) {
    val colors = LocalCalcColors.current
    val rates = (state as? CurrencyUiState.Ready)?.rates
    var topCode by rememberSaveable { mutableStateOf(localCurrency()) }
    var bottomCode by rememberSaveable { mutableStateOf(if (topCode == "USD") "EUR" else "USD") }
    var input by rememberSaveable { mutableStateOf("1") }
    var typed by rememberSaveable { mutableStateOf(false) }
    var topActive by rememberSaveable { mutableStateOf(true) }
    var picking by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val activeCode = if (topActive) topCode else bottomCode
    val otherCode = if (topActive) bottomCode else topCode
    val shownOther = rates?.convert(input.toInput() ?: 0.0, activeCode, otherCode)?.let { formatNumber(it, 2).takeIf { s -> s != "0" || (input.toInput() ?: 0.0) == 0.0 } ?: formatNumber(it) } ?: "0"

    fun select(top: Boolean) {
        if (top == topActive) return
        input = shownOther
        topActive = top
        typed = false
    }
    val pad = listOf(
        listOf(PadKeyKind.Char('7'), PadKeyKind.Char('8'), PadKeyKind.Char('9'), PadKeyKind.Clear),
        listOf(PadKeyKind.Char('4'), PadKeyKind.Char('5'), PadKeyKind.Char('6'), PadKeyKind.Back),
        listOf(PadKeyKind.Char('1'), PadKeyKind.Char('2'), PadKeyKind.Char('3'), PadKeyKind.Blank),
        listOf(PadKeyKind.Char('Z'), PadKeyKind.Char('0'), PadKeyKind.Char('.'), PadKeyKind.Blank),
    )
    ScreenShell(stringResource(R.string.currency_converter), onBack, titleSize = 22) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ConversionCard(currencyLabel(topCode), if (topActive) input else shownOther, topActive, { select(true) }, { picking = true })
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    GlossyKey(
                        "⇅", KeyRole.Sci, stringResource(R.string.swap_currencies), Modifier.size(40.dp),
                    ) {
                        val t = topCode; topCode = bottomCode; bottomCode = t
                    }
                }
                ConversionCard(currencyLabel(bottomCode), if (!topActive) input else shownOther, !topActive, { select(false) }, { picking = false })
            }
            val status = when {
                refreshing -> stringResource(R.string.currency_updating)
                state is CurrencyUiState.Ready && state.failed -> stringResource(R.string.currency_update_failed)
                state is CurrencyUiState.Unavailable -> stringResource(R.string.currency_update_failed)
                rates != null -> stringResource(R.string.currency_updated, DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(rates.updatedAtMillis)))
                else -> stringResource(R.string.currency_updating)
            }
            BasicText(
                status,
                Modifier.fillMaxWidth().clickable(onClick = onRefresh).padding(horizontal = 20.dp, vertical = 12.dp),
                style = TextStyle(color = colors.dim, fontSize = 13.sp),
            )
            Spacer(Modifier.weight(1f))
            ConversionPad(
                pad,
                onChar = { c -> input = input.append(c, replaceStart = !typed); typed = true },
                onClear = { input = "0"; typed = true },
                onBack = { input = input.dropLast(1).ifEmpty { "0" }; typed = true },
                onSign = {},
                modifier = Modifier.weight(1.7f),
            )
        }
    }
    picking?.let { forTop ->
        PickerDialog(
            stringResource(R.string.select_currency), rates?.codes.orEmpty(), ::currencyLabel,
            onPick = { if (forTop) topCode = it else bottomCode = it; picking = null },
            onDismiss = { picking = null }, searchable = true,
        )
    }
}

private fun localCurrency(): String = runCatching { Currency.getInstance(java.util.Locale.getDefault()).currencyCode }.getOrDefault("USD")

private fun currencyLabel(code: String): String {
    val name = runCatching { Currency.getInstance(code).displayName }.getOrNull()
    return if (name == null || name == code) code else "$code  $name"
}
