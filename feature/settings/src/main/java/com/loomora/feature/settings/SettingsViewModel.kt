package com.loomora.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loomora.core.datastore.DarkThemeConfig
import com.loomora.core.datastore.LoomoraPreferencesDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val darkThemeConfig: DarkThemeConfig = DarkThemeConfig.FOLLOW_SYSTEM,
    val languageCode: String = "en"
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesDataSource: LoomoraPreferencesDataSource
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = preferencesDataSource.userPreferences
        .map { prefs ->
            SettingsUiState(
                darkThemeConfig = prefs.darkThemeConfig,
                languageCode = prefs.languageCode
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingsUiState()
        )

    fun setDarkThemeConfig(darkThemeConfig: DarkThemeConfig) {
        viewModelScope.launch {
            preferencesDataSource.setDarkThemeConfig(darkThemeConfig)
        }
    }

    fun setLanguageCode(languageCode: String) {
        viewModelScope.launch {
            preferencesDataSource.setLanguageCode(languageCode)
        }
    }
}
