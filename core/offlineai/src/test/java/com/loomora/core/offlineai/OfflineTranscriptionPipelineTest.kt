package com.loomora.core.offlineai

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.loomora.core.database.LoomoraDatabase
import com.loomora.core.database.entity.OfflineModelEntity
import com.loomora.core.database.entity.RecordingEntity
import com.loomora.core.model.AiJobStatus
import com.loomora.core.model.RecordingStatus
import com.loomora.core.model.TranscriptSegment
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class OfflineTranscriptionPipelineTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var database: LoomoraDatabase
    private lateinit var json: Json

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, LoomoraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        json = Json { ignoreUnknownKeys = true }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun modelMissing_returnsTypedUiState() = runTest {
        val source = writeWav(temporaryFolder.newFile("vi.wav"), durationMs = 500)
        insertRecording(source)
        val coordinator = createCoordinator(engine = FakeTranscriptionEngine())

        coordinator.processAudio("rec-1", "file://${source.absolutePath}")

        assertTrue(coordinator.jobStatus.value is AiJobStatus.ModelRequired)
        assertEquals("MODEL_MISSING", database.recordingDao().getRecordingByIdSync("rec-1")?.transcriptStatus)
    }

    @Test
    fun shortVietnameseFixture_persistsTimestampedRevision() = runTest {
        val source = writeWav(temporaryFolder.newFile("short-vi.wav"), durationMs = 1_200)
        insertRecording(source)
        insertReadyModel()
        val coordinator = createCoordinator(
            engine = FakeTranscriptionEngine(
                segments = listOf(
                    TranscriptSegment(startMs = 0, endMs = 700, rawText = "  Xin   chao Loomora  ", text = "  Xin   chao Loomora  ")
                )
            )
        )

        coordinator.processAudio("rec-1", "file://${source.absolutePath}")

        val status = coordinator.jobStatus.value
        assertTrue(status is AiJobStatus.Completed)
        val savedRevision = database.transcriptDao()
            .getSegmentsForRevisionSync((status as AiJobStatus.Completed).transcript.first().revisionId)
        assertEquals(1, savedRevision.size)
        assertEquals("Xin chao Loomora", savedRevision.first().normalizedText)
        assertEquals("COMPLETE", database.recordingDao().getRecordingByIdSync("rec-1")?.transcriptStatus)
    }

    @Test
    fun mixedLanguageFixture_preservesRawAndNormalizedText() = runTest {
        val source = writeWav(temporaryFolder.newFile("mixed.wav"), durationMs = 1_000)
        insertRecording(source)
        insertReadyModel()
        val coordinator = createCoordinator(
            engine = FakeTranscriptionEngine(
                segments = listOf(
                    TranscriptSegment(startMs = 100, endMs = 900, rawText = "Hom nay review offline ASR", text = "Hom nay review offline ASR")
                )
            )
        )

        coordinator.processAudio("rec-1", "file://${source.absolutePath}")

        val completed = coordinator.jobStatus.value as AiJobStatus.Completed
        assertEquals("Hom nay review offline ASR", completed.transcript.single().text)
        assertEquals("Hom nay review offline ASR", completed.transcript.single().rawText)
    }

    @Test
    fun retrySameSource_doesNotDuplicateSegments() = runTest {
        val source = writeWav(temporaryFolder.newFile("retry.wav"), durationMs = 1_000)
        insertRecording(source)
        insertReadyModel()
        val coordinator = createCoordinator(engine = FakeTranscriptionEngine())

        coordinator.processAudio("rec-1", "file://${source.absolutePath}")
        val first = coordinator.jobStatus.value as AiJobStatus.Completed
        coordinator.resetStatus()
        coordinator.processAudio("rec-1", "file://${source.absolutePath}")
        val second = coordinator.jobStatus.value as AiJobStatus.Completed

        assertEquals(first.transcript.map { it.id }, second.transcript.map { it.id })
        assertEquals(1, database.transcriptDao().getSegmentsForRevisionSync(first.transcript.first().revisionId).size)
    }

    @Test
    fun silenceFixture_preservesEmptySpeechWindowsWithoutFailing() = runTest {
        val source = writeWav(temporaryFolder.newFile("silence.wav"), durationMs = 1_000, amplitude = 0.0)
        insertRecording(source)
        insertReadyModel()
        val engine = CapturingTranscriptionEngine()
        val coordinator = createCoordinator(engine = engine)

        coordinator.processAudio("rec-1", "file://${source.absolutePath}")

        assertTrue(coordinator.jobStatus.value is AiJobStatus.Completed)
        assertTrue(requireNotNull(engine.lastInput).speechWindows.isEmpty())
    }

    @Test
    fun sourceChanged_createsNewRevisionInsteadOfReusingStaleTranscript() = runTest {
        val source = writeWav(temporaryFolder.newFile("changed-source.wav"), durationMs = 1_000, frequencyHz = 440.0)
        insertRecording(source)
        insertReadyModel()
        val coordinator = createCoordinator(engine = FakeTranscriptionEngine())

        coordinator.processAudio("rec-1", "file://${source.absolutePath}")
        val first = coordinator.jobStatus.value as AiJobStatus.Completed

        writeWav(source, durationMs = 1_000, frequencyHz = 880.0)
        coordinator.resetStatus()
        coordinator.processAudio("rec-1", "file://${source.absolutePath}")
        val second = coordinator.jobStatus.value as AiJobStatus.Completed

        assertNotEquals(first.transcript.first().revisionId, second.transcript.first().revisionId)
    }

    @Test
    fun corruptFile_reportsFailed() = runTest {
        val source = temporaryFolder.newFile("corrupt.wav").apply { writeText("not audio") }
        insertRecording(source)
        insertReadyModel()
        val coordinator = createCoordinator(engine = FakeTranscriptionEngine())

        coordinator.processAudio("rec-1", "file://${source.absolutePath}")

        assertTrue(coordinator.jobStatus.value is AiJobStatus.Failed)
        assertEquals("FAILED", database.recordingDao().getRecordingByIdSync("rec-1")?.transcriptStatus)
    }

    @Test
    fun cancel_cleansTempPcm() = runTest {
        val source = writeWav(temporaryFolder.newFile("cancel.wav"), durationMs = 1_000)
        insertRecording(source)
        insertReadyModel()
        val preprocessor = CapturingPreprocessor(context)
        val coordinator = createCoordinator(
            engine = object : FakeTranscriptionEngine() {
                override suspend fun transcribe(input: TranscriptionInput): TranscriptionOutput {
                    throw CancellationException("cancel")
                }
            },
            preprocessor = preprocessor
        )

        val result = runCatching {
            coordinator.processAudio("rec-1", "file://${source.absolutePath}")
        }

        assertTrue(result.exceptionOrNull() is CancellationException)
        assertFalse(requireNotNull(preprocessor.lastPrepared).pcm16kMonoFile.exists())
        assertEquals("CANCELLED", database.recordingDao().getRecordingByIdSync("rec-1")?.transcriptStatus)
    }

    @Test
    fun sherpaEngine_missingTokensFailsTypedBeforeNativeInit() = runTest {
        val modelDir = temporaryFolder.newFolder("sherpa-missing-tokens")
        val encoder = File(modelDir, "tiny-encoder.int8.onnx").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        File(modelDir, "tiny-decoder.int8.onnx").writeBytes(byteArrayOf(4, 5, 6))
        val pcm = temporaryFolder.newFile("speech.pcm").apply { writeBytes(ByteArray(320) { 1 }) }
        val engine = SherpaOnnxTranscriptionEngine(context)

        val result = runCatching {
            engine.transcribe(
                TranscriptionInput(
                    pcm16kMonoFile = pcm,
                    originalAudioFile = pcm,
                    sourceFingerprint = "fixture",
                    languageHint = "vi",
                    model = OfflineModelRecord(
                        manifest = OfflineModelManifest(
                            id = "missing-tokens",
                            version = "1",
                            capability = ModelCapability.TRANSCRIPTION,
                            runtime = RuntimeKind.SHERPA_ONNX,
                            fileName = encoder.name,
                            sizeBytes = encoder.length(),
                            sha256 = "fixture",
                            minimumRamMb = null,
                            supportedAbis = setOf("arm64-v8a", "x86_64"),
                            supportedLanguages = setOf("vi", "en"),
                            licenseName = "Test",
                            pipelineCompatibility = OfflineAiRuntimeVersions.PIPELINE_VERSION
                        ),
                        state = ModelInstallState.READY,
                        installedPath = encoder.absolutePath,
                        installedAt = 1L,
                        lastVerifiedAt = 1L,
                        compatibility = CompatibilityResult.Compatible(ExecutionBackend.CPU)
                    ),
                    speechWindows = emptyList()
                )
            )
        }

        assertTrue(result.exceptionOrNull() is OfflineAiException.ModelFileMissing)
    }

    private fun createCoordinator(
        engine: LocalTranscriptionEngine,
        preprocessor: AudioTranscriptionPreprocessor = AudioTranscriptionPreprocessor(context)
    ): OfflineAnalysisCoordinator {
        val modelRepository = OfflineModelRepository(
            offlineModelDao = database.offlineModelDao(),
            importer = OfflineModelImporter(context, json, ModelCompatibilityChecker(context)),
            compatibilityChecker = object : ModelCompatibilityChecker(context) {
                override fun evaluate(manifest: OfflineModelManifest): CompatibilityResult {
                    return CompatibilityResult.Compatible(ExecutionBackend.CPU)
                }
            },
            catalog = DefaultOfflineModelCatalog(),
            json = json
        )
        return OfflineAnalysisCoordinator(
            modelRepository = modelRepository,
            analysisJobRepository = AnalysisJobRepository(database.analysisJobDao(), json),
            transcriptRepository = TranscriptRepository(database.transcriptDao()),
            diarizationRepository = DiarizationRepository(database.diarizationDao(), json),
            insightRepository = InsightRepository(database.insightDao(), json),
            recordingDao = database.recordingDao(),
            preprocessor = preprocessor,
            transcriptionEngine = engine,
            diarizationEngine = FakeDiarizationEngine(),
            meetingInsightEngine = FakeMeetingInsightEngine(),
            engineLifecycleManager = OfflineEngineLifecycleManager()
        )
    }

    private suspend fun insertRecording(source: File) {
        database.recordingDao().insertRecording(
            RecordingEntity(
                id = "rec-1",
                title = "Fixture",
                createdAt = 1L,
                updatedAt = 1L,
                durationMs = 1_000L,
                status = RecordingStatus.SAVED.name,
                originalFileUri = "file://${source.absolutePath}",
                mimeType = "audio/wav",
                sampleRate = 16_000,
                channels = 1,
                bitrate = 256_000,
                sizeBytes = source.length(),
                languageHint = "vi"
            )
        )
    }

    private suspend fun insertReadyModel() {
        val modelFile = temporaryFolder.newFile("whisper.onnx").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        database.offlineModelDao().upsertModel(
            OfflineModelEntity(
                modelId = "whisper-multilingual-test",
                version = "1",
                capability = ModelCapability.TRANSCRIPTION.name,
                runtime = RuntimeKind.SHERPA_ONNX.name,
                fileName = modelFile.name,
                sizeBytes = modelFile.length(),
                sha256 = "fixture",
                minimumRamMb = null,
                supportedAbisJson = json.encodeToString(SetSerializer(String.serializer()), setOf("arm64-v8a", "x86_64")),
                supportedLanguagesJson = json.encodeToString(SetSerializer(String.serializer()), setOf("vi", "en")),
                licenseName = "Test fixture",
                licenseUrl = null,
                sourceUrl = null,
                pipelineCompatibility = OfflineAiRuntimeVersions.TRANSCRIPTION_PIPELINE_VERSION,
                state = ModelInstallState.READY.name,
                installedPath = modelFile.absolutePath,
                installedAt = 1L,
                lastVerifiedAt = 1L,
                errorCode = null
            )
        )
    }

    private fun writeWav(
        target: File,
        durationMs: Long,
        frequencyHz: Double = 440.0,
        amplitude: Double = 0.4
    ): File {
        val sampleRate = 16_000
        val totalFrames = ((sampleRate.toLong() * durationMs) / 1000L).toInt().coerceAtLeast(1)
        val dataSize = totalFrames * 2
        RandomAccessFile(target, "rw").use { file ->
            file.setLength(0L)
            file.writeBytes("RIFF")
            file.writeInt(Integer.reverseBytes(36 + dataSize))
            file.writeBytes("WAVE")
            file.writeBytes("fmt ")
            file.writeInt(Integer.reverseBytes(16))
            file.writeShort(java.lang.Short.reverseBytes(1.toShort()).toInt())
            file.writeShort(java.lang.Short.reverseBytes(1.toShort()).toInt())
            file.writeInt(Integer.reverseBytes(sampleRate))
            file.writeInt(Integer.reverseBytes(sampleRate * 2))
            file.writeShort(java.lang.Short.reverseBytes(2.toShort()).toInt())
            file.writeShort(java.lang.Short.reverseBytes(16.toShort()).toInt())
            file.writeBytes("data")
            file.writeInt(Integer.reverseBytes(dataSize))
            repeat(totalFrames) { frame ->
                val sample = (sin((2.0 * PI * frequencyHz * frame) / sampleRate) * Short.MAX_VALUE * amplitude)
                    .roundToInt()
                    .toShort()
                file.writeShort(java.lang.Short.reverseBytes(sample).toInt())
            }
        }
        return target
    }

    open class FakeTranscriptionEngine(
        private val segments: List<TranscriptSegment> = listOf(
            TranscriptSegment(startMs = 0, endMs = 500, rawText = "Xin chao", text = "Xin chao")
        )
    ) : LocalTranscriptionEngine {
        override suspend fun transcribe(input: TranscriptionInput): TranscriptionOutput {
            return TranscriptionOutput(
                segments = segments,
                modelId = input.model.manifest.id,
                modelVersion = input.model.manifest.version,
                languageTag = input.languageHint ?: "vi",
                processingDurationMs = 12L,
                memoryObservationKb = 1024L
            )
        }

        override fun close() = Unit
    }

    private class CapturingTranscriptionEngine : FakeTranscriptionEngine() {
        var lastInput: TranscriptionInput? = null

        override suspend fun transcribe(input: TranscriptionInput): TranscriptionOutput {
            lastInput = input
            return super.transcribe(input)
        }
    }

    private class CapturingPreprocessor(context: Context) : AudioTranscriptionPreprocessor(context) {
        var lastPrepared: PreparedTranscriptionAudio? = null

        override suspend fun prepare(sourceFile: File): PreparedTranscriptionAudio {
            return super.prepare(sourceFile).also { lastPrepared = it }
        }
    }

    private class FakeDiarizationEngine : LocalDiarizationEngine {
        override suspend fun diarize(input: DiarizationInput): DiarizationOutput {
            return DiarizationOutput(
                turns = emptyList(),
                modelId = input.model.manifest.id,
                modelVersion = input.model.manifest.version,
                clusteringSettings = input.clustering,
                processingDurationMs = 1L,
                memoryObservationKb = 1L
            )
        }

        override fun close() = Unit
    }

    private class FakeMeetingInsightEngine : LocalMeetingInsightEngine {
        override suspend fun analyze(input: MeetingInsightInput): MeetingInsightOutput {
            return MeetingInsightOutput(
                insights = com.loomora.core.model.AiInsights(
                    suggestedTitle = "Fixture",
                    summary = "Fixture summary"
                ),
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

        override fun close() = Unit
    }
}
