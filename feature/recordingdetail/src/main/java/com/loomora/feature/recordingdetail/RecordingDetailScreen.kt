package com.loomora.feature.recordingdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loomora.core.audio.model.PlayerState
import com.loomora.core.designsystem.component.ErrorState
import com.loomora.core.designsystem.component.LoomoraTopAppBar
import com.loomora.core.designsystem.component.PlaybackControls
import com.loomora.core.model.AiJobStatus
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
    onStartAiProcessing: (Boolean) -> Unit,
    onResetAiStatus: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    var showConsentDialog by remember { mutableStateOf(false) }

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

                    // Playback Controls
                    item {
                        if (!fileExists) {
                            ErrorState(
                                title = "File Missing or Corrupted",
                                message = "The audio file at $filePath is no longer accessible on device storage."
                            )
                        } else {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
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

                    // AI Transcribe & Smart Insights Section
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
                                        text = "AI Transcript & Smart Insights",
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
                                            Text(text = "Transcribe & Generate Summary")
                                        }
                                    }
                                    is AiJobStatus.ConsentRequired -> {
                                        Text(
                                            text = "Explicit user consent is required before transmitting audio data for cloud processing.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(onClick = { showConsentDialog = true }) {
                                            Text(text = "Review Disclosure & Consent")
                                        }
                                    }
                                    is AiJobStatus.Uploading -> {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(text = "Uploading audio package...")
                                        }
                                    }
                                    is AiJobStatus.Transcribing -> {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(text = "Transcribing audio content...")
                                        }
                                    }
                                    is AiJobStatus.Summarizing -> {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(text = "Extracting key insights & decisions...")
                                        }
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
                                            title = "AI Processing Failed",
                                            message = status.message,
                                            retryText = "Retry",
                                            onRetryClick = { onStartAiProcessing(true) }
                                        )
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

    if (showConsentDialog) {
        AlertDialog(
            onDismissRequest = { showConsentDialog = false },
            title = { Text(text = "Cloud AI Data Disclosure") },
            text = {
                Text(
                    text = "Loomora prioritizes your privacy. Transcribing and generating summaries requires sending audio data over secure HTTPS to AI services. No client application keys are stored in the app package. Do you consent to process this audio file?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConsentDialog = false
                        onStartAiProcessing(true)
                    }
                ) {
                    Text(text = "Agree & Process")
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
