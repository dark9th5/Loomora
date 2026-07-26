package com.loomora.core.datastore

import com.loomora.core.model.Capability
import com.loomora.core.model.EntitlementStatus
import com.loomora.core.model.LicenseValidationResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EntitlementManagerTest {

    @Test
    fun localRecordingCapability_isAlwaysGranted() {
        val manager = EntitlementManager()
        assertTrue(manager.isCapabilityGranted(Capability.LOCAL_RECORDING))
    }

    @Test
    fun initialTrialStatus_hasThreeUses() {
        val manager = EntitlementManager()
        val status = manager.getEntitlementStatus()
        assertTrue(status is EntitlementStatus.FreeTrial)
        assertEquals(3, (status as EntitlementStatus.FreeTrial).remainingUses)
    }

    @Test
    fun validProKey_activatesPro() = runTest {
        val manager = EntitlementManager()
        val result = manager.activateLicenseKey("LM-PRO-KEY-1234")
        assertTrue(result is LicenseValidationResult.Valid)
        assertTrue(manager.isCapabilityGranted(Capability.AI_TRANSCRIPTION))
    }
}
