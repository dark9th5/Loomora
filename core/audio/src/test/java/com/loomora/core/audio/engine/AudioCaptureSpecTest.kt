package com.loomora.core.audio.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioCaptureSpecTest {
    @Test
    fun rnnoiseFrameMatchesTenMillisecondsAt48k() {
        assertEquals(48_000, AudioCaptureSpec.SAMPLE_RATE_HZ)
        assertEquals(480, AudioCaptureSpec.RNNOISE_FRAME_SAMPLES)
        assertEquals(1, AudioCaptureSpec.CHANNEL_COUNT)
        assertEquals(128_000, AudioCaptureSpec.AAC_BIT_RATE)
    }
}
