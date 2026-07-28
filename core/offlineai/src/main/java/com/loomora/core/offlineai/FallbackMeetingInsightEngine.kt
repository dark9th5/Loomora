package com.loomora.core.offlineai

import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FallbackMeetingInsightEngine @Inject constructor(
    private val heuristic: HeuristicMeetingInsightEngine,
    private val llamaCpp: LlamaCppMeetingInsightEngine
) : LocalMeetingInsightEngine {
    override suspend fun analyze(input: MeetingInsightInput): MeetingInsightOutput {
        val heuristicOutput = heuristic.analyze(input)
        val model = input.model
        if (model?.manifest?.runtime != RuntimeKind.LLAMA_CPP) {
            return heuristicOutput
        }

        return try {
            llamaCpp.analyze(input)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: OfflineAiException) {
            fallback(heuristicOutput, error::class.simpleName ?: "OfflineAiException")
        } catch (error: OutOfMemoryError) {
            fallback(heuristicOutput, "OutOfMemoryError")
        } catch (error: Throwable) {
            fallback(heuristicOutput, error::class.simpleName ?: "UnknownError")
        }
    }

    override fun close() {
        heuristic.close()
        llamaCpp.close()
    }

    private fun fallback(output: MeetingInsightOutput, reason: String): MeetingInsightOutput {
        return output.copy(
            modelId = OfflineAiRuntimeVersions.HYBRID_INSIGHTS_MODEL_ID,
            modelVersion = OfflineAiRuntimeVersions.HYBRID_INSIGHTS_MODEL_VERSION,
            generationMode = InsightGenerationMode.HEURISTIC_FALLBACK,
            completionQuality = InsightCompletionQuality.DEGRADED_BUT_VALID,
            usedHeuristicFallback = true,
            fallbackReason = reason
        )
    }
}
