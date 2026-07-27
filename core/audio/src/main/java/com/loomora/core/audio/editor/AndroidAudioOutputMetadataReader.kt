package com.loomora.core.audio.editor

import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidAudioOutputMetadataReader @Inject constructor() : AudioOutputMetadataReader {
    override fun read(file: File): ExportedAudioMetadata? {
        if (!file.exists() || !file.isFile || file.length() == 0L) {
            return null
        }

        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(file.absolutePath)
            val audioFormat = (0 until extractor.trackCount)
                .map(extractor::getTrackFormat)
                .firstOrNull { format ->
                    format.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
                }
                ?: return null

            val durationUs = if (audioFormat.containsKey(MediaFormat.KEY_DURATION)) {
                audioFormat.getLong(MediaFormat.KEY_DURATION)
            } else {
                0L
            }

            ExportedAudioMetadata(
                durationMs = (durationUs / 1000L).coerceAtLeast(0L),
                mimeType = audioFormat.getString(MediaFormat.KEY_MIME) ?: "audio/mp4a-latm",
                sampleRate = audioFormat.getIntegerOrNull(MediaFormat.KEY_SAMPLE_RATE),
                channelCount = audioFormat.getIntegerOrNull(MediaFormat.KEY_CHANNEL_COUNT),
                bitrate = audioFormat.getIntegerOrNull(MediaFormat.KEY_BIT_RATE),
                sizeBytes = file.length()
            )
        } catch (_: Exception) {
            null
        } finally {
            extractor.release()
        }
    }

    private fun MediaFormat.getIntegerOrNull(key: String): Int? {
        return if (containsKey(key)) {
            getInteger(key)
        } else {
            null
        }
    }
}
