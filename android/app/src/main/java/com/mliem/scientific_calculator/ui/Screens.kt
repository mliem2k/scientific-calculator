package com.mliem.scientific_calculator.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import com.mliem.scientific_calculator.BuildConfig
import com.mliem.scientific_calculator.R
import com.mliem.scientific_calculator.data.PRECISION_CHOICES
import com.mliem.scientific_calculator.data.Settings
import com.mliem.scientific_calculator.data.ThemeChoice
import com.mliem.scientific_calculator.data.Updater
import com.mliem.scientific_calculator.engine.AngleMode
import com.mliem.scientific_calculator.engine.HistoryEntry
import com.mliem.scientific_calculator.engine.spoken

private const val REPO_URL = "https://github.com/mliem2k/scientific-calculator"

@Composable
fun ScreenShell(
    title: String,
    onBack: () -> Unit,
    action: (@Composable () -> Unit)? = null,
    titleSize: Int = 32,
    content: @Composable () -> Unit,
) {
    val colors = LocalCalcColors.current
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onBack)
                    .semantics { contentDescription = "" },
                contentAlignment = Alignment.Center,
            ) {
                Image(painterResource(R.drawable.ic_back), stringResource(R.string.back), Modifier.size(24.dp), colorFilter = ColorFilter.tint(colors.text))
            }
            Spacer(Modifier.weight(1f))
            action?.invoke()
        }
        BasicText(
            title,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp),
            style = TextStyle(color = colors.text, fontSize = titleSize.sp, fontWeight = FontWeight.SemiBold),
        )
        Box(Modifier.weight(1f).fillMaxWidth()) { content() }
    }
}

@Composable
private fun Section(label: String, content: @Composable () -> Unit) {
    val colors = LocalCalcColors.current
    Column(Modifier.padding(bottom = 20.dp)) {
        BasicText(label, Modifier.padding(start = 20.dp, bottom = 6.dp), style = TextStyle(color = colors.dim, fontSize = 13.sp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.surface)) { content() }
    }
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().padding(start = 16.dp).heightIn(min = 1.dp).background(LocalCalcColors.current.line))
}

@Composable
private fun Row(title: String, hint: String? = null, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    val colors = LocalCalcColors.current
    androidx.compose.foundation.layout.Row(
        modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            BasicText(title, style = TextStyle(color = colors.text, fontSize = 16.sp))
            if (hint != null) BasicText(hint, style = TextStyle(color = colors.dim, fontSize = 14.sp))
        }
        trailing()
    }
}

@Composable
fun SettingsScreen(
    settings: Settings,
    checking: Boolean,
    onBack: () -> Unit,
    onTheme: (ThemeChoice) -> Unit,
    onAngle: (AngleMode) -> Unit,
    onPrecision: (Int) -> Unit,
    onHaptics: (Boolean) -> Unit,
    onVoice: (Boolean) -> Unit,
    talkBackOn: Boolean,
    onCheckUpdates: () -> Unit,
) {
    val colors = LocalCalcColors.current
    val context = LocalContext.current
    ScreenShell(stringResource(R.string.settings), onBack) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp)) {
            ThemePicker(settings.theme, onTheme)

            Section(stringResource(R.string.calculator)) {
                Row(stringResource(R.string.default_angle_mode)) {
                    Row(Modifier.clip(RoundedCornerShape(12.dp)).background(colors.sciKey).padding(2.dp)) {
                        AngleMode.entries.forEach { mode ->
                            val selected = settings.defaultAngleMode == mode
                            BasicText(
                                stringResource(if (mode == AngleMode.Deg) R.string.degrees else R.string.radians),
                                modifier = Modifier
                                    .heightIn(min = 44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) colors.surface else Color.Transparent)
                                    .selectable(selected, role = Role.RadioButton) { onAngle(mode) }
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                style = TextStyle(color = if (selected) colors.text else colors.dim, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                            )
                        }
                    }
                }
                Divider()
                PrecisionRow(settings.resultPrecision, onPrecision)
                Divider()
                val haptic = stringResource(R.string.haptic_feedback)
                Row(haptic, stringResource(R.string.haptic_feedback_hint), Modifier.toggleable(settings.hapticFeedback, role = Role.Switch, onValueChange = onHaptics)) {
                    Switch(
                        checked = settings.hapticFeedback,
                        onCheckedChange = null,
                        colors = SwitchDefaults.colors(checkedTrackColor = colors.equalsBottom, checkedThumbColor = Color.White,
                            uncheckedTrackColor = colors.line, uncheckedThumbColor = Color.White, uncheckedBorderColor = Color.Transparent),
                    )
                }
            }

            Section(stringResource(R.string.voice_feedback)) {
                Row(
                    stringResource(R.string.voice_feedback),
                    stringResource(if (talkBackOn) R.string.voice_feedback_talkback else R.string.voice_feedback_hint),
                    Modifier.toggleable(settings.voiceFeedback && !talkBackOn, enabled = !talkBackOn, role = Role.Switch, onValueChange = onVoice),
                ) {
                    Switch(
                        checked = settings.voiceFeedback && !talkBackOn,
                        onCheckedChange = null,
                        enabled = !talkBackOn,
                        colors = SwitchDefaults.colors(checkedTrackColor = colors.equalsBottom, checkedThumbColor = Color.White,
                            uncheckedTrackColor = colors.line, uncheckedThumbColor = Color.White, uncheckedBorderColor = Color.Transparent),
                    )
                }
            }

            Section(stringResource(R.string.about)) {
                androidx.compose.foundation.layout.Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Image(painterResource(R.drawable.app_icon), null, Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)))
                    Column {
                        BasicText(stringResource(R.string.app_name), style = TextStyle(color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold))
                        BasicText(Updater.versionLabel(BuildConfig.BUILD_NUMBER), style = TextStyle(color = colors.dim, fontSize = 13.sp))
                    }
                }
                Divider()
                Row(
                    stringResource(R.string.check_for_updates),
                    stringResource(if (checking) R.string.checking else R.string.check_for_updates_hint),
                    Modifier.clickable(enabled = !checking, onClick = onCheckUpdates),
                )
                Divider()
                Row(
                    stringResource(R.string.source_code), "github.com/mliem2k/scientific-calculator",
                    Modifier.clickable(role = Role.Button) { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(REPO_URL))) },
                )
            }
            Spacer(Modifier.size(24.dp))
        }
    }
}

@Composable
private fun ThemePicker(selected: ThemeChoice, onSelect: (ThemeChoice) -> Unit) {
    val colors = LocalCalcColors.current
    Column(Modifier.padding(bottom = 20.dp)) {
        BasicText(stringResource(R.string.appearance), Modifier.padding(start = 20.dp, bottom = 6.dp), style = TextStyle(color = colors.dim, fontSize = 13.sp))
        androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeChoice.entries.forEach { choice ->
                val active = choice == selected
                val name = stringResource(when (choice) {
                    ThemeChoice.System -> R.string.theme_system
                    ThemeChoice.Light -> R.string.theme_light
                    ThemeChoice.Dark -> R.string.theme_dark
                    ThemeChoice.Amoled -> R.string.theme_amoled
                })
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(colors.surface)
                        .border(2.dp, if (active) colors.accent else Color.Transparent, RoundedCornerShape(20.dp))
                        .selectable(active, role = Role.RadioButton) { onSelect(choice) }
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ThemeSwatch(choice)
                    BasicText(name, style = TextStyle(color = if (active) colors.accent else colors.text, fontSize = 13.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal))
                }
            }
        }
    }
}

@Composable
private fun ThemeSwatch(choice: ThemeChoice) {
    // System shows both halves; every other choice shows its own palette twice.
    val left = colorsFor(choice, systemDark = false)
    val right = colorsFor(choice, systemDark = true)
    Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).border(1.dp, LocalCalcColors.current.line, RoundedCornerShape(14.dp))) {
        androidx.compose.foundation.layout.Row(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxSize().background(left.background), contentAlignment = Alignment.BottomCenter) {
                Box(Modifier.padding(bottom = 10.dp).size(14.dp).clip(RoundedCornerShape(5.dp)).background(left.equalsBottom))
            }
            Box(Modifier.weight(1f).fillMaxSize().background(right.background), contentAlignment = Alignment.BottomCenter) {
                Box(Modifier.padding(bottom = 10.dp).size(14.dp).clip(RoundedCornerShape(5.dp)).background(right.digitBottom))
            }
        }
    }
}

@Composable
private fun PrecisionRow(precision: Int, onPrecision: (Int) -> Unit) {
    val colors = LocalCalcColors.current
    var open by remember { mutableStateOf(false) }
    @Composable
    fun label(places: Int) =
        if (places == 0) stringResource(R.string.precision_automatic) else stringResource(R.string.precision_decimals, places)
    Row(stringResource(R.string.result_precision), modifier = Modifier.clickable(role = Role.DropdownList) { open = true }) {
        Box {
            BasicText(label(precision), style = TextStyle(color = colors.dim, fontSize = 16.sp))
            DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = colors.surface) {
                PRECISION_CHOICES.forEach { places ->
                    DropdownMenuItem(
                        text = { Text(label(places), color = colors.text) },
                        onClick = { onPrecision(places); open = false },
                    )
                }
            }
        }
    }
}
