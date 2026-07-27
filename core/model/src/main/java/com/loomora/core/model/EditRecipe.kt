package com.loomora.core.model

import kotlin.math.max

sealed interface EditOperation {
    data class Trim(val startMs: Long, val endMs: Long) : EditOperation
    data class DeleteRange(val startMs: Long, val endMs: Long) : EditOperation
    data class Split(val splitTimeMs: Long) : EditOperation
}

data class KeepRange(
    val startMs: Long,
    val endMs: Long
) {
    init {
        require(endMs >= startMs) { "KeepRange end must be >= start" }
    }

    val durationMs: Long
        get() = (endMs - startMs).coerceAtLeast(0L)
}

enum class EditRecipeIssue {
    INVALID_RANGE,
    EMPTY_RESULT,
    UNSUPPORTED_OPERATION
}

sealed interface EditRecipeValidation {
    data class Valid(
        val keepRanges: List<KeepRange>,
        val outputDurationMs: Long
    ) : EditRecipeValidation

    data class Invalid(
        val issue: EditRecipeIssue
    ) : EditRecipeValidation
}

data class EditRecipe(
    val originalRecordingId: String,
    val sourceFingerprint: String? = null,
    val recipeRevision: Long = 0L,
    val operations: List<EditOperation> = emptyList(),
    val isSpeechClarityEnabled: Boolean = false
) {
    fun validate(originalDurationMs: Long): EditRecipeValidation {
        if (originalDurationMs <= 0L) {
            return EditRecipeValidation.Invalid(EditRecipeIssue.EMPTY_RESULT)
        }

        var keepRanges = mutableListOf(KeepRange(0L, originalDurationMs))

        operations.forEach { op ->
            when (op) {
                is EditOperation.Trim -> {
                    if (!isValidRange(op.startMs, op.endMs, originalDurationMs)) {
                        return EditRecipeValidation.Invalid(EditRecipeIssue.INVALID_RANGE)
                    }
                    keepRanges = keepRanges.mapNotNull { current ->
                        val start = max(current.startMs, op.startMs)
                        val end = minOf(current.endMs, op.endMs)
                        if (end <= start) {
                            null
                        } else {
                            KeepRange(start, end)
                        }
                    }.toMutableList()
                }

                is EditOperation.DeleteRange -> {
                    if (!isValidRange(op.startMs, op.endMs, originalDurationMs)) {
                        return EditRecipeValidation.Invalid(EditRecipeIssue.INVALID_RANGE)
                    }
                    keepRanges = keepRanges.flatMap { current ->
                        subtractRange(current, KeepRange(op.startMs, op.endMs))
                    }.toMutableList()
                }

                is EditOperation.Split -> {
                    return EditRecipeValidation.Invalid(EditRecipeIssue.UNSUPPORTED_OPERATION)
                }
            }
        }

        val normalizedRanges = mergeAdjacentRanges(keepRanges)
        val outputDuration = normalizedRanges.sumOf { it.durationMs }
        if (outputDuration <= 0L || normalizedRanges.isEmpty()) {
            return EditRecipeValidation.Invalid(EditRecipeIssue.EMPTY_RESULT)
        }

        return EditRecipeValidation.Valid(
            keepRanges = normalizedRanges,
            outputDurationMs = outputDuration
        )
    }

    fun calculateNewDurationMs(originalDurationMs: Long): Long {
        return when (val validation = validate(originalDurationMs)) {
            is EditRecipeValidation.Valid -> validation.outputDurationMs
            is EditRecipeValidation.Invalid -> 0L
        }
    }

    private fun isValidRange(startMs: Long, endMs: Long, originalDurationMs: Long): Boolean {
        return startMs >= 0L && endMs > startMs && endMs <= originalDurationMs
    }

    private fun subtractRange(source: KeepRange, remove: KeepRange): List<KeepRange> {
        if (remove.endMs <= source.startMs || remove.startMs >= source.endMs) {
            return listOf(source)
        }

        val result = mutableListOf<KeepRange>()
        if (remove.startMs > source.startMs) {
            result += KeepRange(source.startMs, minOf(remove.startMs, source.endMs))
        }
        if (remove.endMs < source.endMs) {
            result += KeepRange(max(remove.endMs, source.startMs), source.endMs)
        }
        return result.filter { it.durationMs > 0L }
    }

    private fun mergeAdjacentRanges(ranges: List<KeepRange>): List<KeepRange> {
        if (ranges.isEmpty()) return emptyList()
        val sorted = ranges.sortedBy { it.startMs }
        val merged = mutableListOf(sorted.first())
        sorted.drop(1).forEach { next ->
            val current = merged.removeAt(merged.lastIndex)
            if (next.startMs <= current.endMs) {
                merged += KeepRange(current.startMs, max(current.endMs, next.endMs))
            } else {
                merged += current
                merged += next
            }
        }
        return merged
    }
}
