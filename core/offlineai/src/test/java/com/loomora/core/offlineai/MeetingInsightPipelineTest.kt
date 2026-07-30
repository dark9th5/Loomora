package com.loomora.core.offlineai

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.loomora.core.database.LoomoraDatabase
import com.loomora.core.database.entity.RecordingEntity
import com.loomora.core.database.entity.TranscriptRevisionEntity
import com.loomora.core.model.AiInsights
import com.loomora.core.model.InsightRevisionKind
import com.loomora.core.model.RecordingStatus
import com.loomora.core.model.TranscriptSegment
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MeetingInsightPipelineTest {
    private lateinit var context: Context
    private lateinit var database: LoomoraDatabase
    private lateinit var json: Json
    private lateinit var parser: MeetingInsightJsonParser

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, LoomoraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        json = Json { ignoreUnknownKeys = false }
        parser = MeetingInsightJsonParser(json)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun validStructuredOutput_parsesEvidenceBackedInsights() {
        val insights = parser.parse(validJson(), setOf("s1", "s2"))

        assertEquals("Weekly Sync", insights.suggestedTitle)
        assertEquals(listOf("Ship beta"), insights.decisions)
        assertEquals("Review copy", insights.actionItems.single().task)
        assertEquals(listOf("s1", "s2"), insights.evidenceSegmentIds)
    }

    @Test
    fun invalidJson_isRetryableParseFailure() {
        val result = runCatching { parser.parse("{not-json", setOf("s1")) }

        assertTrue(result.exceptionOrNull() is OfflineAiException.InsightParseFailed)
    }

    @Test
    fun extractiveEngine_generatesEvidenceBackedInsightsWithoutModel() = runTest {
        val input = MeetingInsightInput(
            transcriptRevision = com.loomora.core.model.TranscriptRevision(
                id = "tr-extractive",
                recordingId = "rec-extractive",
                sourceFingerprint = "source",
                pipelineVersion = OfflineAiRuntimeVersions.TRANSCRIPTION_PIPELINE_VERSION,
                modelId = "asr",
                modelVersion = "1",
                languageTag = "vi",
                createdAt = 1L,
                segments = listOf(
                    TranscriptSegment(id = "s1", startMs = 0L, endMs = 2_000L, text = "Cả nhóm thống nhất chốt bản beta vào thứ sáu.", speakerLabel = "Speaker 1"),
                    TranscriptSegment(id = "s2", startMs = 5_000L, endMs = 8_000L, text = "Cần giao cho Linh review store listing ngày mai.", speakerLabel = "Speaker 2"),
                    TranscriptSegment(id = "s3", startMs = 11_000L, endMs = 14_000L, text = "Câu hỏi là liệu onboarding tiếng Việt cần sửa thêm không?", speakerLabel = "Speaker 1")
                )
            ),
            model = null,
            languageTag = "vi"
        )

        val output = HeuristicMeetingInsightEngine(json).analyze(input)

        assertEquals(OfflineAiRuntimeVersions.HEURISTIC_INSIGHTS_MODEL_ID, output.modelId)
        assertEquals(0L, output.modelSizeBytes)
        assertTrue(output.insights.suggestedTitle.isNotBlank())
        assertTrue(output.insights.summary.isNotBlank())
        assertTrue(output.insights.decisions.isNotEmpty())
        assertTrue(output.insights.actionItems.isNotEmpty())
        assertTrue(output.insights.openQuestions.isNotEmpty())
        assertTrue(output.insights.evidenceSegmentIds.all { it in setOf("s1", "s2", "s3") })
        assertTrue(output.chunkCheckpoints.single().segmentIds.contains("s1"))
        assertTrue(output.generationTimeMs >= 0L)
        assertTrue(requireNotNull(output.memoryObservationKb) > 0L)
    }

    @Test
    fun shortTranscript_doesNotInventStructuredInsights() = runTest {
        val input = MeetingInsightInput(
            transcriptRevision = com.loomora.core.model.TranscriptRevision(
                id = "tr-short",
                recordingId = "rec-short",
                sourceFingerprint = "source",
                pipelineVersion = OfflineAiRuntimeVersions.TRANSCRIPTION_PIPELINE_VERSION,
                modelId = "asr",
                modelVersion = "1",
                languageTag = "vi",
                createdAt = 1L,
                segments = listOf(
                    TranscriptSegment(id = "s1", startMs = 20_000L, endMs = 21_000L, text = "Nói nó"),
                    TranscriptSegment(id = "s2", startMs = 25_000L, endMs = 26_000L, text = "không có"),
                    TranscriptSegment(id = "s3", startMs = 30_000L, endMs = 31_000L, text = "thể đoàn")
                )
            ),
            model = null,
            languageTag = "vi"
        )

        val output = HeuristicMeetingInsightEngine(json).analyze(input)

        assertTrue(output.insights.suggestedTitle.isNotBlank())
        assertTrue(output.insights.summary.isNotBlank())
        assertTrue(output.insights.decisions.isEmpty())
        assertTrue(output.insights.actionItems.isEmpty())
    }

    @Test
    fun fallbackEngine_preservesHeuristicResultWhenLlamaCppUnavailable() = runTest {
        val gguf = java.io.File.createTempFile("loomora-llama", ".gguf")
        val baseInput = meetingInsightInput()
        val input = baseInput.copy(
            transcriptRevision = baseInput.transcriptRevision.copy(
                segments = listOf(
                    TranscriptSegment(id = "s1", startMs = 0L, endMs = 1000L, text = "Ship beta.", speakerLabel = "Speaker 1"),
                    TranscriptSegment(id = "s2", startMs = 3000L, endMs = 4000L, text = "Review copy.", speakerLabel = "Speaker 2")
                )
            ),
            model = OfflineModelRecord(
                manifest = OfflineModelManifest(
                    id = "qwen2.5-0.5b-instruct-q4-gguf",
                    version = "test",
                    capability = ModelCapability.INSIGHTS,
                    runtime = RuntimeKind.LLAMA_CPP,
                    fileName = gguf.name,
                    sizeBytes = gguf.length(),
                    sha256 = "fixture",
                    minimumRamMb = 2048,
                    supportedAbis = setOf("arm64-v8a", "x86_64"),
                    supportedLanguages = setOf("en", "vi"),
                    licenseName = "Test",
                    pipelineCompatibility = OfflineAiRuntimeVersions.INSIGHTS_PIPELINE_VERSION
                ),
                state = ModelInstallState.READY,
                installedPath = gguf.absolutePath,
                installedAt = 1L,
                lastVerifiedAt = 1L,
                compatibility = CompatibilityResult.Compatible(ExecutionBackend.CPU)
            )
        )

        try {
            val output = FallbackMeetingInsightEngine(
                heuristic = HeuristicMeetingInsightEngine(json),
                liteRtLm = LiteRtLmMeetingInsightEngine(context, parser, TranscriptChunker()),
                llamaCpp = LlamaCppMeetingInsightEngine(parser, TranscriptChunker(), UnavailableLlamaCppRuntime())
            ).analyze(input)

            assertEquals(OfflineAiRuntimeVersions.HYBRID_INSIGHTS_MODEL_ID, output.modelId)
            assertTrue(output.usedHeuristicFallback)
            assertEquals("ProcessingUnavailable", output.fallbackReason)
            assertTrue(output.insights.summary.isNotBlank())
            assertTrue(output.insights.evidenceSegmentIds.all { it in setOf("s1", "s2") })
        } finally {
            gguf.delete()
        }
    }

    @Test
    fun llamaCppEngine_parsesConstrainedRuntimeJsonWhenRuntimeAvailable() = runTest {
        val gguf = java.io.File.createTempFile("loomora-llama", ".gguf")
        val baseInput = meetingInsightInput()
        val input = baseInput.copy(
            transcriptRevision = baseInput.transcriptRevision.copy(
                segments = listOf(
                    TranscriptSegment(id = "s1", startMs = 0L, endMs = 1000L, text = "Ship beta.", speakerLabel = "Speaker 1"),
                    TranscriptSegment(id = "s2", startMs = 3000L, endMs = 4000L, text = "Review copy.", speakerLabel = "Speaker 2")
                )
            ),
            model = OfflineModelRecord(
                manifest = OfflineModelManifest(
                    id = "qwen2.5-0.5b-instruct-q4-gguf",
                    version = "test",
                    capability = ModelCapability.INSIGHTS,
                    runtime = RuntimeKind.LLAMA_CPP,
                    fileName = gguf.name,
                    sizeBytes = gguf.length(),
                    sha256 = "fixture",
                    minimumRamMb = 2048,
                    supportedAbis = setOf("arm64-v8a", "x86_64"),
                    supportedLanguages = setOf("en", "vi"),
                    licenseName = "Test",
                    pipelineCompatibility = OfflineAiRuntimeVersions.INSIGHTS_PIPELINE_VERSION
                ),
                state = ModelInstallState.READY,
                installedPath = gguf.absolutePath,
                installedAt = 1L,
                lastVerifiedAt = 1L,
                compatibility = CompatibilityResult.Compatible(ExecutionBackend.CPU)
            )
        )
        val runtime = object : LlamaCppRuntime {
            override suspend fun generateConstrainedJson(input: LlamaCppGenerationInput): LlamaCppGenerationOutput {
                assertTrue(input.grammar.contains("root ::= object"))
                assertTrue(input.maxOutputTokens <= 768)
                return LlamaCppGenerationOutput(
                    json = validJson(),
                    loadTimeMs = 7L,
                    generationTimeMs = 11L,
                    memoryObservationKb = 2048L
                )
            }

            override fun close() = Unit
        }

        try {
            val output = LlamaCppMeetingInsightEngine(parser, TranscriptChunker(), runtime).analyze(input)

            assertEquals("qwen2.5-0.5b-instruct-q4-gguf", output.modelId)
            assertEquals(7L, output.loadTimeMs)
            assertEquals(11L, output.generationTimeMs)
            assertEquals(listOf("s1", "s2"), output.insights.evidenceSegmentIds)
        } finally {
            gguf.delete()
        }
    }

    @Test
    fun missingEvidence_failsValidation() {
        val result = runCatching {
            parser.parse(
                """{"smartTitle":"T","summary":"S","keyPoints":[{"text":"Point","evidenceSegmentIds":[]}],"evidenceSegmentIds":[]}""",
                setOf("s1")
            )
        }

        assertTrue(result.exceptionOrNull() is OfflineAiException.InsightParseFailed)
    }

    @Test
    fun hallucinatedSegmentId_failsValidation() {
        val result = runCatching {
            parser.parse(
                """{"smartTitle":"T","summary":"S","keyPoints":[{"text":"Point","evidenceSegmentIds":["missing"]}],"evidenceSegmentIds":[]}""",
                setOf("s1")
            )
        }

        assertTrue(result.exceptionOrNull() is OfflineAiException.InsightParseFailed)
    }

    @Test
    fun noActionItems_parsesEmptyList() {
        val insights = parser.parse(
            """{"smartTitle":"T","summary":"S","actionItems":[],"evidenceSegmentIds":["s1"]}""",
            setOf("s1")
        )

        assertTrue(insights.actionItems.isEmpty())
    }

    @Test
    fun missingAssigneeAndDeadline_remainNull() {
        val insights = parser.parse(
            """{"smartTitle":"T","summary":"S","actionItems":[{"task":"Follow up","evidenceSegmentIds":["s1"]}],"evidenceSegmentIds":["s1"]}""",
            setOf("s1")
        )

        assertNull(insights.actionItems.single().assignee)
        assertNull(insights.actionItems.single().dueDate)
    }

    @Test
    fun longTranscriptChunking_preservesOrderAndDeduplicatesOutputItems() {
        val segments = (0 until 40).map { index ->
            TranscriptSegment(
                id = "s$index",
                startMs = index * 1_000L,
                endMs = index * 1_000L + 900L,
                text = "segment $index ".repeat(20)
            )
        }

        val chunks = TranscriptChunker().chunk(segments, maxChars = 1_000)

        assertTrue(chunks.size > 1)
        assertEquals("s0", chunks.first().segmentIds.first())
        assertEquals("s39", chunks.last().segmentIds.last())
    }

    @Test
    fun userEditPreserved_asSeparateRevisionKind() = runTest {
        insertRecordingAndTranscript()
        val repository = InsightRepository(database.insightDao(), json)
        val generated = repository.publishGeneratedRevision(
            recordingId = "rec-1",
            transcriptRevisionId = "tr-1",
            sourceFingerprint = "source",
            modelId = "llm",
            modelVersion = "1",
            languageTag = "en",
            insights = AiInsights("Generated", "Summary"),
            checkpoints = emptyList(),
            modelSizeBytes = 10L,
            loadTimeMs = 1L,
            generationTimeMs = 2L,
            memoryObservationKb = 3L
        )
        val edited = repository.publishUserEditedRevision(generated, AiInsights("Edited", "Edited summary"))

        assertEquals(InsightRevisionKind.GENERATED, generated.kind)
        assertEquals(InsightRevisionKind.USER_EDITED, edited.kind)
        assertEquals("Generated", generated.insights.suggestedTitle)
        assertEquals("Edited", edited.insights.suggestedTitle)
    }

    @Test
    fun cancellation_propagatesAsCancellation() = runTest {
        val engine = object : LocalMeetingInsightEngine {
            override suspend fun analyze(input: MeetingInsightInput): MeetingInsightOutput {
                throw CancellationException("cancelled")
            }

            override fun close() = Unit
        }
        val input = meetingInsightInput()

        val result = runCatching { engine.analyze(input) }

        assertTrue(result.exceptionOrNull() is CancellationException)
    }

    @Test
    fun backendPolicy_deduplicatesPreferredAndFallbackOrder() {
        val policy = LiteRtLmBackendPolicy(
            preferred = listOf(ExecutionBackend.NPU, ExecutionBackend.GPU),
            fallback = listOf(ExecutionBackend.GPU, ExecutionBackend.CPU)
        )

        val order = (policy.preferred + policy.fallback).distinct()

        assertEquals(listOf(ExecutionBackend.NPU, ExecutionBackend.GPU, ExecutionBackend.CPU), order)
    }

    @Test
    fun closeAfterError_isRequiredByEngineContract() = runTest {
        val engine = CloseTrackingInsightEngine(fail = true)

        val result = runCatching {
            try {
                engine.analyze(meetingInsightInput())
            } finally {
                engine.close()
            }
        }

        assertTrue(result.exceptionOrNull() is OfflineAiException.InsightParseFailed)
        assertTrue(engine.closed)
    }

    private suspend fun insertRecordingAndTranscript() {
        database.recordingDao().insertRecording(
            RecordingEntity(
                id = "rec-1",
                title = "Fixture",
                createdAt = 1L,
                updatedAt = 1L,
                durationMs = 1_000L,
                status = RecordingStatus.SAVED.name,
                originalFileUri = "file:///tmp/fixture.wav",
                mimeType = "audio/wav",
                sampleRate = 16_000,
                channels = 1,
                bitrate = 128_000,
                sizeBytes = 1L,
                languageHint = "en"
            )
        )
        database.transcriptDao().upsertRevision(
            TranscriptRevisionEntity(
                id = "tr-1",
                recordingId = "rec-1",
                sourceFingerprint = "source",
                pipelineVersion = OfflineAiRuntimeVersions.TRANSCRIPTION_PIPELINE_VERSION,
                modelId = "asr",
                modelVersion = "1",
                languageTag = "en",
                status = "COMPLETE",
                segmentCount = 0,
                processingDurationMs = 1L,
                memoryObservationKb = 1L,
                createdAt = 1L,
                updatedAt = 1L
            )
        )
    }

    private fun meetingInsightInput(): MeetingInsightInput {
        return MeetingInsightInput(
            transcriptRevision = com.loomora.core.model.TranscriptRevision(
                id = "tr-1",
                recordingId = "rec-1",
                sourceFingerprint = "source",
                pipelineVersion = OfflineAiRuntimeVersions.TRANSCRIPTION_PIPELINE_VERSION,
                modelId = "asr",
                modelVersion = "1",
                languageTag = "en",
                createdAt = 1L,
                segments = listOf(TranscriptSegment(id = "s1", startMs = 0L, endMs = 1000L, text = "Review launch"))
            ),
            model = OfflineModelRecord(
                manifest = OfflineModelManifest(
                    id = "llm",
                    version = "1",
                    capability = ModelCapability.INSIGHTS,
                    runtime = RuntimeKind.LITERT_LM,
                    fileName = "model.litertlm",
                    sizeBytes = 1L,
                    sha256 = "fixture",
                    minimumRamMb = null,
                    supportedAbis = setOf("arm64-v8a", "x86_64"),
                    supportedLanguages = setOf("en"),
                    licenseName = "Test",
                    pipelineCompatibility = OfflineAiRuntimeVersions.INSIGHTS_PIPELINE_VERSION
                ),
                state = ModelInstallState.READY,
                installedPath = "/tmp/model.litertlm",
                installedAt = 1L,
                lastVerifiedAt = 1L,
                compatibility = CompatibilityResult.Compatible(ExecutionBackend.CPU)
            ),
            languageTag = "en"
        )
    }

    private fun validJson(): String {
        return """
            {
              "smartTitle":"Weekly Sync",
              "summary":"The team reviewed beta readiness.",
              "keyPoints":[{"text":"Beta is nearly ready","evidenceSegmentIds":["s1"]}],
              "decisions":[{"text":"Ship beta","evidenceSegmentIds":["s1"]}],
              "suggestions":[{"text":"Tighten launch copy","evidenceSegmentIds":["s2"]}],
              "openQuestions":[{"text":"Who owns release notes?","evidenceSegmentIds":["s2"]}],
              "actionItems":[{"task":"Review copy","assignee":null,"dueDate":null,"evidenceSegmentIds":["s2"]}],
              "chapters":[{"title":"Launch","startMs":0,"endMs":1000,"evidenceSegmentIds":["s1","s2"]}],
              "evidenceSegmentIds":["s1","s2"]
            }
        """.trimIndent()
    }

    private class CloseTrackingInsightEngine(
        private val fail: Boolean
    ) : LocalMeetingInsightEngine {
        var closed = false

        override suspend fun analyze(input: MeetingInsightInput): MeetingInsightOutput {
            if (fail) throw OfflineAiException.InsightParseFailed
            return MeetingInsightOutput(
                insights = AiInsights("T", "S"),
                modelId = input.model?.manifest?.id ?: OfflineAiRuntimeVersions.HEURISTIC_INSIGHTS_MODEL_ID,
                modelVersion = input.model?.manifest?.version ?: OfflineAiRuntimeVersions.HEURISTIC_INSIGHTS_MODEL_VERSION,
                promptVersion = OfflineAiRuntimeVersions.INSIGHTS_PROMPT_VERSION,
                schemaVersion = OfflineAiRuntimeVersions.INSIGHTS_SCHEMA_VERSION,
                pipelineVersion = OfflineAiRuntimeVersions.INSIGHTS_PIPELINE_VERSION,
                languageTag = input.languageTag,
                chunkCheckpoints = emptyList(),
                modelSizeBytes = 1L,
                loadTimeMs = 1L,
                generationTimeMs = 1L,
                memoryObservationKb = 1L
            )
        }

        override fun close() {
            closed = true
        }
    }
}
