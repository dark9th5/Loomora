package com.loomora.core.offlineai

import java.io.Closeable
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

interface LlamaCppRuntime : Closeable {
    suspend fun generateConstrainedJson(input: LlamaCppGenerationInput): LlamaCppGenerationOutput
}

data class LlamaCppGenerationInput(
    val modelFile: File,
    val prompt: String,
    val grammar: String,
    val maxContextTokens: Int,
    val maxOutputTokens: Int
)

data class LlamaCppGenerationOutput(
    val json: String,
    val loadTimeMs: Long,
    val generationTimeMs: Long,
    val memoryObservationKb: Long?
)

@Singleton
class UnavailableLlamaCppRuntime @Inject constructor() : LlamaCppRuntime {
    override suspend fun generateConstrainedJson(input: LlamaCppGenerationInput): LlamaCppGenerationOutput {
        throw OfflineAiException.ProcessingUnavailable
    }

    override fun close() = Unit
}
