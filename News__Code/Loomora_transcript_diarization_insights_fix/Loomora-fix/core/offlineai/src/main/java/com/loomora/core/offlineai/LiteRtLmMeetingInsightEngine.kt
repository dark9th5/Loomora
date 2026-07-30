package com.loomora.core.offlineai

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.InputData
import com.google.ai.edge.litertlm.SessionConfig
import com.loomora.core.model.AiInsights
import com.loomora.core.model.Chapter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

class LiteRtLmMeetingInsightEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val parser: MeetingInsightJsonParser,
    private val chunker: TranscriptChunker
) : LocalMeetingInsightEngine {
    override suspend fun analyze(input: MeetingInsightInput): MeetingInsightOutput = withContext(Dispatchers.IO) {
        val insightModel = input.model ?: throw OfflineAiException.ModelMissing
        val modelPath = insightModel.installedPath ?: throw OfflineAiException.ModelFileMissing
        val modelFile = File(modelPath)
        if (!modelFile.exists() || !modelFile.isFile || !modelFile.name.endsWith(".litertlm")) {
            throw OfflineAiException.ModelFileMissing
        }
        val analysisSegments = TranscriptSpeakerFusion.compact(input.transcriptRevision.segments)
        val chunks = chunker.chunk(analysisSegments)
        if (chunks.isEmpty()) {
            return@withContext MeetingInsightOutput(
                insights = emptyInsights(input.languageTag),
                modelId = insightModel.manifest.id,
                modelVersion = insightModel.manifest.version,
                promptVersion = OfflineAiRuntimeVersions.INSIGHTS_PROMPT_VERSION,
                schemaVersion = OfflineAiRuntimeVersions.INSIGHTS_SCHEMA_VERSION,
                pipelineVersion = OfflineAiRuntimeVersions.INSIGHTS_PIPELINE_VERSION,
                languageTag = input.languageTag,
                chunkCheckpoints = emptyList(),
                modelSizeBytes = modelFile.length(),
                loadTimeMs = 0L,
                generationTimeMs = 0L,
                memoryObservationKb = currentUsedMemoryKb(),
                generationMode = InsightGenerationMode.LLM_ENHANCED,
                completionQuality = InsightCompletionQuality.ENHANCED
            )
        }
        val validIds = analysisSegments.map { it.id }.filter(String::isNotBlank).toSet()
        val policy = input.backendPolicy
        var lastError: Throwable? = null
        for (backend in (policy.preferred + policy.fallback).distinct()) {
            coroutineContext.ensureActive()
            val engine = Engine(
                EngineConfig(
                    modelPath = modelFile.absolutePath,
                    backend = backend.toLiteRtBackend(),
                    maxNumTokens = policy.maxTokens,
                    cacheDir = File(context.cacheDir, "litertlm").apply { mkdirs() }.absolutePath
                )
            )
            var loadTimeMs = 0L
            val generationStartedAt: Long
            try {
                val loadStartedAt = System.currentTimeMillis()
                engine.initialize()
                loadTimeMs = System.currentTimeMillis() - loadStartedAt
                val session = engine.createSession(SessionConfig())
                try {
                    generationStartedAt = System.currentTimeMillis()
                    val parsedChunks = mutableListOf<ParsedInsightJson>()
                    val checkpoints = chunks.map { chunk ->
                        coroutineContext.ensureActive()
                        val parsedChunk = generateStrictJson(
                            session = session,
                            prompt = chunkPrompt(chunk, input.languageTag),
                            validSegmentIds = validIds,
                            policy = policy
                        )
                        parsedChunks += parsedChunk
                        InsightChunkCheckpoint(
                            chunkIndex = chunk.index,
                            startMs = chunk.startMs,
                            endMs = chunk.endMs,
                            segmentIds = chunk.segmentIds,
                            outputJson = parsedChunk.rawJson
                        )
                    }
                    val final = if (parsedChunks.size == 1) {
                        parsedChunks.single()
                    } else {
                        generateStrictJson(
                            session = session,
                            prompt = synthesisPrompt(checkpoints, input.languageTag),
                            validSegmentIds = validIds,
                            policy = policy
                        )
                    }
                    val finalInsights = final.insights
                    return@withContext MeetingInsightOutput(
                        insights = finalInsights,
            modelId = insightModel.manifest.id,
            modelVersion = insightModel.manifest.version,
                        promptVersion = OfflineAiRuntimeVersions.INSIGHTS_PROMPT_VERSION,
                        schemaVersion = OfflineAiRuntimeVersions.INSIGHTS_SCHEMA_VERSION,
                        pipelineVersion = OfflineAiRuntimeVersions.INSIGHTS_PIPELINE_VERSION,
                        languageTag = input.languageTag,
                        chunkCheckpoints = checkpoints,
                        modelSizeBytes = modelFile.length(),
                        loadTimeMs = loadTimeMs,
                        generationTimeMs = System.currentTimeMillis() - generationStartedAt,
                        memoryObservationKb = currentUsedMemoryKb(),
                        generationMode = InsightGenerationMode.LLM_ENHANCED,
                        completionQuality = InsightCompletionQuality.ENHANCED
                    )
                } finally {
                    session.close()
                }
            } catch (error: OfflineAiException) {
                throw error
            } catch (error: Throwable) {
                lastError = error
            } finally {
                engine.close()
            }
        }
        throw when (lastError) {
            is OfflineAiException -> lastError
            else -> OfflineAiException.ModelInitializationFailed
        }
    }

    override fun close() = Unit

    private fun chunkPrompt(chunk: TranscriptChunk, languageTag: String?): String {
        return buildString {
            appendLine("You are a local meeting analyst. Analyze the meaning of the complete conversation, not isolated transcript fragments.")
            appendLine("Merge adjacent lines from the same speaker mentally before analyzing. Do not merely copy short phrases as insights.")
            appendLine("Return one valid JSON object only.")
            appendLine("Do not include markdown, explanations, code fences, comments, or text before/after the JSON.")
            appendLine("The first character must be { and the last character must be }.")
            appendLine("Output language: ${languageTag ?: "same as transcript"}.")
            appendLine(schemaInstructions())
            appendLine("Transcript chunk ${chunk.index}, ${chunk.startMs}-${chunk.endMs} ms:")
            chunk.segments.forEach { segment ->
                appendLine("[${segment.id}] ${segment.startMs}-${segment.endMs} ${segment.speakerLabel ?: ""}: ${segment.text}")
            }
        }
    }

    private fun synthesisPrompt(checkpoints: List<InsightChunkCheckpoint>, languageTag: String?): String {
        return buildString {
            appendLine("Synthesize these chunk results into one holistic, deduplicated meeting analysis.")
            appendLine("Explain the overall topic, how the discussion developed, the decisions, unresolved questions, and next actions.")
            appendLine("Do not concatenate chunk summaries or repeat transcript fragments verbatim.")
            appendLine("Return one valid JSON object only. Do not include markdown, explanations, code fences, comments, or text before/after the JSON.")
            appendLine("The first character must be { and the last character must be }.")
            appendLine("Output language: ${languageTag ?: "same as transcript"}.")
            appendLine(schemaInstructions())
            checkpoints.forEach { appendLine(it.outputJson) }
        }
    }

    private suspend fun generateStrictJson(
        session: com.google.ai.edge.litertlm.Session,
        prompt: String,
        validSegmentIds: Set<String>,
        policy: LiteRtLmBackendPolicy
    ): ParsedInsightJson {
        var rawOutput = withTimeout(policy.generationTimeoutMs) {
            session.generateContent(listOf(InputData.Text(asChatPrompt(prompt))))
        }
        runCatching { parser.parse(rawOutput, validSegmentIds) }.getOrNull()?.let { parsed ->
            return ParsedInsightJson(rawJson = rawOutput, insights = parsed)
        }

        rawOutput = withTimeout(policy.generationTimeoutMs) {
            session.generateContent(
                listOf(
                    InputData.Text(
                        asChatPrompt(
                            repairPrompt(
                                invalidOutput = rawOutput,
                                originalPrompt = prompt
                            )
                        )
                    )
                )
            )
        }
        return ParsedInsightJson(
            rawJson = rawOutput,
            insights = parser.parse(rawOutput, validSegmentIds)
        )
    }

    private fun repairPrompt(invalidOutput: String, originalPrompt: String): String {
        return buildString {
            appendLine("The previous answer was invalid because it was not one complete raw JSON object.")
            appendLine("Return the corrected answer now as JSON only.")
            appendLine("No markdown fences. No explanations. The first character must be { and the last character must be }.")
            appendLine("Keep only evidence IDs from the transcript in the original request.")
            appendLine("Original request:")
            appendLine(originalPrompt)
            appendLine("Invalid previous answer:")
            appendLine(invalidOutput.take(4_000))
        }
    }

    private fun schemaInstructions(): String {
        return """
            Minified JSON schema:
            {"smartTitle":"","summary":"","keyPoints":[{"text":"","evidenceSegmentIds":["s1"]}],"decisions":[{"text":"","evidenceSegmentIds":["s1"]}],"suggestions":[{"text":"","evidenceSegmentIds":["s1"]}],"openQuestions":[{"text":"","evidenceSegmentIds":["s1"]}],"actionItems":[{"task":"","assignee":null,"dueDate":null,"evidenceSegmentIds":["s1"]}],"chapters":[{"title":"","startMs":0,"endMs":1,"evidenceSegmentIds":["s1"]}],"evidenceSegmentIds":["s1"]}
            The summary must contain 2-5 complete sentences that synthesize the whole supplied conversation.
            Return up to 4 diverse key points and up to 3 decisions, actions, suggestions, questions, and chapters.
            Do not create an item from a meaningless 2-3 word phrase. Deduplicate overlapping ideas.
            Use only provided segment IDs. Every non-empty item needs evidence. assignee and dueDate are null unless explicit.
        """.trimIndent()
    }

    private fun asChatPrompt(userPrompt: String): String {
        return """
            <start_of_turn>user
            $userPrompt
            <end_of_turn>
            <start_of_turn>model
        """.trimIndent()
    }

    private fun ExecutionBackend.toLiteRtBackend(): Backend {
        return when (this) {
            ExecutionBackend.CPU -> Backend.CPU()
            ExecutionBackend.GPU -> Backend.GPU()
            ExecutionBackend.NPU -> Backend.NPU()
        }
    }

    private fun emptyInsights(languageTag: String?): AiInsights {
        val isVietnamese = languageTag?.lowercase()?.startsWith("vi") == true
        return AiInsights(
            suggestedTitle = if (isVietnamese) "Bản ghi không có lời thoại" else "Empty transcript",
            summary = if (isVietnamese) {
                "Không có nội dung bản chép lời để phân tích."
            } else {
                "No transcript content was available for local analysis."
            },
            chapters = listOf(
                Chapter(
                    title = if (isVietnamese) "Bản chép lời" else "Transcript",
                    startMs = 0L,
                    endMs = 0L
                )
            )
        )
    }

    private fun currentUsedMemoryKb(): Long {
        val runtime = Runtime.getRuntime()
        return (runtime.totalMemory() - runtime.freeMemory()) / 1024L
    }

    private data class ParsedInsightJson(
        val rawJson: String,
        val insights: AiInsights
    )
}
