package com.loomora.core.offlineai

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import android.util.Log
import com.loomora.core.model.TranscriptRevision
import com.loomora.core.model.TranscriptSegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

@RunWith(AndroidJUnit4::class)
class SherpaOnnxDeviceSmokeTest {
    @Test
    fun whisperTinyInt8_transcribesBundledFixtureOffline() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val externalDir = requireNotNull(context.getExternalFilesDir(null))
        val modelPack = File(externalDir, MODEL_PACK_NAME)
        val sourceWav = File(externalDir, FIXTURE_NAME)
        assertPushedSmokeInput(modelPack)
        assertPushedSmokeInput(sourceWav)
        Log.i(TAG, "Inputs ready modelBytes=${modelPack.length()} wavBytes=${sourceWav.length()}")

        val json = Json { ignoreUnknownKeys = true }
        val checker = object : ModelCompatibilityChecker(context) {
            override fun evaluate(manifest: OfflineModelManifest): CompatibilityResult {
                return CompatibilityResult.Compatible(ExecutionBackend.CPU)
            }
        }
        val imported = OfflineModelImporter(context, json, checker)
            .importModel(Uri.fromFile(modelPack))
        Log.i(TAG, "Model imported path=${imported.publishedPayload?.absolutePath}")
        assertTrue(imported.compatibility is CompatibilityResult.Compatible)
        val installedPayload = requireNotNull(imported.publishedPayload)
        assertTrue(installedPayload.exists())

        val prepared = AudioTranscriptionPreprocessor(context).prepare(sourceWav)
        Log.i(TAG, "Audio prepared pcmBytes=${prepared.pcm16kMonoFile.length()} windows=${prepared.speechWindows.size}")
        try {
            Log.i(TAG, "Starting sherpa transcription")
            val output = SherpaOnnxTranscriptionEngine(context).transcribe(
                TranscriptionInput(
                    pcm16kMonoFile = prepared.pcm16kMonoFile,
                    originalAudioFile = sourceWav,
                    sourceFingerprint = prepared.sourceFingerprint,
                    languageHint = "en",
                    model = OfflineModelRecord(
                        manifest = imported.manifest,
                        state = ModelInstallState.READY,
                        installedPath = installedPayload.absolutePath,
                        installedAt = System.currentTimeMillis(),
                        lastVerifiedAt = System.currentTimeMillis(),
                        compatibility = CompatibilityResult.Compatible(ExecutionBackend.CPU)
                    ),
                    speechWindows = prepared.speechWindows
                )
            )

            Log.i(TAG, "Transcription complete segments=${output.segments.size} text=${output.segments.joinToString(" ") { it.text }}")
            assertEquals(imported.manifest.id, output.modelId)
            assertTrue("Expected a non-empty transcript", output.segments.isNotEmpty())
            val transcriptText = output.segments.joinToString(" ") { it.text }.lowercase()
            assertTrue(
                "Expected transcript text from fixture, got: $transcriptText",
                transcriptText.contains("after early nightfall") &&
                    transcriptText.contains("yellow lamps")
            )
        } finally {
            prepared.pcm16kMonoFile.delete()
        }
    }

    @Test
    fun aacM4aFixture_decodesToPcm16kMonoWithoutFullFileBuffer() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val externalDir = requireNotNull(context.getExternalFilesDir(null))
        val sourceM4a = File(externalDir, "loomora-generated-aac.m4a")
        writeSyntheticAacM4a(sourceM4a)

        val prepared = AudioTranscriptionPreprocessor(context).prepare(sourceM4a)
        try {
            Log.i(TAG, "AAC prepared pcmBytes=${prepared.pcm16kMonoFile.length()} windows=${prepared.speechWindows.size}")
            assertTrue("Expected decoded PCM output", prepared.pcm16kMonoFile.length() > 0L)
            assertTrue("Expected speech windows from generated tone", prepared.speechWindows.isNotEmpty())
        } finally {
            prepared.pcm16kMonoFile.delete()
            sourceM4a.delete()
        }
    }

    @Test
    fun pyannote3dSpeaker_diarizesOfficialTwoSpeakerFixtureOffline() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val externalDir = requireNotNull(context.getExternalFilesDir(null))
        val modelPack = File(externalDir, DIARIZATION_MODEL_PACK_NAME)
        val sourceWav = File(externalDir, TWO_SPEAKER_FIXTURE_NAME)
        assertPushedSmokeInput(modelPack)
        assertPushedSmokeInput(sourceWav)

        val json = Json { ignoreUnknownKeys = true }
        val checker = object : ModelCompatibilityChecker(context) {
            override fun evaluate(manifest: OfflineModelManifest): CompatibilityResult {
                return CompatibilityResult.Compatible(ExecutionBackend.CPU)
            }
        }
        val imported = OfflineModelImporter(context, json, checker)
            .importModel(Uri.fromFile(modelPack))
        assertTrue(imported.compatibility is CompatibilityResult.Compatible)
        val installedPayload = requireNotNull(imported.publishedPayload)
        val prepared = AudioTranscriptionPreprocessor(context).prepare(sourceWav)
        try {
            val output = SherpaOnnxDiarizationEngine(context).diarize(
                DiarizationInput(
                    pcm16kMonoFile = prepared.pcm16kMonoFile,
                    originalAudioFile = sourceWav,
                    sourceFingerprint = prepared.sourceFingerprint,
                    model = OfflineModelRecord(
                        manifest = imported.manifest,
                        state = ModelInstallState.READY,
                        installedPath = installedPayload.absolutePath,
                        installedAt = System.currentTimeMillis(),
                        lastVerifiedAt = System.currentTimeMillis(),
                        compatibility = CompatibilityResult.Compatible(ExecutionBackend.CPU)
                    ),
                    clustering = DiarizationClusteringSettings()
                )
            )
            Log.i(TAG, "Diarization complete turns=${output.turns.size} labels=${output.turns.map { it.speakerLabel }.distinct()}")
            assertEquals(imported.manifest.id, output.modelId)
            assertTrue("Expected real diarization turns", output.turns.isNotEmpty())
            assertTrue("Expected at least two generic speaker labels", output.turns.map { it.speakerLabel }.distinct().size >= 2)

            val fused = TranscriptSpeakerFusion.fuse(
                transcript = listOf(
                    TranscriptSegment(
                        startMs = output.turns.minOf { it.startMs },
                        endMs = output.turns.maxOf { it.endMs },
                        text = "Synthetic transcript spanning the official two speaker fixture."
                    )
                ),
                turns = output.turns
            )
            assertTrue("Expected fused speaker timeline", fused.isNotEmpty())
            assertTrue("Expected fused transcript to carry speaker labels", fused.any { it.speakerLabel != null })
        } finally {
            prepared.pcm16kMonoFile.delete()
        }
    }

    @Test
    fun extractiveInsights_generatesMeetingInsightsOffline() = runBlocking {
        val json = Json { ignoreUnknownKeys = false }
        val transcript = TranscriptRevision(
            id = "insight-smoke-revision",
            recordingId = "insight-smoke-recording",
            sourceFingerprint = "manual-fixture",
            pipelineVersion = OfflineAiRuntimeVersions.TRANSCRIPTION_PIPELINE_VERSION,
            modelId = "manual-transcript",
            modelVersion = "1",
            languageTag = "en",
            createdAt = System.currentTimeMillis(),
            segments = listOf(
                TranscriptSegment(id = "s1", startMs = 0L, endMs = 4_000L, text = "Maya says the Android beta is ready for Friday if crash reports stay low."),
                TranscriptSegment(id = "s2", startMs = 4_000L, endMs = 8_000L, text = "Jon decides to publish the beta on Friday and asks Linh to review the store listing."),
                TranscriptSegment(id = "s3", startMs = 8_000L, endMs = 12_000L, text = "The team asks whether Vietnamese onboarding copy needs another pass before release.")
            )
        )

        val output = HeuristicMeetingInsightEngine(json).analyze(
            MeetingInsightInput(
                transcriptRevision = transcript,
                model = null,
                languageTag = "en"
            )
        )

        Log.i(
            TAG,
            "Extractive insights title=${output.insights.suggestedTitle} modelBytes=${output.modelSizeBytes} " +
                "loadMs=${output.loadTimeMs} generationMs=${output.generationTimeMs} memoryKb=${output.memoryObservationKb}"
        )
        assertEquals(OfflineAiRuntimeVersions.HEURISTIC_INSIGHTS_MODEL_ID, output.modelId)
        assertEquals(0L, output.modelSizeBytes)
        assertTrue("Expected non-empty generated title", output.insights.suggestedTitle.isNotBlank())
        assertTrue("Expected non-empty generated summary", output.insights.summary.isNotBlank())
        assertTrue("Expected evidence IDs from real output", output.insights.evidenceSegmentIds.isNotEmpty())
        val validIds = transcript.segments.map { it.id }.toSet()
        assertTrue("Evidence must refer only to transcript segments", output.insights.evidenceSegmentIds.all { it in validIds })
        assertTrue("Expected extracted decisions", output.insights.decisions.isNotEmpty())
        assertTrue("Expected extracted action items", output.insights.actionItems.isNotEmpty())
        assertTrue("Expected extracted open questions", output.insights.openQuestions.isNotEmpty())
        assertTrue("Expected recorded generation time", output.generationTimeMs >= 0L)
        assertTrue("Expected memory observation", requireNotNull(output.memoryObservationKb) > 0L)
    }

    private fun writeSyntheticAacM4a(target: File) {
        val sampleRate = 44_100
        val channelCount = 1
        val durationSeconds = 1
        val totalFrames = sampleRate * durationSeconds
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, 64_000)
        }
        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        val muxer = MediaMuxer(target.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var trackIndex = -1
        var muxerStarted = false
        var submittedFrames = 0
        val bufferInfo = MediaCodec.BufferInfo()

        try {
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()
            var inputDone = false
            var outputDone = false
            while (!outputDone) {
                if (!inputDone) {
                    val inputIndex = codec.dequeueInputBuffer(BUFFER_TIMEOUT_US)
                    if (inputIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputIndex)
                            ?: throw IllegalStateException("Missing AAC encoder input")
                        inputBuffer.clear()
                        val framesThisBuffer = minOf(inputBuffer.capacity() / 2, totalFrames - submittedFrames)
                        if (framesThisBuffer <= 0) {
                            codec.queueInputBuffer(
                                inputIndex,
                                0,
                                0,
                                (submittedFrames * 1_000_000L) / sampleRate,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            inputDone = true
                        } else {
                            repeat(framesThisBuffer) { offset ->
                                val frame = submittedFrames + offset
                                val sample = (sin((2.0 * PI * 440.0 * frame) / sampleRate) * Short.MAX_VALUE * 0.35)
                                    .roundToInt()
                                    .toShort()
                                inputBuffer.put((sample.toInt() and 0xff).toByte())
                                inputBuffer.put(((sample.toInt() shr 8) and 0xff).toByte())
                            }
                            codec.queueInputBuffer(
                                inputIndex,
                                0,
                                framesThisBuffer * 2,
                                (submittedFrames * 1_000_000L) / sampleRate,
                                0
                            )
                            submittedFrames += framesThisBuffer
                        }
                    }
                }

                when (val outputIndex = codec.dequeueOutputBuffer(bufferInfo, BUFFER_TIMEOUT_US)) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        trackIndex = muxer.addTrack(codec.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    }

                    else -> if (outputIndex >= 0) {
                        if (!muxerStarted) {
                            throw IllegalStateException("AAC muxer was not started")
                        }
                        val outputBuffer = codec.getOutputBuffer(outputIndex)
                            ?: throw IllegalStateException("Missing AAC encoder output")
                        if (bufferInfo.size > 0) {
                            outputBuffer.position(bufferInfo.offset)
                            outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(trackIndex, outputBuffer, bufferInfo)
                            outputBuffer.clear()
                        }
                        codec.releaseOutputBuffer(outputIndex, false)
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            outputDone = true
                        }
                    }
                }
            }
        } finally {
            runCatching { codec.stop() }
            codec.release()
            if (muxerStarted) {
                runCatching { muxer.stop() }
            }
            muxer.release()
        }
    }

    private companion object {
        const val TAG = "LoomoraSherpaSmoke"
        const val MODEL_PACK_NAME = "loomora-whisper-tiny-int8.modelpack.zip"
        const val FIXTURE_NAME = "loomora-whisper-test-0.wav"
        const val DIARIZATION_MODEL_PACK_NAME = "sherpa-onnx-pyannote-3-0-3dspeaker-int8.modelpack.zip"
        const val TWO_SPEAKER_FIXTURE_NAME = "two-speakers-en.wav"
        const val BUFFER_TIMEOUT_US = 10_000L
    }

    private fun assertPushedSmokeInput(file: File) {
        assertTrue(
            "Missing smoke input at ${file.absolutePath}; push it with adb before running this device smoke test.",
            file.exists() && file.length() > 0L
        )
    }
}
