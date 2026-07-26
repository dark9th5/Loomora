package com.loomora.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class EditRecipeTest {

    @Test
    fun calculateNewDurationMs_withTrim_returnsCorrectDuration() {
        val recipe = EditRecipe(
            originalRecordingId = "rec-1",
            operations = listOf(EditOperation.Trim(startMs = 5000L, endMs = 25000L))
        )
        val newDuration = recipe.calculateNewDurationMs(originalDurationMs = 60000L)
        assertEquals(20000L, newDuration)
    }

    @Test
    fun calculateNewDurationMs_withDeleteRange_returnsSubtractedDuration() {
        val recipe = EditRecipe(
            originalRecordingId = "rec-1",
            operations = listOf(EditOperation.DeleteRange(startMs = 10000L, endMs = 20000L))
        )
        val newDuration = recipe.calculateNewDurationMs(originalDurationMs = 60000L)
        assertEquals(50000L, newDuration)
    }
}
