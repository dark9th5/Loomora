package com.loomora

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loomora.core.datastore.DarkThemeConfig
import com.loomora.core.datastore.LoomoraPreferencesDataSource
import com.loomora.core.datastore.SupportedAppLanguage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val darkThemeConfig: DarkThemeConfig = DarkThemeConfig.FOLLOW_SYSTEM,
    val languageCode: String = "en",
    val hasCompletedOnboarding: Boolean = false,
    val onboardingVersionSeen: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val preferencesDataSource: LoomoraPreferencesDataSource
) : ViewModel() {

    val uiState: StateFlow<MainUiState> = preferencesDataSource.userPreferences
        .map { prefs ->
            MainUiState(
                darkThemeConfig = prefs.darkThemeConfig,
                languageCode = prefs.appLanguage.tag,
                hasCompletedOnboarding = prefs.hasCompletedOnboarding,
                onboardingVersionSeen = prefs.onboardingVersionSeen,
                isLoading = false
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MainUiState(isLoading = true)
        )

    fun syncAppLanguage(language: SupportedAppLanguage) {
        viewModelScope.launch {
            preferencesDataSource.setAppLanguage(language)
        }
    }
}
