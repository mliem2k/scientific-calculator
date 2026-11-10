package com.mliem.scientific_calculator.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.mliem.android.update.UpdateInfo
import com.mliem.scientific_calculator.R

@Composable
fun UpdateDialog(prompt: UpdatePrompt, onInstall: (UpdateInfo) -> Unit, onDismiss: () -> Unit) {
    val colors = LocalCalcColors.current
    val context = LocalContext.current
    val available = prompt as? UpdatePrompt.Available
    // A download in progress cannot be dismissed; once the system installer has the file it can.
    val busy = (prompt as? UpdatePrompt.Installing)?.percent != null
    val title = when (prompt) {
        is UpdatePrompt.Available -> R.string.update_available
        UpdatePrompt.UpToDate -> R.string.up_to_date
        UpdatePrompt.Failed -> R.string.check_failed
        is UpdatePrompt.Installing -> R.string.update_available
        is UpdatePrompt.InstallFailed -> R.string.install_failed_title
    }
    val body = when (prompt) {
        is UpdatePrompt.Available -> stringResource(R.string.update_ready, formatTag(prompt.info.tag))
        UpdatePrompt.UpToDate -> stringResource(R.string.up_to_date_body)
        UpdatePrompt.Failed -> stringResource(R.string.check_failed_body)
        is UpdatePrompt.Installing ->
            if (prompt.percent != null) stringResource(R.string.update_downloading, prompt.percent) else stringResource(R.string.update_installing)
        is UpdatePrompt.InstallFailed -> prompt.message
    }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        containerColor = colors.surface,
        title = { Text(stringResource(title), color = colors.text) },
        text = { Text(body, color = colors.dim) },
        dismissButton = if (available != null) {
            { TextButton(onClick = onDismiss) { Text(stringResource(R.string.later), color = colors.dim) } }
        } else null,
        confirmButton = {
            if (!busy) TextButton(onClick = {
                val info = available?.info
                when {
                    info?.apkUrl != null -> onInstall(info)
                    info != null -> {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.releaseUrl)))
                        onDismiss()
                    }
                    else -> onDismiss()
                }
            }) {
                val label = when {
                    available == null -> R.string.got_it
                    available.info.apkUrl != null -> R.string.update
                    else -> R.string.open_in_browser
                }
                Text(stringResource(label), color = colors.accent)
            }
        },
    )
}
