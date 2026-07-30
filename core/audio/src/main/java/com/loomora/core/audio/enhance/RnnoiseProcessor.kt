package com.loomora.core.audio.enhance

internal class RnnoiseProcessor : AutoCloseable {
    private var handle: Long = nativeCreate()

    init {
        check(handle != 0L) { "Unable to create RNNoise state" }
    }

    fun process(samples: ShortArray, strength: Float) {
        require(samples.size == FRAME_SIZE)
        check(handle != 0L) { "RNNoise processor is closed" }
        nativeProcess(handle, samples, strength)
    }

    override fun close() {
        if (handle != 0L) {
            nativeDestroy(handle)
            handle = 0L
        }
    }

    private external fun nativeCreate(): Long
    private external fun nativeProcess(handle: Long, samples: ShortArray, strength: Float)
    private external fun nativeDestroy(handle: Long)

    companion object {
        const val FRAME_SIZE = 480

        init {
            System.loadLibrary("loomora-rnnoise")
        }
    }
}
