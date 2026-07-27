package com.loomora.feature.editor

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loomora.core.audio.waveform.WaveformLoadState
import com.loomora.core.designsystem.component.AudioWaveform
import com.loomora.core.designsystem.component.ErrorState
import com.loomora.core.designsystem.component.LoomoraTopAppBar

@Composable
fun EditorRoute(
    recordingId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditorViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.shareIntent) {
        val shareIntent = uiState.shareIntent ?: return@LaunchedEffect
        context.startActivity(Intent.createChooser(shareIntent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        viewModel.consumeShareIntent()
    }

    EditorScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onUpdateSelection = viewModel::updateSelection,
        onApplyTrim = viewModel::applyTrim,
        onApplyDeleteSelection = viewModel::applyDeleteSelection,
        onToggleSpeechClarity = viewModel::toggleSpeechClarity,
        onUndo = viewModel::undo,
        onRedo = viewModel::redo,
        onExport = viewModel::exportRecording,
        onCancelExport = viewModel::cancelExport,
        onShareExport = viewModel::shareLastExportedRecording,
        modifier = modifier
    )
}

@Composable
fun EditorScreen(
    uiState: EditorUiState,
    onNavigateBack: () -> Unit,
    onUpdateSelection: (Long, Long) -> Unit,
    onApplyTrim: () -> Unit,
    onApplyDeleteSelection: () -> Unit,
    onToggleSpeechClarity: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onExport: () -> Unit,
    onCancelExport: () -> Unit,
    onShareExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            LoomoraTopAppBar(
                title = "Non-Destructive Audio Editor",
                onBackClick = onNavigateBack,
                actions = {
                    IconButton(onClick = onUndo, enabled = uiState.canUndo) {
                        Icon(imageVector = Icons.Default.Undo, contentDescription = "Undo")
                    }
                    IconButton(onClick = onRedo, enabled = uiState.canRedo) {
                        Icon(imageVector = Icons.Default.Redo, contentDescription = "Redo")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (uiState.recording == null) {
                ErrorState(
                    title = "Recording Not Found",
                    message = "Could not load recording for editing.",
                    retryText = "Back",
                    onRetryClick = onNavigateBack,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                val recording = uiState.recording

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = recording.title,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Non-destructive editing preserves your original recording intact.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Edit Preview",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Estimated output duration: ${uiState.previewDurationMs} ms",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (uiState.previewSegments.isEmpty()) {
                                    Text(
                                        text = "Preview uses the full original recording until you apply edits.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    uiState.previewSegments.forEachIndexed { index, segment ->
                                        Text(
                                            text = "Segment ${index + 1}: ${segment.startMs} - ${segment.endMs} ms",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (uiState.validationIssue != null) {
                                    Text(
                                        text = "Current recipe needs adjustment before export.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }

                    // Waveform Timeline View
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Timeline Waveform",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                when (val waveform = uiState.waveform) {
                                    WaveformLoadState.Idle,
                                    WaveformLoadState.Loading -> {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    }

                                    is WaveformLoadState.Error -> {
                                        ErrorState(
                                            title = "Waveform unavailable",
                                            message = "Loomora could not load the saved waveform for this recording."
                                        )
                                    }

                                    is WaveformLoadState.Ready -> {
                                        AudioWaveform(amplitudes = waveform.waveform.bins)
                                    }
                                }
                            }
                        }
                    }

                    // Accessible Time Selection Numerical Inputs
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Selection Range (ms)",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    var startText by remember(uiState.selectionStartMs) { mutableStateOf(uiState.selectionStartMs.toString()) }
                                    var endText by remember(uiState.selectionEndMs) { mutableStateOf(uiState.selectionEndMs.toString()) }

                                    OutlinedTextField(
                                        value = startText,
                                        onValueChange = {
                                            startText = it
                                            val start = it.toLongOrNull() ?: 0L
                                            val end = endText.toLongOrNull() ?: recording.durationMs
                                            onUpdateSelection(start, end)
                                        },
                                        label = { Text("Start Ms") },
                                        modifier = Modifier.weight(1f)
                                    )

                                    OutlinedTextField(
                                        value = endText,
                                        onValueChange = {
                                            endText = it
                                            val start = startText.toLongOrNull() ?: 0L
                                            val end = it.toLongOrNull() ?: recording.durationMs
                                            onUpdateSelection(start, end)
                                        },
                                        label = { Text("End Ms") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // Editor Action Buttons (Trim, Delete Range)
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = onApplyTrim,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.ContentCut, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Trim Selection")
                            }

                            OutlinedButton(
                                onClick = onApplyDeleteSelection,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Delete Range")
                            }
                        }
                    }

                    // Speech Clarity Enhancement Toggle
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Speech Clarity Enhancement",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Applies high-pass filter & loudness normalization",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = uiState.recipe.isSpeechClarityEnabled,
                                    onCheckedChange = { onToggleSpeechClarity() }
                                )
                            }
                        }
                    }

                    // Export Non-Destructive Recording CTA
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onExport,
                            enabled = !uiState.isExporting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            if (uiState.isExporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(imageVector = Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Export as New Recording")
                            }
                        }

                        if (uiState.isExporting && uiState.exportProgress != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { uiState.exportProgress / 100f },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = onCancelExport,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = "Cancel Export")
                            }
                        }

                        if (uiState.exportSuccessMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = uiState.exportSuccessMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (uiState.lastExportedRecording != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = onShareExport,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Share Exported Recording")
                            }
                        }

                        if (uiState.errorMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = uiState.errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
