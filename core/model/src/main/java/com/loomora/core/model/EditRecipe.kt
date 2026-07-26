package com.loomora.core.model

sealed interface EditOperation {
    data class Trim(val startMs: Long, val endMs: Long) : EditOperation
    data class DeleteRange(val startMs: Long, val endMs: Long) : EditOperation
    data class Split(val splitTimeMs: Long) : EditOperation
}

data class EditRecipe(
    val originalRecordingId: String,
    val operations: List<EditOperation> = emptyList(),
    val isSpeechClarityEnabled: Boolean = false
) {
    fun calculateNewDurationMs(originalDurationMs: Long): Long {
        var currentDuration = originalDurationMs
        operations.forEach { op ->
            when (op) {
                is EditOperation.Trim -> {
                    currentDuration = (op.endMs - op.startMs).coerceAtLeast(0L)
                }
                is EditOperation.DeleteRange -> {
                    val deletedDuration = (op.endMs - op.startMs).coerceAtLeast(0L)
                    currentDuration = (currentDuration - deletedDuration).coerceAtLeast(0L)
                }
                is EditOperation.Split -> {}
            }
        }
        return currentDuration
    }
}
