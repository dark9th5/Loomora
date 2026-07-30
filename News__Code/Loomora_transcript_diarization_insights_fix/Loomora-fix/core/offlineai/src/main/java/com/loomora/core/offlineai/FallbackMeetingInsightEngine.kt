package com.loomora.core.offlineai

import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Routes an installed insight model to its real runtime. The previous implementation
 * only called llama.cpp; LiteRT-LM models silently fell back to the extractive engine.
 */
@Singleton
class FallbackMeetingInsightEngine @Inject constructor(
    private val heuristic: HeuristicMeetingInsightEngine,
    private val liteRtLm: LiteRtLmMeetingInsightEngine,
    private val llamaCpp: LlamaCppMeetingInsightEngine
) : LocalMeetingInsightEngine {
    override suspend fun analyze(input: MeetingInsightInput): MeetingInsightOutput {
        val enhancedEngine = when (input.model?.manifest?.runtime) {
            RuntimeKind.LITERT_LM -> liteRtLm
            RuntimeKind.LLAMA_CPP -> llamaCpp
            else -> null
        }
        if (enhancedEngine == null) return heuristic.analyze(input)

        return try {
            enhancedEngine.analyze(input)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: OfflineAiException) {
            fallback(heuristic.analyze(input), error::class.simpleName ?: "OfflineAiException")
        } catch (error: OutOfMemoryError) {
            fallback(heuristic.analyze(input), "OutOfMemoryError")
        } catch (error: Throwable) {
            fallback(heuristic.analyze(input), error::class.simpleName ?: "UnknownError")
        }
    }

    override fun close() {
        heuristic.close()
        liteRtLm.close()
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
