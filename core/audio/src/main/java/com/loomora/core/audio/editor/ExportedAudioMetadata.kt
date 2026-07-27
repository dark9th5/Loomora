package com.loomora.core.audio.editor

import java.io.File

data class ExportedAudioMetadata(
    val durationMs: Long,
    val mimeType: String,
    val sampleRate: Int?,
    val channelCount: Int?,
    val bitrate: Int?,
    val sizeBytes: Long
)

interface AudioOutputMetadataReader {
    fun read(file: File): ExportedAudioMetadata?
}
