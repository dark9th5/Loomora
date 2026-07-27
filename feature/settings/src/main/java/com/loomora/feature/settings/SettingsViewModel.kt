package com.loomora.feature.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loomora.core.datastore.DarkThemeConfig
import com.loomora.core.datastore.LoomoraPreferencesDataSource
import com.loomora.core.offlineai.OfflineModelRecord
import com.loomora.core.offlineai.OfflineModelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val darkThemeConfig: DarkThemeConfig = DarkThemeConfig.FOLLOW_SYSTEM,
    val languageCode: String = "en",
    val offlineModels: List<OfflineModelRecord> = emptyList(),
    val modelImportError: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesDataSource: LoomoraPreferencesDataSource,
    private val offlineModelRepository: OfflineModelRepository
) : ViewModel() {
    private val importErrorFlow = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesDataSource.userPreferences,
        offlineModelRepository.models,
        importErrorFlow
    ) { prefs, models, importError ->
            SettingsUiState(
                darkThemeConfig = prefs.darkThemeConfig,
                languageCode = prefs.languageCode,
                offlineModels = models,
                modelImportError = importError
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

    fun importModel(uri: Uri) {
        viewModelScope.launch {
            importErrorFlow.value = null
            runCatching {
                offlineModelRepository.importModel(uri)
            }.onFailure { error ->
                importErrorFlow.value = error.message ?: "Model import failed."
            }
        }
    }

    fun removeModel(modelId: String) {
        viewModelScope.launch {
            offlineModelRepository.removeModel(modelId)
        }
    }
}
