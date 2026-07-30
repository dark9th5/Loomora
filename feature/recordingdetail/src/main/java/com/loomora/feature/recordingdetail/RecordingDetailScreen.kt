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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loomora.core.audio.model.PlayerState
import com.loomora.core.audio.waveform.WaveformLoadState
import com.loomora.core.designsystem.component.ErrorState
import com.loomora.core.designsystem.component.AudioWaveform
import com.loomora.core.designsystem.component.LoomoraTopAppBar
import com.loomora.core.designsystem.component.PlaybackControls
import com.loomora.core.model.AiJobStatus
import com.loomora.core.model.AiProcessingStage
import com.loomora.core.model.RecordingOperationResult
import com.loomora.core.datastore.DefaultAnalysisMode
import com.loomora.core.database.entity.RecordingTaskEntity
import com.loomora.core.offlineai.TranscriptSpeakerFusion
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingDetailScreen(
    uiState: RecordingDetailUiState,
    onNavigateBack: () -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onPlayFrom: (Long) -> Unit,
    onSeekForward: () -> Unit,
    onSeekRewind: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onToggleFavorite: () -> Unit,
    onUpdateTitle: (String) -> Unit,
    onShare: () -> Unit,
    onExport: () -> Unit,
    onShareInsights: () -> Unit,
    onExportInsights: () -> Unit,
    onSoftDelete: () -> Unit,
    onRestore: () -> Unit,
    onPermanentDelete: () -> Unit,
    onDismissOperationResult: () -> Unit,
    onStartAiProcessing: (DefaultAnalysisMode?) -> Unit,
    onResetAiStatus: () -> Unit,
    onCancelAiProcessing: () -> Unit,
    onDismissMissingModelMessage: () -> Unit,
    onRenameSpeaker: (String, String) -> Unit,
    onUpdateInsights: (String, String) -> Unit,
    onSetTaskCompleted: (String, Boolean) -> Unit,
    onUpdateTask: (String, String, String?, String?) -> Unit,
    onArchiveTask: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(RecordingDetailTab.OVERVIEW) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var speakerRenameTarget by remember { mutableStateOf<String?>(null) }
    var showConsentDialog by remember { mutableStateOf(false) }
    var selectedAnalysisMode by remember { mutableStateOf(DefaultAnalysisMode.TRANSCRIPT_AND_INSIGHTS) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var pendingDeleteAction by remember { mutableStateOf<DeleteAction?>(null) }
    var showInsightEditDialog by remember { mutableStateOf(false) }
    var taskEditTarget by remember { mutableStateOf<RecordingTaskEntity?>(null) }

    Scaffold(
        topBar = {
            LoomoraTopAppBar(
                title = stringResource(R.string.detail_title),
                onBackClick = onNavigateBack,
                actions = {
                    uiState.recording?.let { rec ->
                        IconButton(onClick = onToggleFavorite) {
                            Icon(
                                imageVector = if (rec.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = stringResource(
                                    if (rec.isFavorite) R.string.detail_cd_unfavorite else R.string.detail_cd_favorite
                                ),
                                tint = if (rec.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = onShare) {
                            Icon(
                                imageVector = Icons.Default.IosShare,
                                contentDescription = stringResource(R.string.detail_cd_share)
                            )
                        }
                        IconButton(onClick = onExport) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = stringResource(R.string.detail_cd_export)
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
                    title = stringResource(R.string.detail_not_found_title),
                    message = stringResource(R.string.detail_not_found_message),
                    retryText = stringResource(R.string.detail_go_back),
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

                    item {
                        PrimaryTabRow(selectedTabIndex = selectedTab.ordinal) {
                            RecordingDetailTab.entries.forEach { tab ->
                                Tab(
                                    selected = selectedTab == tab,
                                    onClick = { selectedTab = tab },
                                    text = { Text(stringResource(tab.titleRes)) }
                                )
                            }
                        }
                    }

                    // Header Title & Rename Card
                    item {
                        if (selectedTab == RecordingDetailTab.OVERVIEW) {
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
                                    val date = Date(recording.createdAt)
                                    val dateText = DateFormat.getDateInstance(
                                        DateFormat.MEDIUM,
                                        Locale.getDefault()
                                    ).format(date)
                                    val timeText = DateFormat.getTimeInstance(
                                        DateFormat.SHORT,
                                        Locale.getDefault()
                                    ).format(date)
                                    Text(
                                        text = "$dateText • $timeText",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { showRenameDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = stringResource(R.string.detail_rename),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        }
                    }

                    item {
                        if (selectedTab == RecordingDetailTab.OVERVIEW) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = stringResource(if (isTrashed) R.string.detail_trash_actions else R.string.detail_library_actions),
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
                                            Text(text = stringResource(R.string.detail_restore))
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
                                            Text(text = stringResource(R.string.detail_move_to_trash))
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
                                        Text(text = stringResource(R.string.detail_delete_forever))
                                    }
                                }
                            }
                        }
                        }
                    }

                    // Playback Controls
                    item {
                        if (selectedTab == RecordingDetailTab.OVERVIEW) {
                        if (!fileExists) {
                            ErrorState(
                                title = stringResource(R.string.detail_file_unavailable),
                                message = stringResource(R.string.detail_file_unavailable_message),
                                retryText = stringResource(R.string.detail_remove_entry),
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
                                                title = stringResource(R.string.detail_waveform_unavailable),
                                                message = stringResource(R.string.detail_waveform_unavailable_message)
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
                                        text = stringResource(R.string.detail_ai_title),
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
                                            Text(text = stringResource(R.string.detail_ai_queue))
                                        }
                                    }
                                    is AiJobStatus.ModelRequired -> {
                                        Text(
                                            text = stringResource(R.string.detail_ai_models_missing, status.requiredCapabilities.joinToString()),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(onClick = { showConsentDialog = true }) {
                                            Text(text = stringResource(R.string.detail_ai_recheck))
                                        }
                                    }
                                    is AiJobStatus.VerifyingModels -> {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(text = stringResource(R.string.detail_ai_verifying))
                                        }
                                    }
                                    is AiJobStatus.PreparingAudio -> {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(text = stringResource(R.string.detail_ai_preparing))
                                        }
                                    }
                                    is AiJobStatus.Queued -> {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(text = stringResource(R.string.detail_ai_queued))
                                        }
                                    }
                                    is AiJobStatus.Processing -> {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CircularProgressIndicator(
                                                progress = { status.overallProgress.coerceIn(0f, 1f) },
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(text = status.stage.localizedLabel())
                                        }
                                        TextButton(onClick = onCancelAiProcessing) {
                                            Text(stringResource(R.string.detail_cancel_analysis))
                                        }
                                    }
                                    is AiJobStatus.Partial -> {
                                        Text(
                                            text = stringResource(R.string.detail_ai_partial, status.transcript.size),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    is AiJobStatus.Cancelled -> {
                                        Text(
                                            text = stringResource(R.string.detail_ai_cancelled),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    is AiJobStatus.Completed -> {
                                        Text(
                                            text = stringResource(R.string.detail_ai_complete),
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        TextButton(onClick = { showConsentDialog = true }) {
                                            Text(stringResource(R.string.detail_analyze_again))
                                        }
                                    }
                                    is AiJobStatus.CompletedWithHeuristicFallback -> {
                                        Text(
                                            text = stringResource(R.string.detail_ai_heuristic_complete),
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = status.reason,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        TextButton(onClick = { showConsentDialog = true }) {
                                            Text(stringResource(R.string.detail_analyze_again))
                                        }
                                    }
                                    is AiJobStatus.Failed -> {
                                        ErrorState(
                                            title = status.stage?.let {
                                                stringResource(R.string.detail_ai_failed_stage, it.localizedLabel())
                                            } ?: stringResource(R.string.detail_ai_failed),
                                            message = status.message.ifBlank { stringResource(R.string.detail_ai_failed_message) },
                                            retryText = stringResource(R.string.detail_retry),
                                            onRetryClick = { onStartAiProcessing(null) }
                                        )
                                        TextButton(onClick = { showConsentDialog = true }) {
                                            Text(stringResource(R.string.detail_choose_analysis_mode))
                                        }
                                    }
                                }

                                if (selectedTab == RecordingDetailTab.INSIGHTS) uiState.insights?.let { revision ->
                                    val insights = revision.insights
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = insights.suggestedTitle,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(onClick = { showInsightEditDialog = true }) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = stringResource(R.string.detail_edit_insights)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = insights.summary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = stringResource(
                                            if (revision.generationMode == "HEURISTIC" || revision.completionQuality == "EXTRACTIVE_ONLY") {
                                                R.string.detail_insight_extractive
                                            } else {
                                                R.string.detail_insight_enhanced
                                            }
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(onClick = onShareInsights, modifier = Modifier.weight(1f)) {
                                            Icon(Icons.Default.IosShare, contentDescription = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text(stringResource(R.string.detail_share_insights))
                                        }
                                        OutlinedButton(onClick = onExportInsights, modifier = Modifier.weight(1f)) {
                                            Icon(Icons.Default.Download, contentDescription = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text(stringResource(R.string.detail_export_insights))
                                        }
                                    }
                                    InsightList(stringResource(R.string.detail_insight_key_points), insights.keyPoints)
                                    InsightList(stringResource(R.string.detail_insight_decisions), insights.decisions)
                                    InsightList(stringResource(R.string.detail_insight_suggestions), insights.suggestions)
                                    InsightList(stringResource(R.string.detail_insight_questions), insights.openQuestions)
                                    if (uiState.tasks.isNotEmpty()) {
                                        RecordingTaskList(
                                            tasks = uiState.tasks,
                                            onSetCompleted = onSetTaskCompleted,
                                            onEdit = { taskEditTarget = it },
                                            onArchive = onArchiveTask
                                        )
                                    } else if (insights.actionItems.isNotEmpty()) {
                                        InsightList(
                                            title = stringResource(R.string.detail_insight_actions),
                                            values = insights.actionItems.map { item ->
                                                buildString {
                                                    append(item.task)
                                                    item.assignee?.let { append(" • $it") }
                                                    item.dueDate?.let { append(" • $it") }
                                                }
                                            }
                                        )
                                    }
                                    if (insights.chapters.isNotEmpty()) {
                                        InsightList(
                                            title = stringResource(R.string.detail_insight_topics),
                                            values = insights.chapters.map { "${formatDuration(it.startMs)} ${it.title}" }
                                        )
                                    }
                                    if (insights.keyPoints.isEmpty() && insights.decisions.isEmpty() &&
                                        insights.actionItems.isEmpty() && uiState.tasks.isEmpty() && insights.openQuestions.isEmpty() &&
                                        insights.suggestions.isEmpty()
                                    ) {
                                        Text(
                                            text = stringResource(R.string.detail_insight_no_structured_items),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                val transcript = uiState.transcript
                                val aliasMap = uiState.speakerAliases.associate { it.genericLabel to it.displayName }
                                val speakerRows = remember(transcript?.segments) {
                                    TranscriptSpeakerFusion.displayRows(transcript?.segments.orEmpty())
                                }
                                if (selectedTab == RecordingDetailTab.INSIGHTS && speakerRows.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = stringResource(R.string.detail_speaker_timeline),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(R.string.detail_speaker_privacy),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(
                                            items = speakerRows,
                                            key = { "timeline-${it.startMs}-${it.endMs}-${it.speakerLabel.orEmpty()}-${it.text.hashCode()}" }
                                        ) { segment ->
                                            val genericLabel = segment.speakerLabel
                                            val displaySpeaker = genericLabel?.let { aliasMap[it] ?: it }
                                                ?: stringResource(R.string.detail_uncertain)
                                            Card(
                                                modifier = Modifier.widthIn(min = 300.dp, max = 420.dp),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = displaySpeaker,
                                                            style = MaterialTheme.typography.labelLarge,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                        if (genericLabel != null) {
                                                            TextButton(onClick = { speakerRenameTarget = genericLabel }) {
                                                                Text(text = stringResource(R.string.detail_rename))
                                                            }
                                                        }
                                                    }
                                                    Text(
                                                        text = "${formatDuration(segment.startMs)} - ${formatDuration(segment.endMs)}" +
                                                            if (segment.speakerIsUncertain) " • ${stringResource(R.string.detail_uncertain)}" else "",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = segment.text,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    TextButton(onClick = { onPlayFrom(segment.startMs) }) {
                                                        Text(formatDuration(segment.startMs))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                if (selectedTab == RecordingDetailTab.TRANSCRIPT && speakerRows.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = stringResource(R.string.detail_tab_transcript),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(
                                            items = speakerRows,
                                            key = { "transcript-${it.startMs}-${it.endMs}-${it.speakerLabel.orEmpty()}-${it.text.hashCode()}" }
                                        ) { segment ->
                                            val displaySpeaker = segment.speakerLabel?.let { aliasMap[it] ?: it }
                                            val isCurrent = currentPos in segment.startMs until segment.endMs
                                            Card(
                                                modifier = Modifier.widthIn(min = 300.dp, max = 420.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                                )
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        TextButton(onClick = { onSeekTo(segment.startMs) }) {
                                                            Text("${formatDuration(segment.startMs)} - ${formatDuration(segment.endMs)}")
                                                        }
                                                        displaySpeaker?.let {
                                                            Text(
                                                                text = if (segment.speakerIsUncertain) "$it • ${stringResource(R.string.detail_uncertain)}" else it,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        text = segment.text,
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
                    }

                    // Metadata Card & Markers
                    item {
                        if (selectedTab == RecordingDetailTab.OVERVIEW) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = stringResource(R.string.detail_technical_metadata),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.detail_mime_type, recording.mimeType),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.detail_size_kb, recording.sizeBytes / 1024),
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
    }

    if (showInsightEditDialog) {
        uiState.insights?.let { revision ->
            EditInsightsDialog(
                currentTitle = revision.insights.suggestedTitle,
                currentSummary = revision.insights.summary,
                onConfirm = { title, summary ->
                    onUpdateInsights(title, summary)
                    showInsightEditDialog = false
                },
                onDismiss = { showInsightEditDialog = false }
            )
        }
    }

    taskEditTarget?.let { task ->
        EditTaskDialog(
            task = task,
            onConfirm = { title, assignee, dueDate ->
                onUpdateTask(task.id, title, assignee, dueDate)
                taskEditTarget = null
            },
            onDismiss = { taskEditTarget = null }
        )
    }

    if (showRenameDialog && uiState.recording != null) {
        RenameRecordingDialog(
            currentTitle = uiState.recording.title,
            onConfirm = { onUpdateTitle(it); showRenameDialog = false },
            onDismiss = { showRenameDialog = false }
        )
    }
    speakerRenameTarget?.let { genericLabel ->
        RenameSpeakerDialog(
            genericLabel = genericLabel,
            currentName = uiState.speakerAliases.firstOrNull { it.genericLabel == genericLabel }?.displayName ?: genericLabel,
            onConfirm = { onRenameSpeaker(genericLabel, it); speakerRenameTarget = null },
            onDismiss = { speakerRenameTarget = null }
        )
    }
    if (showDeleteDialog && pendingDeleteAction != null) {
        DeleteRecordingDialog(
            onConfirm = { onPermanentDelete(); showDeleteDialog = false; pendingDeleteAction = null; onNavigateBack() },
            onDismiss = { showDeleteDialog = false; pendingDeleteAction = null }
        )
    }
    if (showConsentDialog) {
        AnalysisModeDialog(
            selectedMode = selectedAnalysisMode,
            onModeSelected = { selectedAnalysisMode = it },
            onConfirm = { showConsentDialog = false; onStartAiProcessing(selectedAnalysisMode) },
            onDismiss = { showConsentDialog = false }
        )
    }
    if (uiState.isTranscriptionModelMissing || uiState.isDiarizationModelMissing) {
        MissingModelDialog(
            missingDiarizationModel = uiState.isDiarizationModelMissing,
            onDismiss = onDismissMissingModelMessage
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
        RecordingOperationResult.Success -> stringResource(R.string.detail_result_done_title) to stringResource(R.string.detail_result_done_message)
        RecordingOperationResult.NotFound -> stringResource(R.string.detail_result_not_found_title) to stringResource(R.string.detail_result_not_found_message)
        RecordingOperationResult.SourceMissing -> stringResource(R.string.detail_result_source_missing_title) to stringResource(R.string.detail_result_source_missing_message)
        RecordingOperationResult.ExportCancelled -> stringResource(R.string.detail_result_export_cancelled_title) to stringResource(R.string.detail_result_export_cancelled_message)
        is RecordingOperationResult.LowStorage -> stringResource(R.string.detail_result_low_storage_title) to stringResource(R.string.detail_result_low_storage_message)
        is RecordingOperationResult.FileSystemFailure -> stringResource(R.string.detail_result_storage_failed_title) to stringResource(R.string.detail_result_storage_failed_message)
        is RecordingOperationResult.DatabaseFailure -> stringResource(R.string.detail_result_library_failed_title) to stringResource(R.string.detail_result_library_failed_message)
    }

    ErrorState(
        title = title,
        message = message,
        retryText = stringResource(R.string.detail_dismiss),
        onRetryClick = onDismiss,
        modifier = modifier
    )
}

@Composable
private fun RecordingTaskList(
    tasks: List<RecordingTaskEntity>,
    onSetCompleted: (String, Boolean) -> Unit,
    onEdit: (RecordingTaskEntity) -> Unit,
    onArchive: (String) -> Unit
) {
    if (tasks.isEmpty()) return
    val vietnamese = Locale.getDefault().language.equals("vi", ignoreCase = true)
    Spacer(modifier = Modifier.height(10.dp))
    Text(
        text = stringResource(R.string.detail_insight_actions),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(6.dp))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tasks.forEach { task ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Checkbox(
                        checked = task.isCompleted,
                        onCheckedChange = { onSetCompleted(task.id, it) }
                    )
                    Column(
                        modifier = Modifier.weight(1f).padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.bodyMedium,
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                            color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )
                        val metadata = buildList {
                            task.assignee?.let { add((if (vietnamese) "Phụ trách: " else "Owner: ") + it) }
                            task.dueDate?.let { add((if (vietnamese) "Hạn: " else "Due: ") + it) }
                        }
                        if (metadata.isNotEmpty()) {
                            Text(
                                text = metadata.joinToString(" • "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = if (task.sourceGenerationMode == "LLM_ENHANCED") {
                                if (vietnamese) "Tạo bởi AI cục bộ" else "Generated by local AI"
                            } else {
                                if (vietnamese) "Trích xuất cục bộ — cần xác nhận" else "Local extraction — review recommended"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column {
                        IconButton(onClick = { onEdit(task) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = if (vietnamese) "Sửa việc cần làm" else "Edit task"
                            )
                        }
                        IconButton(onClick = { onArchive(task.id) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = if (vietnamese) "Ẩn việc cần làm" else "Archive task"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightList(
    title: String,
    values: List<String>
) {
    if (values.isEmpty()) return
    Spacer(modifier = Modifier.height(10.dp))
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    values.forEach { value ->
        Text(
            text = "- $value",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private enum class DeleteAction {
    Permanent
}

private enum class RecordingDetailTab(val titleRes: Int) {
    OVERVIEW(R.string.detail_tab_overview),
    TRANSCRIPT(R.string.detail_tab_transcript),
    INSIGHTS(R.string.detail_tab_insights)
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

@Composable
private fun AiProcessingStage.localizedLabel(): String = stringResource(
    when (this) {
        AiProcessingStage.PREPARING_AUDIO -> R.string.detail_stage_preparing
        AiProcessingStage.TRANSCRIBING -> R.string.detail_stage_transcribing
        AiProcessingStage.DIARIZING -> R.string.detail_stage_diarizing
        AiProcessingStage.ALIGNING -> R.string.detail_stage_aligning
        AiProcessingStage.GENERATING_INSIGHTS -> R.string.detail_stage_insights
        AiProcessingStage.OPTIONAL_ENHANCEMENT -> R.string.detail_stage_enhancement
        AiProcessingStage.VALIDATING -> R.string.detail_stage_validating
        AiProcessingStage.PUBLISHING -> R.string.detail_stage_publishing
        AiProcessingStage.CLEANING_UP -> R.string.detail_stage_cleanup
        AiProcessingStage.CANCELLING -> R.string.detail_stage_cancelling
        AiProcessingStage.RUNNING -> R.string.detail_stage_running
    }
)
