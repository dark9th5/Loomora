package com.loomora.core.audio.recovery

object RecordingRecoveryState {
    const val NORMAL = "NORMAL"
    const val RECOVERED = "RECOVERED"
    const val MISSING_FILE = "MISSING_FILE"
    const val ZERO_BYTE_FILE = "ZERO_BYTE_FILE"
    const val CORRUPT_FILE = "CORRUPT_FILE"
    const val ORPHAN_FILE = "ORPHAN_FILE"
}

object RecordingRecoveryRetentionPolicy {
    const val TEMP_FILE_EXTENSION = "tmp"
    const val TEMP_FILE_RETENTION_MS = 7L * 24L * 60L * 60L * 1000L
}
