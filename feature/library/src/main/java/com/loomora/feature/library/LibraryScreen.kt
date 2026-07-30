package com.loomora.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loomora.core.designsystem.component.EmptyState
import com.loomora.core.designsystem.component.LoomoraTopAppBar
import com.loomora.core.designsystem.component.RecordingListItem
import com.loomora.core.model.Recording
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LibraryRoute(
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LibraryScreen(
        uiState = uiState,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onToggleFavoritesOnly = viewModel::toggleFavoritesOnly,
        onToggleTrashOnly = viewModel::toggleTrashOnly,
        onFavoriteToggle = viewModel::toggleFavorite,
        onKeepRecoveryDiagnostic = viewModel::keepRecoveryDiagnostic,
        onDeleteRecoveryDiagnostic = viewModel::deleteRecoveryDiagnostic,
        onSoftDeleteRecording = viewModel::softDeleteRecording,
        onRestoreRecording = viewModel::restoreRecording,
        onPermanentlyDeleteRecording = viewModel::permanentlyDeleteRecording,
        onNavigateToDetail = onNavigateToDetail,
        modifier = modifier
    )
}

@Composable
fun LibraryScreen(
    uiState: LibraryUiState,
    onSearchQueryChange: (String) -> Unit,
    onToggleFavoritesOnly: () -> Unit,
    onToggleTrashOnly: () -> Unit,
    onFavoriteToggle: (String, Boolean) -> Unit,
    onKeepRecoveryDiagnostic: (String) -> Unit,
    onDeleteRecoveryDiagnostic: (String) -> Unit,
    onSoftDeleteRecording: (String) -> Unit,
    onRestoreRecording: (String) -> Unit,
    onPermanentlyDeleteRecording: (String) -> Unit,
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingDeleteRecordingId by remember { mutableStateOf<String?>(null) }
    val recordingPendingDelete = (
        uiState.recoveryDiagnostics + uiState.trashedRecordings
        ).firstOrNull { it.id == pendingDeleteRecordingId }

    Scaffold(
        topBar = {
            LoomoraTopAppBar(
                title = stringResource(id = R.string.library_title)
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
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(text = stringResource(id = R.string.library_search_hint)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null
                    )
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.showFavoritesOnly,
                    onClick = onToggleFavoritesOnly,
                    label = { Text(text = stringResource(id = R.string.library_filter_favorites)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null
                        )
                    }
                )
                FilterChip(
                    selected = uiState.showTrashOnly,
                    onClick = onToggleTrashOnly,
                    label = { Text(text = stringResource(id = R.string.library_filter_trash)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.RestoreFromTrash,
                            contentDescription = null
                        )
                    }
                )
            }

            StorageUsageCard(
                uiState = uiState,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (uiState.recoveryDiagnostics.isEmpty() &&
                uiState.recordings.isEmpty() &&
                uiState.trashedRecordings.isEmpty()
            ) {
                EmptyState(
                    icon = Icons.Default.FolderOpen,
                    title = stringResource(id = R.string.library_empty_title),
                    message = stringResource(id = R.string.library_empty_desc),
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    if (!uiState.showTrashOnly && uiState.recoveryDiagnostics.isNotEmpty()) {
                        item {
                            RecoveryDiagnosticsSection(
                                diagnostics = uiState.recoveryDiagnostics,
                                onKeepRecoveryDiagnostic = onKeepRecoveryDiagnostic,
                                onDeleteRecoveryDiagnostic = { pendingDeleteRecordingId = it },
                                onNavigateToDetail = onNavigateToDetail
                            )
                        }
                    }

                    if (uiState.showTrashOnly) {
                        items(
                            items = uiState.trashedRecordings,
                            key = { it.id }
                        ) { recording ->
                            TrashRecordingItem(
                                recording = recording,
                                onRestore = { onRestoreRecording(recording.id) },
                                onDelete = { pendingDeleteRecordingId = recording.id }
                            )
                        }
                    } else {
                        items(
                            items = uiState.recordings,
                            key = { it.id }
                        ) { recording ->
                            val dateText = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
                                .format(Date(recording.createdAt))
                            val durationSeconds = recording.durationMs / 1000
                            val durationText = String.format(
                                Locale.getDefault(),
                                "%d:%02d",
                                durationSeconds / 60,
                                durationSeconds % 60
                            )

                            RecordingListItem(
                                title = recording.title,
                                dateText = dateText,
                                durationText = durationText,
                                isFavorite = recording.isFavorite,
                                onItemClick = { onNavigateToDetail(recording.id) },
                                onFavoriteToggle = { onFavoriteToggle(recording.id, recording.isFavorite) },
                                onDelete = { onSoftDeleteRecording(recording.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (recordingPendingDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteRecordingId = null },
            title = {
                Text(text = stringResource(id = R.string.library_recovery_delete_confirm_title))
            },
            text = {
                Text(
                    text = stringResource(
                        id = R.string.library_recovery_delete_confirm_message,
                        recordingPendingDelete.title
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (uiState.recoveryDiagnostics.any { it.id == recordingPendingDelete.id }) {
                            onDeleteRecoveryDiagnostic(recordingPendingDelete.id)
                        } else {
                            onPermanentlyDeleteRecording(recordingPendingDelete.id)
                        }
                        pendingDeleteRecordingId = null
                    }
                ) {
                    Text(text = stringResource(id = R.string.library_recovery_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteRecordingId = null }) {
                    Text(text = stringResource(id = R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun StorageUsageCard(
    uiState: LibraryUiState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (uiState.lowStorageWarning) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(id = R.string.library_storage_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (uiState.lowStorageWarning) {
                Text(
                    text = stringResource(id = R.string.library_storage_low_space_warning),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Text(
                text = stringResource(
                    id = R.string.library_storage_recordings,
                    formatMegabytes(uiState.storageUsage.recordingsBytes)
                ),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(
                    id = R.string.library_storage_exports,
                    formatMegabytes(uiState.storageUsage.exportsBytes)
                ),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(
                    id = R.string.library_storage_temp,
                    formatMegabytes(uiState.storageUsage.tempBytes)
                ),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(
                    id = R.string.library_storage_models,
                    formatMegabytes(uiState.storageUsage.modelsBytes)
                ),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(
                    id = R.string.library_storage_free,
                    formatMegabytes(uiState.storageUsage.freeBytes)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RecoveryDiagnosticsSection(
    diagnostics: List<Recording>,
    onKeepRecoveryDiagnostic: (String) -> Unit,
    onDeleteRecoveryDiagnostic: (String) -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = stringResource(id = R.string.library_recovery_section_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(id = R.string.library_recovery_section_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        diagnostics.forEach { diagnostic ->
            RecoveryDiagnosticItem(
                recording = diagnostic,
                onKeep = { onKeepRecoveryDiagnostic(diagnostic.id) },
                onDelete = { onDeleteRecoveryDiagnostic(diagnostic.id) },
                onOpen = { onNavigateToDetail(diagnostic.id) }
            )
        }
    }
}

@Composable
private fun RecoveryDiagnosticItem(
    recording: Recording,
    onKeep: () -> Unit,
    onDelete: () -> Unit,
    onOpen: () -> Unit
) {
    val dateText = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
        .format(Date(recording.createdAt))
    val durationSeconds = recording.durationMs / 1000
    val durationText = String.format(Locale.getDefault(), "%d:%02d", durationSeconds / 60, durationSeconds % 60)
    val issueLabel = recoveryIssueLabel(recording.recoveryState)
    val issueDescription = recoveryIssueDescription(recording.recoveryState)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ReportProblem,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recording.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = issueLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Text(
                text = issueDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$dateText • $durationText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(onClick = onOpen) {
                    Text(text = stringResource(id = R.string.library_recovery_open))
                }
                TextButton(onClick = onKeep) {
                    Text(text = stringResource(id = R.string.library_recovery_keep))
                }
                Button(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(id = R.string.library_recovery_delete))
                }
            }
        }
    }
}

@Composable
private fun TrashRecordingItem(
    recording: Recording,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val deletedDate = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
        .format(Date(recording.updatedAt))
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = recording.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(id = R.string.library_trash_deleted_on, deletedDate),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onRestore) {
                    Text(text = stringResource(id = R.string.library_trash_restore))
                }
                Button(onClick = onDelete) {
                    Text(text = stringResource(id = R.string.library_trash_delete_forever))
                }
            }
        }
    }
}

@Composable
private fun recoveryIssueLabel(recoveryState: String): String {
    val labelRes = when (recoveryState) {
        "MISSING_FILE" -> R.string.library_recovery_issue_missing_title
        "ZERO_BYTE_FILE" -> R.string.library_recovery_issue_zero_byte_title
        "CORRUPT_FILE" -> R.string.library_recovery_issue_corrupt_title
        "ORPHAN_FILE" -> R.string.library_recovery_issue_orphan_title
        else -> R.string.library_recovery_issue_generic_title
    }
    return stringResource(id = labelRes)
}

@Composable
private fun recoveryIssueDescription(recoveryState: String): String {
    val descriptionRes = when (recoveryState) {
        "MISSING_FILE" -> R.string.library_recovery_issue_missing_message
        "ZERO_BYTE_FILE" -> R.string.library_recovery_issue_zero_byte_message
        "CORRUPT_FILE" -> R.string.library_recovery_issue_corrupt_message
        "ORPHAN_FILE" -> R.string.library_recovery_issue_orphan_message
        else -> R.string.library_recovery_issue_generic_message
    }
    return stringResource(id = descriptionRes)
}

private fun formatMegabytes(bytes: Long): String {
    return String.format(Locale.getDefault(), "%.1f MB", bytes / (1024f * 1024f))
}
