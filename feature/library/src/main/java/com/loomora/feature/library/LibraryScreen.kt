package com.loomora.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loomora.core.designsystem.R
import com.loomora.core.designsystem.component.EmptyState
import com.loomora.core.designsystem.component.LoomoraTopAppBar
import com.loomora.core.designsystem.component.RecordingListItem
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
        onFavoriteToggle = viewModel::toggleFavorite,
        onNavigateToDetail = onNavigateToDetail,
        modifier = modifier
    )
}

@Composable
fun LibraryScreen(
    uiState: LibraryUiState,
    onSearchQueryChange: (String) -> Unit,
    onToggleFavoritesOnly: () -> Unit,
    onFavoriteToggle: (String, Boolean) -> Unit,
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
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
            // Search TextField
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

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = uiState.showFavoritesOnly,
                    onClick = onToggleFavoritesOnly,
                    label = { Text(text = "Favorites") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null
                        )
                    }
                )
            }

            // List Content or Real Empty State
            if (uiState.recordings.isEmpty()) {
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
                    items(
                        items = uiState.recordings,
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
                            onItemClick = { onNavigateToDetail(recording.id) },
                            onFavoriteToggle = { onFavoriteToggle(recording.id, recording.isFavorite) }
                        )
                    }
                }
            }
        }
    }
}
