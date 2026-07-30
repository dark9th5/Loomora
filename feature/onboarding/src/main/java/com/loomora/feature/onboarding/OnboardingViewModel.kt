package com.loomora.feature.onboarding

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loomora.core.datastore.LoomoraPreferencesDataSource
import com.loomora.core.datastore.SupportedAppLanguage
import com.loomora.core.offlineai.DefaultOfflineModelCatalog
import com.loomora.core.offlineai.ModelInstallState
import com.loomora.core.offlineai.OfflineModelAutoInstaller
import com.loomora.core.offlineai.OfflineModelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

const val CURRENT_ONBOARDING_VERSION = 2
const val ONBOARDING_PAGE_COUNT = 5

data class OnboardingUiState(
    val pageIndex: Int = 0,
    val selectedLanguage: SupportedAppLanguage = SupportedAppLanguage.ENGLISH,
    val microphonePermissionGranted: Boolean = false,
    val isCompleting: Boolean = false,
    val isCompleted: Boolean = false,
    val isRecommendedModelInstalled: Boolean = false,
    val modelDownloadProgress: Int? = null,
    val modelDownloadFailed: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferencesDataSource: LoomoraPreferencesDataSource,
    offlineModelRepository: OfflineModelRepository,
    private val offlineModelAutoInstaller: OfflineModelAutoInstaller
) : ViewModel() {
    private val mutableState = kotlinx.coroutines.flow.MutableStateFlow(OnboardingUiState())

    val uiState: StateFlow<OnboardingUiState> = combine(
        preferencesDataSource.userPreferences,
        mutableState,
        offlineModelRepository.models
    ) { prefs, state, models ->
            state.copy(
                selectedLanguage = prefs.appLanguage,
                isCompleted = prefs.hasCompletedOnboarding ||
                    prefs.onboardingVersionSeen >= CURRENT_ONBOARDING_VERSION,
                isRecommendedModelInstalled = setOf(
                    DefaultOfflineModelCatalog.RECOMMENDED_TRANSCRIPTION_MODEL_ID,
                    DefaultOfflineModelCatalog.RECOMMENDED_VAD_MODEL_ID,
                    DefaultOfflineModelCatalog.RECOMMENDED_DIARIZATION_MODEL_ID
                ).all { modelId ->
                    models.any { it.manifest.id == modelId && it.state == ModelInstallState.READY }
                }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = OnboardingUiState()
        )

    fun selectLanguage(language: SupportedAppLanguage) {
        if (language == mutableState.value.selectedLanguage) return
        mutableState.value = mutableState.value.copy(selectedLanguage = language)
        viewModelScope.launch {
            preferencesDataSource.setAppLanguage(language)
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(language.tag)
            )
        }
    }

    fun nextPage() {
        mutableState.value = mutableState.value.copy(
            pageIndex = (mutableState.value.pageIndex + 1).coerceAtMost(ONBOARDING_PAGE_COUNT - 1)
        )
    }

    fun previousPage() {
        mutableState.value = mutableState.value.copy(
            pageIndex = (mutableState.value.pageIndex - 1).coerceAtLeast(0)
        )
    }

    fun setMicrophonePermissionGranted(granted: Boolean) {
        mutableState.value = mutableState.value.copy(microphonePermissionGranted = granted)
    }

    fun skipOptionalPermission() {
        mutableState.value = mutableState.value.copy(microphonePermissionGranted = false)
    }

    fun installRecommendedModel() {
        if (mutableState.value.modelDownloadProgress != null || mutableState.value.isRecommendedModelInstalled) return
        mutableState.value = mutableState.value.copy(modelDownloadProgress = 0, modelDownloadFailed = false)
        viewModelScope.launch {
            runCatching {
                offlineModelAutoInstaller.installRecommendedModels { progress ->
                    mutableState.value = mutableState.value.copy(modelDownloadProgress = progress)
                }
                preferencesDataSource.setTranscriptionModelId(
                    DefaultOfflineModelCatalog.RECOMMENDED_TRANSCRIPTION_MODEL_ID
                )
            }.onFailure {
                mutableState.value = mutableState.value.copy(modelDownloadFailed = true)
            }
            mutableState.value = mutableState.value.copy(modelDownloadProgress = null)
        }
    }

    fun completeOnboarding() {
        if (mutableState.value.isCompleting || mutableState.value.isCompleted) return
        mutableState.value = mutableState.value.copy(isCompleting = true)
        viewModelScope.launch {
            preferencesDataSource.setOnboardingVersionSeen(CURRENT_ONBOARDING_VERSION)
            preferencesDataSource.setHasCompletedOnboarding(true)
        }
    }
}
