package com.loomora.core.audio.waveform

import java.io.File

interface AudioWaveformDecoder {
    fun canDecode(sourceFile: File): Boolean

    suspend fun decode(
        sourceFile: File,
        resolution: Int
    ): Result<PersistedWaveform>
}
