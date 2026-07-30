package com.loomora.feature.settings

import android.net.Uri
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loomora.core.datastore.DarkThemeConfig
import com.loomora.core.datastore.LoomoraPreferencesDataSource
import com.loomora.core.datastore.SupportedAppLanguage
import com.loomora.core.datastore.TranscriptLanguagePreference
import com.loomora.core.datastore.OfflinePerformanceMode
import com.loomora.core.datastore.DefaultAnalysisMode
import com.loomora.core.datastore.RecordingAudioSource
import com.loomora.core.datastore.NoiseReductionLevel
import com.loomora.core.offlineai.OfflineModelRecord
import com.loomora.core.offlineai.OfflineModelRepository
import com.loomora.core.offlineai.OfflineModelAutoInstaller
import com.loomora.core.offlineai.DefaultOfflineModelCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    val transcriptLanguage: TranscriptLanguagePreference = TranscriptLanguagePreference.AUTO,
    val performanceMode: OfflinePerformanceMode = OfflinePerformanceMode.BALANCED,
    val analysisMode: DefaultAnalysisMode = DefaultAnalysisMode.TRANSCRIPT_AND_INSIGHTS,
    val recordingAudioSource: RecordingAudioSource = RecordingAudioSource.VOICE_RECOGNITION,
    val noiseReductionLevel: NoiseReductionLevel = NoiseReductionLevel.LIGHT,
    val transcriptionModelId: String = DefaultOfflineModelCatalog.RECOMMENDED_TRANSCRIPTION_MODEL_ID,
    val appVersion: String = "",
    val offlineModels: List<OfflineModelRecord> = emptyList(),
    val modelImportError: ModelImportError? = null,
    val modelDownloadProgress: Int? = null
)

enum class ModelImportError {
    IMPORT_FAILED
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val preferencesDataSource: LoomoraPreferencesDataSource,
    private val offlineModelRepository: OfflineModelRepository,
    private val offlineModelAutoInstaller: OfflineModelAutoInstaller
) : ViewModel() {
    private val appVersion = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }.getOrDefault("")
    private val importErrorFlow = kotlinx.coroutines.flow.MutableStateFlow<ModelImportError?>(null)
    private val downloadProgressFlow = kotlinx.coroutines.flow.MutableStateFlow<Int?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesDataSource.userPreferences,
        offlineModelRepository.models,
        importErrorFlow,
        downloadProgressFlow
    ) { prefs, models, importError, downloadProgress ->
            SettingsUiState(
                darkThemeConfig = prefs.darkThemeConfig,
                languageCode = prefs.appLanguage.tag,
                transcriptLanguage = prefs.transcriptLanguage,
                performanceMode = prefs.offlinePerformanceMode,
                analysisMode = prefs.defaultAnalysisMode,
                recordingAudioSource = prefs.recordingAudioSource,
                noiseReductionLevel = prefs.noiseReductionLevel,
                transcriptionModelId = prefs.transcriptionModelId
                    ?: DefaultOfflineModelCatalog.RECOMMENDED_TRANSCRIPTION_MODEL_ID,
                appVersion = appVersion,
                offlineModels = models,
                modelImportError = importError,
                modelDownloadProgress = downloadProgress
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
            val language = SupportedAppLanguage.fromLanguageTag(languageCode)
            preferencesDataSource.setAppLanguage(language)
            if (AppCompatDelegate.getApplicationLocales().toLanguageTags() != language.tag) {
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(language.tag)
                )
            }
        }
    }

    fun setTranscriptLanguage(language: TranscriptLanguagePreference) {
        viewModelScope.launch { preferencesDataSource.setTranscriptLanguage(language) }
    }

    fun setPerformanceMode(mode: OfflinePerformanceMode) {
        viewModelScope.launch { preferencesDataSource.setOfflinePerformanceMode(mode) }
    }

    fun setAnalysisMode(mode: DefaultAnalysisMode) {
        viewModelScope.launch { preferencesDataSource.setDefaultAnalysisMode(mode) }
    }

    fun setRecordingAudioSource(source: RecordingAudioSource) {
        viewModelScope.launch { preferencesDataSource.setRecordingAudioSource(source) }
    }

    fun setNoiseReductionLevel(level: NoiseReductionLevel) {
        viewModelScope.launch { preferencesDataSource.setNoiseReductionLevel(level) }
    }

    fun setTranscriptionModel(modelId: String) {
        viewModelScope.launch {
            val model = offlineModelRepository.getModelDetails(modelId)
            if (model?.state == com.loomora.core.offlineai.ModelInstallState.READY) {
                preferencesDataSource.setTranscriptionModelId(modelId)
            }
        }
    }

    fun importModel(uri: Uri) {
        viewModelScope.launch {
            importErrorFlow.value = null
            runCatching {
                offlineModelRepository.importModel(uri)
            }.onFailure {
                importErrorFlow.value = ModelImportError.IMPORT_FAILED
            }
        }
    }

    fun installRecommendedModel() {
        if (downloadProgressFlow.value != null) return
        viewModelScope.launch {
            importErrorFlow.value = null
            downloadProgressFlow.value = 0
            runCatching {
                offlineModelAutoInstaller.installRecommendedModels { progress ->
                    downloadProgressFlow.value = progress
                }
                preferencesDataSource.setTranscriptionModelId(
                    DefaultOfflineModelCatalog.RECOMMENDED_TRANSCRIPTION_MODEL_ID
                )
            }.onFailure {
                importErrorFlow.value = ModelImportError.IMPORT_FAILED
            }
            downloadProgressFlow.value = null
        }
    }

    fun installModel(modelId: String) {
        if (downloadProgressFlow.value != null) return
        viewModelScope.launch {
            importErrorFlow.value = null
            downloadProgressFlow.value = 0
            runCatching {
                offlineModelAutoInstaller.installModel(modelId) { progress ->
                    downloadProgressFlow.value = progress
                }
            }.onFailure {
                importErrorFlow.value = ModelImportError.IMPORT_FAILED
            }
            downloadProgressFlow.value = null
        }
    }

    fun removeModel(modelId: String) {
        viewModelScope.launch {
            offlineModelRepository.removeModel(modelId)
            if (uiState.value.transcriptionModelId == modelId) {
                preferencesDataSource.setTranscriptionModelId(null)
            }
        }
    }
}
