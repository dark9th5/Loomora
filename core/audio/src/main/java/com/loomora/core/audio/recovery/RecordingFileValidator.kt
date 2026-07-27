package com.loomora.core.audio.recovery

import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class RecordingFileValidation(
    val isPlayable: Boolean,
    val durationMs: Long,
    val mimeType: String?,
    val sampleRate: Int?,
    val channels: Int?
)

interface RecordingFileValidator {
    fun validate(file: File): RecordingFileValidation
}

@Singleton
class AndroidRecordingFileValidator @Inject constructor() : RecordingFileValidator {
    override fun validate(file: File): RecordingFileValidation {
        if (!file.exists() || !file.isFile || file.length() <= 0L) {
            return RecordingFileValidation(
                isPlayable = false,
                durationMs = 0L,
                mimeType = null,
                sampleRate = null,
                channels = null
            )
        }

        return runCatching {
            val metadata = readMetadata(file)
            val track = readAudioTrack(file)
            RecordingFileValidation(
                isPlayable = metadata.durationMs > 0L && track.mimeType?.startsWith("audio/") == true,
                durationMs = metadata.durationMs,
                mimeType = track.mimeType ?: metadata.mimeType,
                sampleRate = track.sampleRate,
                channels = track.channels
            )
        }.getOrElse {
            RecordingFileValidation(
                isPlayable = false,
                durationMs = 0L,
                mimeType = null,
                sampleRate = null,
                channels = null
            )
        }
    }

    private fun readMetadata(file: File): MetadataProbe {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val duration = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?: 0L
            val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            MetadataProbe(durationMs = duration, mimeType = mimeType)
        } finally {
            retriever.release()
        }
    }

    private fun readAudioTrack(file: File): AudioTrackProbe {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(file.absolutePath)
            var index = 0
            while (index < extractor.trackCount) {
                val format = extractor.getTrackFormat(index)
                val mimeType = format.getString(MediaFormat.KEY_MIME)
                if (mimeType?.startsWith("audio/") == true) {
                    return AudioTrackProbe(
                        mimeType = mimeType,
                        sampleRate = format.optionalInteger(MediaFormat.KEY_SAMPLE_RATE),
                        channels = format.optionalInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    )
                }
                index += 1
            }
            AudioTrackProbe(mimeType = null, sampleRate = null, channels = null)
        } finally {
            extractor.release()
        }
    }

    private fun MediaFormat.optionalInteger(key: String): Int? {
        return if (containsKey(key)) getInteger(key) else null
    }
}

private data class MetadataProbe(
    val durationMs: Long,
    val mimeType: String?
)

private data class AudioTrackProbe(
    val mimeType: String?,
    val sampleRate: Int?,
    val channels: Int?
)
