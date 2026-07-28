package com.loomora.core.offlineai

import kotlinx.serialization.Serializable

@Serializable
data class OfflineProcessingOptions(
    val enhanceAudio: Boolean = false,
    val transcriptionModelId: String? = null,
    val diarizationEnabled: Boolean = true,
    val insightsMode: String = "HEURISTIC",
    val outputLanguage: String? = null,
    val optionalLlmEnhancement: Boolean = false
) {
    fun canonical(): OfflineProcessingOptions {
        return copy(
            transcriptionModelId = transcriptionModelId?.trim()?.takeIf { it.isNotBlank() },
            insightsMode = insightsMode.trim().uppercase(),
            outputLanguage = outputLanguage?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
        )
    }
}
