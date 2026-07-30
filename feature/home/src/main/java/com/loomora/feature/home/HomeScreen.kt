package com.loomora.feature.home

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.loomora.core.designsystem.component.EmptyState
import com.loomora.core.designsystem.component.LoomoraTopAppBar
import com.loomora.core.designsystem.component.RecordingListItem
import com.loomora.core.designsystem.theme.LoomoraDimensions
import com.loomora.core.designsystem.theme.LoomoraSpacing
import java.text.DateFormat
import java.util.Date
import java.util.Locale

enum class HomeRecordingMode { MEETING, INTERVIEW, LECTURE, VOICE_NOTE }

@Composable
fun HomeRoute(
    onNavigateToRecorder: (HomeRecordingMode?) -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    HomeScreen(
        uiState = uiState,
        onNavigateToRecorder = onNavigateToRecorder,
        onNavigateToLibrary = onNavigateToLibrary,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToDetail = onNavigateToDetail,
        onFavoriteToggle = viewModel::toggleFavorite,
        modifier = modifier
    )
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onNavigateToRecorder: (HomeRecordingMode?) -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onFavoriteToggle: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            LoomoraTopAppBar(
                title = stringResource(R.string.home_title),
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.home_settings))
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().widthIn(max = LoomoraDimensions.contentMaxWidth),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = LoomoraSpacing.large,
                    end = LoomoraSpacing.large,
                    bottom = LoomoraSpacing.xxLarge
                ),
                verticalArrangement = Arrangement.spacedBy(LoomoraSpacing.large)
            ) {
                item {
                    Column {
                        Text(stringResource(R.string.home_greeting), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(LoomoraSpacing.xSmall))
                        Text(stringResource(R.string.home_tagline), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                item {
                    HomeRecordingHero(
                        title = stringResource(R.string.home_new_recording),
                        subtitle = stringResource(R.string.home_tagline),
                        actionDescription = stringResource(R.string.home_start_record_cta),
                        onRecordClick = { onNavigateToRecorder(null) }
                    )
                }
                item { Text(stringResource(R.string.home_quick_modes), style = MaterialTheme.typography.titleMedium) }
                item { QuickModeGrid(onSelect = { onNavigateToRecorder(it) }) }
                uiState.activeAiRecording?.let { active ->
                    item {
                        ActiveAiCard(
                            title = stringResource(R.string.home_active_ai_title),
                            message = stringResource(R.string.home_active_ai_message, active.title),
                            onClick = { onNavigateToDetail(active.id) }
                        )
                    }
                }
                item { TrialBanner(uiState.remainingTrialUses) }
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.home_recent_recordings), style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = onNavigateToLibrary) { Text(stringResource(R.string.home_view_all)) }
                    }
                }
                if (uiState.recentRecordings.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Default.GraphicEq,
                            title = stringResource(R.string.home_empty_title),
                            message = stringResource(R.string.home_empty_desc),
                            actionText = stringResource(R.string.home_start_record_cta),
                            onActionClick = { onNavigateToRecorder(null) }
                        )
                    }
                } else {
                    items(uiState.recentRecordings, key = { it.id }) { recording ->
                        val date = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(Date(recording.createdAt))
                        val seconds = recording.durationMs / 1000
                        RecordingListItem(
                            title = recording.title,
                            dateText = date,
                            durationText = String.format(Locale.getDefault(), "%d:%02d", seconds / 60, seconds % 60),
                            isFavorite = recording.isFavorite,
                            onItemClick = { onNavigateToDetail(recording.id) },
                            onFavoriteToggle = { onFavoriteToggle(recording.id, recording.isFavorite) },
                            statusText = recordingStatusText(
                                transcriptStatus = recording.transcriptStatus,
                                insightStatus = recording.insightStatus
                            ),
                            modifier = Modifier
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun recordingStatusText(transcriptStatus: String, insightStatus: String): String {
    val normalizedTranscript = transcriptStatus.uppercase(Locale.ROOT)
    val normalizedInsights = insightStatus.uppercase(Locale.ROOT)
    return when {
        normalizedTranscript.contains("PROCESS") || normalizedInsights.contains("PROCESS") ->
            stringResource(R.string.home_status_processing)
        normalizedTranscript.contains("FAIL") || normalizedInsights.contains("FAIL") ->
            stringResource(R.string.home_status_failed)
        normalizedInsights.contains("COMPLETE") || normalizedInsights.contains("READY") ->
            stringResource(R.string.home_status_transcript_insights)
        normalizedTranscript.contains("COMPLETE") || normalizedTranscript.contains("READY") ->
            stringResource(R.string.home_status_transcript)
        else -> stringResource(R.string.home_status_audio_only)
    }
}

@Composable
private fun QuickModeGrid(onSelect: (HomeRecordingMode) -> Unit) {
    val modes = listOf(
        Triple(HomeRecordingMode.MEETING, Icons.Default.Groups, R.string.home_mode_meeting to R.string.home_mode_meeting_desc),
        Triple(HomeRecordingMode.INTERVIEW, Icons.Default.Mic, R.string.home_mode_interview to R.string.home_mode_interview_desc),
        Triple(HomeRecordingMode.LECTURE, Icons.Default.School, R.string.home_mode_lecture to R.string.home_mode_lecture_desc),
        Triple(HomeRecordingMode.VOICE_NOTE, Icons.Default.Lightbulb, R.string.home_mode_voicenote to R.string.home_mode_voicenote_desc)
    )
    Column(verticalArrangement = Arrangement.spacedBy(LoomoraSpacing.small)) {
        modes.chunked(2).forEach { rowModes ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LoomoraSpacing.small)) {
                rowModes.forEach { (mode, icon, labels) ->
                    ModeCard(icon, stringResource(labels.first), stringResource(labels.second), { onSelect(mode) }, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ModeCard(icon: ImageVector, title: String, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier.clickable(onClick = onClick), shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(LoomoraSpacing.medium), verticalArrangement = Arrangement.spacedBy(LoomoraSpacing.small)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ActiveAiCard(title: String, message: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = MaterialTheme.shapes.medium) {
        Row(Modifier.fillMaxWidth().padding(LoomoraSpacing.large), horizontalArrangement = Arrangement.spacedBy(LoomoraSpacing.medium), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Column { Text(title, style = MaterialTheme.typography.titleSmall); Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun TrialBanner(remainingUses: Int, maxUses: Int = 3) {
    Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Text(stringResource(R.string.home_trial_remaining, remainingUses, maxUses), Modifier.padding(horizontal = LoomoraSpacing.large, vertical = LoomoraSpacing.medium), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}
