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
        const val BUFFER_TIMEOUT_US = 10_000L
    }

    private fun assertPushedSmokeInput(file: File) {
        assertTrue(
            "Missing smoke input at ${file.absolutePath}; push it with adb before running this device smoke test.",
            file.exists() && file.length() > 0L
        )
    }
}
