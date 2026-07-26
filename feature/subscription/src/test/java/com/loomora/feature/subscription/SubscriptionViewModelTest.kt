package com.loomora.feature.subscription

import com.loomora.core.datastore.EntitlementManager
import com.loomora.core.model.EntitlementStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionViewModelTest {

    @Test
    fun initialSubscriptionUiState_displaysFreeTrial() {
        val manager = EntitlementManager()
        val viewModel = SubscriptionViewModel(manager)
        val state = viewModel.uiState.value
        assertTrue(state.status is EntitlementStatus.FreeTrial)
        assertEquals(3, (state.status as EntitlementStatus.FreeTrial).remainingUses)
    }
}
