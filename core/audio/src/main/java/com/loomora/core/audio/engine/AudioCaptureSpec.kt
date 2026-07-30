package com.loomora.core.audio.engine

/** Single source of truth for the current MediaRecorder/RNNoise recording format. */
object AudioCaptureSpec {
    const val SAMPLE_RATE_HZ = 48_000
    const val CHANNEL_COUNT = 1
    const val AAC_BIT_RATE = 128_000
    const val RNNOISE_FRAME_SAMPLES = 480
    const val MIME_TYPE = "audio/aac"
}
