package com.loomora.core.offlineai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LlamaCppMeetingInsightEngine @Inject constructor(
    private val parser: MeetingInsightJsonParser,
    private val chunker: TranscriptChunker,
    private val runtime: LlamaCppRuntime
) : LocalMeetingInsightEngine {
    override suspend fun analyze(input: MeetingInsightInput): MeetingInsightOutput = withContext(Dispatchers.IO) {
        val model = input.model ?: throw OfflineAiException.ModelMissing
        if (model.manifest.runtime != RuntimeKind.LLAMA_CPP) {
            throw OfflineAiException.DeviceIncompatible
        }
        val modelPath = model.installedPath ?: throw OfflineAiException.ModelFileMissing
        val modelFile = File(modelPath)
        if (!modelFile.exists() || modelFile.extension.lowercase() != "gguf") {
            throw OfflineAiException.ModelFileMissing
        }

        val chunks = chunker.chunk(input.transcriptRevision.segments)
        val validSegmentIds = input.transcriptRevision.segments.map { it.id }.toSet()
        if (chunks.isEmpty() || validSegmentIds.isEmpty()) {
            throw OfflineAiException.InsightSemanticInvalid
        }

        val output = runtime.generateConstrainedJson(
            LlamaCppGenerationInput(
                modelFile = modelFile,
                prompt = buildPrompt(chunks),
                grammar = MEETING_INSIGHTS_GBNF,
                maxContextTokens = DEFAULT_POLICY.maxContextTokens,
                maxOutputTokens = DEFAULT_POLICY.maxOutputTokens
            )
        )
        val insights = parser.parse(output.json, validSegmentIds)
        MeetingInsightOutput(
            insights = insights,
            modelId = model.manifest.id,
            modelVersion = model.manifest.version,
            promptVersion = OfflineAiRuntimeVersions.INSIGHTS_PROMPT_VERSION,
            schemaVersion = OfflineAiRuntimeVersions.INSIGHTS_SCHEMA_VERSION,
            pipelineVersion = OfflineAiRuntimeVersions.INSIGHTS_PIPELINE_VERSION,
            languageTag = input.languageTag,
            chunkCheckpoints = listOf(
                InsightChunkCheckpoint(
                    chunkIndex = 0,
                    startMs = chunks.first().startMs,
                    endMs = chunks.last().endMs,
                    segmentIds = chunks.flatMap { it.segmentIds },
                    outputJson = output.json
                )
            ),
            modelSizeBytes = modelFile.length(),
            loadTimeMs = output.loadTimeMs,
            generationTimeMs = output.generationTimeMs,
            memoryObservationKb = output.memoryObservationKb,
            generationMode = InsightGenerationMode.LLM_ENHANCED,
            completionQuality = InsightCompletionQuality.ENHANCED
        )
    }

    override fun close() {
        runtime.close()
    }

    private fun buildPrompt(chunks: List<TranscriptChunk>): String {
        val transcript = chunks.joinToString("\n") { chunk ->
            chunk.segments.joinToString("\n") { segment ->
                "[${segment.id} ${segment.startMs}-${segment.endMs}] ${segment.text}"
            }
        }
        return """
            Extract meeting insights as JSON only. Use only listed evidence segment IDs.
            Keep assignee and dueDate null unless explicit in the transcript.
            Separate decisions, suggestions, and open questions.

            Transcript:
            $transcript
        """.trimIndent()
    }

    private companion object {
        val DEFAULT_POLICY = LlamaCppInsightPolicy()
        const val MEETING_INSIGHTS_GBNF = """
root ::= object
object ::= "{" members "}"
members ::= pair ("," pair)*
pair ::= string ":" value
value ::= object | array | string | "null" | number | "true" | "false"
array ::= "[" (value ("," value)*)? "]"
string ::= "\"" ([^"\\] | "\\" ["\\/bfnrt])* "\""
number ::= "-"? [0-9]+ ("." [0-9]+)?
"""
    }
}
