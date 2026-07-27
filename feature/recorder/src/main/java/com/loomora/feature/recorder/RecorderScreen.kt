package com.loomora.feature.recorder

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.loomora.core.audio.model.RecorderErrorType
import com.loomora.core.audio.model.RecorderState
import com.loomora.core.designsystem.R
import com.loomora.core.designsystem.component.AudioWaveform
import com.loomora.core.designsystem.component.LoomoraTopAppBar
import com.loomora.core.designsystem.component.RecorderStatusPill
import com.loomora.core.designsystem.component.StatusPillType
import java.util.Locale

@Composable
fun RecorderRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecorderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.state) {
        if (uiState.state is RecorderState.Saved) {
            onNavigateBack()
        }
    }

    RecorderScreen(
        uiState = uiState,
        onStartRecording = { title -> viewModel.startRecording(context, title) },
        onPauseRecording = { viewModel.pauseRecording(context) },
        onResumeRecording = { viewModel.resumeRecording(context) },
        onStopRecording = { viewModel.stopRecording(context) },
        onAddMarker = { viewModel.addMarker(context) },
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}

@Composable
fun RecorderScreen(
    uiState: RecorderUiState,
    onStartRecording: (String) -> Unit,
    onPauseRecording: () -> Unit,
    onResumeRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onAddMarker: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
    }

    var showStopConfirmation by remember { mutableStateOf(false) }
    val amplitudeHistory = remember { mutableStateListOf<Float>() }
    val hasActiveRecording = uiState.state is RecorderState.Recording || uiState.state is RecorderState.Paused
    val defaultRecordingTitle = stringResource(id = R.string.recorder_default_title)

    BackHandler(enabled = hasActiveRecording) {
        showStopConfirmation = true
    }

    LaunchedEffect(uiState.amplitude) {
        if (uiState.state is RecorderState.Recording) {
            amplitudeHistory.add(uiState.amplitude)
            if (amplitudeHistory.size > 100) {
                amplitudeHistory.removeAt(0)
            }
        }
    }

    Scaffold(
        topBar = {
            LoomoraTopAppBar(
                title = stringResource(id = R.string.nav_recorder),
                onBackClick = {
                    if (hasActiveRecording) {
                        showStopConfirmation = true
                    } else {
                        onNavigateBack()
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            if (!hasMicPermission) {
                // Point-of-use Permission Rationale Screen
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(id = R.string.onboarding_permission_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(id = R.string.onboarding_permission_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                    ) {
                        Text(text = stringResource(id = R.string.recorder_permission_grant))
                    }
                }
            } else {
                // Main Active Recorder View
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val statusPillType = when (uiState.state) {
                        is RecorderState.Preparing -> StatusPillType.PREPARING
                        is RecorderState.Recording -> StatusPillType.RECORDING
                        is RecorderState.Paused -> StatusPillType.PAUSED
                        is RecorderState.Finalizing,
                        is RecorderState.Saving -> StatusPillType.FINALIZING
                        else -> StatusPillType.PREPARING
                    }

                    val statusLabel = when (uiState.state) {
                        is RecorderState.Idle,
                        is RecorderState.Ready -> stringResource(id = R.string.recorder_status_ready)
                        is RecorderState.Preparing -> stringResource(id = R.string.recorder_status_preparing)
                        is RecorderState.Recording -> stringResource(id = R.string.recorder_status_recording)
                        is RecorderState.Paused -> stringResource(id = R.string.recorder_status_paused)
                        is RecorderState.Finalizing -> stringResource(id = R.string.recorder_status_finalizing)
                        is RecorderState.Saving -> stringResource(id = R.string.recorder_status_saving)
                        is RecorderState.Saved -> stringResource(id = R.string.recorder_status_saved)
                        is RecorderState.Error -> stringResource(id = R.string.recorder_status_error)
                    }

                    RecorderStatusPill(type = statusPillType, label = statusLabel)

                    Spacer(modifier = Modifier.height(32.dp))

                    // Duration Timer (Authoritative timestamp, not UI-only timer!)
                    val durationMs = when (val s = uiState.state) {
                        is RecorderState.Recording -> s.durationMs
                        is RecorderState.Paused -> s.durationMs
                        else -> 0L
                    }
                    val seconds = durationMs / 1000
                    val timerText = String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60)

                    Text(
                        text = timerText,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Real Audio Waveform derived from microphone RMS/Peak amplitudes
                    AudioWaveform(amplitudes = amplitudeHistory)

                    if (uiState.state is RecorderState.Error) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = recorderErrorMessage(uiState.state.type),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }

	                // Controls Row (Record, Pause/Resume, Add Marker, Stop)
	                Column(
	                    horizontalAlignment = Alignment.CenterHorizontally,
	                    modifier = Modifier.fillMaxWidth()
	                ) {
                        if (uiState.state is RecorderState.Idle || uiState.state is RecorderState.Ready || uiState.state is RecorderState.Saved || uiState.state is RecorderState.Error) {
                            Button(
                                onClick = { onStartRecording(defaultRecordingTitle) },
                                enabled = hasMicPermission
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = stringResource(id = R.string.recorder_action_record))
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }

	                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Add Marker Button
	                        IconButton(
	                            onClick = onAddMarker,
	                            enabled = uiState.state is RecorderState.Recording || uiState.state is RecorderState.Paused,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
	                                Icon(
	                                    imageVector = Icons.Default.Bookmark,
	                                    contentDescription = stringResource(id = R.string.recorder_cd_add_marker),
	                                    tint = MaterialTheme.colorScheme.primary
	                                )
                                Text(
                                    text = "${uiState.markersCount}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Pause / Resume Primary Button
                        IconButton(
                            onClick = {
                                when (uiState.state) {
                                    is RecorderState.Recording -> onPauseRecording()
                                    is RecorderState.Paused -> onResumeRecording()
                                    else -> {}
                                }
                            },
                            enabled = uiState.state is RecorderState.Recording || uiState.state is RecorderState.Paused,
                            modifier = Modifier
                                .size(72.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = if (uiState.state is RecorderState.Paused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = if (uiState.state is RecorderState.Paused) {
                                    stringResource(id = R.string.recorder_cd_resume)
                                } else {
                                    stringResource(id = R.string.recorder_cd_pause)
                                },
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Stop Button with deliberate input confirmation
	                        IconButton(
	                            onClick = { showStopConfirmation = true },
	                            enabled = uiState.state is RecorderState.Recording || uiState.state is RecorderState.Paused,
                            modifier = Modifier
                                .size(56.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = stringResource(id = R.string.recorder_cd_stop),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showStopConfirmation) {
        AlertDialog(
            onDismissRequest = { showStopConfirmation = false },
            title = { Text(text = stringResource(id = R.string.recorder_stop_dialog_title)) },
            text = { Text(text = stringResource(id = R.string.recorder_stop_dialog_message)) },
            confirmButton = {
                Button(
	                    onClick = {
	                        showStopConfirmation = false
	                        onStopRecording()
	                    }
                ) {
                    Text(text = stringResource(id = R.string.recorder_action_stop_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopConfirmation = false }) {
                    Text(text = stringResource(id = R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun recorderErrorMessage(type: RecorderErrorType): String {
    val messageResId = when (type) {
        RecorderErrorType.START_FAILED -> R.string.recorder_error_start_failed
        RecorderErrorType.PAUSE_FAILED -> R.string.recorder_error_pause_failed
        RecorderErrorType.RESUME_FAILED -> R.string.recorder_error_resume_failed
        RecorderErrorType.FINALIZE_FAILED -> R.string.recorder_error_finalize_failed
        RecorderErrorType.SAVE_FAILED -> R.string.recorder_error_save_failed
    }
    return stringResource(id = messageResId)
}
