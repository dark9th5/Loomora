package com.loomora.core.datastore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class UserPreferencesTest {

    @Test
    fun userPreferences_defaultValuesAreValid() {
        val prefs = UserPreferences()
        assertEquals(DarkThemeConfig.FOLLOW_SYSTEM, prefs.darkThemeConfig)
        assertEquals(SupportedAppLanguage.ENGLISH, prefs.appLanguage)
        assertEquals(TranscriptLanguagePreference.AUTO, prefs.transcriptLanguage)
        assertEquals(OfflinePerformanceMode.BALANCED, prefs.offlinePerformanceMode)
        assertEquals(DefaultAnalysisMode.TRANSCRIPT_AND_INSIGHTS, prefs.defaultAnalysisMode)
        assertEquals(RecordingAudioSource.VOICE_RECOGNITION, prefs.recordingAudioSource)
        assertEquals(NoiseReductionLevel.LIGHT, prefs.noiseReductionLevel)
        assertFalse(prefs.hasCompletedOnboarding)
        assertEquals(0, prefs.onboardingVersionSeen)
    }

    @Test
    fun legacyAppLanguage_migratesSupportedValues() {
        assertEquals(SupportedAppLanguage.ENGLISH, SupportedAppLanguage.fromLanguageTag("en"))
        assertEquals(SupportedAppLanguage.VIETNAMESE, SupportedAppLanguage.fromLanguageTag("vi"))
        assertEquals(SupportedAppLanguage.VIETNAMESE, SupportedAppLanguage.fromLanguageTag("vi-VN"))
    }

    @Test
    fun legacyAppLanguage_unknownValueFallsBackToEnglish() {
        assertEquals(SupportedAppLanguage.ENGLISH, SupportedAppLanguage.fromLanguageTag("fr"))
        assertEquals(SupportedAppLanguage.ENGLISH, SupportedAppLanguage.fromLanguageTag(null))
    }

    @Test
    fun processingPreferences_unknownValuesUseSafeDefaults() {
        assertEquals(OfflinePerformanceMode.BALANCED, OfflinePerformanceMode.fromStoredValue("UNKNOWN"))
        assertEquals(DefaultAnalysisMode.TRANSCRIPT_AND_INSIGHTS, DefaultAnalysisMode.fromStoredValue(null))
        assertEquals(RecordingAudioSource.VOICE_RECOGNITION, RecordingAudioSource.fromStoredValue("UNKNOWN"))
        assertEquals(NoiseReductionLevel.LIGHT, NoiseReductionLevel.fromStoredValue(null))
    }
}
