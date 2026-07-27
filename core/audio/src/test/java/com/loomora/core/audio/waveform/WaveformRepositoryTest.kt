package com.loomora.core.audio.waveform

import androidx.test.core.app.ApplicationProvider
import com.loomora.core.model.Recording
import com.loomora.core.model.RecordingStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class WaveformRepositoryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var cacheStore: WaveformCacheStore

    @Before
    fun setUp() {
        cacheStore = WaveformCacheStore(ApplicationProvider.getApplicationContext())
        File(ApplicationProvider.getApplicationContext<android.content.Context>().filesDir, "waveforms")
            .deleteRecursively()
    }

    @Test
    fun silence_fileProducesZeroBins() = runTest {
        val source = WaveformTestFixtures.silence(temporaryFolder.newFile("silence.wav"), durationMs = 500)
        val decoder = WavAudioWaveformDecoder()

        val result = decoder.decode(source, resolution = 32).getOrThrow()

        assertEquals(32, result.bins.size)
        assertTrue(result.bins.all { it == 0f })
    }

    @Test
    fun constantTone_fileProducesNormalizedBins() = runTest {
        val source = WaveformTestFixtures.constantTone(temporaryFolder.newFile("tone.wav"), durationMs = 500)
        val decoder = WavAudioWaveformDecoder()

        val result = decoder.decode(source, resolution = 32).getOrThrow()

        assertEquals(32, result.bins.size)
        assertTrue(result.bins.all { it in 0.95f..1f })
    }

    @Test
    fun shortFile_keepsRequestedResolution() = runTest {
        val source = WaveformTestFixtures.sineTone(temporaryFolder.newFile("short.wav"), durationMs = 80)
        val decoder = WavAudioWaveformDecoder()

        val result = decoder.decode(source, resolution = 20).getOrThrow()

        assertEquals(20, result.bins.size)
        assertTrue(result.durationMs > 0L)
    }

    @Test
    fun longFile_streamsToFixedBinCount() = runTest {
        val source = WaveformTestFixtures.sineTone(temporaryFolder.newFile("long.wav"), durationMs = 30_000)
        val decoder = WavAudioWaveformDecoder()

        val result = decoder.decode(source, resolution = 128).getOrThrow()

        assertEquals(128, result.bins.size)
        assertTrue(result.durationMs in 29_995L..30_005L)
    }

    @Test
    fun corruptInput_returnsFailure() = runTest {
        val source = temporaryFolder.newFile("broken.wav").apply {
            writeText("not-audio")
        }
        val decoder = WavAudioWaveformDecoder()

        val result = decoder.decode(source, resolution = 32)

        assertTrue(result.isFailure)
    }

    @Test
    fun cacheHit_skipsDecoderWork() = runTest {
        val source = WaveformTestFixtures.sineTone(temporaryFolder.newFile("cache.wav"), durationMs = 500)
        val decoder = object : WavAudioWaveformDecoder() {
            var calls = 0
            override suspend fun decode(sourceFile: File, resolution: Int): Result<PersistedWaveform> {
                calls += 1
                return super.decode(sourceFile, resolution)
            }
        }
        val repository = WaveformRepository(
            cacheStore = cacheStore,
            wavDecoder = decoder,
            androidDecoder = object : AndroidAudioWaveformDecoder() {
                override fun canDecode(sourceFile: File): Boolean = false
            }
        )
        val recording = recording(source)

        repository.loadWaveform(recording, resolution = 64).toList()
        repository.loadWaveform(recording, resolution = 64).toList()

        assertEquals(1, decoder.calls)
    }

    @Test
    fun cacheInvalidation_recomputesAfterSourceChanges() = runTest {
        val source = WaveformTestFixtures.sineTone(temporaryFolder.newFile("invalidate.wav"), durationMs = 500)
        val decoder = object : WavAudioWaveformDecoder() {
            var calls = 0
            override suspend fun decode(sourceFile: File, resolution: Int): Result<PersistedWaveform> {
                calls += 1
                return super.decode(sourceFile, resolution)
            }
        }
        val repository = WaveformRepository(
            cacheStore = cacheStore,
            wavDecoder = decoder,
            androidDecoder = object : AndroidAudioWaveformDecoder() {
                override fun canDecode(sourceFile: File): Boolean = false
            }
        )
        val recording = recording(source)

        repository.loadWaveform(recording, resolution = 64).toList()
        source.appendBytes(byteArrayOf(0, 0, 0, 0))
        repository.loadWaveform(recording, resolution = 64).toList()

        assertEquals(2, decoder.calls)
    }

    @Test
    fun timestampToBinMapping_roundTripsWithinBinTolerance() {
        val durationMs = 10_000L
        val bins = 100
        val positionMs = 4_550L

        val bin = WaveformTimelineMapper.positionMsToBinIndex(positionMs, durationMs, bins)
        val mappedMs = WaveformTimelineMapper.binIndexToPositionMs(bin, durationMs, bins)

        assertTrue(mappedMs in 4_400L..4_600L)
    }

    private fun recording(source: File) = Recording(
        id = "rec-1",
        title = "Fixture",
        createdAt = 1_000L,
        updatedAt = 1_000L,
        durationMs = 500L,
        status = RecordingStatus.SAVED,
        originalFileUri = "file://${source.absolutePath}"
    )
}
