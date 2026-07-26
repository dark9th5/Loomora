package com.loomora.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loomora.core.datastore.LoomoraPreferencesDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val isCompleted: Boolean = false,
    val currentStepIndex: Int = 0
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferencesDataSource: LoomoraPreferencesDataSource
) : ViewModel() {

    val uiState: StateFlow<OnboardingUiState> = preferencesDataSource.userPreferences
        .map { prefs ->
            OnboardingUiState(isCompleted = prefs.hasCompletedOnboarding)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = OnboardingUiState()
        )

    fun completeOnboarding() {
        viewModelScope.launch {
            preferencesDataSource.setHasCompletedOnboarding(true)
        }
    }
}
