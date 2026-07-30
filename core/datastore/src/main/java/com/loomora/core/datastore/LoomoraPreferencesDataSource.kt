package com.loomora.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

enum class DarkThemeConfig {
    FOLLOW_SYSTEM,
    LIGHT,
    DARK
}

enum class SupportedAppLanguage(val tag: String) {
    ENGLISH("en"),
    VIETNAMESE("vi");

    companion object {
        fun fromLanguageTag(tag: String?): SupportedAppLanguage = when (
            tag?.trim()?.replace('_', '-')?.lowercase()
        ) {
            "vi", "vi-vn" -> VIETNAMESE
            else -> ENGLISH
        }
    }
}

enum class TranscriptLanguagePreference(val tag: String?) {
    AUTO(null),
    ENGLISH("en"),
    VIETNAMESE("vi");

    companion object {
        fun fromStoredValue(value: String?): TranscriptLanguagePreference = when (
            value?.trim()?.replace('_', '-')?.lowercase()
        ) {
            "en", "english" -> ENGLISH
            "vi", "vi-vn", "vietnamese" -> VIETNAMESE
            else -> AUTO
        }
    }
}

enum class OfflinePerformanceMode {
    BATTERY_SAVER,
    BALANCED,
    FAST;

    companion object {
        fun fromStoredValue(value: String?): OfflinePerformanceMode = entries
            .firstOrNull { it.name == value } ?: BALANCED
    }
}

enum class DefaultAnalysisMode {
    QUICK_TRANSCRIPT,
    TRANSCRIPT_AND_INSIGHTS,
    FULL_ANALYSIS;

    companion object {
        fun fromStoredValue(value: String?): DefaultAnalysisMode = entries
            .firstOrNull { it.name == value } ?: TRANSCRIPT_AND_INSIGHTS
    }
}

enum class RecordingAudioSource {
    MIC,
    VOICE_RECOGNITION,
    CAMCORDER;

    companion object {
        fun fromStoredValue(value: String?): RecordingAudioSource = entries
            .firstOrNull { it.name == value } ?: VOICE_RECOGNITION
    }
}

enum class NoiseReductionLevel(val strength: Float) {
    OFF(0f),
    LIGHT(0.55f),
    STRONG(1f);

    companion object {
        fun fromStoredValue(value: String?): NoiseReductionLevel = entries
            .firstOrNull { it.name == value } ?: LIGHT
    }
}

data class UserPreferences(
    val darkThemeConfig: DarkThemeConfig = DarkThemeConfig.FOLLOW_SYSTEM,
    val appLanguage: SupportedAppLanguage = SupportedAppLanguage.ENGLISH,
    val transcriptLanguage: TranscriptLanguagePreference = TranscriptLanguagePreference.AUTO,
    val offlinePerformanceMode: OfflinePerformanceMode = OfflinePerformanceMode.BALANCED,
    val defaultAnalysisMode: DefaultAnalysisMode = DefaultAnalysisMode.TRANSCRIPT_AND_INSIGHTS,
    val recordingAudioSource: RecordingAudioSource = RecordingAudioSource.VOICE_RECOGNITION,
    val noiseReductionLevel: NoiseReductionLevel = NoiseReductionLevel.LIGHT,
    val transcriptionModelId: String? = null,
    val hasCompletedOnboarding: Boolean = false,
    val onboardingVersionSeen: Int = 0
)

class LoomoraPreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object PreferencesKeys {
        val DARK_THEME_CONFIG = stringPreferencesKey("dark_theme_config")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val TRANSCRIPT_LANGUAGE = stringPreferencesKey("transcript_language")
        val OFFLINE_PERFORMANCE_MODE = stringPreferencesKey("offline_performance_mode")
        val DEFAULT_ANALYSIS_MODE = stringPreferencesKey("default_analysis_mode")
        val RECORDING_AUDIO_SOURCE = stringPreferencesKey("recording_audio_source")
        val NOISE_REDUCTION_LEVEL = stringPreferencesKey("noise_reduction_level")
        val TRANSCRIPTION_MODEL_ID = stringPreferencesKey("transcription_model_id")
        // Retained as a read-only migration source for installs created before P3.
        val LANGUAGE_CODE = stringPreferencesKey("language_code")
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val ONBOARDING_VERSION_SEEN = intPreferencesKey("onboarding_version_seen")
    }

    val userPreferences: Flow<UserPreferences> = dataStore.data.map { preferences ->
        UserPreferences(
            darkThemeConfig = when (preferences[PreferencesKeys.DARK_THEME_CONFIG]) {
                DarkThemeConfig.LIGHT.name -> DarkThemeConfig.LIGHT
                DarkThemeConfig.DARK.name -> DarkThemeConfig.DARK
                else -> DarkThemeConfig.FOLLOW_SYSTEM
            },
            appLanguage = SupportedAppLanguage.fromLanguageTag(
                preferences[PreferencesKeys.APP_LANGUAGE]
                    ?: preferences[PreferencesKeys.LANGUAGE_CODE]
            ),
            transcriptLanguage = TranscriptLanguagePreference.fromStoredValue(
                preferences[PreferencesKeys.TRANSCRIPT_LANGUAGE]
            ),
            offlinePerformanceMode = OfflinePerformanceMode.fromStoredValue(
                preferences[PreferencesKeys.OFFLINE_PERFORMANCE_MODE]
            ),
            defaultAnalysisMode = DefaultAnalysisMode.fromStoredValue(
                preferences[PreferencesKeys.DEFAULT_ANALYSIS_MODE]
            ),
            recordingAudioSource = RecordingAudioSource.fromStoredValue(
                preferences[PreferencesKeys.RECORDING_AUDIO_SOURCE]
            ),
            noiseReductionLevel = NoiseReductionLevel.fromStoredValue(
                preferences[PreferencesKeys.NOISE_REDUCTION_LEVEL]
            ),
            transcriptionModelId = preferences[PreferencesKeys.TRANSCRIPTION_MODEL_ID]
                ?.trim()
                ?.takeIf { it.isNotEmpty() },
            hasCompletedOnboarding = preferences[PreferencesKeys.HAS_COMPLETED_ONBOARDING] ?: false,
            onboardingVersionSeen = preferences[PreferencesKeys.ONBOARDING_VERSION_SEEN] ?: 0
        )
    }

    suspend fun setDarkThemeConfig(darkThemeConfig: DarkThemeConfig) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DARK_THEME_CONFIG] = darkThemeConfig.name
        }
    }

    suspend fun setAppLanguage(language: SupportedAppLanguage) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_LANGUAGE] = language.tag
        }
    }

    suspend fun setTranscriptLanguage(language: TranscriptLanguagePreference) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.TRANSCRIPT_LANGUAGE] = language.name
        }
    }

    suspend fun setOfflinePerformanceMode(mode: OfflinePerformanceMode) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.OFFLINE_PERFORMANCE_MODE] = mode.name
        }
    }

    suspend fun setDefaultAnalysisMode(mode: DefaultAnalysisMode) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_ANALYSIS_MODE] = mode.name
        }
    }

    suspend fun setRecordingAudioSource(source: RecordingAudioSource) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.RECORDING_AUDIO_SOURCE] = source.name
        }
    }

    suspend fun setNoiseReductionLevel(level: NoiseReductionLevel) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOISE_REDUCTION_LEVEL] = level.name
        }
    }

    suspend fun setTranscriptionModelId(modelId: String?) {
        dataStore.edit { preferences ->
            val normalized = modelId?.trim()?.takeIf { it.isNotEmpty() }
            if (normalized == null) {
                preferences.remove(PreferencesKeys.TRANSCRIPTION_MODEL_ID)
            } else {
                preferences[PreferencesKeys.TRANSCRIPTION_MODEL_ID] = normalized
            }
        }
    }

    suspend fun setHasCompletedOnboarding(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAS_COMPLETED_ONBOARDING] = completed
        }
    }

    suspend fun setOnboardingVersionSeen(version: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_VERSION_SEEN] = version.coerceAtLeast(0)
        }
    }
}
