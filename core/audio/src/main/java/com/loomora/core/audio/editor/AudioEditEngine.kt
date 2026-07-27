package com.loomora.core.audio.editor

import com.loomora.core.model.KeepRange
import java.io.File

interface AudioEditEngine {
    suspend fun export(
        sourceFile: File,
        keepRanges: List<KeepRange>,
        outputFile: File,
        onProgress: (Int) -> Unit
    )
}
