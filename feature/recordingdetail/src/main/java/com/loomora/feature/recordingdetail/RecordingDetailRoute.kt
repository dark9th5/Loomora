package com.loomora.feature.recordingdetail

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel

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
        if (destinationUri == null) viewModel.onExportCancelled()
        else viewModel.exportRecording(destinationUri)
    }
    val insightsExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { destinationUri ->
        if (destinationUri != null) viewModel.exportInsights(destinationUri)
    }

    LaunchedEffect(uiState.shareIntent) {
        val shareIntent = uiState.shareIntent ?: return@LaunchedEffect
        context.startActivity(
            Intent.createChooser(
                shareIntent,
                context.getString(
                    if (shareIntent.type == "text/plain") R.string.detail_share_insights_chooser
                    else R.string.detail_share_chooser
                )
            )
        )
        viewModel.consumeShareIntent()
    }

    RecordingDetailScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onPlay = viewModel::playAudio,
        onPause = viewModel::pauseAudio,
        onResume = viewModel::resumeAudio,
        onSeekTo = viewModel::seekTo,
        onPlayFrom = viewModel::playFrom,
        onSeekForward = viewModel::seekForward,
        onSeekRewind = viewModel::seekRewind,
        onSpeedChange = viewModel::setPlaybackSpeed,
        onToggleFavorite = viewModel::toggleFavorite,
        onUpdateTitle = viewModel::updateTitle,
        onShare = viewModel::shareRecording,
        onExport = { exportLauncher.launch(uiState.suggestedExportFileName) },
        onShareInsights = viewModel::shareInsights,
        onExportInsights = { insightsExportLauncher.launch(uiState.suggestedInsightsFileName) },
        onSoftDelete = viewModel::softDeleteRecording,
        onRestore = viewModel::restoreRecording,
        onPermanentDelete = viewModel::permanentlyDeleteRecording,
        onDismissOperationResult = viewModel::clearOperationResult,
        onStartAiProcessing = { mode -> viewModel.startAiProcessing(true, mode) },
        onResetAiStatus = viewModel::resetAiStatus,
        onCancelAiProcessing = viewModel::cancelAiProcessing,
        onDismissMissingModelMessage = viewModel::dismissMissingModelMessage,
        onRenameSpeaker = viewModel::renameSpeaker,
        onUpdateInsights = viewModel::updateInsights,
        modifier = modifier
    )
}
