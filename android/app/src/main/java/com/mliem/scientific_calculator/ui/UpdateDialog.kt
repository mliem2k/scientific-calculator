package com.mliem.scientific_calculator.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.mliem.scientific_calculator.R
import com.mliem.scientific_calculator.data.Updater

@Composable
fun UpdateDialog(prompt: UpdatePrompt, onDismiss: () -> Unit) {
    val colors = LocalCalcColors.current
    val context = LocalContext.current
    val available = prompt as? UpdatePrompt.Available
    val title = when (prompt) {
        is UpdatePrompt.Available -> R.string.update_available
        UpdatePrompt.UpToDate -> R.string.up_to_date
        UpdatePrompt.Failed -> R.string.check_failed
    }
    val body = when (prompt) {
        is UpdatePrompt.Available -> stringResource(R.string.update_ready, Updater.formatTag(prompt.info.tagName))
        UpdatePrompt.UpToDate -> stringResource(R.string.up_to_date_body)
        UpdatePrompt.Failed -> stringResource(R.string.check_failed_body)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text(stringResource(title), color = colors.text) },
        text = { Text(body, color = colors.dim) },
        dismissButton = if (available != null) {
            { TextButton(onClick = onDismiss) { Text(stringResource(R.string.later), color = colors.dim) } }
        } else null,
        confirmButton = {
            TextButton(onClick = {
                // The system browser downloads the APK and hands it to the package installer.
                available?.info?.let { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it.apkUrl ?: it.releaseUrl))) }
                onDismiss()
            }) {
                val label = when {
                    available == null -> R.string.got_it
                    available.info.apkUrl != null -> R.string.download
                    else -> R.string.open_in_browser
                }
                Text(stringResource(label), color = colors.accent)
            }
        },
    )
}
