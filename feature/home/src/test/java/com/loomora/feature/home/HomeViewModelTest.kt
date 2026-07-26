package com.loomora.feature.home

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeViewModelTest {

    @Test
    fun homeUiState_defaultValuesAreValid() {
        val state = HomeUiState()
        assertEquals(3, state.remainingTrialUses)
    }
}
