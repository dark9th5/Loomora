package com.loomora.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loomora.core.designsystem.R
import com.loomora.core.designsystem.component.EmptyState
import com.loomora.core.designsystem.component.LoomoraTopAppBar
import com.loomora.core.designsystem.component.PrimaryRecordButton
import com.loomora.core.designsystem.component.RecordingListItem
import com.loomora.core.designsystem.component.TrialUsageChip
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeRoute(
    onNavigateToRecorder: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    HomeScreen(
        uiState = uiState,
        onNavigateToRecorder = onNavigateToRecorder,
        onNavigateToSettings = onNavigateToSettings,
        onFavoriteToggle = viewModel::toggleFavorite,
        modifier = modifier
    )
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onNavigateToRecorder: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onFavoriteToggle: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            LoomoraTopAppBar(
                title = stringResource(id = R.string.home_title),
                actions = {
                    TrialUsageChip(
                        remainingUses = uiState.remainingTrialUses,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(id = R.string.nav_settings)
                        )
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
        ) {
            // Hero Card for Start Recording
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.home_new_recording),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(id = R.string.app_tagline),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    PrimaryRecordButton(
                        isRecording = false,
                        onClick = onNavigateToRecorder,
                        contentDescriptionText = stringResource(id = R.string.home_start_record_cta)
                    )
                }
            }

            // Quick Modes
            Text(
                text = stringResource(id = R.string.home_quick_modes),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            val modeTitles = listOf(
                stringResource(id = R.string.home_mode_meeting),
                stringResource(id = R.string.home_mode_interview),
                stringResource(id = R.string.home_mode_lecture),
                stringResource(id = R.string.home_mode_voicenote)
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                items(modeTitles) { modeTitle ->
                    ModeChip(
                        title = modeTitle,
                        onClick = onNavigateToRecorder
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Recent Recordings Title
            Text(
                text = stringResource(id = R.string.home_recent_recordings),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (uiState.recentRecordings.isEmpty()) {
                // Real Empty State (No fake recent items)
                EmptyState(
                    icon = Icons.Default.GraphicEq,
                    title = stringResource(id = R.string.home_empty_title),
                    message = stringResource(id = R.string.home_empty_desc),
                    actionText = stringResource(id = R.string.home_start_record_cta),
                    onActionClick = onNavigateToRecorder,
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(
                        items = uiState.recentRecordings,
                        key = { it.id }
                    ) { recording ->
                        val dateText = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
                            .format(Date(recording.createdAt))
                        val durationSeconds = recording.durationMs / 1000
                        val durationText = String.format(Locale.getDefault(), "%d:%02d", durationSeconds / 60, durationSeconds % 60)

                        RecordingListItem(
                            title = recording.title,
                            dateText = dateText,
                            durationText = durationText,
                            isFavorite = recording.isFavorite,
                            onItemClick = { /* Detail navigation */ },
                            onFavoriteToggle = { onFavoriteToggle(recording.id, recording.isFavorite) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeChip(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.size(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
