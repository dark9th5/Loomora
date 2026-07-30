package com.loomora.feature.recordingdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.loomora.core.datastore.DefaultAnalysisMode

@Composable
internal fun RenameRecordingDialog(currentTitle: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var title by remember(currentTitle) { mutableStateOf(currentTitle) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.detail_rename_recording)) },
        text = { OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.detail_title_field)) }, singleLine = true) },
        confirmButton = { TextButton(onClick = { onConfirm(title) }, enabled = title.isNotBlank()) { Text(stringResource(R.string.detail_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.detail_cancel)) } }
    )
}

@Composable
internal fun RenameSpeakerDialog(genericLabel: String, currentName: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember(genericLabel, currentName) { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.detail_rename_speaker, genericLabel)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.detail_display_name)) }, singleLine = true)
                Text(stringResource(R.string.detail_speaker_rename_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text(stringResource(R.string.detail_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.detail_cancel)) } }
    )
}

@Composable
internal fun EditInsightsDialog(
    currentTitle: String,
    currentSummary: String,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember(currentTitle) { mutableStateOf(currentTitle) }
    var summary by remember(currentSummary) { mutableStateOf(currentSummary) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.detail_edit_insights)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.detail_insight_title_field)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.detail_insight_summary_field)) },
                    minLines = 3,
                    maxLines = 6
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title, summary) },
                enabled = title.isNotBlank() && summary.isNotBlank()
            ) { Text(stringResource(R.string.detail_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.detail_cancel)) } }
    )
}

@Composable
internal fun DeleteRecordingDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.detail_delete_title)) },
        text = { Text(stringResource(R.string.detail_delete_message)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.detail_delete)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.detail_cancel)) } }
    )
}

@Composable
internal fun AnalysisModeDialog(selectedMode: DefaultAnalysisMode, onModeSelected: (DefaultAnalysisMode) -> Unit, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.detail_ai_check_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.detail_ai_check_message))
                DefaultAnalysisMode.entries.forEach { mode ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selectedMode == mode, { onModeSelected(mode) })
                        Text(stringResource(when (mode) {
                            DefaultAnalysisMode.QUICK_TRANSCRIPT -> R.string.detail_mode_quick
                            DefaultAnalysisMode.TRANSCRIPT_AND_INSIGHTS -> R.string.detail_mode_insights
                            DefaultAnalysisMode.FULL_ANALYSIS -> R.string.detail_mode_full
                        }))
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onConfirm) { Text(stringResource(R.string.detail_ai_check_action)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.detail_cancel)) } }
    )
}

@Composable
internal fun MissingModelDialog(
    missingDiarizationModel: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.detail_model_missing_title)) },
        text = {
            Text(
                stringResource(
                    if (missingDiarizationModel) R.string.detail_speaker_model_missing_message
                    else R.string.detail_model_missing_message
                )
            )
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.detail_model_missing_action))
            }
        }
    )
}
