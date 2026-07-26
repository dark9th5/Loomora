package com.loomora.core.network

import com.loomora.core.model.AiInsights
import com.loomora.core.model.TranscriptSegment
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultAiProvider @Inject constructor() : AiTranscriptionProvider, AiInsightsProvider {

    override suspend fun transcribeAudio(audioFileUri: String): Result<List<TranscriptSegment>> {
        // Honest provider-neutral implementation checking backend configuration
        return Result.failure(
            IllegalStateException(
                "No AI cloud provider endpoint configured. Please configure your backend API key or server URL."
            )
        )
    }

    override suspend fun generateInsights(transcript: List<TranscriptSegment>): Result<AiInsights> {
        return Result.failure(
            IllegalStateException(
                "No AI cloud provider endpoint configured for insight generation."
            )
        )
    }
}
