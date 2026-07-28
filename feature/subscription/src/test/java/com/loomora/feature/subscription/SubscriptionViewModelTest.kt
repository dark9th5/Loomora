package com.loomora.feature.subscription

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.loomora.core.datastore.EntitlementManager
import com.loomora.core.datastore.EntitlementRepository
import com.loomora.core.datastore.OfflineLicenseVerifier
import com.loomora.core.model.EntitlementStatus
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SubscriptionViewModelTest {

    @Test
    fun initialSubscriptionUiState_displaysFreeTrial() {
        val manager = EntitlementManager(repository())
        val viewModel = SubscriptionViewModel(manager)
        val state = viewModel.uiState.value
        assertTrue(state.status is EntitlementStatus.FreeTrial)
        assertEquals(3, (state.status as EntitlementStatus.FreeTrial).remainingUses)
    }

    private fun repository(): EntitlementRepository {
        val dataStore = PreferenceDataStoreFactory.create {
            File.createTempFile("loomora-subscription", ".preferences_pb").apply { deleteOnExit() }
        }
        return EntitlementRepository(
            dataStore = dataStore,
            verifier = OfflineLicenseVerifier(Json { encodeDefaults = true }, emptyMap())
        )
    }
}
