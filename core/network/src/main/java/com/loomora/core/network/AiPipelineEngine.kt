package com.loomora.core.network

import com.loomora.core.model.AiJobStatus
import com.loomora.core.model.TranscriptSegment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiPipelineEngine @Inject constructor(
    private val transcriptionProvider: DefaultAiProvider,
    private val insightsProvider: DefaultAiProvider
) {

    private val _jobStatus = MutableStateFlow<AiJobStatus>(AiJobStatus.Idle)
    val jobStatus: StateFlow<AiJobStatus> = _jobStatus.asStateFlow()

    suspend fun processAudio(audioFileUri: String, hasUserConsented: Boolean) {
        if (!hasUserConsented) {
            _jobStatus.value = AiJobStatus.ConsentRequired
            return
        }

        _jobStatus.value = AiJobStatus.Uploading
        _jobStatus.value = AiJobStatus.Transcribing

        val transcribeResult = transcriptionProvider.transcribeAudio(audioFileUri)
        if (transcribeResult.isFailure) {
            val err = transcribeResult.exceptionOrNull()?.localizedMessage ?: "Transcription failed"
            _jobStatus.value = AiJobStatus.Failed(
                message = err,
                isRetryable = true
            )
            return
        }

        val transcript = transcribeResult.getOrDefault(emptyList())

        _jobStatus.value = AiJobStatus.Summarizing
        val insightsResult = insightsProvider.generateInsights(transcript)

        if (insightsResult.isFailure) {
            val err = insightsResult.exceptionOrNull()?.localizedMessage ?: "Insights generation failed"
            // Failure isolation: preserve transcript if insights fail!
            _jobStatus.value = AiJobStatus.Failed(
                message = "Transcription completed, but insights generation failed: $err",
                isRetryable = true,
                preservedTranscript = transcript
            )
            return
        }

        val insights = insightsResult.getOrNull()
        _jobStatus.value = AiJobStatus.Completed(transcript, insights)
    }

    fun resetStatus() {
        _jobStatus.value = AiJobStatus.Idle
    }
}
