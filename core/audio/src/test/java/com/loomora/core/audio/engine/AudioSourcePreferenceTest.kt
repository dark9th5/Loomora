package com.loomora.core.audio.engine

import android.media.MediaRecorder
import com.loomora.core.datastore.RecordingAudioSource
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioSourcePreferenceTest {
    @Test
    fun selectedSource_isAttemptedFirstWithUniqueFallbacks() {
        assertEquals(
            listOf(
                MediaRecorder.AudioSource.CAMCORDER,
                MediaRecorder.AudioSource.MIC,
                MediaRecorder.AudioSource.VOICE_RECOGNITION
            ),
            preferredAudioSources(RecordingAudioSource.CAMCORDER)
        )
    }

    @Test
    fun voiceRecognition_isTheDefaultFirstChoice() {
        assertEquals(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            preferredAudioSources(RecordingAudioSource.VOICE_RECOGNITION).first()
        )
    }
}
