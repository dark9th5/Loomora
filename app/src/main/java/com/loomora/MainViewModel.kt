package com.loomora

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loomora.core.datastore.DarkThemeConfig
import com.loomora.core.datastore.LoomoraPreferencesDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class MainUiState(
    val darkThemeConfig: DarkThemeConfig = DarkThemeConfig.FOLLOW_SYSTEM,
    val hasCompletedOnboarding: Boolean = false,
    val isLoading: Boolean = false
)

@HiltViewModel
class MainViewModel @Inject constructor(
    preferencesDataSource: LoomoraPreferencesDataSource
) : ViewModel() {

    val uiState: StateFlow<MainUiState> = preferencesDataSource.userPreferences
        .map { prefs ->
            MainUiState(
                darkThemeConfig = prefs.darkThemeConfig,
                hasCompletedOnboarding = prefs.hasCompletedOnboarding,
                isLoading = false
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MainUiState(isLoading = true)
        )
}
