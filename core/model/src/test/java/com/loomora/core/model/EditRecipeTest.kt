package com.loomora.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EditRecipeTest {

    @Test
    fun calculateNewDurationMs_withTrim_returnsCorrectDuration() {
        val recipe = EditRecipe(
            originalRecordingId = "rec-1",
            operations = listOf(EditOperation.Trim(startMs = 5_000L, endMs = 25_000L))
        )

        assertEquals(20_000L, recipe.calculateNewDurationMs(originalDurationMs = 60_000L))
    }

    @Test
    fun calculateNewDurationMs_withDeleteRange_returnsSubtractedDuration() {
        val recipe = EditRecipe(
            originalRecordingId = "rec-1",
            operations = listOf(EditOperation.DeleteRange(startMs = 10_000L, endMs = 20_000L))
        )

        assertEquals(50_000L, recipe.calculateNewDurationMs(originalDurationMs = 60_000L))
    }

    @Test
    fun validate_withDeleteMiddle_returnsMultipleKeepRanges() {
        val recipe = EditRecipe(
            originalRecordingId = "rec-1",
            operations = listOf(EditOperation.DeleteRange(startMs = 10_000L, endMs = 20_000L))
        )

        val validation = recipe.validate(60_000L) as EditRecipeValidation.Valid
        assertEquals(
            listOf(KeepRange(0L, 10_000L), KeepRange(20_000L, 60_000L)),
            validation.keepRanges
        )
    }

    @Test
    fun validate_withTrimAndDeleteNormalizesFinalKeepRanges() {
        val recipe = EditRecipe(
            originalRecordingId = "rec-1",
            operations = listOf(
                EditOperation.Trim(startMs = 5_000L, endMs = 50_000L),
                EditOperation.DeleteRange(startMs = 15_000L, endMs = 20_000L)
            )
        )

        val validation = recipe.validate(60_000L) as EditRecipeValidation.Valid
        assertEquals(
            listOf(KeepRange(5_000L, 15_000L), KeepRange(20_000L, 50_000L)),
            validation.keepRanges
        )
        assertEquals(40_000L, validation.outputDurationMs)
    }

    @Test
    fun validate_withInvalidRange_returnsInvalid() {
        val recipe = EditRecipe(
            originalRecordingId = "rec-1",
            operations = listOf(EditOperation.Trim(startMs = 30_000L, endMs = 5_000L))
        )

        val validation = recipe.validate(60_000L)
        assertTrue(validation is EditRecipeValidation.Invalid)
        assertEquals(EditRecipeIssue.INVALID_RANGE, (validation as EditRecipeValidation.Invalid).issue)
    }

    @Test
    fun validate_withUnsupportedSplit_returnsUnsupportedOperation() {
        val recipe = EditRecipe(
            originalRecordingId = "rec-1",
            operations = listOf(EditOperation.Split(splitTimeMs = 10_000L))
        )

        val validation = recipe.validate(60_000L)
        assertTrue(validation is EditRecipeValidation.Invalid)
        assertEquals(EditRecipeIssue.UNSUPPORTED_OPERATION, (validation as EditRecipeValidation.Invalid).issue)
    }

    @Test
    fun validate_withFullDeleteRejectsEmptyResult() {
        val recipe = EditRecipe(
            originalRecordingId = "rec-1",
            operations = listOf(EditOperation.DeleteRange(startMs = 0L, endMs = 60_000L))
        )

        val validation = recipe.validate(60_000L)
        assertTrue(validation is EditRecipeValidation.Invalid)
        assertEquals(EditRecipeIssue.EMPTY_RESULT, (validation as EditRecipeValidation.Invalid).issue)
    }
}
