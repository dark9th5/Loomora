package com.loomora.core.network

import com.loomora.core.model.AiInsights
import com.loomora.core.model.TranscriptSegment

interface AiTranscriptionProvider {
    suspend fun transcribeAudio(audioFileUri: String): Result<List<TranscriptSegment>>
}

interface AiInsightsProvider {
    suspend fun generateInsights(transcript: List<TranscriptSegment>): Result<AiInsights>
}
