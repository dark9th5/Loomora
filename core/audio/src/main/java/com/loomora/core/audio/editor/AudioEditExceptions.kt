package com.loomora.core.audio.editor

sealed class AudioEditException(message: String) : Exception(message) {
    data object SourceMissing : AudioEditException("Source audio file is missing.")
    data object InvalidRecipe : AudioEditException("The current edit recipe is invalid.")
    data object EmptyResult : AudioEditException("The current edit recipe would export an empty file.")
    data object UnsupportedOperation : AudioEditException("This edit operation is not supported on the current export path.")
    data object OutputValidationFailed : AudioEditException("The exported audio output could not be validated.")
    data object ExportCancelled : AudioEditException("Audio export was cancelled.")
}
