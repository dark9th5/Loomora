package com.loomora.feature.recordingdetail

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.loomora.core.audio.model.PlayerState
import com.loomora.core.audio.waveform.WaveformLoadState
import com.loomora.core.designsystem.component.ErrorState
import com.loomora.core.designsystem.component.AudioWaveform
import com.loomora.core.designsystem.component.LoomoraTopAppBar
import com.loomora.core.designsystem.component.PlaybackControls
import com.loomora.core.model.AiJobStatus
import com.loomora.core.model.RecordingOperationResult
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecordingDetailRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecordingDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("audio/mp4")
    ) { destinationUri ->
        if (destinationUri == null) {
            viewModel.onExportCancelled()
        } else {
            viewModel.exportRecording(destinationUri)
        }
    }

    LaunchedEffect(uiState.shareIntent) {
        val shareIntent = uiState.shareIntent ?: return@LaunchedEffect
        context.startActivity(Intent.createChooser(shareIntent, "Share recording"))
        viewModel.consumeShareIntent()
    }

    RecordingDetailScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onPlay = viewModel::playAudio,
        onPause = viewModel::pauseAudio,
        onResume = viewModel::resumeAudio,
        onSeekTo = viewModel::seekTo,
        onSeekForward = viewModel::seekForward,
        onSeekRewind = viewModel::seekRewind,
        onSpeedChange = viewModel::setPlaybackSpeed,
        onToggleFavorite = viewModel::toggleFavorite,
        onUpdateTitle = viewModel::updateTitle,
        onShare = viewModel::shareRecording,
        onExport = {
            exportLauncher.launch(uiState.suggestedExportFileName)
        },
        onSoftDelete = viewModel::softDeleteRecording,
        onRestore = viewModel::restoreRecording,
        onPermanentDelete = viewModel::permanentlyDeleteRecording,
        onDismissOperationResult = viewModel::clearOperationResult,
        onStartAiProcessing = viewModel::startAiProcessing,
        onResetAiStatus = viewModel::resetAiStatus,
        modifier = modifier
    )
}

@Composable
fun RecordingDetailScreen(
    uiState: RecordingDetailUiState,
    onNavigateBack: () -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekForward: () -> Unit,
    onSeekRewind: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onToggleFavorite: () -> Unit,
    onUpdateTitle: (String) -> Unit,
    onShare: () -> Unit,
    onExport: () -> Unit,
    onSoftDelete: () -> Unit,
    onRestore: () -> Unit,
    onPermanentDelete: () -> Unit,
    onDismissOperationResult: () -> Unit,
    onStartAiProcessing: (Boolean) -> Unit,
    onResetAiStatus: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    var showConsentDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var pendingDeleteAction by remember { mutableStateOf<DeleteAction?>(null) }

    Scaffold(
        topBar = {
            LoomoraTopAppBar(
                title = "Recording Details",
                onBackClick = onNavigateBack,
                actions = {
                    uiState.recording?.let { rec ->
                        IconButton(onClick = onToggleFavorite) {
                            Icon(
                                imageVector = if (rec.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Favorite",
                                tint = if (rec.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = onShare) {
                            Icon(
                                imageVector = Icons.Default.IosShare,
                                contentDescription = "Share recording"
                            )
                        }
                        IconButton(onClick = onExport) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Export recording"
                            )
                        }
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
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.recording == null) {
                ErrorState(
                    title = "Recording Not Found",
                    message = "The requested recording metadata could not be retrieved from local database.",
                    retryText = "Go Back",
                    onRetryClick = onNavigateBack,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                val recording = uiState.recording
                val filePath = recording.originalFileUri.removePrefix("file://")
                val fileExists = File(filePath).exists()
                val isTrashed = recording.deletedAt != null

                val isPlaying = uiState.playerState is PlayerState.Playing
                val (currentPos, currentDur, currentSpeed) = when (val p = uiState.playerState) {
                    is PlayerState.Playing -> Triple(p.positionMs, p.durationMs, p.speed)
                    is PlayerState.Paused -> Triple(p.positionMs, p.durationMs, p.speed)
                    else -> Triple(0L, recording.durationMs, 1.0f)
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        uiState.operationResult?.let { result ->
                            OperationResultBanner(
                                result = result,
                                onDismiss = onDismissOperationResult,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Header Title & Rename Card
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
                                        text = recording.title,
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    val dateFormat = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
                                    Text(
                                        text = dateFormat.format(Date(recording.createdAt)),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { showRenameDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Rename",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = if (isTrashed) "Trash actions" else "Library actions",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    if (isTrashed) {
                                        OutlinedButton(
                                            onClick = onRestore,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.RestoreFromTrash,
                                                contentDescription = null
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(text = "Restore")
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = onSoftDelete,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = null
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(text = "Move to trash")
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            pendingDeleteAction = DeleteAction.Permanent
                                            showDeleteDialog = true
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = "Delete forever")
                                    }
                                }
                            }
                        }
                    }

                    // Playback Controls
                    item {
                        if (!fileExists) {
                            ErrorState(
                                title = "Recording file unavailable",
                                message = "This recording file is missing or can no longer be opened from local storage.",
                                retryText = "Remove entry",
                                onRetryClick = {
                                    pendingDeleteAction = DeleteAction.Permanent
                                    showDeleteDialog = true
                                }
                            )
                        } else {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    when (val waveform = uiState.waveform) {
                                        WaveformLoadState.Idle,
                                        WaveformLoadState.Loading -> {
                                            CircularProgressIndicator(
                                                modifier = Modifier
                                                    .padding(16.dp)
                                                    .size(24.dp)
                                            )
                                        }

                                        is WaveformLoadState.Error -> {
                                            ErrorState(
                                                title = "Waveform unavailable",
                                                message = "Loomora could not build a waveform preview for this recording."
                                            )
                                        }

                                        is WaveformLoadState.Ready -> {
                                            AudioWaveform(
                                                amplitudes = waveform.waveform.bins,
                                                playedFraction = if (recording.durationMs > 0L) {
                                                    (currentPos.toFloat() / recording.durationMs.toFloat()).coerceIn(0f, 1f)
                                                } else {
                                                    0f
                                                },
                                                onSeekFraction = { fraction ->
                                                    onSeekTo((fraction * recording.durationMs.toFloat()).toLong())
                                                },
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                    PlaybackControls(
                                        isPlaying = isPlaying,
                                        positionMs = currentPos,
                                        durationMs = if (currentDur > 0) currentDur else recording.durationMs,
                                        speed = currentSpeed,
                                        onPlayPauseToggle = {
                                            when (uiState.playerState) {
                                                is PlayerState.Playing -> onPause()
                                                is PlayerState.Paused -> onResume()
                                                else -> onPlay()
                                            }
                                        },
                                        onSeekTo = onSeekTo,
                                        onSeekForward = onSeekForward,
                                        onSeekRewind = onSeekRewind,
                                        onSpeedChange = onSpeedChange
                                    )
                                }
                            }
                        }
                    }

                    // Offline AI Transcript & Smart Insights Section
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Offline AI Transcript & Smart Insights",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                when (val status = uiState.aiJobStatus) {
                                    is AiJobStatus.Idle -> {
                                        Button(
                                            onClick = { showConsentDialog = true },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(text = "Queue Offline Analysis")
                                        }
                                    }
                                    is AiJobStatus.ModelRequired -> {
                                        Text(
                                            text = "Required offline models are missing for: ${status.requiredCapabilities.joinToString()}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(onClick = { showConsentDialog = true }) {
                                            Text(text = "Re-check Offline Models")
                                        }
                                    }
                                    is AiJobStatus.VerifyingModels -> {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(text = "Verifying offline model installation...")
                                        }
                                    }
                                    is AiJobStatus.PreparingAudio -> {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(text = "Preparing local audio package...")
                                        }
                                    }
                                    is AiJobStatus.Queued -> {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(text = "Offline analysis queued locally.")
                                        }
                                    }
                                    is AiJobStatus.Processing -> {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CircularProgressIndicator(
                                                progress = { status.progress.coerceIn(0f, 1f) },
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(text = status.stage)
                                        }
                                    }
                                    is AiJobStatus.Partial -> {
                                        Text(
                                            text = "Partial transcript available (${status.transcript.size} segments)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    is AiJobStatus.Cancelled -> {
                                        Text(
                                            text = "Offline transcription cancelled.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    is AiJobStatus.Completed -> {
                                        Text(
                                            text = "Transcript & Insights Complete",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    is AiJobStatus.Failed -> {
                                        ErrorState(
                                            title = "Offline Analysis Failed",
                                            message = status.message,
                                            retryText = "Retry",
                                            onRetryClick = { onStartAiProcessing(true) }
                                        )
                                    }
                                }

                                val transcript = uiState.transcript
                                if (transcript != null && transcript.segments.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Transcript",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    transcript.segments.forEach { segment ->
                                        OutlinedButton(
                                            onClick = { onSeekTo(segment.startMs) },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = "${formatDuration(segment.startMs)} - ${formatDuration(segment.endMs)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = segment.text,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Metadata Card & Markers
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Technical Metadata",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "MIME Type: ${recording.mimeType ?: "audio/aac"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Size: ${recording.sizeBytes / 1024} KB",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRenameDialog && uiState.recording != null) {
        var newTitleText by remember { mutableStateOf(uiState.recording.title) }

        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text(text = "Rename Recording") },
            text = {
                OutlinedTextField(
                    value = newTitleText,
                    onValueChange = { newTitleText = it },
                    label = { Text(text = "Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdateTitle(newTitleText)
                        showRenameDialog = false
                    }
                ) {
                    Text(text = "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text(text = "Cancel")
                }
            }
        )
    }

    if (showDeleteDialog && pendingDeleteAction != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                pendingDeleteAction = null
            },
            title = { Text(text = "Delete recording permanently?") },
            text = {
                Text(text = "This removes the recording entry and tries to remove local files that belong to it.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onPermanentDelete()
                        showDeleteDialog = false
                        pendingDeleteAction = null
                        onNavigateBack()
                    }
                ) {
                    Text(text = "Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        pendingDeleteAction = null
                    }
                ) {
                    Text(text = "Cancel")
                }
            }
        )
    }

    if (showConsentDialog) {
        AlertDialog(
            onDismissRequest = { showConsentDialog = false },
            title = { Text(text = "Offline Analysis Check") },
            text = {
                Text(
                    text = "Loomora keeps analysis on-device. This action checks local model availability and queues offline processing only when the required models are installed."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConsentDialog = false
                        onStartAiProcessing(true)
                    }
                ) {
                    Text(text = "Check & Queue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConsentDialog = false }) {
                    Text(text = "Cancel")
                }
            }
        )
    }
}

@Composable
private fun OperationResultBanner(
    result: RecordingOperationResult,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (title, message) = when (result) {
        RecordingOperationResult.Success -> "Done" to "The recording operation completed successfully."
        RecordingOperationResult.NotFound -> "Recording not found" to "The selected recording could not be found anymore."
        RecordingOperationResult.SourceMissing -> "Source file missing" to "The local audio file is missing or no longer readable."
        RecordingOperationResult.ExportCancelled -> "Export cancelled" to "The export was cancelled before a file was created."
        is RecordingOperationResult.LowStorage -> "Low storage" to "There is not enough free storage to finish this operation."
        is RecordingOperationResult.FileSystemFailure -> "Storage operation failed" to "Loomora could not finish the local file operation safely."
        is RecordingOperationResult.DatabaseFailure -> "Library update failed" to "Loomora could not update local library metadata."
    }

    ErrorState(
        title = title,
        message = message,
        retryText = "Dismiss",
        onRetryClick = onDismiss,
        modifier = modifier
    )
}

private enum class DeleteAction {
    Permanent
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
