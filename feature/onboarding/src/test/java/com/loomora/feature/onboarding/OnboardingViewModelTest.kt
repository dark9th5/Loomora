package com.loomora.feature.onboarding

import org.junit.Assert.assertFalse
import org.junit.Test

class OnboardingViewModelTest {

    @Test
    fun onboardingUiState_defaultIsNotCompleted() {
        val state = OnboardingUiState()
        assertFalse(state.isCompleted)
    }
}
