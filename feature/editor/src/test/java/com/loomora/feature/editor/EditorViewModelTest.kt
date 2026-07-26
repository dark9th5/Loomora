package com.loomora.feature.editor

import com.loomora.core.model.EditOperation
import com.loomora.core.model.EditRecipe
import org.junit.Assert.assertEquals
import org.junit.Test

class EditorViewModelTest {

    @Test
    fun editorUiState_initialStateIsValid() {
        val state = EditorUiState(
            recording = null,
            recipe = EditRecipe("rec-1"),
            canUndo = false,
            canRedo = false
        )
        assertEquals(false, state.canUndo)
        assertEquals(false, state.canRedo)
        assertEquals(false, state.isExporting)
    }
}
